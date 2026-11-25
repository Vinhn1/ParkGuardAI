package com.example.parkguardai.data.datasource;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Chỉ tải ảnh chứng cứ từ đúng CDN/cloud đã cấu hình và giới hạn dung lượng. */
public final class CloudinaryImageDataSource {
    public Bitmap loadPreview(String secureUrl) throws IOException {
        Uri uri = Uri.parse(secureUrl);
        if (secureUrl.length() > 2048 || !"https".equals(uri.getScheme())
                || !"res.cloudinary.com".equals(uri.getHost())
                || uri.getPath() == null
                || !uri.getPath().startsWith("/ds1g5ryn1/image/upload/")
                || uri.getUserInfo() != null || uri.getPort() != -1) {
            throw new IOException("Invalid evidence URL");
        }
        HttpURLConnection connection = (HttpURLConnection) new URL(secureUrl).openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(15_000);
        try {
            String contentType = connection.getContentType();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK
                    || connection.getContentLengthLong() > 12L * 1024L * 1024L
                    || contentType == null || !contentType.startsWith("image/")) {
                throw new IOException("Image download failed");
            }
            byte[] bytes;
            try (InputStream input = connection.getInputStream();
                 ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                byte[] chunk = new byte[8192];
                int count;
                while ((count = input.read(chunk)) != -1) {
                    if (buffer.size() + count > 12L * 1024L * 1024L) {
                        throw new IOException("Image exceeds limit");
                    }
                    buffer.write(chunk, 0, count);
                }
                bytes = buffer.toByteArray();
            }
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
            if (bounds.outWidth < 1 || bounds.outHeight < 1
                    || bounds.outWidth > 10000 || bounds.outHeight > 10000) {
                throw new IOException("Invalid image dimensions");
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            while (bounds.outWidth / options.inSampleSize > 1600
                    || bounds.outHeight / options.inSampleSize > 1600) {
                options.inSampleSize *= 2;
            }
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
            if (bitmap == null) throw new IOException("Image decode failed");
            return bitmap;
        } finally {
            connection.disconnect();
        }
    }
}
