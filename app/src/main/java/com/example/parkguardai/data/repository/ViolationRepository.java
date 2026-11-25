package com.example.parkguardai.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.parkguardai.data.datasource.CloudinaryDataSource;
import com.example.parkguardai.data.datasource.FirestoreAccessGrantDataSource;
import com.example.parkguardai.data.model.EvidenceImage;
import com.example.parkguardai.data.model.GuardDraft;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.WriteBatch;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Điều phối ảnh Cloudinary và hồ sơ Firestore; chỉ báo xong sau xác nhận server. */
public final class ViolationRepository {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    public enum Stage { CHECKING_ACCESS, UPLOADING_ORIGINAL, UPLOADING_CROP, SAVING_RECORD, COMPLETE }
    public interface StageListener { void onStage(Stage stage); }

    private final GuardDraftStore store;
    private final CloudinaryDataSource cloudinary = new CloudinaryDataSource();
    private final FirestoreAccessGrantDataSource grants =
            new FirestoreAccessGrantDataSource(FirebaseFirestore.getInstance());
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public ViolationRepository(Context context) { store = new GuardDraftStore(context); }

    public Task<String> submit(GuardDraft draft, @Nullable StageListener listener) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || !user.isEmailVerified() || !user.getUid().equals(draft.ownerUid)
                || !draft.locationAccepted || draft.originalName == null
                || draft.typeCode.isEmpty() || draft.scopeId.isEmpty()
                || (draft.plate.isEmpty() && draft.note.trim().isEmpty())) {
            return Tasks.forException(new IllegalArgumentException("Hồ sơ vi phạm chưa đầy đủ thông tin hoặc thiếu định vị GPS hợp lệ"));
        }
        notifyStage(listener, Stage.CHECKING_ACCESS);
        return grants.getSnapshotFromServer(user.getUid()).onSuccessTask(grant -> {
            if (grant.access != FirestoreAccessGrantDataSource.Access.GUARD
                    || !grant.scopes.contains(draft.scopeId)) {
                return Tasks.forException(new SecurityException("Tài khoản không có quyền bảo vệ trong khu vực này"));
            }
            return uploadOriginal(draft, listener);
        }).onSuccessTask(ignored -> uploadCrop(draft, listener))
                .onSuccessTask(ignored -> fetchGuardName(user))
                .onSuccessTask(guardName -> resolveVerifiedOwnerAndSave(draft, guardName, listener))
                .onSuccessTask(id -> {
                    draft.sent = true;
                    try { store.save(draft); }
                    catch (IOException ignored) {
                        // Máy chủ đã xác nhận hồ sơ; lỗi cập nhật bản nháp không đảo kết quả.
                    }
                    notifyStage(listener, Stage.COMPLETE);
                    return Tasks.forResult(id);
                });
    }

    private void notifyStage(@Nullable StageListener listener, Stage stage) {
        if (listener != null) listener.onStage(stage);
    }

    private Task<String> fetchGuardName(FirebaseUser user) {
        return firestore.collection("users").document(user.getUid()).get()
                .continueWith(t -> {
                    if (t.isSuccessful() && t.getResult() != null && t.getResult().exists()) {
                        String fn = t.getResult().getString("fullName");
                        if (fn != null && !fn.isEmpty()) return fn;
                    }
                    return user.getEmail() != null ? user.getEmail() : user.getUid();
                });
    }

    private Task<Void> uploadOriginal(GuardDraft draft, @Nullable StageListener listener) {
        if (draft.originalUpload != null) return Tasks.forResult(null);
        notifyStage(listener, Stage.UPLOADING_ORIGINAL);
        File file = store.imageFile(draft.id, false);
        return cloudinary.uploadImage(file, "ORIGINAL", true).onSuccessTask(receipt -> {
            draft.originalUpload = receipt;
            store.save(draft);
            return Tasks.forResult(null);
        });
    }

    private Task<Void> uploadCrop(GuardDraft draft, @Nullable StageListener listener) {
        if (draft.cropName == null || draft.cropName.isEmpty() || draft.cropUpload != null) {
            return Tasks.forResult(null);
        }
        File cropFile = store.imageFile(draft.id, true);
        if (!cropFile.exists()) return Tasks.forResult(null);
        notifyStage(listener, Stage.UPLOADING_CROP);
        return cloudinary.uploadImage(cropFile, "PLATE_CROP", false)
                .onSuccessTask(receipt -> {
                    draft.cropUpload = receipt;
                    store.save(draft);
                    return Tasks.forResult(null);
                });
    }

    /**
     * Chỉ gắn ownerUid khi tìm thấy phương tiện đã được Quản trị viên xác minh (APPROVED/VERIFIED),
     * không tự gắn chủ xe cho phương tiện chưa xác minh.
     */
    private Task<String> resolveVerifiedOwnerAndSave(GuardDraft draft, String guardName,
                                                     @Nullable StageListener listener) {
        String plateCanonical = normalizePlate(draft.plate);
        if (plateCanonical.isEmpty() || draft.isBatch) {
            return saveRecord(draft, guardName, null, listener);
        }

        return firestore.collection("vehicles")
                .whereEqualTo("plateCanonical", plateCanonical)
                .limit(1)
                .get()
                .continueWithTask(vTask -> {
                    if (!vTask.isSuccessful() || vTask.getResult() == null || vTask.getResult().isEmpty()) {
                        return saveRecord(draft, guardName, null, listener);
                    }
                    DocumentSnapshot vDoc = vTask.getResult().getDocuments().get(0);
                    String vehicleId = vDoc.getId();
                    String createdBy = vDoc.getString("createdBy");
                    if (createdBy == null || createdBy.isEmpty()) {
                        return saveRecord(draft, guardName, null, listener);
                    }
                    return firestore.collection("vehicleVerificationRequests")
                            .whereEqualTo("vehicleId", vehicleId)
                            .limit(5)
                            .get()
                            .continueWithTask(rTask -> {
                                String verifiedOwnerUid = null;
                                if (rTask.isSuccessful() && rTask.getResult() != null) {
                                    for (DocumentSnapshot rDoc : rTask.getResult().getDocuments()) {
                                        String st = rDoc.getString("status");
                                        if ("APPROVED".equalsIgnoreCase(st) || "VERIFIED".equalsIgnoreCase(st)) {
                                            verifiedOwnerUid = createdBy;
                                            break;
                                        }
                                    }
                                }
                                return saveRecord(draft, guardName, verifiedOwnerUid, listener);
                            });
                });
    }

    private Task<String> saveRecord(GuardDraft draft, String guardName,
                                    @Nullable String verifiedOwnerUid,
                                    @Nullable StageListener listener) {
        notifyStage(listener, Stage.SAVING_RECORD);
        String id = draft.id;
        DocumentReference record = firestore.collection("violations").document(id);
        DocumentReference original = record.collection("evidence").document(id + "_primary");
        DocumentReference crop = record.collection("evidence").document(id + "_crop");
        if (draft.originalUpload == null) {
            return Tasks.forException(new IllegalStateException("Primary image missing"));
        }

        String plateCanonical = normalizePlate(draft.plate);
        Map<String, Object> data = new HashMap<>();
        data.put("guardUid", draft.ownerUid);
        data.put("guardName", guardName);
        data.put("scopeId", draft.scopeId);
        data.put("primaryEvidenceId", original.getId());
        data.put("primaryPublicId", draft.originalUpload.publicId);
        data.put("primaryEvidenceUrl", draft.originalUpload.secureUrl);
        data.put("plateFinal", plateCanonical.isEmpty() ? draft.plate.trim() : plateCanonical);
        data.put("plateCanonical", plateCanonical);
        data.put("typeCode", draft.typeCode);
        data.put("typeVersion", 1);
        data.put("status", "PENDING");
        data.put("note", draft.note != null ? draft.note : "");
        data.put("penaltyAmount", draft.penaltyAmount != null ? draft.penaltyAmount : "");
        if (verifiedOwnerUid != null && !verifiedOwnerUid.isEmpty()) {
            data.put("ownerUid", verifiedOwnerUid);
        }
        data.put("capturedAt", new Date(draft.capturedAtMillis));
        data.put("locationAt", new Date(draft.locationAtMillis));
        data.put("latitude", draft.latitude);
        data.put("longitude", draft.longitude);
        data.put("locationAccuracy", (double) draft.accuracyMeters);
        data.put("locationSource", "FUSED_AUTO");
        data.put("locationStatus", "AUTO_ACCEPTED");
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("schemaVersion", 1);

        return Tasks.call(IO, () -> {
            WriteBatch batch = firestore.batch();
            batch.set(record, data);
            batch.set(original, evidenceData(draft, draft.originalUpload,
                    original.getId(), store.imageFile(id, false)));
            if (draft.cropUpload != null) {
                File cropFile = store.imageFile(id, true);
                if (cropFile.exists()) {
                    batch.set(crop, evidenceData(draft, draft.cropUpload,
                            crop.getId(), cropFile));
                }
            }
            if (verifiedOwnerUid != null && !verifiedOwnerUid.isEmpty()) {
                DocumentReference notifRef = firestore.collection("notifications").document();
                Map<String, Object> notif = new HashMap<>();
                notif.put("recipientUid", verifiedOwnerUid);
                notif.put("title", "Ghi nhận vi phạm mới");
                notif.put("message", "Phương tiện " + draft.plate + " vừa bị ghi nhận vi phạm tại " + draft.scopeId
                        + ". Bạn có thể xem bằng chứng và gửi khiếu nại.");
                notif.put("type", "VIOLATION");
                notif.put("read", false);
                notif.put("requiresAction", false);
                notif.put("relatedViolationId", id);
                notif.put("createdAt", FieldValue.serverTimestamp());
                batch.set(notifRef, notif);
            }
            return batch;
        }).onSuccessTask(WriteBatch::commit).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException() != null ? task.getException() : new IllegalStateException("Commit failed");
            }
            return confirmRecord(record, draft);
        });
    }

    /**
     * Bổ sung ảnh bằng chứng phụ vào hồ sơ vi phạm hiện có trên Cloudinary & Firestore.
     */
    public Task<Void> supplementEvidence(@NonNull String violationId, @NonNull File newImageFile,
                                         double lat, double lng, float accuracy) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || !user.isEmailVerified()) {
            return Tasks.forException(new SecurityException("Chưa đăng nhập bảo vệ"));
        }
        return cloudinary.uploadImage(newImageFile, "SUPPLEMENTAL", false).onSuccessTask(receipt ->
                Tasks.call(IO, () -> {
                    String evidenceId = violationId + "_sup_" + UUID.randomUUID().toString().substring(0, 8);
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", evidenceId);
                    data.put("violationId", violationId);
                    data.put("createdBy", user.getUid());
                    data.put("secureUrl", receipt.secureUrl);
                    data.put("publicId", receipt.publicId);
                    data.put("uploadedAt", receipt.uploadedAt);
                    data.put("imageType", "SUPPLEMENTAL");
                    data.put("isPrimary", false);
                    data.put("capturedAt", new Date());
                    data.put("sha256", sha256(newImageFile));
                    data.put("mimeType", "image/jpeg");
                    data.put("sizeBytes", newImageFile.length());
                    data.put("sourceEvidenceId", violationId + "_primary");
                    data.put("schemaVersion", 1);
                    return firestore.collection("violations").document(violationId)
                            .collection("evidence").document(evidenceId).set(data);
                }).onSuccessTask(t -> t)
        );
    }

    private Task<String> confirmRecord(DocumentReference record, GuardDraft draft) {
        return record.get(Source.SERVER).continueWith(task -> {
            if (!task.isSuccessful()) throw task.getException();
            DocumentSnapshot snapshot = task.getResult();
            if (!snapshot.exists() || !draft.ownerUid.equals(snapshot.getString("guardUid"))
                    || !draft.originalUpload.publicId.equals(snapshot.getString("primaryPublicId"))) {
                throw new IllegalStateException("Record confirmation failed");
            }
            return draft.id;
        });
    }

    private Map<String, Object> evidenceData(GuardDraft draft, EvidenceImage image,
                                             String evidenceId, File file) throws IOException {
        Map<String, Object> data = new HashMap<>();
        data.put("id", evidenceId);
        data.put("violationId", draft.id);
        data.put("createdBy", draft.ownerUid);
        data.put("secureUrl", image.secureUrl);
        data.put("publicId", image.publicId);
        data.put("uploadedAt", image.uploadedAt);
        data.put("imageType", image.imageType);
        data.put("isPrimary", image.isPrimary);
        data.put("capturedAt", new Date(draft.capturedAtMillis));
        if (image.isPrimary) {
            data.put("locationAt", new Date(draft.locationAtMillis));
            data.put("latitude", draft.latitude);
            data.put("longitude", draft.longitude);
            data.put("locationAccuracy", (double) draft.accuracyMeters);
            data.put("locationSource", "FUSED_AUTO");
            data.put("locationStatus", "AUTO_ACCEPTED");
        }
        data.put("sha256", sha256(file));
        data.put("mimeType", "image/jpeg");
        data.put("sizeBytes", file.length());
        data.put("sourceEvidenceId", image.isPrimary ? "" : draft.id + "_primary");
        data.put("schemaVersion", 1);
        return data;
    }

    private String sha256(File file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = new byte[8192];
            try (FileInputStream input = new FileInputStream(file)) {
                int count;
                while ((count = input.read(bytes)) > 0) digest.update(bytes, 0, count);
            }
            StringBuilder hex = new StringBuilder();
            for (byte value : digest.digest()) hex.append(String.format("%02x", value & 255));
            return hex.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IOException(impossible);
        }
    }

    public Task<List<DocumentSnapshot>> recentForCurrentGuard() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return Tasks.forException(new SecurityException("Not signed in"));
        return grants.getSnapshotFromServer(user.getUid()).onSuccessTask(grant -> {
            if (grant.access != FirestoreAccessGrantDataSource.Access.GUARD
                    || grant.scopes.size() > 10) {
                return Tasks.forException(new SecurityException("Guard scope unavailable"));
            }
            return firestore.collection("violations").whereEqualTo("guardUid", user.getUid())
                    .whereIn("scopeId", grant.scopes).limit(50).get(Source.SERVER);
        }).continueWith(task -> {
            if (!task.isSuccessful()) throw task.getException();
            QuerySnapshot query = task.getResult();
            List<DocumentSnapshot> result = new ArrayList<>(query.getDocuments());
            result.sort(Comparator.comparing((DocumentSnapshot doc) -> {
                Date value = doc.getDate("capturedAt");
                return value == null ? new Date(0) : value;
            }).reversed());
            return result;
        });
    }

    public Task<DocumentSnapshot> getRecord(String id) {
        return firestore.collection("violations").document(id).get(Source.SERVER);
    }

    public Task<DocumentSnapshot> getPrimaryEvidence(String id) {
        return firestore.collection("violations").document(id).collection("evidence")
                .document(id + "_primary").get(Source.SERVER);
    }

    public Task<DocumentSnapshot> getCropEvidence(String id) {
        return firestore.collection("violations").document(id).collection("evidence")
                .document(id + "_crop").get(Source.SERVER);
    }

    private static String normalizePlate(@Nullable String raw) {
        if (raw == null) return "";
        return raw.replaceAll("[^a-zA-Z0-9]", "").toUpperCase(Locale.ROOT);
    }
}
