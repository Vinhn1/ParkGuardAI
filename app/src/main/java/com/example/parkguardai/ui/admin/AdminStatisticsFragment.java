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
import com.example.parkguardai.databinding.FragmentAdminStatisticsBinding;

/** Hiển thị khung quản trị; dữ liệu đặc quyền chỉ được cấp từ máy chủ. */
public class AdminStatisticsFragment extends Fragment {
    private FragmentAdminStatisticsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminStatisticsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.backButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigateUp());
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
