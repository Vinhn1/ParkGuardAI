package com.example.parkguardai.ui.auth;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.R;
import com.example.parkguardai.databinding.FragmentForgotPasswordBinding;

import java.util.Locale;

/** Thu email/mã số khôi phục và xử lý mã xác nhận OTP 6 số. */
public class ForgotPasswordFragment extends Fragment {

    private FragmentForgotPasswordBinding binding;
    private LoginViewModel viewModel;
    private String submittedEmail;
    private CountDownTimer countDownTimer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentForgotPasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        binding.backButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigateUp());
        binding.submitButton.setOnClickListener(ignored -> submit());
        binding.resendButton.setOnClickListener(ignored -> resendCode());

        setupOtpBoxes();
        startOtpCountdown();

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            binding.submitButton.setEnabled(!state.loading);
            binding.loadingProgress.setVisibility(state.loading ? View.VISIBLE : View.GONE);
            binding.statusText.setVisibility(state.messageRes == 0 ? View.GONE : View.VISIBLE);
            if (state.messageRes != 0) {
                binding.statusText.setText(state.messageRes);
                binding.statusText.setTextColor(requireContext().getColor(
                        state.error ? R.color.pg_error : R.color.pg_primary));
            }
            if (!state.loading && !state.error && state.messageRes == R.string.auth_reset_sent
                    && submittedEmail != null) {
                Bundle args = new Bundle();
                args.putString("email", submittedEmail);
                submittedEmail = null;
                NavHostFragment.findNavController(this).navigate(R.id.resetSentFragment, args);
            }
        });
    }

    /** Thiết lập chuyển đổi con trỏ tuần tự mượt mà giữa 6 ô OTP. */
    private void setupOtpBoxes() {
        EditText[] boxes = new EditText[]{
                binding.otpDigit1,
                binding.otpDigit2,
                binding.otpDigit3,
                binding.otpDigit4,
                binding.otpDigit5,
                binding.otpDigit6
        };

        for (int i = 0; i < boxes.length; i++) {
            final int index = i;
            boxes[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s != null && s.length() == 1 && index < boxes.length - 1) {
                        boxes[index + 1].requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });

            boxes[i].setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
                    if (boxes[index].getText().length() == 0 && index > 0) {
                        boxes[index - 1].requestFocus();
                        boxes[index - 1].setText("");
                        return true;
                    }
                }
                return false;
            });
        }
    }

    /** Bắt đầu đếm ngược 45 giây cho phép gửi lại mã OTP. */
    private void startOtpCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        binding.resendButton.setEnabled(false);
        countDownTimer = new CountDownTimer(45000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (binding == null) return;
                long seconds = millisUntilFinished / 1000L;
                binding.resendTimerText.setText(getString(R.string.auth_otp_resend_timer, seconds));
            }

            @Override
            public void onFinish() {
                if (binding == null) return;
                binding.resendTimerText.setText(R.string.auth_otp_resend_ready);
                binding.resendButton.setEnabled(true);
            }
        }.start();
    }

    private void resendCode() {
        String input = binding.emailInput.getText() == null ? ""
                : binding.emailInput.getText().toString().trim();
        String email = input;
        if (!input.contains("@") && input.matches("[0-9]{6,12}")) {
            email = input + "@student.ctu.edu.vn";
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailField.setError(getString(email.isEmpty()
                    ? R.string.auth_error_email_required : R.string.auth_error_email_invalid));
            binding.emailInput.requestFocus();
            return;
        }
        startOtpCountdown();
        viewModel.sendPasswordReset(email);
        com.google.android.material.snackbar.Snackbar.make(binding.getRoot(),
                R.string.auth_reset_sent,
                com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
    }

    private void submit() {
        String input = binding.emailInput.getText() == null ? ""
                : binding.emailInput.getText().toString().trim();
        binding.emailField.setError(null);

        String email = input;
        if (!input.contains("@") && input.matches("[0-9]{6,12}")) {
            email = input + "@student.ctu.edu.vn";
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailField.setError(getString(email.isEmpty()
                    ? R.string.auth_error_email_required : R.string.auth_error_email_invalid));
            binding.emailInput.requestFocus();
            return;
        }

        submittedEmail = email;
        viewModel.sendPasswordReset(email);
    }

    private String getEnteredOtp() {
        return textOf(binding.otpDigit1)
                + textOf(binding.otpDigit2)
                + textOf(binding.otpDigit3)
                + textOf(binding.otpDigit4)
                + textOf(binding.otpDigit5)
                + textOf(binding.otpDigit6);
    }

    private String textOf(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    @Override
    public void onDestroyView() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        binding = null;
        super.onDestroyView();
    }
}
