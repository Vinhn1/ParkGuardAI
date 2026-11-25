package com.example.parkguardai.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.R;
import com.example.parkguardai.databinding.FragmentAdminViolationTypesBinding;

/** Hiển thị khung quản trị; dữ liệu đặc quyền chỉ được cấp từ máy chủ. */
public class AdminViolationTypesFragment extends Fragment {
    private FragmentAdminViolationTypesBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminViolationTypesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.backButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigateUp());

        binding.addRuleButton.setOnClickListener(v -> {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Thêm loại vi phạm mới")
                    .setMessage("Tính năng tạo và cấu hình biểu phí phạt mới dành riêng cho Ban Giám Hiệu và Phòng Quản sinh.")
                    .setPositiveButton("Đã hiểu", null)
                    .show();
        });
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
