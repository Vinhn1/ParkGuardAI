package com.example.parkguardai.ui.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.parkguardai.R;
import com.example.parkguardai.data.repository.AuthRepository;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.firestore.FirebaseFirestoreException;

/**
 * ViewModel điều phối xác thực đăng nhập, kiểm tra phiên và phân giải quyền truy cập.
 */
public class LoginViewModel extends ViewModel {

    public enum AccessState {
        UNKNOWN,
        STUDENT,
        STUDENT_PENDING,
        GUARD,
        ADMIN,
        RESTRICTED,
        ERROR
    }

    public static final class UiState {
        public final boolean signedIn;
        public final boolean verified;
        public final boolean loading;
        public final boolean error;
        public final AccessState accessState;
        @StringRes public final int messageRes;

        public UiState(boolean signedIn, boolean verified, boolean loading,
                       boolean error, @StringRes int messageRes, AccessState accessState) {
            this.signedIn = signedIn;
            this.verified = verified;
            this.loading = loading;
            this.error = error;
            this.messageRes = messageRes;
            this.accessState = accessState;
        }
    }

    private final AuthRepository authRepository = AuthRepository.getInstance();
    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(new UiState(
            authRepository.getCurrentUser() != null,
            authRepository.isCurrentEmailVerified(),
            false,
            false,
            0,
            AccessState.UNKNOWN
    ));
    private final MutableLiveData<Integer> navigationEvent = new MutableLiveData<>(null);

    public LoginViewModel() {
        // Phiên đăng nhập có sẵn sẽ được LoginFragment kiểm tra qua sinh trắc học (nếu bật) trước khi vào app.
    }

    public void resumeSessionIfVerified() {
        if (authRepository.getCurrentUser() != null && authRepository.isCurrentEmailVerified()) {
            resolveAccess();
        }
    }

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public LiveData<Integer> getNavigationEvent() {
        return navigationEvent;
    }

    public void clearNavigationEvent() {
        navigationEvent.setValue(null);
    }

    public void signIn(@NonNull String email, @NonNull String password) {
        if (isLoading()) return;
        uiState.setValue(new UiState(false, false, true, false, 0, AccessState.UNKNOWN));

        authRepository.signIn(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                handleAuthResult(task.getResult());
            } else {
                uiState.setValue(new UiState(false, false, false, true,
                        errorMessage(task.getException(), true), AccessState.UNKNOWN));
            }
        });
    }

    private void handleAuthResult(@NonNull AuthRepository.AuthRoleResult role) {
        switch (role) {
            case ADMIN:
                uiState.setValue(new UiState(true, true, false, false, 0, AccessState.ADMIN));
                navigationEvent.setValue(R.id.adminDashboardFragment);
                break;
            case SECURITY_GUARD:
                uiState.setValue(new UiState(true, true, false, false, 0, AccessState.GUARD));
                navigationEvent.setValue(R.id.guardCameraFragment);
                break;
            case STUDENT_ACTIVE:
                uiState.setValue(new UiState(true, true, false, false, 0, AccessState.STUDENT));
                navigationEvent.setValue(R.id.studentHomeFragment);
                break;
            case STUDENT_PENDING:
                uiState.setValue(new UiState(true, true, false, false,
                        R.string.onboarding_already_sent, AccessState.STUDENT_PENDING));
                navigationEvent.setValue(R.id.studentHomeFragment);
                break;
            case STUDENT_NEED_ONBOARDING:
                uiState.setValue(new UiState(true, true, false, false, 0, AccessState.STUDENT));
                navigationEvent.setValue(R.id.studentOnboardingFragment);
                break;
            case EMAIL_NOT_VERIFIED:
                uiState.setValue(new UiState(true, false, false, false,
                        R.string.auth_email_unverified, AccessState.UNKNOWN));
                break;
            case RESTRICTED:
            default:
                uiState.setValue(new UiState(true, true, false, true,
                        R.string.guard_access_restricted, AccessState.RESTRICTED));
                break;
        }
    }

    public void resolveAccess() {
        if (authRepository.getCurrentUser() == null) return;
        uiState.setValue(new UiState(true, true, true, false, 0, AccessState.UNKNOWN));

        authRepository.resolveUserAccess(authRepository.getCurrentUser()).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                handleAuthResult(task.getResult());
            } else {
                uiState.setValue(new UiState(true, true, false, true,
                        accessErrorMessage(task.getException()), AccessState.ERROR));
            }
        });
    }

    public void refreshAccess() {
        resolveAccess();
    }

    public void sendPasswordReset(@NonNull String email) {
        if (isLoading()) return;
        uiState.setValue(new UiState(false, false, true, false, 0, AccessState.UNKNOWN));

        authRepository.sendPasswordReset(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                uiState.setValue(new UiState(false, false, false, false,
                        R.string.auth_reset_sent, AccessState.UNKNOWN));
            } else {
                uiState.setValue(new UiState(false, false, false, true,
                        errorMessage(task.getException(), false), AccessState.UNKNOWN));
            }
        });
    }

    public void sendVerificationEmail() {
        UiState current = uiState.getValue();
        if (isLoading() || current == null || !current.signedIn || current.verified) return;
        uiState.setValue(new UiState(true, false, true, false, 0, AccessState.UNKNOWN));

        authRepository.sendEmailVerification().addOnCompleteListener(task -> {
            uiState.setValue(new UiState(true, false, false, !task.isSuccessful(),
                    task.isSuccessful() ? R.string.auth_verification_sent
                            : errorMessage(task.getException(), false), AccessState.UNKNOWN));
        });
    }

    public void checkVerification() {
        UiState current = uiState.getValue();
        if (isLoading() || current == null || !current.signedIn || current.verified) return;
        uiState.setValue(new UiState(true, false, true, false, 0, AccessState.UNKNOWN));

        authRepository.checkCurrentSession().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                handleAuthResult(task.getResult());
            } else {
                boolean verified = authRepository.isCurrentEmailVerified();
                uiState.setValue(new UiState(true, verified, false, !verified,
                        verified ? R.string.auth_verification_complete
                                : R.string.auth_verification_pending, AccessState.UNKNOWN));
                if (verified) {
                    resolveAccess();
                }
            }
        });
    }

    public void signOut() {
        authRepository.signOut(null);
        uiState.setValue(new UiState(false, false, false, false, 0, AccessState.UNKNOWN));
        navigationEvent.setValue(null);
    }

    private boolean isLoading() {
        UiState current = uiState.getValue();
        return current != null && current.loading;
    }

    @StringRes
    private int accessErrorMessage(@Nullable Exception failure) {
        if (failure instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException.Code code =
                    ((FirebaseFirestoreException) failure).getCode();
            if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                    || code == FirebaseFirestoreException.Code.UNAUTHENTICATED) {
                return R.string.guard_access_denied;
            }
            if (code == FirebaseFirestoreException.Code.UNAVAILABLE
                    || code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED) {
                return R.string.guard_access_network;
            }
        }
        if (failure instanceof FirebaseNetworkException) return R.string.guard_access_network;
        return R.string.guard_access_error;
    }

    @StringRes
    private int errorMessage(@Nullable Exception exception, boolean isSignIn) {
        if (exception instanceof FirebaseNetworkException) {
            return R.string.auth_error_network;
        }
        if (exception instanceof FirebaseTooManyRequestsException) {
            return R.string.auth_error_too_many_requests;
        }
        if (isSignIn && exception instanceof FirebaseAuthException) {
            return R.string.auth_error_invalid_credentials;
        }
        return R.string.auth_error_unknown;
    }
}
