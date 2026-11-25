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
import com.example.parkguardai.databinding.FragmentAdminVerificationSuccessBinding;

/** Hiển thị khung quản trị; dữ liệu đặc quyền chỉ được cấp từ máy chủ. */
public class AdminVerificationSuccessFragment extends Fragment {
    private FragmentAdminVerificationSuccessBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminVerificationSuccessBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.backButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigateUp());
        binding.returnQueueButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigate(R.id.adminVerificationQueueFragment));
        binding.returnDashboardButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigate(R.id.adminDashboardFragment));
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
