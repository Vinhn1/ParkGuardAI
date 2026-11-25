package com.example.parkguardai.data.datasource;

import androidx.annotation.NonNull;

import com.example.parkguardai.data.model.EvidenceImage;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Source;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Ghi hồ sơ sinh viên và yêu cầu xe đầu tiên bằng một giao dịch server. */
public class FirestoreStudentOnboardingDataSource {

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final CloudinaryDataSource cloudinary = new CloudinaryDataSource();

    public FirestoreStudentOnboardingDataSource(FirebaseAuth auth, FirebaseFirestore firestore) {
        this.auth = Objects.requireNonNull(auth, "auth");
        this.firestore = Objects.requireNonNull(firestore, "firestore");
    }

    /** Đọc yêu cầu của chính tài khoản từ server để phục hồi sau khi mở lại app. */
    public Task<Boolean> hasInitialRequest() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return Tasks.forException(new IllegalStateException());
        return user.reload().continueWithTask(reload -> {
            if (!reload.isSuccessful()) {
                return Tasks.forException(Objects.requireNonNull(reload.getException()));
            }
            if (!user.isEmailVerified()) {
                return Tasks.forException(new SecurityException("Email chưa xác minh"));
            }
            return user.getIdToken(true).continueWithTask(token -> {
                if (!token.isSuccessful()) {
                    return Tasks.forException(Objects.requireNonNull(token.getException()));
                }
                // Kiểm tra users/{uid} trước để tránh đọc document chưa tồn tại ở vehicleVerificationRequests
                return firestore.collection("users").document(user.getUid()).get(Source.SERVER)
                        .continueWithTask(profileTask -> {
                            if (!profileTask.isSuccessful()) {
                                throw Objects.requireNonNull(profileTask.getException());
                            }
                            DocumentSnapshot profile = profileTask.getResult();
                            if (profile == null || !profile.exists()) {
                                return Tasks.forResult(false);
                            }
                            return firstRequest(user.getUid()).get(Source.SERVER).continueWith(task -> {
                                if (!task.isSuccessful()) {
                                    throw Objects.requireNonNull(task.getException());
                                }
                                DocumentSnapshot snapshot = task.getResult();
                                return snapshot != null && snapshot.exists()
                                        && user.getUid().equals(snapshot.getString("requesterUid"));
                            });
                        });
            });
        });
    }

    /**
     * Chỉ tạo bản ghi tự khai PENDING; không tạo grant, link VERIFIED hay plate claim.
     * Giao dịch có ID ổn định nên thử lại sau lỗi mạng không tạo hồ sơ/xe thứ hai.
     */
    public Task<Boolean> saveInitialRequest(String fullName, String phoneCanonical,
                                            String plateRaw, String plateCanonical,
                                            String vehicleType) {
        return saveInitialRequest(fullName, "", "", phoneCanonical, plateRaw,
                plateCanonical, vehicleType, "", "", "");
    }

    /**
     * Ghi hồ sơ sinh viên đầy đủ (gồm MSSV, lớp, hãng xe, màu xe, ảnh cà vẹt & ảnh biển số lên Cloudinary) và yêu cầu xe.
     */
    public Task<Boolean> saveInitialRequest(String fullName, String studentCode,
                                            String studentClass, String phoneCanonical,
                                            String plateRaw, String plateCanonical,
                                            String vehicleType, String brand,
                                            String color, String registrationCardKey) {
        return saveInitialRequest(fullName, studentCode, studentClass, phoneCanonical,
                plateRaw, plateCanonical, vehicleType, brand, color, registrationCardKey, null, null);
    }

    public Task<Boolean> saveInitialRequest(String fullName, String studentCode,
                                            String studentClass, String phoneCanonical,
                                            String plateRaw, String plateCanonical,
                                            String vehicleType, String brand,
                                            String color, String registrationCardKey,
                                            File registrationCardFile, File vehiclePhotoFile) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return Tasks.forException(new IllegalStateException());
        return user.reload().continueWithTask(reload -> {
            if (!reload.isSuccessful()) {
                return Tasks.forException(Objects.requireNonNull(reload.getException()));
            }
            if (!user.isEmailVerified()) {
                return Tasks.forException(new SecurityException("Email chưa xác minh"));
            }
            // Làm mới ID token để Rules nhận email_verified hiện tại.
            return user.getIdToken(true).continueWithTask(token -> {
                if (!token.isSuccessful()) {
                    return Tasks.forException(Objects.requireNonNull(token.getException()));
                }
                Task<EvidenceImage> regUploadTask = registrationCardFile != null && registrationCardFile.exists()
                        ? cloudinary.uploadImage(registrationCardFile, "REGISTRATION_CARD", false)
                        : Tasks.forResult(null);
                Task<EvidenceImage> vehUploadTask = vehiclePhotoFile != null && vehiclePhotoFile.exists()
                        ? cloudinary.uploadImage(vehiclePhotoFile, "VEHICLE_PHOTO", false)
                        : Tasks.forResult(null);

                return Tasks.whenAll(regUploadTask, vehUploadTask).onSuccessTask(ignored -> {
                    String regUrl = regUploadTask.getResult() != null
                            ? regUploadTask.getResult().secureUrl
                            : (registrationCardKey != null ? registrationCardKey : "");
                    String vehUrl = vehUploadTask.getResult() != null
                            ? vehUploadTask.getResult().secureUrl : "";

                    String uid = user.getUid();
                    String email = user.getEmail();
                    if (email == null) return Tasks.forException(new IllegalStateException());
                    String safePhone = phoneCanonical == null ? ""
                            : phoneCanonical.replaceAll("[\\s\\-.]", "").trim();
                    DocumentReference profile = firestore.collection("users").document(uid);
                    DocumentReference vehicle = firstVehicle(uid);
                    DocumentReference request = firstRequest(uid);
                    return firestore.runTransaction(transaction -> {
                        // Chỉ đọc users/{uid} (cho phép đọc kể cả khi tài liệu chưa tồn tại),
                        // tránh gọi get() lên vehicles/first_{uid} và vehicleVerificationRequests/first_{uid} chưa tồn tại.
                        DocumentSnapshot profileSnapshot = transaction.get(profile);
                        if (profileSnapshot.exists()) {
                            return false;
                        }
                        transaction.set(profile, profileData(fullName, email, safePhone,
                                studentCode, studentClass));
                        transaction.set(vehicle, vehicleData(uid, plateRaw, plateCanonical, vehicleType,
                                brand, color, regUrl, vehUrl));
                        transaction.set(request, requestData(uid, vehicle.getId()));
                        return true;
                    });
                });
            });
        });
    }

    private DocumentReference firstVehicle(String uid) {
        return firestore.collection("vehicles").document("first_" + uid);
    }

    private DocumentReference firstRequest(String uid) {
        return firestore.collection("vehicleVerificationRequests").document("first_" + uid);
    }

    @NonNull
    private Map<String, Object> profileData(String fullName, String email,
                                            String phoneCanonical, String studentCode,
                                            String studentClass) {
        Map<String, Object> data = new HashMap<>();
        data.put("fullName", fullName);
        data.put("emailCanonical", email);
        data.put("phoneCanonical", phoneCanonical);
        if (studentCode != null && !studentCode.isEmpty()) {
            data.put("studentCode", studentCode);
        }
        if (studentClass != null && !studentClass.isEmpty()) {
            data.put("studentClass", studentClass);
        }
        data.put("accountStatus", "ONBOARDING_PENDING");
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("schemaVersion", 1);
        return data;
    }

    @NonNull
    private Map<String, Object> vehicleData(String uid, String plateRaw,
                                            String plateCanonical, String vehicleType,
                                            String brand, String color,
                                            String registrationCardKey, String vehiclePhotoKey) {
        Map<String, Object> data = new HashMap<>();
        data.put("plateRaw", plateRaw);
        data.put("plateCanonical", plateCanonical);
        data.put("plateNormVersion", 1);
        data.put("type", vehicleType);
        if (brand != null && !brand.isEmpty()) {
            data.put("brand", brand);
        }
        if (color != null && !color.isEmpty()) {
            data.put("color", color);
        }
        if (registrationCardKey != null && !registrationCardKey.isEmpty()) {
            data.put("registrationCardPhotoKey", registrationCardKey);
        }
        if (vehiclePhotoKey != null && !vehiclePhotoKey.isEmpty()) {
            data.put("photoObjectKey", vehiclePhotoKey);
        }
        data.put("createdBy", uid);
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("schemaVersion", 1);
        return data;
    }

    @NonNull
    private Map<String, Object> requestData(String uid, String vehicleId) {
        Map<String, Object> data = new HashMap<>();
        data.put("requesterUid", uid);
        data.put("vehicleId", vehicleId);
        data.put("requestType", "NEW_REGISTRATION");
        data.put("status", "PENDING");
        data.put("requestedAt", FieldValue.serverTimestamp());
        data.put("schemaVersion", 1);
        return data;
    }
}
