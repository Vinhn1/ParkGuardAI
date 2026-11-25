package com.example.parkguardai.ui.common;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.databinding.FragmentAboutBinding;

/** Hiển thị thông tin ứng dụng từ bản cài đặt hiện tại. */
public class AboutFragment extends Fragment {
    private FragmentAboutBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAboutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.backButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigateUp());
        try {
            PackageInfo info = requireContext().getPackageManager().getPackageInfo(
                    requireContext().getPackageName(), 0);
            if (info.versionName != null) binding.versionText.setText(info.versionName);
        } catch (PackageManager.NameNotFoundException ignored) {
            // Giữ nội dung mặc định nếu hệ thống chưa trả thông tin bản cài đặt.
        }
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
