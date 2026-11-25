package com.example.parkguardai.ui.common;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.R;
import com.example.parkguardai.databinding.DialogLogoutConfirmBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;

/** Hiển thị xác nhận đăng xuất thống nhất cho các vai trò. */
public final class LogoutConfirmationDialog {
    private LogoutConfirmationDialog() { }

    public static void show(@NonNull Fragment fragment) {
        DialogLogoutConfirmBinding content = DialogLogoutConfirmBinding.inflate(
                fragment.getLayoutInflater());
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(
                fragment.requireContext()).setView(content.getRoot()).create();
        content.cancelButton.setOnClickListener(ignored -> dialog.dismiss());
        content.confirmButton.setOnClickListener(ignored -> {
            com.example.parkguardai.data.repository.AuthRepository.getInstance().signOut(fragment.requireContext());
            dialog.dismiss();
            NavController nav = NavHostFragment.findNavController(fragment);
            NavOptions options = new NavOptions.Builder()
                    .setPopUpTo(nav.getGraph().getId(), true).build();
            nav.navigate(R.id.loginFragment, null, options);
        });
        dialog.show();
    }
}
