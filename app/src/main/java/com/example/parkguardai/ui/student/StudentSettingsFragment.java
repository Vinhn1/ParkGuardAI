package com.example.parkguardai.ui.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.R;
import com.example.parkguardai.ui.common.LogoutConfirmationDialog;
import com.example.parkguardai.databinding.FragmentStudentSettingsBinding;

/** Nối các tùy chọn tài khoản sinh viên đến màn dùng chung. */
public class StudentSettingsFragment extends Fragment {
    private FragmentStudentSettingsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.backButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigateUp());
        binding.changePasswordButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigate(R.id.changePasswordFragment));
        binding.privacyButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigate(R.id.privacyFragment));
        binding.aboutButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigate(R.id.aboutFragment));
        binding.signOutButton.setOnClickListener(ignored ->
                LogoutConfirmationDialog.show(this));
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
