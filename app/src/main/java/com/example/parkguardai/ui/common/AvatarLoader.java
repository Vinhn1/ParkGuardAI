package com.example.parkguardai.ui.common;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.text.TextUtils;
import android.util.LruCache;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.parkguardai.data.datasource.CloudinaryImageDataSource;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Tiện ích tải ảnh đại diện từ Cloudinary và cắt bo tròn gọn gàng cho tất cả các vai trò.
 * Dùng thread pool và bộ nhớ đệm LruCache dùng chung theo vòng đời ứng dụng, giúp hiển thị tức thì
 * và tránh lỗi RejectedExecutionException khi Fragment quay lại từ backstack sau onDestroyView().
 */
public final class AvatarLoader {

    private static final CloudinaryImageDataSource IMAGE_LOADER = new CloudinaryImageDataSource();
    private static final ExecutorService SHARED_IO = Executors.newFixedThreadPool(3);
    private static final LruCache<String, Bitmap> CIRCLE_CACHE = new LruCache<String, Bitmap>(4 * 1024 * 1024) {
        @Override
        protected int sizeOf(@NonNull String key, @NonNull Bitmap value) {
            return value.getByteCount();
        }
    };

    private AvatarLoader() {
    }

    public static void loadCircular(@Nullable Activity activity,
                                    @Nullable ExecutorService ignoredIo,
                                    @Nullable String avatarUrl,
                                    @NonNull ImageView targetView,
                                    @Nullable View fallbackView) {
        if (TextUtils.isEmpty(avatarUrl) || !avatarUrl.startsWith("https://")) {
            targetView.setTag(null);
            targetView.setVisibility(View.GONE);
            if (fallbackView != null) fallbackView.setVisibility(View.VISIBLE);
            return;
        }
        targetView.setTag(avatarUrl);
        Bitmap cached = CIRCLE_CACHE.get(avatarUrl);
        if (cached != null && !cached.isRecycled()) {
            targetView.setImageBitmap(cached);
            targetView.setVisibility(View.VISIBLE);
            if (fallbackView != null) fallbackView.setVisibility(View.GONE);
            return;
        }
        SHARED_IO.execute(() -> {
            try {
                Bitmap raw = IMAGE_LOADER.loadPreview(avatarUrl);
                Bitmap circle = toCircleBitmap(raw);
                CIRCLE_CACHE.put(avatarUrl, circle);
                if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                    activity.runOnUiThread(() -> {
                        if (targetView.isAttachedToWindow() && avatarUrl.equals(targetView.getTag())) {
                            targetView.setImageBitmap(circle);
                            targetView.setVisibility(View.VISIBLE);
                            if (fallbackView != null) fallbackView.setVisibility(View.GONE);
                        }
                    });
                }
            } catch (Exception ignored) {
                if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                    activity.runOnUiThread(() -> {
                        if (targetView.isAttachedToWindow() && avatarUrl.equals(targetView.getTag())) {
                            targetView.setVisibility(View.GONE);
                            if (fallbackView != null) fallbackView.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        });
    }

    @NonNull
    public static Bitmap toCircleBitmap(@NonNull Bitmap source) {
        int size = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - size) / 2;
        int y = (source.getHeight() - size) / 2;
        Bitmap squared = Bitmap.createBitmap(source, x, y, size, size);
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new BitmapShader(squared, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        float r = size / 2f;
        canvas.drawCircle(r, r, r, paint);
        if (squared != source) {
            squared.recycle();
        }
        return output;
    }
}
