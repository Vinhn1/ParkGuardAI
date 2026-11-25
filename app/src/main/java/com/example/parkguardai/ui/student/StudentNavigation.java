package com.example.parkguardai.ui.student;

import android.view.ViewGroup;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/** Nối bốn đích giao diện sinh viên trong cùng NavHost và chuẩn hóa thanh điều hướng cân đối, hiện đại. */
final class StudentNavigation {
    private StudentNavigation() { }

    static void bind(Fragment fragment, BottomNavigationView navigation, int currentId) {
        if (navigation == null) return;

        configureModernBottomBar(navigation);

        navigation.setSelectedItemId(currentId);
        navigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == currentId) return true;
            try {
                NavController controller = NavHostFragment.findNavController(fragment);
                controller.navigate(item.getItemId());
                return true;
            } catch (Exception ignored) {
                return false;
            }
        });
    }

    static void configureModernBottomBar(BottomNavigationView navigation) {
        // Loại bỏ khoảng đệm WindowInsets kép nhưng vẫn giữ chiều cao chuẩn 64dp cân đối,
        // không cho phép chữ tiếng Việt có dấu bị cắt chân hoặc icon bị dính sát mép trên.
        ViewCompat.setOnApplyWindowInsetsListener(navigation, (v, insets) -> {
            v.setPadding(0, 0, 0, 0);
            return WindowInsetsCompat.CONSUMED;
        });
        navigation.setPadding(0, 0, 0, 0);
        navigation.setClipToPadding(false);
        if (navigation instanceof ViewGroup) {
            ((ViewGroup) navigation).setClipChildren(false);
        }
    }
}
