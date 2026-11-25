package com.example.parkguardai.ui.auth;

import android.util.Log;

import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.parkguardai.R;
import com.example.parkguardai.data.datasource.FirestoreStudentOnboardingDataSource;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.io.File;

/** Quản lý trạng thái hoàn tất hồ sơ và gửi xe đầu tiên để duyệt. */
public class StudentOnboardingViewModel extends ViewModel {

    public static final class UiState {
        public final boolean loading;
        public final boolean saved;
        public final boolean error;
        @StringRes public final int messageRes;

        private UiState(boolean loading, boolean saved, boolean error,
                        @StringRes int messageRes) {
            this.loading = loading;
            this.saved = saved;
            this.error = error;
            this.messageRes = messageRes;
        }
    }

    private final FirestoreStudentOnboardingDataSource dataSource =
            new FirestoreStudentOnboardingDataSource(
                    FirebaseAuth.getInstance(), FirebaseFirestore.getInstance());
    private final MutableLiveData<UiState> uiState =
            new MutableLiveData<>(new UiState(false, false, false, 0));

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public void load() {
        UiState current = uiState.getValue();
        if (isLoading() || (current != null && current.saved)) return;
        uiState.setValue(new UiState(true, false, false, 0));
        dataSource.hasInitialRequest().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                boolean saved = Boolean.TRUE.equals(task.getResult());
                uiState.setValue(new UiState(false, saved, false,
                        saved ? R.string.onboarding_already_sent : 0));
            } else {
                uiState.setValue(new UiState(false, false, true,
                        errorMessage(task.getException())));
            }
        });
    }

    public void save(String fullName, String phoneCanonical, String plateRaw,
                     String plateCanonical, String vehicleType) {
        save(fullName, "", "", phoneCanonical, plateRaw, plateCanonical, vehicleType, "", "", "");
    }

    public void save(String fullName, String studentCode, String studentClass,
                     String phoneCanonical, String plateRaw, String plateCanonical,
                     String vehicleType, String brand, String color,
                     String registrationCardKey) {
        save(fullName, studentCode, studentClass, phoneCanonical, plateRaw, plateCanonical,
                vehicleType, brand, color, registrationCardKey, null, null);
    }

    public void save(String fullName, String studentCode, String studentClass,
                     String phoneCanonical, String plateRaw, String plateCanonical,
                     String vehicleType, String brand, String color,
                     String registrationCardKey, File registrationCardFile, File vehiclePhotoFile) {
        if (isLoading()) return;
        uiState.setValue(new UiState(true, false, false, 0));
        dataSource.saveInitialRequest(fullName, studentCode, studentClass, phoneCanonical,
                plateRaw, plateCanonical, vehicleType, brand, color,
                registrationCardKey, registrationCardFile, vehiclePhotoFile).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                uiState.setValue(new UiState(false, true, false,
                        Boolean.TRUE.equals(task.getResult())
                                ? R.string.onboarding_saved_pending
                                : R.string.onboarding_already_sent));
            } else {
                uiState.setValue(new UiState(false, false, true,
                        errorMessage(task.getException())));
            }
        });
    }

    private boolean isLoading() {
        UiState current = uiState.getValue();
        return current != null && current.loading;
    }

    @StringRes
    private int errorMessage(Exception exception) {
        if (exception != null) {
            String detail = exception instanceof FirebaseFirestoreException
                    ? ((FirebaseFirestoreException) exception).getCode().name()
                    : exception.getClass().getSimpleName();
            Log.e("StudentOnboarding", "Onboarding Firestore error: " + detail);
        }
        if (exception instanceof SecurityException) {
            return R.string.onboarding_email_required;
        }
        if (exception instanceof FirebaseNetworkException) return R.string.auth_error_network;
        if (exception instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) exception).getCode();
            if (code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                return R.string.auth_error_network;
            }
            if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return R.string.onboarding_permission_denied;
            }
            if (code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                return R.string.onboarding_incomplete_state;
            }
        }
        return R.string.onboarding_save_failed;
    }
}
