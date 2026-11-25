package com.example.parkguardai.ui.guard;

import android.view.ViewGroup;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Điều hướng giữa 4 tab chính của phân hệ Bảo vệ (Quét, Lịch sử, Cảnh báo, Hồ sơ).
 */
public final class GuardNavigation {
    private GuardNavigation() { }

    public static void bind(Fragment fragment, BottomNavigationView navigation, int currentId) {
        if (navigation == null) return;

        ViewCompat.setOnApplyWindowInsetsListener(navigation, (v, insets) -> {
            v.setPadding(0, 0, 0, 0);
            return WindowInsetsCompat.CONSUMED;
        });
        navigation.setPadding(0, 0, 0, 0);
        navigation.setClipToPadding(false);
        if (navigation instanceof ViewGroup) {
            ((ViewGroup) navigation).setClipChildren(false);
        }

        navigation.setSelectedItemId(currentId);
        navigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == currentId) return true;
            NavController controller = NavHostFragment.findNavController(fragment);
            NavOptions options = new NavOptions.Builder().setLaunchSingleTop(true)
                    .setPopUpTo(R.id.guardCameraFragment, false).build();
            controller.navigate(item.getItemId(), null, options);
            return true;
        });
    }
}
