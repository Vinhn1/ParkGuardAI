package com.example.parkguardai.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.R;
import com.example.parkguardai.databinding.FragmentAccessGateBinding;

/** Hiển thị trạng thái quyền sau khi đọc grant từ máy chủ. */
public class AccessGateFragment extends Fragment {
    private FragmentAccessGateBinding binding;
    private LoginViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAccessGateBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        binding.retryButton.setOnClickListener(ignored -> viewModel.refreshAccess());
        binding.signOutButton.setOnClickListener(ignored -> {
            viewModel.signOut();
            NavHostFragment.findNavController(this).navigate(R.id.loginFragment);
        });
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            binding.loadingProgress.setVisibility(state.loading ? View.VISIBLE : View.GONE);
            binding.retryButton.setEnabled(!state.loading);
            binding.statusText.setVisibility(state.messageRes == 0 ? View.GONE : View.VISIBLE);
            if (state.messageRes != 0) binding.statusText.setText(state.messageRes);
            NavController nav = NavHostFragment.findNavController(this);
            if (nav.getCurrentDestination() == null
                    || nav.getCurrentDestination().getId() != R.id.accessGateFragment) return;
            if (state.accessState == LoginViewModel.AccessState.STUDENT) {
                nav.navigate(R.id.studentOnboardingFragment);
            } else if (state.accessState == LoginViewModel.AccessState.STUDENT_PENDING) {
                nav.navigate(R.id.registrationSubmittedFragment);
            } else if (state.accessState == LoginViewModel.AccessState.GUARD) {
                nav.navigate(R.id.guardCameraFragment);
            } else if (state.accessState == LoginViewModel.AccessState.ADMIN) {
                nav.navigate(R.id.adminDashboardFragment);
            }
        });
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
