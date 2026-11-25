package com.example.parkguardai.ui.auth;

import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.parkguardai.R;
import com.example.parkguardai.data.datasource.FirebaseEmailAuthDataSource;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

/** Điều phối tạo danh tính Auth và xác minh email; không cấp vai trò hoặc hồ sơ. */
public class RegisterViewModel extends ViewModel {

    public static final class UiState {
        public final boolean accountCreated;
        public final boolean verified;
        public final boolean loading;
        public final boolean error;
        @StringRes public final int messageRes;

        private UiState(boolean accountCreated, boolean verified, boolean loading,
                        boolean error, @StringRes int messageRes) {
            this.accountCreated = accountCreated;
            this.verified = verified;
            this.loading = loading;
            this.error = error;
            this.messageRes = messageRes;
        }
    }

    private final FirebaseEmailAuthDataSource authDataSource =
            new FirebaseEmailAuthDataSource(FirebaseAuth.getInstance());
    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(currentSessionState());

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public void register(String email, String password) {
        if (isLoading()) return;
        uiState.setValue(new UiState(false, false, true, false, 0));
        authDataSource.register(email, password).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                uiState.setValue(new UiState(false, false, false, true,
                        registerError(task.getException())));
                return;
            }
            // Tài khoản đã tồn tại ngay cả khi gửi thư lỗi; giữ panel để gửi lại.
            uiState.setValue(new UiState(true, false, true, false, 0));
            authDataSource.sendVerificationEmail().addOnCompleteListener(mailTask ->
                    uiState.setValue(new UiState(true, false, false, !mailTask.isSuccessful(),
                            mailTask.isSuccessful() ? R.string.auth_account_created
                                    : R.string.auth_account_created_email_failed)));
        });
    }

    public void sendVerificationEmail() {
        UiState current = uiState.getValue();
        if (isLoading() || current == null || !current.accountCreated || current.verified) return;
        uiState.setValue(new UiState(true, false, true, false, 0));
        authDataSource.sendVerificationEmail().addOnCompleteListener(task ->
                uiState.setValue(new UiState(true, false, false, !task.isSuccessful(),
                        task.isSuccessful() ? R.string.auth_verification_sent
                                : R.string.auth_verification_send_failed)));
    }

    public void checkVerification() {
        UiState current = uiState.getValue();
        if (isLoading() || current == null || !current.accountCreated || current.verified) return;
        uiState.setValue(new UiState(true, false, true, false, 0));
        authDataSource.reloadCurrentUser().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                boolean verified = authDataSource.isCurrentEmailVerified();
                uiState.setValue(new UiState(true, verified, false, false,
                        verified ? R.string.auth_verification_complete
                                : R.string.auth_verification_pending));
            } else {
                uiState.setValue(new UiState(true, false, false, true,
                        task.getException() instanceof FirebaseNetworkException
                                ? R.string.auth_error_network : R.string.auth_error_unknown));
            }
        });
    }

    public void signOut() {
        authDataSource.signOut();
        uiState.setValue(new UiState(false, false, false, false, 0));
    }

    private boolean isLoading() {
        UiState current = uiState.getValue();
        return current != null && current.loading;
    }

    private UiState currentSessionState() {
        boolean accountCreated = FirebaseAuth.getInstance().getCurrentUser() != null;
        boolean verified = accountCreated && authDataSource.isCurrentEmailVerified();
        return new UiState(accountCreated, verified, false, false, 0);
    }

    @StringRes
    private int registerError(Exception exception) {
        if (exception instanceof FirebaseNetworkException) return R.string.auth_error_network;
        if (exception instanceof FirebaseTooManyRequestsException) {
            return R.string.auth_error_too_many_requests;
        }
        if (exception instanceof FirebaseAuthWeakPasswordException) {
            return R.string.auth_error_weak_password;
        }
        // Không tiết lộ địa chỉ email đã đăng ký qua lỗi tạo tài khoản.
        return R.string.auth_error_register;
    }
}
