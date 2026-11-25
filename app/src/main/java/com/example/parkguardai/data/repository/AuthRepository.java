package com.example.parkguardai.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.parkguardai.data.datasource.FirebaseEmailAuthDataSource;
import com.example.parkguardai.data.datasource.FirestoreAccessGrantDataSource;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;

import java.util.Objects;

/**
 * Repository trung tâm quản lý danh tính, xác thực Firebase Authentication
 * và phân giải vai trò người dùng (ADMIN, SECURITY_GUARD, STUDENT) từ Cloud Firestore.
 */
public final class AuthRepository {

    public enum AuthRoleResult {
        ADMIN,
        SECURITY_GUARD,
        STUDENT_ACTIVE,
        STUDENT_PENDING,
        STUDENT_NEED_ONBOARDING,
        EMAIL_NOT_VERIFIED,
        RESTRICTED
    }

    private static volatile AuthRepository instance;

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final FirebaseEmailAuthDataSource authDataSource;
    private final FirestoreAccessGrantDataSource grantDataSource;

    public static AuthRepository getInstance() {
        if (instance == null) {
            synchronized (AuthRepository.class) {
                if (instance == null) {
                    instance = new AuthRepository(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance());
                }
            }
        }
        return instance;
    }

    public AuthRepository(@NonNull FirebaseAuth auth, @NonNull FirebaseFirestore firestore) {
        this.auth = Objects.requireNonNull(auth, "auth");
        this.firestore = Objects.requireNonNull(firestore, "firestore");
        this.authDataSource = new FirebaseEmailAuthDataSource(this.auth);
        this.grantDataSource = new FirestoreAccessGrantDataSource(this.firestore);
    }

    /**
     * Đăng ký tài khoản mới bằng Email và Mật khẩu.
     * Tự động gửi email xác minh sau khi tạo tài khoản thành công.
     */
    public Task<AuthResult> register(@NonNull String email, @NonNull String password) {
        return authDataSource.register(email, password).onSuccessTask(authResult -> {
            FirebaseUser user = authResult.getUser();
            if (user != null) {
                return user.sendEmailVerification().continueWith(task -> authResult);
            }
            return Tasks.forResult(authResult);
        });
    }

    /**
     * Gửi lại liên kết xác minh đến email hiện tại.
     */
    public Task<Void> sendEmailVerification() {
        return authDataSource.sendVerificationEmail();
    }

    /**
     * Yêu cầu gửi email đặt lại mật khẩu an toàn.
     */
    public Task<Void> sendPasswordReset(@NonNull String email) {
        return authDataSource.sendPasswordResetEmail(email);
    }

    /**
     * Đăng nhập bằng Email và Mật khẩu, sau đó tự động phân giải vai trò truy cập từ server.
     */
    public Task<AuthRoleResult> signIn(@NonNull String email, @NonNull String password) {
        return authDataSource.signIn(email, password).onSuccessTask(authResult -> {
            FirebaseUser user = authResult.getUser();
            if (user == null) {
                return Tasks.forException(new IllegalStateException("User not found after sign in"));
            }
            return resolveUserAccess(user);
        });
    }

    /**
     * Kiểm tra phiên đăng nhập hiện tại từ Firebase Auth và phân giải vai trò.
     * Trả về null nếu chưa có phiên đăng nhập.
     */
    public Task<AuthRoleResult> checkCurrentSession() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return Tasks.forResult(null);
        }
        return user.reload().continueWithTask(reloadTask -> {
            if (!reloadTask.isSuccessful()) {
                return Tasks.forException(Objects.requireNonNull(reloadTask.getException()));
            }
            return resolveUserAccess(user);
        });
    }

    /**
     * Phân giải quyền truy cập và vai trò cụ thể của tài khoản từ máy chủ.
     */
    public Task<AuthRoleResult> resolveUserAccess(@NonNull FirebaseUser user) {
        if (!user.isEmailVerified()) {
            return Tasks.forResult(AuthRoleResult.EMAIL_NOT_VERIFIED);
        }

        String uid = user.getUid();
        return user.getIdToken(true).onSuccessTask(ignored ->
                grantDataSource.getSnapshotFromServer(uid)).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Exception failure = task.getException();
                return Tasks.forException(failure == null ? new IllegalStateException("Access grant failed") : failure);
            }

            FirestoreAccessGrantDataSource.Snapshot snapshot = task.getResult();
            if (snapshot.access == FirestoreAccessGrantDataSource.Access.ADMIN) {
                return Tasks.forResult(AuthRoleResult.ADMIN);
            }
            if (snapshot.access == FirestoreAccessGrantDataSource.Access.GUARD) {
                return Tasks.forResult(AuthRoleResult.SECURITY_GUARD);
            }
            if (snapshot.access == FirestoreAccessGrantDataSource.Access.STUDENT_PENDING) {
                return Tasks.forResult(AuthRoleResult.STUDENT_PENDING);
            }
            if (snapshot.access == FirestoreAccessGrantDataSource.Access.RESTRICTED) {
                return Tasks.forResult(AuthRoleResult.RESTRICTED);
            }

            // Đối với Student: Kiểm tra xem đã tạo hồ sơ người dùng trong Firestore chưa
            return firestore.collection("users").document(uid).get(Source.SERVER).continueWith(profileTask -> {
                if (!profileTask.isSuccessful() || !profileTask.getResult().exists()) {
                    return AuthRoleResult.STUDENT_NEED_ONBOARDING;
                }
                DocumentSnapshot doc = profileTask.getResult();
                String status = doc.getString("accountStatus");
                if ("ONBOARDING_PENDING".equalsIgnoreCase(status)) {
                    return AuthRoleResult.STUDENT_PENDING;
                }
                return AuthRoleResult.STUDENT_ACTIVE;
            });
        });
    }

    /**
     * Đăng xuất phiên làm việc hiện tại trên Firebase Auth.
     */
    public void signOut(@Nullable Context context) {
        authDataSource.signOut();
    }

    @Nullable
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public boolean isCurrentEmailVerified() {
        return authDataSource.isCurrentEmailVerified();
    }
}
