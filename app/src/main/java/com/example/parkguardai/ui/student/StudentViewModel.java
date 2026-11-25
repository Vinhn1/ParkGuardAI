package com.example.parkguardai.ui.student;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.parkguardai.data.model.StudentDashboardData;
import com.example.parkguardai.data.model.StudentNotificationItem;
import com.example.parkguardai.data.model.StudentParkingRecord;
import com.example.parkguardai.data.model.UserProfile;
import com.example.parkguardai.data.model.VehicleItem;
import com.example.parkguardai.data.model.ViolationItem;
import com.example.parkguardai.data.repository.StudentRepository;

import java.io.File;
import java.util.List;

/**
 * ViewModel chuẩn MVVM kết nối phân hệ Sinh viên với StudentRepository (Cloud Firestore thật).
 */
public class StudentViewModel extends ViewModel {

    private final StudentRepository repository;

    private final MutableLiveData<StudentDashboardData> dashboardData = new MutableLiveData<>();
    private final MutableLiveData<UserProfile> profile = new MutableLiveData<>();
    private final MutableLiveData<List<VehicleItem>> vehicles = new MutableLiveData<>();
    private final MutableLiveData<List<ViolationItem>> violations = new MutableLiveData<>();
    private final MutableLiveData<ViolationItem> violationDetail = new MutableLiveData<>();
    private final MutableLiveData<List<StudentNotificationItem>> notifications = new MutableLiveData<>();
    private final MutableLiveData<List<StudentParkingRecord>> parkingHistory = new MutableLiveData<>();

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    public StudentViewModel() {
        this.repository = StudentRepository.getInstance();
    }

    public LiveData<StudentDashboardData> getDashboardData() {
        return dashboardData;
    }

    public LiveData<UserProfile> getProfile() {
        return profile;
    }

    public LiveData<List<VehicleItem>> getVehicles() {
        return vehicles;
    }

    public LiveData<List<ViolationItem>> getViolations() {
        return violations;
    }

    public LiveData<ViolationItem> getViolationDetail() {
        return violationDetail;
    }

    public LiveData<List<StudentNotificationItem>> getNotifications() {
        return notifications;
    }

    public LiveData<List<StudentParkingRecord>> getParkingHistory() {
        return parkingHistory;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public void loadDashboard(@NonNull Context context) {
        isLoading.setValue(true);
        repository.getDashboardData(context).addOnCompleteListener(task -> {
            isLoading.setValue(false);
            if (task.isSuccessful() && task.getResult() != null) {
                dashboardData.setValue(task.getResult());
            }
        });
    }

    public void loadProfile(@NonNull Context context) {
        isLoading.setValue(true);
        repository.getMyProfile(context).addOnCompleteListener(task -> {
            isLoading.setValue(false);
            if (task.isSuccessful() && task.getResult() != null) {
                profile.setValue(task.getResult());
            }
        });
    }

    public void updateProfile(@NonNull String fullName, @NonNull String phone, Runnable onSuccess) {
        isLoading.setValue(true);
        repository.updateMyProfile(fullName, phone).addOnCompleteListener(task -> {
            isLoading.setValue(false);
            if (task.isSuccessful()) {
                toastMessage.setValue("Đã cập nhật thông tin hồ sơ thành công");
                if (onSuccess != null) onSuccess.run();
            } else {
                String err = task.getException() != null ? task.getException().getMessage() : "Lỗi kết nối";
                toastMessage.setValue("Không thể cập nhật hồ sơ: " + err);
            }
        });
    }

    public void loadVehicles(@NonNull Context context) {
        isLoading.setValue(true);
        repository.getMyVehicles(context).addOnCompleteListener(task -> {
            isLoading.setValue(false);
            if (task.isSuccessful() && task.getResult() != null) {
                vehicles.setValue(task.getResult());
            }
        });
    }

    public void loadViolations(@NonNull Context context) {
        isLoading.setValue(true);
        repository.getMyViolations(context).addOnCompleteListener(task -> {
            isLoading.setValue(false);
            if (task.isSuccessful() && task.getResult() != null) {
                violations.setValue(task.getResult());
            }
        });
    }

    public void loadViolationDetail(@NonNull String violationId) {
        isLoading.setValue(true);
        repository.getViolationById(violationId).addOnCompleteListener(task -> {
            isLoading.setValue(false);
            if (task.isSuccessful() && task.getResult() != null) {
                violationDetail.setValue(task.getResult());
            } else {
                toastMessage.setValue("Không thể tải chi tiết biên bản vi phạm");
            }
        });
    }

    public void loadNotifications(@NonNull Context context, String filter) {
        isLoading.setValue(true);
        repository.getNotifications(context, filter).addOnCompleteListener(task -> {
            isLoading.setValue(false);
            if (task.isSuccessful() && task.getResult() != null) {
                notifications.setValue(task.getResult());
            }
        });
    }

    public void loadParkingHistory(@NonNull Context context, int filterMode) {
        isLoading.setValue(true);
        repository.getParkingHistory(context, filterMode).addOnCompleteListener(task -> {
            isLoading.setValue(false);
            if (task.isSuccessful() && task.getResult() != null) {
                parkingHistory.setValue(task.getResult());
            }
        });
    }

    public void registerVehicle(@NonNull Context context,
                                @NonNull String plateRaw,
                                @NonNull String vehicleType,
                                String brand,
                                String color,
                                File cardFile,
                                File photoFile,
                                Runnable onSuccess) {
        isLoading.setValue(true);
        repository.registerVehicle(context, plateRaw, vehicleType, brand, color, cardFile, photoFile)
                .addOnCompleteListener(task -> {
                    isLoading.setValue(false);
                    if (task.isSuccessful()) {
                        toastMessage.setValue("Đã gửi hồ sơ đăng ký xe " + plateRaw + " tới Quản trị viên xét duyệt");
                        if (onSuccess != null) {
                            onSuccess.run();
                        }
                    } else {
                        String err = task.getException() != null ? task.getException().getMessage() : "Lỗi gửi đăng ký";
                        toastMessage.setValue("Không thể gửi đăng ký xe: " + err);
                    }
                });
    }

    public void submitComplaint(@NonNull Context context,
                                @NonNull String violationId,
                                @NonNull String reason,
                                Runnable onSuccess) {
        isLoading.setValue(true);
        repository.submitViolationComplaint(context, violationId, reason)
                .addOnCompleteListener(task -> {
                    isLoading.setValue(false);
                    if (task.isSuccessful()) {
                        toastMessage.setValue("Đã gửi khiếu nại vi phạm tới Ban quản lý bãi xe");
                        if (onSuccess != null) {
                            onSuccess.run();
                        }
                    } else {
                        String err = task.getException() != null ? task.getException().getMessage() : "Vui lòng thử lại";
                        toastMessage.setValue("Không thể gửi khiếu nại: " + err);
                    }
                });
    }

    public void respondNotification(@NonNull Context context,
                                    @NonNull String notifId,
                                    boolean isMe) {
        repository.respondToNotification(context, notifId, isMe)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (isMe) {
                            toastMessage.setValue("Đã xác nhận lượt vào hợp lệ là của bạn");
                        } else {
                            toastMessage.setValue("Đã gửi cảnh báo tới Bảo vệ: Nghi vấn xe bị mạo danh biển số");
                        }
                        loadNotifications(context, "ALL");
                    } else {
                        toastMessage.setValue("Không thể cập nhật phản hồi, vui lòng thử lại");
                    }
                });
    }
}
