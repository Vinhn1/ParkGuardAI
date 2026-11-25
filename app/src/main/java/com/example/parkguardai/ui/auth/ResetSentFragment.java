package com.example.parkguardai.ui.auth;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.R;
import com.example.parkguardai.databinding.FragmentResetSentBinding;

/** Hiển thị kết quả khôi phục trung tính và giới hạn nhấn gửi lại tại UI. */
public class ResetSentFragment extends Fragment {
    private static final long COOLDOWN_MS = 30000L;
    private FragmentResetSentBinding binding;
    private LoginViewModel viewModel;
    private CountDownTimer timer;
    private long cooldownEndsAt;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentResetSentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        cooldownEndsAt = savedInstanceState == null
                ? SystemClock.elapsedRealtime() + COOLDOWN_MS
                : savedInstanceState.getLong("cooldownEndsAt", SystemClock.elapsedRealtime());
        binding.backButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigateUp());
        binding.loginButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigate(R.id.loginFragment));
        binding.resendButton.setOnClickListener(ignored -> {
            String email = getArguments() == null ? null : getArguments().getString("email");
            if (email != null && SystemClock.elapsedRealtime() >= cooldownEndsAt) {
                viewModel.sendPasswordReset(email);
            }
        });
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            binding.statusText.setVisibility(state.messageRes == 0 ? View.GONE : View.VISIBLE);
            if (state.messageRes != 0) {
                binding.statusText.setText(state.messageRes);
                binding.statusText.setTextColor(requireContext().getColor(
                        state.error ? R.color.pg_error : R.color.pg_primary));
            }
            if (state.messageRes == R.string.auth_reset_sent && !state.loading) {
                cooldownEndsAt = SystemClock.elapsedRealtime() + COOLDOWN_MS;
            }
            updateCooldown(state.loading);
        });
        updateCooldown(false);
    }

    private void updateCooldown(boolean loading) {
        if (timer != null) timer.cancel();
        long remaining = Math.max(0L, cooldownEndsAt - SystemClock.elapsedRealtime());
        binding.resendButton.setEnabled(!loading && remaining == 0L);
        binding.cooldownText.setVisibility(remaining == 0L ? View.GONE : View.VISIBLE);
        if (remaining > 0L) {
            timer = new CountDownTimer(remaining, 1000L) {
                @Override public void onTick(long millisUntilFinished) {
                    long seconds = (millisUntilFinished + 999L) / 1000L;
                    binding.cooldownText.setText(getString(R.string.auth_reset_cooldown,
                            seconds / 60L, seconds % 60L));
                }
                @Override public void onFinish() { updateCooldown(false); }
            }.start();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putLong("cooldownEndsAt", cooldownEndsAt);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        if (timer != null) timer.cancel();
        binding = null;
        super.onDestroyView();
    }
}
