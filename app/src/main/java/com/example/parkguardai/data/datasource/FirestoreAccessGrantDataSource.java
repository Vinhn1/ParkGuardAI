package com.example.parkguardai.data.datasource;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/** Đọc quyền do người quản trị cấp từ máy chủ, không lấy vai trò từ dữ liệu thiết bị. */
public final class FirestoreAccessGrantDataSource {

    public enum Access { STUDENT, STUDENT_PENDING, GUARD, ADMIN, RESTRICTED }

    public static final class Snapshot {
        public final Access access;
        public final List<String> scopes;

        Snapshot(Access access, List<String> scopes) {
            this.access = access;
            this.scopes = scopes;
        }
    }

    private final FirebaseFirestore firestore;

    public FirestoreAccessGrantDataSource(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public Task<Access> getAccessFromServer(@NonNull String uid) {
        return getSnapshotFromServer(uid).continueWith(task -> {
            if (!task.isSuccessful()) {
                Exception failure = task.getException();
                throw failure == null ? new IllegalStateException("Grant read failed") : failure;
            }
            return task.getResult().access;
        });
    }

    public Task<Snapshot> getSnapshotFromServer(@NonNull String uid) {
        return firestore.collection("accessGrants").document(uid).get(Source.SERVER)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Exception failure = task.getException();
                        throw failure == null ? new IllegalStateException("Grant read failed") : failure;
                    }
                    DocumentSnapshot grant = task.getResult();
                    if (grant == null || !grant.exists()) {
                        // Sinh viên mới chỉ được tạo hồ sơ/xe PENDING của chính mình.
                        return firestore.collection("users").document(uid).get(Source.SERVER)
                                .continueWith(profileTask -> {
                                    if (!profileTask.isSuccessful()) {
                                        Exception failure = profileTask.getException();
                                        throw failure == null
                                                ? new IllegalStateException("Profile read failed") : failure;
                                    }
                                    DocumentSnapshot profile = profileTask.getResult();
                                    if (profile == null || !profile.exists()) {
                                        return new Snapshot(Access.STUDENT, Collections.emptyList());
                                    }
                                    String status = profile.getString("accountStatus");
                                    if ("BANNED".equalsIgnoreCase(status) || "DISABLED".equalsIgnoreCase(status)
                                            || "RESTRICTED".equalsIgnoreCase(status)) {
                                        return new Snapshot(Access.RESTRICTED, Collections.emptyList());
                                    }
                                    if ("ONBOARDING_PENDING".equalsIgnoreCase(status)) {
                                        return new Snapshot(Access.STUDENT_PENDING, Collections.emptyList());
                                    }
                                    return new Snapshot(Access.STUDENT, Collections.emptyList());
                                });
                    }
                    String role = grant.getString("role");
                    Boolean active = grant.getBoolean("active");
                    Object rawScopes = grant.get("guardScopes");
                    List<?> scopes = rawScopes instanceof List ? (List<?>) rawScopes : null;
                    Long version = grant.getLong("schemaVersion");
                    boolean validVersion = version == null || Long.valueOf(1L).equals(version);
                    if ("SECURITY_GUARD".equals(role) && Boolean.TRUE.equals(active)
                            && scopes != null && !scopes.isEmpty() && scopes.size() <= 10
                            && validVersion) {
                        List<String> safeScopes = new ArrayList<>();
                        for (Object scope : scopes) {
                            if (!(scope instanceof String) || ((String) scope).isEmpty()) {
                                return com.google.android.gms.tasks.Tasks.forResult(
                                        new Snapshot(Access.RESTRICTED, Collections.emptyList()));
                            }
                            safeScopes.add((String) scope);
                        }
                        return com.google.android.gms.tasks.Tasks.forResult(
                                new Snapshot(Access.GUARD, safeScopes));
                    }
                    Object rawPermissions = grant.get("adminPermissions");
                    List<?> permissions = rawPermissions instanceof List ? (List<?>) rawPermissions : null;
                    if ("ADMIN".equals(role) && Boolean.TRUE.equals(active) && validVersion) {
                        if (permissions != null) {
                            for (Object permission : permissions) {
                                if (!(permission instanceof String) || ((String) permission).isEmpty()) {
                                    return com.google.android.gms.tasks.Tasks.forResult(
                                            new Snapshot(Access.RESTRICTED, Collections.emptyList()));
                                }
                            }
                        }
                        return com.google.android.gms.tasks.Tasks.forResult(
                                new Snapshot(Access.ADMIN, Collections.emptyList()));
                    }
                    return com.google.android.gms.tasks.Tasks.forResult(
                            new Snapshot(Access.RESTRICTED, Collections.emptyList()));
                });
    }
}
