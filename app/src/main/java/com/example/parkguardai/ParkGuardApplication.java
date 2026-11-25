package com.example.parkguardai;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.cloudinary.android.MediaManager;

import java.util.HashMap;
import java.util.Map;

/** Khởi tạo Cloudinary một lần và đặt chế độ giao diện sáng ấm chuẩn của ứng dụng. */
public final class ParkGuardApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        Map<String, Object> config = new HashMap<>();
        config.put("cloud_name", "ds1g5ryn1");
        config.put("secure", true);
        MediaManager.init(this, config);
    }
}
