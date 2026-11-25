package com.example.parkguardai.ui.common;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.R;
import com.example.parkguardai.databinding.FragmentChangePasswordBinding;
import com.example.parkguardai.ui.auth.LoginViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/** Gửi yêu cầu liên kết đổi mật khẩu qua ViewModel Auth hiện có. */
public class ChangePasswordFragment extends Fragment {
    private FragmentChangePasswordBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentChangePasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        LoginViewModel viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String email = user == null ? null : user.getEmail();
        binding.emailText.setText(email == null ? getString(R.string.student_data_pending) : email);
        binding.sendButton.setEnabled(user != null && user.isEmailVerified() && email != null);
        binding.backButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigateUp());
        binding.sendButton.setOnClickListener(ignored -> viewModel.sendPasswordReset(email));
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            binding.sendButton.setEnabled(!state.loading && user != null && user.isEmailVerified());
            binding.statusText.setVisibility(state.messageRes == 0 ? View.GONE : View.VISIBLE);
            if (state.messageRes != 0) binding.statusText.setText(state.messageRes);
        });
        if (user == null || !user.isEmailVerified()) {
            binding.statusText.setText(R.string.common_change_password_unavailable);
            binding.statusText.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
