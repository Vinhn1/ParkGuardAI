package com.example.parkguardai.ui.admin;

import android.view.ViewGroup;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/** Nối năm đích giao diện quản trị sau khi grant được xác nhận. */
final class AdminNavigation {
    private AdminNavigation() { }

    static void bind(Fragment fragment, BottomNavigationView navigation, int currentId) {
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
                    .setPopUpTo(R.id.adminDashboardFragment, false).build();
            controller.navigate(item.getItemId(), null, options);
            return true;
        });
    }
}
