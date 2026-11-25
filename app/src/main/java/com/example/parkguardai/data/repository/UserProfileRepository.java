package com.example.parkguardai.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.parkguardai.data.datasource.CloudinaryDataSource;
import com.example.parkguardai.data.model.EvidenceImage;
import com.example.parkguardai.data.model.UserProfile;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Repository quản lý hồ sơ cá nhân dùng chung cho cả 3 vai trò (Sinh viên, Bảo vệ, Quản trị viên):
 * đọc thông tin từ users/{uid}, tải ảnh đại diện lên Cloudinary và lưu cập nhật lên Cloud Firestore.
 */
public final class UserProfileRepository {

    private static volatile UserProfileRepository INSTANCE;

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final CloudinaryDataSource cloudinary;

    public static UserProfileRepository getInstance() {
        if (INSTANCE == null) {
            synchronized (UserProfileRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new UserProfileRepository();
                }
            }
        }
        return INSTANCE;
    }

    private UserProfileRepository() {
        this.auth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
        this.cloudinary = new CloudinaryDataSource();
    }

    /**
     * Đọc hồ sơ của người dùng đang đăng nhập từ Firestore users/{uid}.
     * Nếu tài liệu chưa tồn tại (ví dụ Admin/Bảo vệ tạo trực tiếp trong accessGrants), trả về hồ sơ cơ bản từ Firebase Auth.
     */
    public Task<UserProfile> getMyProfile() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return Tasks.forException(new SecurityException("Chưa đăng nhập tài khoản"));
        }
        String uid = user.getUid();
        return firestore.collection("users").document(uid).get().continueWith(task -> {
            UserProfile profile = new UserProfile();
            profile.setUid(uid);
            profile.setEmailCanonical(user.getEmail() != null ? user.getEmail() : "");

            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                DocumentSnapshot doc = task.getResult();
                String fn = doc.getString("fullName");
                profile.setFullName(fn != null && !fn.isEmpty() ? fn
                        : (user.getDisplayName() != null ? user.getDisplayName() : ""));
                String email = doc.getString("emailCanonical");
                if (email != null && !email.isEmpty()) profile.setEmailCanonical(email);
                profile.setPhoneCanonical(doc.getString("phoneCanonical"));
                profile.setStudentCode(doc.getString("studentCode"));
                profile.setStudentClass(doc.getString("studentClass"));
                profile.setStaffCode(doc.getString("staffCode"));
                profile.setAvatarUrl(doc.getString("avatarUrl"));
                profile.setAccountStatus(doc.getString("accountStatus"));
                profile.setCreatedAt(doc.getDate("createdAt"));
                profile.setUpdatedAt(doc.getDate("updatedAt"));
            } else {
                profile.setFullName(user.getDisplayName() != null ? user.getDisplayName() : "");
            }
            return profile;
        });
    }

    /**
     * Cập nhật hồ sơ cá nhân (Họ tên, SĐT, Lớp/Khoa, MSSV nếu bổ sung và Ảnh đại diện lên Cloudinary).
     */
    public Task<UserProfile> updateMyProfile(@NonNull String fullName,
                                             @Nullable String phone,
                                             @Nullable String studentClass,
                                             @Nullable String studentCode,
                                             @Nullable File avatarFile) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return Tasks.forException(new SecurityException("Chưa đăng nhập tài khoản"));
        }
        String uid = user.getUid();
        String cleanName = fullName.trim();
        String cleanPhone = phone != null ? phone.replaceAll("[\\s\\-.]", "").trim() : "";
        String cleanClass = studentClass != null ? studentClass.trim() : null;
        String cleanCode = studentCode != null ? studentCode.trim() : null;

        Task<EvidenceImage> uploadTask = (avatarFile != null && avatarFile.exists())
                ? cloudinary.uploadImage(avatarFile, "AVATAR", false)
                : Tasks.forResult(null);

        return uploadTask.onSuccessTask(uploaded -> {
            String newAvatarUrl = uploaded != null ? uploaded.secureUrl : null;
            DocumentReference docRef = firestore.collection("users").document(uid);

            return docRef.get().onSuccessTask(existing -> {
                Map<String, Object> updates = new HashMap<>();
                updates.put("fullName", cleanName);
                updates.put("phoneCanonical", cleanPhone);
                if (cleanClass != null) {
                    updates.put("studentClass", cleanClass);
                }
                if (cleanCode != null && !cleanCode.isEmpty()) {
                    updates.put("studentCode", cleanCode);
                }
                if (newAvatarUrl != null && !newAvatarUrl.isEmpty()) {
                    updates.put("avatarUrl", newAvatarUrl);
                }
                updates.put("updatedAt", FieldValue.serverTimestamp());

                Task<Void> writeTask;
                if (existing != null && existing.exists()) {
                    writeTask = docRef.update(updates);
                } else {
                    updates.put("emailCanonical", user.getEmail() != null ? user.getEmail() : "");
                    updates.put("accountStatus", "ACTIVE");
                    updates.put("createdAt", FieldValue.serverTimestamp());
                    updates.put("schemaVersion", 1);
                    writeTask = docRef.set(updates);
                }

                return writeTask.onSuccessTask(unused -> {
                    UserProfileChangeRequest req = new UserProfileChangeRequest.Builder()
                            .setDisplayName(cleanName)
                            .build();
                    return user.updateProfile(req).continueWithTask(t -> getMyProfile());
                });
            });
        });
    }
}
