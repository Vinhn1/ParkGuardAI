package com.example.parkguardai.ui.auth;

import android.os.Bundle;
import android.util.Patterns;
import android.animation.ValueAnimator;
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
import com.example.parkguardai.databinding.FragmentLoginBinding;
import com.example.parkguardai.ui.common.BiometricAuthHelper;
import com.google.android.material.snackbar.Snackbar;

/** Thu dữ liệu đăng nhập, hiện lỗi tại trường và kết quả Firebase Auth. */
public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private LoginViewModel viewModel;
    private String pendingEmail;
    private String pendingPassword;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        binding.backButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigateUp());
        binding.registerButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigate(R.id.action_login_to_register));
        binding.signInButton.setOnClickListener(ignored -> signIn());
        binding.forgotPasswordButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigate(R.id.forgotPasswordFragment));
        binding.signOutButton.setOnClickListener(ignored -> viewModel.signOut());
        binding.continueOnboardingButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigate(R.id.action_login_to_onboarding));
        binding.continueGuardButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigate(R.id.action_login_to_guard));
        binding.continueStudentButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigate(R.id.registrationSubmittedFragment));
        binding.continueAdminButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigate(R.id.adminDashboardFragment));
        binding.retryAccessButton.setOnClickListener(ignored ->
                NavHostFragment.findNavController(this).navigate(R.id.accessGateFragment));
        binding.resendVerificationButton.setOnClickListener(ignored -> viewModel.sendVerificationEmail());
        binding.checkVerificationButton.setOnClickListener(ignored -> viewModel.checkVerification());
        binding.passwordInput.setOnEditorActionListener((ignored, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) return false;
            signIn();
            return true;
        });

        // Xử lý chuyển đổi vai trò trên Segmented Pill Group
        binding.roleToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            updateRoleToggleStyle(checkedId);
        });

        // Kiểm tra phiên đã đăng nhập sẵn: nếu bật vân tay/Face ID thì yêu cầu xác thực trước khi vào app
        com.google.firebase.auth.FirebaseUser existingUser =
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (existingUser != null && existingUser.isEmailVerified()) {
            if (BiometricAuthHelper.isEnabled(requireContext())
                    && BiometricAuthHelper.canAuthenticate(requireContext())) {
                BiometricAuthHelper.authenticateSession(this, new BiometricAuthHelper.SessionCallback() {
                    @Override
                    public void onVerified() {
                        if (isAdded()) viewModel.resumeSessionIfVerified();
                    }

                    @Override
                    public void onCancelled() {
                        if (!isAdded()) return;
                        viewModel.signOut();
                    }

                    @Override
                    public void onError(@NonNull String message) {
                        if (!isAdded() || binding == null) return;
                        viewModel.signOut();
                        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
                    }
                });
            } else {
                viewModel.resumeSessionIfVerified();
            }
        }

        // Nút Đăng nhập bằng Sinh trắc học (Vân tay / Face ID)
        binding.biometricLoginButton.setOnClickListener(v -> {
            com.google.firebase.auth.FirebaseUser current =
                    com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (current != null && current.isEmailVerified()
                    && BiometricAuthHelper.isEnabled(requireContext())
                    && BiometricAuthHelper.canAuthenticate(requireContext())) {
                BiometricAuthHelper.authenticateSession(this, new BiometricAuthHelper.SessionCallback() {
                    @Override
                    public void onVerified() {
                        if (isAdded()) viewModel.resumeSessionIfVerified();
                    }

                    @Override
                    public void onCancelled() { }

                    @Override
                    public void onError(@NonNull String message) {
                        if (isAdded() && binding != null) {
                            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
                        }
                    }
                });
                return;
            }
            BiometricAuthHelper.authenticateAndLoadCredentials(this, new BiometricAuthHelper.AuthCallback() {
                @Override
                public void onSuccess(@NonNull String email, @NonNull String password) {
                    if (!isAdded()) return;
                    pendingEmail = email;
                    pendingPassword = password;
                    viewModel.signIn(email, password);
                }

                @Override
                public void onError(@NonNull String message) {
                    if (!isAdded() || binding == null) return;
                    Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
                }
            });
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getNavigationEvent().observe(getViewLifecycleOwner(), navTarget -> {
            if (navTarget != null) {
                if (pendingEmail != null && pendingPassword != null && isAdded()) {
                    BiometricAuthHelper.saveCredentials(requireContext(), pendingEmail, pendingPassword);
                    pendingEmail = null;
                    pendingPassword = null;
                }
                int resId = navTarget.intValue();
                try {
                    androidx.navigation.NavOptions navOptions = new androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(R.id.loginFragment, true)
                            .build();
                    NavHostFragment.findNavController(this).navigate(resId, null, navOptions);
                } catch (Exception e) {
                    android.util.Log.e("LoginFragment", "Navigation failed to destination " + resId, e);
                }
                viewModel.clearNavigationEvent();
            }
        });
        if (savedInstanceState == null && ValueAnimator.areAnimatorsEnabled()) {
            float rise = 12f * getResources().getDisplayMetrics().density;
            binding.authHeader.setAlpha(0f);
            binding.authHeader.setTranslationY(rise);
            binding.authHeader.animate().alpha(1f).translationY(0f).setDuration(300L).start();
            if (binding.loginForm.getVisibility() == View.VISIBLE) {
                binding.loginForm.setAlpha(0f);
                binding.loginForm.setTranslationY(rise);
                binding.loginForm.animate().alpha(1f).translationY(0f)
                        .setStartDelay(70L).setDuration(330L).start();
            }
        }
    }

    private void updateRoleToggleStyle(int checkedId) {
        int activeBg = requireContext().getColor(R.color.accent_cyan);
        int inactiveBg = android.graphics.Color.TRANSPARENT;
        int activeText = requireContext().getColor(R.color.white);
        int inactiveText = requireContext().getColor(R.color.text_muted);

        boolean isStudent = checkedId == R.id.roleStudentButton;
        boolean isGuard = checkedId == R.id.roleGuardButton;
        boolean isAdmin = checkedId == R.id.roleAdminButton;

        binding.roleStudentButton.setBackgroundColor(isStudent ? activeBg : inactiveBg);
        binding.roleStudentButton.setTextColor(isStudent ? activeText : inactiveText);

        binding.roleGuardButton.setBackgroundColor(isGuard ? activeBg : inactiveBg);
        binding.roleGuardButton.setTextColor(isGuard ? activeText : inactiveText);

        binding.roleAdminButton.setBackgroundColor(isAdmin ? activeBg : inactiveBg);
        binding.roleAdminButton.setTextColor(isAdmin ? activeText : inactiveText);
    }

    private void signIn() {
        String input = binding.emailInput.getText() == null ? ""
                : binding.emailInput.getText().toString().trim();
        String password = binding.passwordInput.getText() == null ? ""
                : binding.passwordInput.getText().toString();
        binding.emailField.setError(null);
        binding.passwordField.setError(null);

        // Hỗ trợ nhập mã sinh viên (chỉ gồm số) -> quy đổi sang định dạng email sinh viên
        String email = input;
        if (!input.contains("@") && input.matches("[0-9]{6,12}")) {
            email = input + "@student.ctu.edu.vn";
        }

        if (!validateEmail(input, email)) return;
        if (password.isEmpty()) {
            binding.passwordField.setError(getString(R.string.auth_error_password_required));
            binding.passwordInput.requestFocus();
            return;
        }

        pendingEmail = email;
        pendingPassword = password;
        viewModel.signIn(email, password);
    }

    private boolean validateEmail(String rawInput, String email) {
        if (rawInput.isEmpty()) {
            binding.emailField.setError(getString(R.string.auth_error_email_required));
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailField.setError(getString(R.string.auth_error_email_invalid));
        } else {
            return true;
        }
        binding.emailInput.requestFocus();
        return false;
    }

    private void render(LoginViewModel.UiState state) {
        binding.loginForm.setVisibility(state.signedIn ? View.GONE : View.VISIBLE);
        binding.signedInPanel.setVisibility(state.signedIn ? View.VISIBLE : View.GONE);
        binding.signInButton.setEnabled(!state.loading);
        binding.forgotPasswordButton.setEnabled(!state.loading);
        binding.registerButton.setEnabled(!state.loading);
        binding.loadingProgress.setVisibility(state.loading && !state.signedIn ? View.VISIBLE : View.GONE);
        binding.formStatus.setVisibility(state.messageRes == 0 ? View.GONE : View.VISIBLE);
        if (!state.signedIn && state.messageRes != 0) {
            binding.formStatus.setText(state.messageRes);
            int color = state.error ? R.color.pg_error : R.color.pg_primary;
            binding.formStatus.setTextColor(requireContext().getColor(color));
        }
        binding.verificationActions.setVisibility(state.verified ? View.GONE : View.VISIBLE);
        binding.continueOnboardingButton.setVisibility(state.accessState == LoginViewModel.AccessState.STUDENT
                ? View.VISIBLE : View.GONE);
        binding.continueGuardButton.setVisibility(state.accessState == LoginViewModel.AccessState.GUARD
                ? View.VISIBLE : View.GONE);
        binding.continueStudentButton.setVisibility(
                state.accessState == LoginViewModel.AccessState.STUDENT_PENDING
                        ? View.VISIBLE : View.GONE);
        binding.continueAdminButton.setVisibility(
                state.accessState == LoginViewModel.AccessState.ADMIN
                        ? View.VISIBLE : View.GONE);
        binding.retryAccessButton.setVisibility(state.accessState == LoginViewModel.AccessState.ERROR
                || state.accessState == LoginViewModel.AccessState.RESTRICTED
                ? View.VISIBLE : View.GONE);
        binding.signedInDescription.setText(state.verified
                ? R.string.auth_signed_in_description : R.string.auth_email_unverified);
        binding.signedInTitle.setText(state.verified
                ? R.string.auth_signed_in_title : R.string.auth_verify_title);
        binding.resendVerificationButton.setEnabled(!state.loading);
        binding.checkVerificationButton.setEnabled(!state.loading);
        binding.signOutButton.setEnabled(!state.loading);
        binding.verificationProgress.setVisibility(state.loading && state.signedIn
                ? View.VISIBLE : View.GONE);
        binding.verificationStatus.setVisibility(state.signedIn && state.messageRes != 0
                ? View.VISIBLE : View.GONE);
        if (state.signedIn && state.messageRes != 0) {
            binding.verificationStatus.setText(state.messageRes);
            binding.verificationStatus.setTextColor(requireContext().getColor(
                    state.error ? R.color.pg_error : R.color.pg_primary));
        }
    }

    @Override
    public void onDestroyView() {
        binding.authHeader.animate().cancel();
        binding.loginForm.animate().cancel();
        binding = null;
        super.onDestroyView();
    }
}
