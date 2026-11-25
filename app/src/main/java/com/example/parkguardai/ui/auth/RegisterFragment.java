package com.example.parkguardai.ui.auth;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.parkguardai.R;
import com.example.parkguardai.databinding.FragmentRegisterBinding;

/** Tạo danh tính email/mật khẩu và hướng dẫn xác minh email. */
public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private RegisterViewModel viewModel;
    private String pendingFullName = "";
    private String pendingStudentCode = "";
    private String pendingStudentClass = "";
    private String pendingPhone = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);
        binding.backButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigateUp());
        binding.signInButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigate(R.id.action_register_to_login));
        binding.createAccountButton.setOnClickListener(ignored -> register());
        binding.resendVerificationButton.setOnClickListener(ignored -> viewModel.sendVerificationEmail());
        binding.checkVerificationButton.setOnClickListener(ignored -> viewModel.checkVerification());
        binding.signOutButton.setOnClickListener(ignored -> viewModel.signOut());
        binding.continueOnboardingButton.setOnClickListener(ignored -> {
            Bundle args = new Bundle();
            args.putString("fullName", pendingFullName);
            args.putString("studentCode", pendingStudentCode);
            args.putString("studentClass", pendingStudentClass);
            args.putString("phone", pendingPhone);
            NavHostFragment.findNavController(this).navigate(R.id.action_register_to_onboarding, args);
        });
        binding.confirmPasswordInput.setOnEditorActionListener((ignored, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) return false;
            register();
            return true;
        });
        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
        if (savedInstanceState == null && ValueAnimator.areAnimatorsEnabled()) {
            float rise = 12f * getResources().getDisplayMetrics().density;
            binding.authHeader.setAlpha(0f);
            binding.authHeader.setTranslationY(rise);
            binding.authHeader.animate().alpha(1f).translationY(0f).setDuration(300L).start();
            if (binding.registerForm.getVisibility() == View.VISIBLE) {
                binding.registerForm.setAlpha(0f);
                binding.registerForm.setTranslationY(rise);
                binding.registerForm.animate().alpha(1f).translationY(0f)
                        .setStartDelay(70L).setDuration(330L).start();
            }
        }
    }

    private void register() {
        String fullName = binding.fullNameInput.getText() == null ? ""
                : binding.fullNameInput.getText().toString().trim();
        String studentCode = binding.studentCodeInput.getText() == null ? ""
                : binding.studentCodeInput.getText().toString().trim();
        String studentClass = binding.studentClassInput.getText() == null ? ""
                : binding.studentClassInput.getText().toString().trim();
        String phone = binding.phoneInput.getText() == null ? ""
                : binding.phoneInput.getText().toString().trim();
        String rawEmail = binding.emailInput.getText() == null ? ""
                : binding.emailInput.getText().toString().trim();
        String password = binding.passwordInput.getText() == null ? ""
                : binding.passwordInput.getText().toString();
        String confirmation = binding.confirmPasswordInput.getText() == null ? ""
                : binding.confirmPasswordInput.getText().toString();

        binding.fullNameField.setError(null);
        binding.studentCodeField.setError(null);
        binding.studentClassField.setError(null);
        binding.phoneField.setError(null);
        binding.emailField.setError(null);
        binding.passwordField.setError(null);
        binding.confirmPasswordField.setError(null);

        if (fullName.length() < 2) {
            binding.fullNameField.setError("Vui lòng nhập họ và tên.");
            binding.fullNameInput.requestFocus();
            return;
        }

        if (studentCode.length() < 4) {
            binding.studentCodeField.setError("Vui lòng nhập mã số sinh viên hợp lệ.");
            binding.studentCodeInput.requestFocus();
            return;
        }

        if (studentClass.length() < 2) {
            binding.studentClassField.setError("Vui lòng nhập tên lớp.");
            binding.studentClassInput.requestFocus();
            return;
        }

        if (!phone.isEmpty() && !phone.matches("[+]?[0-9]{9,15}")) {
            binding.phoneField.setError(getString(R.string.onboarding_phone_error));
            binding.phoneInput.requestFocus();
            return;
        }

        String email = rawEmail;
        if (email.isEmpty()) {
            email = studentCode.toLowerCase() + "@student.ctu.edu.vn";
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailField.setError(getString(R.string.auth_error_email_invalid));
            binding.emailInput.requestFocus();
            return;
        }

        if (password.length() < 8) {
            binding.passwordField.setError("Mật khẩu cần tối thiểu 8 ký tự.");
            binding.passwordInput.requestFocus();
            return;
        }

        if (!password.equals(confirmation)) {
            binding.confirmPasswordField.setError(getString(R.string.auth_error_password_mismatch));
            binding.confirmPasswordInput.requestFocus();
            return;
        }

        pendingFullName = fullName;
        pendingStudentCode = studentCode;
        pendingStudentClass = studentClass;
        pendingPhone = phone;

        // Lưu tạm thông tin đăng ký để không bị mất nếu sinh viên thoát app đi xác minh email rồi đăng nhập lại
        requireContext().getSharedPreferences("parkguard_onboarding_draft", android.content.Context.MODE_PRIVATE)
                .edit()
                .putString("email", email.toLowerCase(java.util.Locale.ROOT))
                .putString("fullName", fullName)
                .putString("studentCode", studentCode)
                .putString("studentClass", studentClass)
                .putString("phone", phone)
                .apply();

        viewModel.register(email, password);
    }

    private void render(RegisterViewModel.UiState state) {
        binding.registerForm.setVisibility(state.accountCreated ? View.GONE : View.VISIBLE);
        binding.verificationPanel.setVisibility(state.accountCreated ? View.VISIBLE : View.GONE);
        binding.createAccountButton.setEnabled(!state.loading);
        binding.signInButton.setEnabled(!state.loading);
        binding.formProgress.setVisibility(state.loading && !state.accountCreated
                ? View.VISIBLE : View.GONE);
        binding.formStatus.setVisibility(!state.accountCreated && state.messageRes != 0
                ? View.VISIBLE : View.GONE);
        if (!state.accountCreated && state.messageRes != 0) {
            binding.formStatus.setText(state.messageRes);
            binding.formStatus.setTextColor(requireContext().getColor(
                    state.error ? R.color.pg_error : R.color.pg_primary));
        }
        binding.verificationTitle.setText(state.verified
                ? R.string.auth_verification_complete : R.string.auth_verify_title);
        binding.verificationDescription.setText(state.verified
                ? R.string.auth_verified_waiting_profile : R.string.auth_account_created_description);
        binding.verificationActions.setVisibility(state.verified ? View.GONE : View.VISIBLE);
        binding.continueOnboardingButton.setVisibility(state.accountCreated && state.verified
                ? View.VISIBLE : View.GONE);
        binding.resendVerificationButton.setEnabled(!state.loading);
        binding.checkVerificationButton.setEnabled(!state.loading);
        binding.signOutButton.setEnabled(!state.loading);
        binding.verificationProgress.setVisibility(state.loading && state.accountCreated
                ? View.VISIBLE : View.GONE);
        binding.verificationStatus.setVisibility(state.accountCreated && state.messageRes != 0
                ? View.VISIBLE : View.GONE);
        if (state.accountCreated && state.messageRes != 0) {
            binding.verificationStatus.setText(state.messageRes);
            binding.verificationStatus.setTextColor(requireContext().getColor(
                    state.error ? R.color.pg_error : R.color.pg_primary));
        }
    }

    @Override
    public void onDestroyView() {
        binding.authHeader.animate().cancel();
        binding.registerForm.animate().cancel();
        binding = null;
        super.onDestroyView();
    }
}
