package com.example.parkguardai.data.datasource;

import android.net.Uri;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.parkguardai.data.model.EvidenceImage;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** Tải JPEG/PNG lên preset unsigned; không chứa API key hoặc secret. */
public final class CloudinaryDataSource {
    private static final String PRESET = "parkguard_violation_upload";
    private static final long MAX_IMAGE_BYTES = 12L * 1024L * 1024L;

    public Task<EvidenceImage> uploadImage(File file, String imageType, boolean isPrimary) {
        TaskCompletionSource<EvidenceImage> completion = new TaskCompletionSource<>();
        try {
            validateImage(file);
        } catch (IOException | IllegalArgumentException failure) {
            completion.setException(failure);
            return completion.getTask();
        }
        AtomicBoolean settled = new AtomicBoolean();
        try {
            MediaManager.get().upload(file.getAbsolutePath()).unsigned(PRESET)
                    .callback(new UploadCallback() {
                        @Override public void onStart(String requestId) { }
                        @Override public void onProgress(String requestId, long bytes, long totalBytes) { }
                        @Override public void onReschedule(String requestId, ErrorInfo error) { }
                        @Override public void onError(String requestId, ErrorInfo error) {
                            if (settled.compareAndSet(false, true)) {
                                completion.setException(new IOException("Cloudinary upload failed"));
                            }
                        }
                        @Override public void onSuccess(String requestId, Map resultData) {
                            if (!settled.compareAndSet(false, true)) return;
                            try {
                                completion.setResult(parseReceipt(resultData, imageType, isPrimary));
                            } catch (IllegalArgumentException failure) {
                                completion.setException(failure);
                            }
                        }
                    }).dispatch();
        } catch (RuntimeException failure) {
            if (settled.compareAndSet(false, true)) completion.setException(failure);
        }
        return completion.getTask();
    }

    private EvidenceImage parseReceipt(Map<?, ?> result, String imageType, boolean isPrimary) {
        Object rawUrl = result.get("secure_url");
        Object rawId = result.get("public_id");
        Object resourceType = result.get("resource_type");
        if (!(rawUrl instanceof String) || !(rawId instanceof String)
                || !"image".equals(resourceType)) {
            throw new IllegalArgumentException("Invalid image upload receipt");
        }
        String url = (String) rawUrl;
        String publicId = (String) rawId;
        Uri uri = Uri.parse(url);
        if (!"https".equals(uri.getScheme()) || !"res.cloudinary.com".equals(uri.getHost())
                || uri.getPath() == null || !uri.getPath().startsWith("/ds1g5ryn1/image/upload/")
                || url.length() > 2048 || publicId.isEmpty() || publicId.length() > 255) {
            throw new IllegalArgumentException("Invalid image upload receipt");
        }
        Date uploadedAt = new Date();
        Object rawCreatedAt = result.get("created_at");
        if (rawCreatedAt instanceof String) {
            try { uploadedAt = Date.from(Instant.parse((String) rawCreatedAt)); }
            catch (RuntimeException ignored) { /* Giữ thời điểm callback khi thiếu ngày hợp lệ. */ }
        }
        return new EvidenceImage(url, publicId, uploadedAt, imageType, isPrimary);
    }

    private void validateImage(File file) throws IOException {
        if (file == null || !file.isFile() || file.length() < 4 || file.length() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Invalid image file size");
        }
        byte[] header = new byte[8];
        try (FileInputStream input = new FileInputStream(file)) {
            int bytes = input.read(header);
            boolean jpeg = bytes >= 3 && (header[0] & 255) == 0xFF
                    && (header[1] & 255) == 0xD8 && (header[2] & 255) == 0xFF;
            boolean png = bytes >= 8 && (header[0] & 255) == 0x89
                    && header[1] == 'P' && header[2] == 'N' && header[3] == 'G';
            if (!jpeg && !png) throw new IllegalArgumentException("Only JPEG/PNG images are allowed");
        }
    }
}
