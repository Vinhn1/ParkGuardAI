package com.example.parkguardai.data.datasource;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Đọc ảnh đúng chiều và tạo JPEG crop; file ảnh gốc không bị sửa. */
public final class GuardImageProcessor {
    public Bitmap loadPreview(File original) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(original.getAbsolutePath(), bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new IOException("Image unreadable");
        int sample = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / sample > 4096) sample *= 2;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        Bitmap bitmap = BitmapFactory.decodeFile(original.getAbsolutePath(), options);
        if (bitmap == null) throw new IOException("Image unreadable");
        int orientation = new ExifInterface(original.getAbsolutePath())
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        Matrix transform = new Matrix();
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90) transform.postRotate(90);
        else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) transform.postRotate(180);
        else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) transform.postRotate(270);
        if (transform.isIdentity()) return bitmap;
        Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                transform, true);
        bitmap.recycle();
        return rotated;
    }

    public void saveCrop(Bitmap preview, RectF normalizedSelection, File output) throws IOException {
        float left = Math.max(0, Math.min(1, normalizedSelection.left));
        float top = Math.max(0, Math.min(1, normalizedSelection.top));
        float right = Math.max(0, Math.min(1, normalizedSelection.right));
        float bottom = Math.max(0, Math.min(1, normalizedSelection.bottom));
        int x = Math.round(left * preview.getWidth());
        int y = Math.round(top * preview.getHeight());
        int width = Math.round((right - left) * preview.getWidth());
        int height = Math.round((bottom - top) * preview.getHeight());
        if (width < 80 || height < 40 || x + width > preview.getWidth()
                || y + height > preview.getHeight()) throw new IOException("Crop too small");
        Bitmap crop = Bitmap.createBitmap(preview, x, y, width, height);
        try (FileOutputStream stream = new FileOutputStream(output)) {
            if (!crop.compress(Bitmap.CompressFormat.JPEG, 92, stream)) {
                throw new IOException("Crop write failed");
            }
            stream.getFD().sync();
        } finally {
            crop.recycle();
        }
    }
}
