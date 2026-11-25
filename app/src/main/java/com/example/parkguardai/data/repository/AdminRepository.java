package com.example.parkguardai.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.parkguardai.data.model.AdminDashboardKPIs;
import com.example.parkguardai.data.model.AdminUserItem;
import com.example.parkguardai.data.model.ScanRuleConfig;
import com.example.parkguardai.data.model.VehicleItem;
import com.example.parkguardai.data.model.VehicleVerificationItem;
import com.example.parkguardai.data.model.ViolationItem;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Repository trung tâm quản lý dữ liệu phân hệ Quản trị viên (Admin Portal).
 * Kết nối 100% Cloud Firestore & Firebase Authentication, không dùng dữ liệu mock cục bộ.
 */
public final class AdminRepository {

    public interface AdminDataListener {
        void onAdminDataChanged();
    }

    private static final String SECONDARY_APP_NAME = "ParkGuardSecondaryAuth";
    private static volatile AdminRepository INSTANCE;

    private final Context context;
    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    private final List<AdminDataListener> listeners = new CopyOnWriteArrayList<>();
    private ListenerRegistration violationsReg;
    private ListenerRegistration requestsReg;
    private ListenerRegistration usersReg;

    public static AdminRepository getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (AdminRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AdminRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    private AdminRepository(Context context) {
        this.context = context;
        this.auth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }

    public void addDashboardListener(@Nullable AdminDataListener listener) {
        if (listener == null) return;
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
        ensureRealtimeListeners();
    }

    public void removeDashboardListener(@Nullable AdminDataListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
        if (listeners.isEmpty()) {
            stopRealtimeListeners();
        }
    }

    private void ensureRealtimeListeners() {
        if (auth.getCurrentUser() == null) return;
        if (violationsReg == null) {
            violationsReg = firestore.collection("violations").limit(50)
                    .addSnapshotListener((s, e) -> { if (e == null) notifyListeners(); });
        }
        if (requestsReg == null) {
            requestsReg = firestore.collection("vehicleVerificationRequests").limit(50)
                    .addSnapshotListener((s, e) -> { if (e == null) notifyListeners(); });
        }
        if (usersReg == null) {
            usersReg = firestore.collection("users").limit(100)
                    .addSnapshotListener((s, e) -> { if (e == null) notifyListeners(); });
        }
    }

    private void stopRealtimeListeners() {
        if (violationsReg != null) { violationsReg.remove(); violationsReg = null; }
        if (requestsReg != null) { requestsReg.remove(); requestsReg = null; }
        if (usersReg != null) { usersReg.remove(); usersReg = null; }
    }

    private void notifyListeners() {
        for (AdminDataListener l : listeners) {
            try { l.onAdminDataChanged(); } catch (Exception ignored) { }
        }
    }

    /**
     * Tải tên hiển thị thật của Quản trị viên đang đăng nhập từ users/{uid} hoặc Firebase Auth.
     */
    public Task<String> getCurrentAdminName() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return Tasks.forResult("Quản trị viên");
        return firestore.collection("users").document(user.getUid()).get().continueWith(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                String fn = task.getResult().getString("fullName");
                if (fn != null && !fn.isEmpty()) return fn;
            }
            if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                return user.getDisplayName();
            }
            return user.getEmail() != null ? user.getEmail() : "Quản trị viên";
        });
    }

    /**
     * Tải 4 chỉ số KPI tổng quan thật từ Cloud Firestore.
     */
    public Task<AdminDashboardKPIs> getDashboardKPIs() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfToday = cal.getTimeInMillis();

        Task<QuerySnapshot> violationsTask = firestore.collection("violations").limit(200).get();
        Task<QuerySnapshot> parkingTask = firestore.collection("parkingHistory").limit(200).get();
        Task<QuerySnapshot> grantsTask = firestore.collection("accessGrants").limit(100).get();
        Task<QuerySnapshot> requestsTask = firestore.collection("vehicleVerificationRequests").limit(200).get();

        return Tasks.whenAllComplete(violationsTask, parkingTask, grantsTask, requestsTask).continueWith(all -> {
            int violationsToday = 0;
            if (violationsTask.isSuccessful() && violationsTask.getResult() != null) {
                for (DocumentSnapshot d : violationsTask.getResult().getDocuments()) {
                    Date cap = d.getDate("capturedAt");
                    if (cap == null) cap = d.getDate("createdAt");
                    if (cap != null && cap.getTime() >= startOfToday) violationsToday++;
                }
            }

            // Đếm số xe đang ở trong bãi dựa trên sự kiện mới nhất của mỗi biển số trong parkingHistory
            Map<String, DocumentSnapshot> latestByPlate = new HashMap<>();
            if (parkingTask.isSuccessful() && parkingTask.getResult() != null) {
                for (DocumentSnapshot d : parkingTask.getResult().getDocuments()) {
                    String plate = d.getString("plateCanonical");
                    if (plate == null || plate.isEmpty()) continue;
                    Date ts = d.getDate("timestamp");
                    if (ts == null) ts = d.getDate("createdAt");
                    long time = ts != null ? ts.getTime() : 0L;
                    DocumentSnapshot prev = latestByPlate.get(plate);
                    if (prev == null) {
                        latestByPlate.put(plate, d);
                    } else {
                        Date prevTs = prev.getDate("timestamp");
                        if (prevTs == null) prevTs = prev.getDate("createdAt");
                        if (time > (prevTs != null ? prevTs.getTime() : 0L)) {
                            latestByPlate.put(plate, d);
                        }
                    }
                }
            }
            int vehiclesInLot = 0;
            for (DocumentSnapshot latest : latestByPlate.values()) {
                if ("ENTRY".equalsIgnoreCase(latest.getString("actionType"))
                        || "IN_LOT".equalsIgnoreCase(latest.getString("status"))) {
                    vehiclesInLot++;
                }
            }

            // Số bảo vệ/chốt trực đang hoạt động trên tổng số bảo vệ được cấp quyền
            int activeGuards = 0;
            int totalGuards = 0;
            if (grantsTask.isSuccessful() && grantsTask.getResult() != null) {
                for (DocumentSnapshot g : grantsTask.getResult().getDocuments()) {
                    if ("SECURITY_GUARD".equalsIgnoreCase(g.getString("role"))) {
                        totalGuards++;
                        if (Boolean.TRUE.equals(g.getBoolean("active"))) activeGuards++;
                    }
                }
            }
            String guardNodesRatio = activeGuards + "/" + totalGuards;

            // Số yêu cầu xe đang chờ duyệt
            int pendingVehicles = 0;
            if (requestsTask.isSuccessful() && requestsTask.getResult() != null) {
                for (DocumentSnapshot r : requestsTask.getResult().getDocuments()) {
                    if ("PENDING".equalsIgnoreCase(r.getString("status"))) pendingVehicles++;
                }
            }

            return new AdminDashboardKPIs(vehiclesInLot, violationsToday, guardNodesRatio, pendingVehicles);
        });
    }

    /**
     * Tính toán lưu lượng hoạt động 7 ngày trong tuần hiện tại (T2 -> CN) từ Firestore.
     */
    public Task<int[]> getWeeklyTraffic() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        int offsetFromMonday = (dow == Calendar.SUNDAY) ? 6 : (dow - Calendar.MONDAY);
        cal.add(Calendar.DAY_OF_YEAR, -offsetFromMonday);
        long startOfWeek = cal.getTimeInMillis();

        Task<QuerySnapshot> parkTask = firestore.collection("parkingHistory").limit(300).get();
        Task<QuerySnapshot> vioTask = firestore.collection("violations").limit(200).get();

        return Tasks.whenAllComplete(parkTask, vioTask).continueWith(all -> {
            int[] traffic = new int[7];
            Calendar itemCal = Calendar.getInstance();

            if (parkTask.isSuccessful() && parkTask.getResult() != null) {
                for (DocumentSnapshot doc : parkTask.getResult().getDocuments()) {
                    Date d = doc.getDate("timestamp");
                    if (d == null) d = doc.getDate("createdAt");
                    if (d != null && d.getTime() >= startOfWeek) {
                        itemCal.setTime(d);
                        int idx = itemCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                                ? 6 : (itemCal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY);
                        if (idx >= 0 && idx < 7) traffic[idx]++;
                    }
                }
            }
            if (vioTask.isSuccessful() && vioTask.getResult() != null) {
                for (DocumentSnapshot doc : vioTask.getResult().getDocuments()) {
                    Date d = doc.getDate("capturedAt");
                    if (d == null) d = doc.getDate("createdAt");
                    if (d != null && d.getTime() >= startOfWeek) {
                        itemCal.setTime(d);
                        int idx = itemCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                                ? 6 : (itemCal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY);
                        if (idx >= 0 && idx < 7) traffic[idx]++;
                    }
                }
            }
            return traffic;
        });
    }

    /**
     * Nhận định điều hành dựa trên số liệu thực tế từ Firestore.
     */
    public String getAiSecurityInsight(int violationsCount, int pendingVehiclesCount) {
        if (violationsCount >= 3) {
            return "Cảnh báo cao điểm: Ghi nhận " + violationsCount + " phương tiện vi phạm hôm nay. Khuyến nghị tăng cường tuần tra tại khu vực trọng điểm.";
        } else if (violationsCount > 0) {
            return "Ghi nhận " + violationsCount + " trường hợp vi phạm mới trong ngày. Hồ sơ và bằng chứng hiện trường đã đồng bộ lên hệ thống.";
        } else if (pendingVehiclesCount > 0) {
            return "Hiện có " + pendingVehiclesCount + " hồ sơ đăng ký xe đang chờ xét duyệt. Vui lòng kiểm tra hàng đợi duyệt xe.";
        } else {
            return "Tình hình an ninh trật tự bãi xe hiện tại ổn định. Không có vi phạm phát sinh trong ngày.";
        }
    }

    /**
     * Lấy danh sách yêu cầu xác minh xe thật từ Firestore (vehicleVerificationRequests + vehicles + users).
     */
    public Task<List<VehicleVerificationItem>> getVerificationRequests(
            @Nullable String filterStatus, @Nullable String searchQuery) {
        Task<QuerySnapshot> reqTask = firestore.collection("vehicleVerificationRequests").limit(200).get();
        Task<QuerySnapshot> vehTask = firestore.collection("vehicles").limit(200).get();
        Task<QuerySnapshot> userTask = firestore.collection("users").limit(200).get();

        return Tasks.whenAllComplete(reqTask, vehTask, userTask).continueWith(all -> {
            Map<String, DocumentSnapshot> vehMap = new HashMap<>();
            if (vehTask.isSuccessful() && vehTask.getResult() != null) {
                for (DocumentSnapshot d : vehTask.getResult().getDocuments()) {
                    vehMap.put(d.getId(), d);
                }
            }
            Map<String, DocumentSnapshot> userMap = new HashMap<>();
            if (userTask.isSuccessful() && userTask.getResult() != null) {
                for (DocumentSnapshot d : userTask.getResult().getDocuments()) {
                    userMap.put(d.getId(), d);
                }
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN"));
            String queryClean = searchQuery != null ? searchQuery.trim().toLowerCase(Locale.ROOT) : "";
            List<VehicleVerificationItem> filtered = new ArrayList<>();

            if (reqTask.isSuccessful() && reqTask.getResult() != null) {
                for (DocumentSnapshot rDoc : reqTask.getResult().getDocuments()) {
                    String reqId = rDoc.getId();
                    String vehicleId = rDoc.getString("vehicleId");
                    String requesterUid = rDoc.getString("requesterUid");
                    String status = rDoc.getString("status");
                    if (status == null) status = "PENDING";
                    String reason = rDoc.getString("reason");
                    Date requestedAt = rDoc.getDate("requestedAt");
                    long reqMillis = requestedAt != null ? requestedAt.getTime() : 0L;

                    DocumentSnapshot vDoc = vehicleId != null ? vehMap.get(vehicleId) : null;
                    DocumentSnapshot uDoc = requesterUid != null ? userMap.get(requesterUid) : null;

                    String plateRaw = vDoc != null ? vDoc.getString("plateRaw") : "";
                    String plateCanonical = vDoc != null ? vDoc.getString("plateCanonical") : "";
                    if (plateRaw == null || plateRaw.isEmpty()) plateRaw = plateCanonical != null ? plateCanonical : "Chưa rõ";
                    String vType = vDoc != null ? vDoc.getString("type") : "MOTORBIKE";
                    String brand = vDoc != null ? vDoc.getString("brand") : "";
                    String color = vDoc != null ? vDoc.getString("color") : "";
                    String regUrl = vDoc != null ? vDoc.getString("registrationCardPhotoKey") : "";
                    String photoUrl = vDoc != null ? vDoc.getString("photoObjectKey") : "";
                    String modelDisplay = (brand != null && !brand.isEmpty())
                            ? brand : StudentRepository.vehicleTypeLabel(vType);

                    String studentName = uDoc != null && uDoc.getString("fullName") != null
                            ? uDoc.getString("fullName") : "Sinh viên";
                    String studentCode = uDoc != null && uDoc.getString("studentCode") != null
                            ? uDoc.getString("studentCode") : (requesterUid != null ? requesterUid : "");

                    VehicleVerificationItem item = new VehicleVerificationItem(
                            reqId,
                            vehicleId != null ? vehicleId : "",
                            requesterUid != null ? requesterUid : "",
                            studentName,
                            studentCode,
                            plateRaw,
                            plateCanonical != null ? plateCanonical : "",
                            vType != null ? vType : "MOTORBIKE",
                            modelDisplay,
                            color != null ? color : "",
                            requestedAt != null ? sdf.format(requestedAt) : "",
                            reqMillis,
                            status,
                            reason != null ? reason : "",
                            regUrl != null ? regUrl : "",
                            photoUrl != null ? photoUrl : ""
                    );

                    boolean matchesFilter = true;
                    if ("PENDING".equalsIgnoreCase(filterStatus)) {
                        matchesFilter = item.isPending();
                    } else if ("APPROVED".equalsIgnoreCase(filterStatus)) {
                        matchesFilter = item.isApproved();
                    } else if ("BLOCKED".equalsIgnoreCase(filterStatus)) {
                        matchesFilter = item.isBlocked();
                    }

                    boolean matchesSearch = true;
                    if (!queryClean.isEmpty()) {
                        matchesSearch = item.plateNumber.toLowerCase(Locale.ROOT).contains(queryClean)
                                || item.studentName.toLowerCase(Locale.ROOT).contains(queryClean)
                                || item.studentCode.toLowerCase(Locale.ROOT).contains(queryClean);
                    }

                    if (matchesFilter && matchesSearch) {
                        filtered.add(item);
                    }
                }
            }
            filtered.sort((a, b) -> Long.compare(b.requestedAtMillis, a.requestedAtMillis));
            return filtered;
        });
    }

    /**
     * Phê duyệt hoặc từ chối yêu cầu xác minh xe trực tiếp trên Cloud Firestore
     * kèm cập nhật vehicleLinks, kích hoạt tài khoản sinh viên, gửi thông báo và ghi auditLogs.
     */
    public Task<Boolean> reviewVehicleRequest(@NonNull String requestId, boolean isApproved, @Nullable String reason) {
        FirebaseUser admin = auth.getCurrentUser();
        if (admin == null) {
            return Tasks.forException(new SecurityException("Chưa đăng nhập Quản trị viên"));
        }

        DocumentReference reqRef = firestore.collection("vehicleVerificationRequests").document(requestId);
        return reqRef.get().onSuccessTask(reqDoc -> {
            if (!reqDoc.exists()) {
                return Tasks.forException(new IllegalStateException("Không tìm thấy yêu cầu xác minh " + requestId));
            }
            String requesterUid = reqDoc.getString("requesterUid");
            String vehicleId = reqDoc.getString("vehicleId");

            WriteBatch batch = firestore.batch();

            Map<String, Object> reqUpdate = new HashMap<>();
            reqUpdate.put("status", isApproved ? "APPROVED" : "REJECTED");
            reqUpdate.put("reviewedAt", FieldValue.serverTimestamp());
            reqUpdate.put("reviewerUid", admin.getUid());
            if (reason != null && !reason.trim().isEmpty()) {
                reqUpdate.put("reason", reason.trim());
            }
            batch.update(reqRef, reqUpdate);

            if (requesterUid != null && !requesterUid.isEmpty() && vehicleId != null && !vehicleId.isEmpty()) {
                DocumentReference linkRef = firestore.collection("vehicleLinks").document(requestId);
                Map<String, Object> linkData = new HashMap<>();
                linkData.put("uid", requesterUid);
                linkData.put("vehicleId", vehicleId);
                linkData.put("status", isApproved ? "VERIFIED" : "REJECTED");
                linkData.put("verificationRequestId", requestId);
                linkData.put("reviewerUid", admin.getUid());
                linkData.put("reviewedAt", FieldValue.serverTimestamp());
                if (isApproved) {
                    linkData.put("validFrom", FieldValue.serverTimestamp());
                }
                linkData.put("schemaVersion", 1);
                batch.set(linkRef, linkData, SetOptions.merge());

                if (isApproved) {
                    Map<String, Object> userUpdate = new HashMap<>();
                    userUpdate.put("accountStatus", "ACTIVE");
                    batch.set(firestore.collection("users").document(requesterUid), userUpdate, SetOptions.merge());
                }

                DocumentReference notifRef = firestore.collection("notifications").document();
                Map<String, Object> notif = new HashMap<>();
                notif.put("recipientUid", requesterUid);
                notif.put("type", "VEHICLE");
                notif.put("title", isApproved ? "Phương tiện đã được phê duyệt" : "Yêu cầu đăng ký xe bị từ chối");
                notif.put("message", isApproved
                        ? "Hồ sơ đăng ký xe của bạn đã được Quản trị viên phê duyệt thành công."
                        : "Hồ sơ đăng ký xe chưa đạt yêu cầu" + (reason != null && !reason.isEmpty() ? ": " + reason : "."));
                notif.put("read", false);
                notif.put("requiresAction", false);
                notif.put("createdAt", FieldValue.serverTimestamp());
                batch.set(notifRef, notif);
            }

            appendAudit(batch, admin.getUid(), isApproved ? "APPROVE_VEHICLE" : "REJECT_VEHICLE",
                    "vehicleVerificationRequests/" + requestId, reason);

            return batch.commit().continueWith(t -> {
                if (!t.isSuccessful()) throw Objects.requireNonNull(t.getException());
                notifyListeners();
                return true;
            });
        });
    }

    /**
     * Lấy toàn bộ danh mục vi phạm thật từ Cloud Firestore kèm bộ lọc.
     */
    public Task<List<ViolationItem>> getAllViolations(
            @Nullable String filterStatus, @Nullable String searchQuery) {
        return firestore.collection("violations")
                .limit(200)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        throw task.getException() != null
                                ? task.getException() : new IllegalStateException("Không tải được danh sách vi phạm");
                    }
                    String queryClean = searchQuery != null ? searchQuery.trim().toLowerCase(Locale.ROOT) : "";
                    List<ViolationItem> filtered = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        ViolationItem vio = StudentRepository.mapViolation(doc, "");

                        boolean matchesFilter = true;
                        if ("UNPROCESSED".equalsIgnoreCase(filterStatus)) {
                            matchesFilter = vio.isUnprocessed();
                        } else if ("REVIEWING".equalsIgnoreCase(filterStatus)) {
                            matchesFilter = vio.isAppealed();
                        } else if ("PROCESSED".equalsIgnoreCase(filterStatus)) {
                            matchesFilter = vio.isProcessed();
                        }

                        boolean matchesSearch = true;
                        if (!queryClean.isEmpty()) {
                            matchesSearch = vio.plateNumber.toLowerCase(Locale.ROOT).contains(queryClean)
                                    || vio.id.toLowerCase(Locale.ROOT).contains(queryClean)
                                    || vio.violationName.toLowerCase(Locale.ROOT).contains(queryClean);
                        }

                        if (matchesFilter && matchesSearch) {
                            filtered.add(vio);
                        }
                    }
                    filtered.sort((a, b) -> Long.compare(b.capturedAtMillis, a.capturedAtMillis));
                    return filtered;
                });
    }

    /**
     * Tải chi tiết 1 biên bản vi phạm từ Firestore kèm ảnh chứng cứ chính.
     */
    public Task<ViolationItem> getViolationById(@NonNull String violationId) {
        return StudentRepository.getInstance().getViolationById(violationId);
    }

    /**
     * Xác nhận đã thu phạt và đóng biên bản vi phạm trên Cloud Firestore.
     */
    public Task<Boolean> confirmViolationPayment(@NonNull String violationId) {
        FirebaseUser admin = auth.getCurrentUser();
        if (admin == null) {
            return Tasks.forException(new SecurityException("Chưa đăng nhập Quản trị viên"));
        }

        DocumentReference vRef = firestore.collection("violations").document(violationId);
        return vRef.get().onSuccessTask(doc -> {
            WriteBatch batch = firestore.batch();
            Map<String, Object> update = new HashMap<>();
            update.put("status", "RESOLVED");
            update.put("paidAt", FieldValue.serverTimestamp());
            update.put("resolvedByUid", admin.getUid());
            batch.update(vRef, update);

            String ownerUid = doc.exists() ? doc.getString("ownerUid") : null;
            if (ownerUid != null && !ownerUid.isEmpty()) {
                DocumentReference notifRef = firestore.collection("notifications").document();
                Map<String, Object> notif = new HashMap<>();
                notif.put("recipientUid", ownerUid);
                notif.put("type", "VIOLATION");
                notif.put("title", "Biên bản vi phạm đã hoàn tất xử lý");
                notif.put("message", "Biên bản #" + violationId + " đã được Quản trị viên xác nhận hoàn tất xử lý.");
                notif.put("relatedViolationId", violationId);
                notif.put("read", false);
                notif.put("requiresAction", false);
                notif.put("createdAt", FieldValue.serverTimestamp());
                batch.set(notifRef, notif);
            }

            appendAudit(batch, admin.getUid(), "RESOLVE_VIOLATION", "violations/" + violationId, "Xác nhận nộp phạt");
            return batch.commit().continueWith(t -> {
                if (!t.isSuccessful()) throw Objects.requireNonNull(t.getException());
                notifyListeners();
                return true;
            });
        });
    }

    /**
     * Hủy biên bản vi phạm trên Cloud Firestore kèm lý do.
     */
    public Task<Boolean> cancelViolation(@NonNull String violationId, @Nullable String reason) {
        FirebaseUser admin = auth.getCurrentUser();
        if (admin == null) {
            return Tasks.forException(new SecurityException("Chưa đăng nhập Quản trị viên"));
        }

        DocumentReference vRef = firestore.collection("violations").document(violationId);
        return vRef.get().onSuccessTask(doc -> {
            WriteBatch batch = firestore.batch();
            Map<String, Object> update = new HashMap<>();
            update.put("status", "CANCELLED");
            update.put("cancelledAt", FieldValue.serverTimestamp());
            update.put("cancelledByUid", admin.getUid());
            if (reason != null) update.put("cancelReason", reason);
            batch.update(vRef, update);

            String ownerUid = doc.exists() ? doc.getString("ownerUid") : null;
            if (ownerUid != null && !ownerUid.isEmpty()) {
                DocumentReference notifRef = firestore.collection("notifications").document();
                Map<String, Object> notif = new HashMap<>();
                notif.put("recipientUid", ownerUid);
                notif.put("type", "VIOLATION");
                notif.put("title", "Biên bản vi phạm đã được hủy");
                notif.put("message", "Biên bản #" + violationId + " đã được hủy bỏ"
                        + (reason != null && !reason.isEmpty() ? ": " + reason : "."));
                notif.put("relatedViolationId", violationId);
                notif.put("read", false);
                notif.put("requiresAction", false);
                notif.put("createdAt", FieldValue.serverTimestamp());
                batch.set(notifRef, notif);
            }

            appendAudit(batch, admin.getUid(), "CANCEL_VIOLATION", "violations/" + violationId, reason);
            return batch.commit().continueWith(t -> {
                if (!t.isSuccessful()) throw Objects.requireNonNull(t.getException());
                notifyListeners();
                return true;
            });
        });
    }

    /**
     * Lấy danh sách người dùng và bảo vệ thật từ Firestore (users + accessGrants + vehicles).
     */
    public Task<List<AdminUserItem>> getUsersAndGuards(
            @Nullable String roleFilter, @Nullable String searchQuery) {
        return getUsersAndGuards(roleFilter, searchQuery, null);
    }

    public Task<List<AdminUserItem>> getUsersAndGuards(
            @Nullable String roleFilter, @Nullable String searchQuery, @Nullable String statusFilter) {
        Task<QuerySnapshot> usersTask = firestore.collection("users").limit(200).get();
        Task<QuerySnapshot> grantsTask = firestore.collection("accessGrants").limit(200).get();
        Task<QuerySnapshot> vehiclesTask = firestore.collection("vehicles").limit(200).get();

        return Tasks.whenAllComplete(usersTask, grantsTask, vehiclesTask).continueWith(all -> {
            Map<String, DocumentSnapshot> userDocs = new HashMap<>();
            if (usersTask.isSuccessful() && usersTask.getResult() != null) {
                for (DocumentSnapshot d : usersTask.getResult().getDocuments()) {
                    userDocs.put(d.getId(), d);
                }
            }
            Map<String, DocumentSnapshot> grantDocs = new HashMap<>();
            if (grantsTask.isSuccessful() && grantsTask.getResult() != null) {
                for (DocumentSnapshot d : grantsTask.getResult().getDocuments()) {
                    grantDocs.put(d.getId(), d);
                }
            }
            Map<String, String> plateByUser = new HashMap<>();
            if (vehiclesTask.isSuccessful() && vehiclesTask.getResult() != null) {
                for (DocumentSnapshot d : vehiclesTask.getResult().getDocuments()) {
                    String cb = d.getString("createdBy");
                    String plate = d.getString("plateRaw");
                    if (cb != null && plate != null && !plateByUser.containsKey(cb)) {
                        plateByUser.put(cb, plate);
                    }
                }
            }

            Set<String> allUids = new HashSet<>();
            allUids.addAll(userDocs.keySet());
            allUids.addAll(grantDocs.keySet());

            String queryClean = searchQuery != null ? searchQuery.trim().toLowerCase(Locale.ROOT) : "";
            List<AdminUserItem> filtered = new ArrayList<>();

            for (String uid : allUids) {
                DocumentSnapshot uDoc = userDocs.get(uid);
                DocumentSnapshot gDoc = grantDocs.get(uid);

                String role = "STUDENT";
                boolean grantActive = true;
                String scopesStr = "";
                if (gDoc != null && gDoc.exists()) {
                    String gRole = gDoc.getString("role");
                    if ("ADMIN".equalsIgnoreCase(gRole)) {
                        continue; // Không hiện tài khoản Admin trong danh sách bảo vệ/sinh viên
                    }
                    if ("SECURITY_GUARD".equalsIgnoreCase(gRole)) {
                        role = "GUARD";
                        grantActive = Boolean.TRUE.equals(gDoc.getBoolean("active"));
                        Object rawScopes = gDoc.get("guardScopes");
                        if (rawScopes instanceof List) {
                            List<String> sl = new ArrayList<>();
                            for (Object o : (List<?>) rawScopes) {
                                if (o instanceof String && !((String) o).isEmpty()) sl.add((String) o);
                            }
                            scopesStr = String.join(", ", sl);
                        }
                    }
                }

                String name = uDoc != null && uDoc.getString("fullName") != null
                        ? uDoc.getString("fullName")
                        : ("GUARD".equals(role) ? "Nhân sự bảo vệ" : "Sinh viên");
                String email = uDoc != null && uDoc.getString("emailCanonical") != null
                        ? uDoc.getString("emailCanonical") : "";
                String phone = uDoc != null && uDoc.getString("phoneCanonical") != null
                        ? uDoc.getString("phoneCanonical") : "";
                String code;
                String shiftOrMajor;
                String scopeOrPlate;
                String status;

                if ("GUARD".equals(role)) {
                    String staffCode = uDoc != null ? uDoc.getString("staffCode") : null;
                    code = staffCode != null && !staffCode.isEmpty()
                            ? staffCode : uid.substring(0, Math.min(8, uid.length())).toUpperCase(Locale.ROOT);
                    String sh = uDoc != null ? uDoc.getString("shift") : null;
                    shiftOrMajor = sh != null && !sh.isEmpty() ? sh : "Chưa phân ca";
                    scopeOrPlate = !scopesStr.isEmpty() ? scopesStr : "Chưa gán khu vực";
                    String uSt = uDoc != null ? uDoc.getString("accountStatus") : null;
                    status = (grantActive && !"LOCKED".equalsIgnoreCase(uSt)) ? "ACTIVE" : "LOCKED";
                } else {
                    String sc = uDoc != null ? uDoc.getString("studentCode") : null;
                    code = sc != null && !sc.isEmpty() ? sc : "";
                    String cls = uDoc != null ? uDoc.getString("studentClass") : null;
                    shiftOrMajor = cls != null && !cls.isEmpty() ? cls : "Sinh viên";
                    scopeOrPlate = plateByUser.containsKey(uid) ? plateByUser.get(uid) : "Chưa đăng ký xe";
                    String accStatus = uDoc != null ? uDoc.getString("accountStatus") : "ONBOARDING_PENDING";
                    if ("ONBOARDING_PENDING".equalsIgnoreCase(accStatus) || "PENDING".equalsIgnoreCase(accStatus)) {
                        status = "PENDING";
                    } else if ("REJECTED".equalsIgnoreCase(accStatus)) {
                        status = "REJECTED";
                    } else if ("LOCKED".equalsIgnoreCase(accStatus) || "BANNED".equalsIgnoreCase(accStatus)
                            || "DISABLED".equalsIgnoreCase(accStatus)) {
                        status = "LOCKED";
                    } else {
                        status = "ACTIVE";
                    }
                }

                Date createdAt = uDoc != null ? uDoc.getDate("createdAt") : null;
                if (createdAt == null && gDoc != null) createdAt = gDoc.getDate("updatedAt");
                long createdMillis = createdAt != null ? createdAt.getTime() : 0L;

                AdminUserItem item = new AdminUserItem(
                        uid, name, code, email, phone, role, shiftOrMajor, scopeOrPlate, status, createdMillis);
                item.setAvatarUrl(uDoc != null ? uDoc.getString("avatarUrl") : null);

                boolean matchesRole = true;
                if ("GUARD".equalsIgnoreCase(roleFilter)) matchesRole = item.isGuard();
                else if ("STUDENT".equalsIgnoreCase(roleFilter)) matchesRole = item.isStudent();

                boolean matchesStatus = true;
                if (statusFilter != null && !statusFilter.isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
                    if ("PENDING".equalsIgnoreCase(statusFilter)) matchesStatus = item.isPending();
                    else if ("ACTIVE".equalsIgnoreCase(statusFilter)) matchesStatus = item.isActive();
                    else if ("LOCKED".equalsIgnoreCase(statusFilter)) matchesStatus = item.isLocked();
                    else if ("REJECTED".equalsIgnoreCase(statusFilter)) matchesStatus = item.isRejected();
                }

                boolean matchesSearch = true;
                if (!queryClean.isEmpty()) {
                    matchesSearch = (item.getName() != null && item.getName().toLowerCase(Locale.ROOT).contains(queryClean))
                            || (item.getCode() != null && item.getCode().toLowerCase(Locale.ROOT).contains(queryClean))
                            || (item.getPhone() != null && item.getPhone().toLowerCase(Locale.ROOT).contains(queryClean))
                            || (item.getEmail() != null && item.getEmail().toLowerCase(Locale.ROOT).contains(queryClean));
                }

                if (matchesRole && matchesStatus && matchesSearch) {
                    filtered.add(item);
                }
            }
            filtered.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
            return filtered;
        });
    }

    /**
     * Cập nhật thông tin nhân viên bảo vệ trên Firestore (users + accessGrants).
     */
    public Task<Boolean> updateGuardAccount(@NonNull String uid,
                                            @NonNull String name,
                                            @NonNull String code,
                                            @NonNull String phone,
                                            @NonNull String email,
                                            @NonNull String shift,
                                            @NonNull String scope,
                                            @NonNull String status) {
        FirebaseUser admin = auth.getCurrentUser();
        if (admin == null) return Tasks.forException(new SecurityException("Chưa đăng nhập Admin"));

        WriteBatch batch = firestore.batch();
        Map<String, Object> uData = new HashMap<>();
        uData.put("fullName", name.trim());
        uData.put("staffCode", code.trim());
        uData.put("phoneCanonical", phone.trim());
        uData.put("emailCanonical", email.trim());
        uData.put("shift", shift.trim());
        uData.put("accountStatus", status);
        uData.put("updatedAt", FieldValue.serverTimestamp());
        batch.set(firestore.collection("users").document(uid), uData, SetOptions.merge());

        List<String> scopeList = new ArrayList<>();
        for (String part : scope.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) scopeList.add(trimmed);
        }
        if (scopeList.isEmpty()) scopeList.add("Cổng chính A");

        Map<String, Object> gData = new HashMap<>();
        gData.put("role", "SECURITY_GUARD");
        gData.put("guardScopes", scopeList);
        gData.put("active", "ACTIVE".equalsIgnoreCase(status));
        gData.put("updatedAt", FieldValue.serverTimestamp());
        gData.put("updatedBy", admin.getUid());
        gData.put("schemaVersion", 1);
        batch.set(firestore.collection("accessGrants").document(uid), gData, SetOptions.merge());

        appendAudit(batch, admin.getUid(), "UPDATE_GUARD", "users/" + uid, "Cập nhật thông tin ca trực/khu vực");
        return batch.commit().continueWith(t -> {
            if (!t.isSuccessful()) throw Objects.requireNonNull(t.getException());
            notifyListeners();
            return true;
        });
    }

    /**
     * Xóa hồ sơ người dùng và thu hồi grant trên Cloud Firestore.
     */
    public Task<Boolean> deleteUserAccount(@NonNull String uid) {
        FirebaseUser admin = auth.getCurrentUser();
        if (admin == null) return Tasks.forException(new SecurityException("Chưa đăng nhập Admin"));

        WriteBatch batch = firestore.batch();
        batch.delete(firestore.collection("users").document(uid));
        batch.delete(firestore.collection("accessGrants").document(uid));
        appendAudit(batch, admin.getUid(), "DELETE_USER", "users/" + uid, "Xóa tài khoản khỏi hệ thống");
        return batch.commit().continueWith(t -> {
            if (!t.isSuccessful()) throw Objects.requireNonNull(t.getException());
            notifyListeners();
            return true;
        });
    }

    /**
     * Tải toàn bộ danh sách phương tiện cùng ảnh giấy tờ / ảnh xe mà một sinh viên đã gửi lên Firestore.
     */
    public Task<List<VehicleItem>> getVehiclesForUser(@NonNull String uid) {
        Task<QuerySnapshot> vehTask = firestore.collection("vehicles")
                .whereEqualTo("createdBy", uid).limit(20).get();
        Task<QuerySnapshot> reqTask = firestore.collection("vehicleVerificationRequests")
                .whereEqualTo("requesterUid", uid).limit(20).get();

        return Tasks.whenAllComplete(vehTask, reqTask).continueWith(all -> {
            Map<String, String> statusByVehicleId = new HashMap<>();
            if (reqTask.isSuccessful() && reqTask.getResult() != null) {
                for (DocumentSnapshot r : reqTask.getResult().getDocuments()) {
                    String vid = r.getString("vehicleId");
                    String st = r.getString("status");
                    if (vid != null && st != null) {
                        statusByVehicleId.put(vid, st);
                    }
                }
            }

            List<VehicleItem> result = new ArrayList<>();
            if (vehTask.isSuccessful() && vehTask.getResult() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN"));
                List<DocumentSnapshot> docs = new ArrayList<>(vehTask.getResult().getDocuments());
                docs.sort((a, b) -> {
                    Date da = a.getDate("createdAt");
                    Date db = b.getDate("createdAt");
                    return Long.compare(db != null ? db.getTime() : 0L, da != null ? da.getTime() : 0L);
                });
                for (DocumentSnapshot vDoc : docs) {
                    String vid = vDoc.getId();
                    String plateRaw = vDoc.getString("plateRaw");
                    String plateCanonical = vDoc.getString("plateCanonical");
                    if (plateRaw == null || plateRaw.isEmpty()) {
                        plateRaw = plateCanonical != null ? plateCanonical : "--";
                    }
                    String type = vDoc.getString("type");
                    String brand = vDoc.getString("brand");
                    String modelField = vDoc.getString("model");
                    String color = vDoc.getString("color");
                    String regUrl = vDoc.getString("registrationCardPhotoKey");
                    String photoUrl = vDoc.getString("photoObjectKey");
                    Date createdAt = vDoc.getDate("createdAt");

                    String displayModel;
                    if (brand != null && !brand.isEmpty() && modelField != null && !modelField.isEmpty()) {
                        displayModel = brand + " " + modelField;
                    } else if (brand != null && !brand.isEmpty()) {
                        displayModel = brand;
                    } else if (modelField != null && !modelField.isEmpty()) {
                        displayModel = modelField;
                    } else {
                        displayModel = "CAR".equalsIgnoreCase(type) ? "Ô tô"
                                : "ELECTRIC_BIKE".equalsIgnoreCase(type) ? "Xe đạp điện" : "Xe máy";
                    }

                    String status = statusByVehicleId.containsKey(vid)
                            ? statusByVehicleId.get(vid) : "PENDING";
                    String stamp = ("APPROVED".equalsIgnoreCase(status) || "VERIFIED".equalsIgnoreCase(status))
                            ? "PG-" + vid.substring(0, Math.min(8, vid.length())).toUpperCase(Locale.ROOT)
                            : "Chưa cấp";

                    result.add(new VehicleItem(
                            vid,
                            plateRaw,
                            plateCanonical != null ? plateCanonical : "",
                            type != null ? type : "MOTORBIKE",
                            displayModel,
                            color != null ? color : "",
                            status,
                            stamp,
                            createdAt != null ? sdf.format(createdAt) : "",
                            "",
                            uid,
                            regUrl != null ? regUrl : "",
                            photoUrl != null ? photoUrl : ""
                    ));
                }
            }
            return result;
        });
    }

    /**
     * Phê duyệt tài khoản sinh viên đồng thời duyệt các yêu cầu đăng ký xe đang PENDING của sinh viên đó.
     */
    public Task<Boolean> approveStudentAccount(@NonNull String uid) {
        FirebaseUser admin = auth.getCurrentUser();
        if (admin == null) return Tasks.forException(new SecurityException("Chưa đăng nhập Admin"));

        return firestore.collection("vehicleVerificationRequests")
                .whereEqualTo("requesterUid", uid)
                .limit(20)
                .get()
                .onSuccessTask(reqSnap -> {
                    WriteBatch batch = firestore.batch();
                    Map<String, Object> uUpdate = new HashMap<>();
                    uUpdate.put("accountStatus", "ACTIVE");
                    uUpdate.put("updatedAt", FieldValue.serverTimestamp());
                    batch.set(firestore.collection("users").document(uid), uUpdate, SetOptions.merge());

                    int approvedVehicles = 0;
                    if (reqSnap != null) {
                        for (DocumentSnapshot rDoc : reqSnap.getDocuments()) {
                            String st = rDoc.getString("status");
                            if ("PENDING".equalsIgnoreCase(st)) {
                                Map<String, Object> rUpdate = new HashMap<>();
                                rUpdate.put("status", "APPROVED");
                                rUpdate.put("reviewedBy", admin.getUid());
                                rUpdate.put("reviewedAt", FieldValue.serverTimestamp());
                                batch.update(rDoc.getReference(), rUpdate);

                                String vehicleId = rDoc.getString("vehicleId");
                                if (vehicleId != null && !vehicleId.isEmpty()) {
                                    DocumentReference linkRef = firestore.collection("vehicleLinks").document(vehicleId);
                                    Map<String, Object> link = new HashMap<>();
                                    link.put("uid", uid);
                                    link.put("vehicleId", vehicleId);
                                    link.put("status", "VERIFIED");
                                    link.put("approvedBy", admin.getUid());
                                    link.put("approvedAt", FieldValue.serverTimestamp());
                                    link.put("schemaVersion", 1);
                                    batch.set(linkRef, link, SetOptions.merge());
                                }
                                approvedVehicles++;
                            }
                        }
                    }

                    DocumentReference notifRef = firestore.collection("notifications").document();
                    Map<String, Object> notif = new HashMap<>();
                    notif.put("recipientUid", uid);
                    notif.put("type", "VEHICLE");
                    notif.put("title", "Hồ sơ sinh viên đã được phê duyệt");
                    notif.put("message", approvedVehicles > 0
                            ? "Tài khoản sinh viên và " + approvedVehicles + " phương tiện đăng ký của bạn đã được Quản trị viên phê duyệt."
                            : "Tài khoản sinh viên của bạn đã được Quản trị viên phê duyệt và kích hoạt.");
                    notif.put("read", false);
                    notif.put("requiresAction", false);
                    notif.put("createdAt", FieldValue.serverTimestamp());
                    batch.set(notifRef, notif);

                    appendAudit(batch, admin.getUid(), "APPROVE_STUDENT", "users/" + uid,
                            "Phê duyệt tài khoản sinh viên" + (approvedVehicles > 0 ? " và " + approvedVehicles + " xe" : ""));
                    return batch.commit().continueWith(t -> {
                        if (!t.isSuccessful()) throw Objects.requireNonNull(t.getException());
                        notifyListeners();
                        return true;
                    });
                });
    }

    public Task<Boolean> rejectStudentAccount(@NonNull String uid, @Nullable String reason) {
        FirebaseUser admin = auth.getCurrentUser();
        if (admin == null) return Tasks.forException(new SecurityException("Chưa đăng nhập Admin"));

        return firestore.collection("vehicleVerificationRequests")
                .whereEqualTo("requesterUid", uid)
                .limit(20)
                .get()
                .onSuccessTask(reqSnap -> {
                    WriteBatch batch = firestore.batch();
                    Map<String, Object> uUpdate = new HashMap<>();
                    uUpdate.put("accountStatus", "REJECTED");
                    uUpdate.put("updatedAt", FieldValue.serverTimestamp());
                    batch.set(firestore.collection("users").document(uid), uUpdate, SetOptions.merge());

                    if (reqSnap != null) {
                        for (DocumentSnapshot rDoc : reqSnap.getDocuments()) {
                            if ("PENDING".equalsIgnoreCase(rDoc.getString("status"))) {
                                Map<String, Object> rUpdate = new HashMap<>();
                                rUpdate.put("status", "REJECTED");
                                rUpdate.put("reviewedBy", admin.getUid());
                                rUpdate.put("reviewedAt", FieldValue.serverTimestamp());
                                if (reason != null && !reason.isEmpty()) rUpdate.put("reason", reason);
                                batch.update(rDoc.getReference(), rUpdate);
                            }
                        }
                    }

                    DocumentReference notifRef = firestore.collection("notifications").document();
                    Map<String, Object> notif = new HashMap<>();
                    notif.put("recipientUid", uid);
                    notif.put("type", "SYSTEM");
                    notif.put("title", "Hồ sơ đăng ký cần bổ sung / bị từ chối");
                    notif.put("message", reason != null && !reason.isEmpty()
                            ? "Hồ sơ đăng ký của bạn bị từ chối: " + reason
                            : "Hồ sơ đăng ký của bạn chưa đạt yêu cầu xét duyệt. Vui lòng liên hệ Ban quản lý.");
                    notif.put("read", false);
                    notif.put("requiresAction", false);
                    notif.put("createdAt", FieldValue.serverTimestamp());
                    batch.set(notifRef, notif);

                    appendAudit(batch, admin.getUid(), "REJECT_STUDENT", "users/" + uid, reason);
                    return batch.commit().continueWith(t -> {
                        if (!t.isSuccessful()) throw Objects.requireNonNull(t.getException());
                        notifyListeners();
                        return true;
                    });
                });
    }

    public Task<Boolean> setUserStatus(@NonNull String uid, @NonNull String status) {
        FirebaseUser admin = auth.getCurrentUser();
        if (admin == null) return Tasks.forException(new SecurityException("Chưa đăng nhập Admin"));

        return firestore.collection("accessGrants").document(uid).get().onSuccessTask(grantDoc -> {
            WriteBatch batch = firestore.batch();
            Map<String, Object> uUpdate = new HashMap<>();
            uUpdate.put("accountStatus", status);
            uUpdate.put("updatedAt", FieldValue.serverTimestamp());
            batch.set(firestore.collection("users").document(uid), uUpdate, SetOptions.merge());

            if (grantDoc.exists() && "SECURITY_GUARD".equalsIgnoreCase(grantDoc.getString("role"))) {
                Map<String, Object> gUpdate = new HashMap<>();
                gUpdate.put("active", "ACTIVE".equalsIgnoreCase(status));
                gUpdate.put("updatedAt", FieldValue.serverTimestamp());
                gUpdate.put("updatedBy", admin.getUid());
                batch.update(firestore.collection("accessGrants").document(uid), gUpdate);
            }

            appendAudit(batch, admin.getUid(), "SET_USER_STATUS_" + status, "users/" + uid, null);
            return batch.commit().continueWith(t -> {
                if (!t.isSuccessful()) throw Objects.requireNonNull(t.getException());
                notifyListeners();
                return true;
            });
        });
    }

    /**
     * Gửi email đặt lại mật khẩu thật qua Firebase Authentication.
     */
    public Task<Void> sendPasswordResetEmail(@NonNull String email) {
        return auth.sendPasswordResetEmail(email.trim());
    }

    /**
     * Tạo tài khoản Firebase Auth thật cho Bảo vệ bằng secondary FirebaseApp (không làm đăng xuất Admin)
     * và ghi đồng thời users/{uid} + accessGrants/{uid} lên Cloud Firestore.
     */
    public Task<AdminUserItem> createGuardAccount(@NonNull String name,
                                                  @NonNull String code,
                                                  @NonNull String phone,
                                                  @NonNull String email,
                                                  @NonNull String password,
                                                  @NonNull String shift,
                                                  @NonNull String scope) {
        FirebaseUser admin = auth.getCurrentUser();
        if (admin == null) {
            return Tasks.forException(new SecurityException("Chưa đăng nhập Quản trị viên"));
        }
        if (!admin.isEmailVerified()) {
            return Tasks.forException(new SecurityException("Email tài khoản Quản trị viên chưa được xác minh"));
        }

        FirebaseAuth secondaryAuth = getOrCreateSecondaryAuth();
        return secondaryAuth.createUserWithEmailAndPassword(email.trim(), password)
                .onSuccessTask(authResult -> {
                    FirebaseUser createdUser = authResult.getUser();
                    if (createdUser == null) {
                        secondaryAuth.signOut();
                        return Tasks.forException(new IllegalStateException("Không tạo được tài khoản Firebase Auth"));
                    }
                    String newUid = createdUser.getUid();

                    WriteBatch batch = firestore.batch();
                    DocumentReference userRef = firestore.collection("users").document(newUid);
                    Map<String, Object> uData = new HashMap<>();
                    uData.put("fullName", name.trim());
                    uData.put("staffCode", code.trim());
                    uData.put("phoneCanonical", phone.trim());
                    uData.put("emailCanonical", email.trim());
                    uData.put("shift", shift.trim());
                    uData.put("accountStatus", "ACTIVE");
                    uData.put("createdAt", FieldValue.serverTimestamp());
                    uData.put("schemaVersion", 1);
                    batch.set(userRef, uData);

                    DocumentReference grantRef = firestore.collection("accessGrants").document(newUid);
                    Map<String, Object> gData = new HashMap<>();
                    gData.put("role", "SECURITY_GUARD");
                    gData.put("guardScopes", Collections.singletonList(scope.trim()));
                    gData.put("active", true);
                    gData.put("updatedAt", FieldValue.serverTimestamp());
                    gData.put("updatedBy", admin.getUid());
                    gData.put("schemaVersion", 1);
                    batch.set(grantRef, gData);

                    appendAudit(batch, admin.getUid(), "CREATE_GUARD", "accessGrants/" + newUid, "Cấp tài khoản bảo vệ " + code);

                    return batch.commit().continueWithTask(commitTask -> {
                        if (!commitTask.isSuccessful()) {
                            Exception commitError = commitTask.getException() != null
                                    ? commitTask.getException()
                                    : new IllegalStateException("Ghi dữ liệu Firestore thất bại");
                            Log.e("AdminRepository", "createGuardAccount Firestore commit failed: "
                                    + commitError.getClass().getSimpleName());
                            // Hoàn tác tài khoản Auth vừa tạo để tránh kẹt email nếu Firestore từ chối quyền ghi
                            return createdUser.delete().continueWithTask(delTask -> {
                                secondaryAuth.signOut();
                                return Tasks.forException(commitError);
                            });
                        }
                        // Chỉ gửi email xác minh sau khi Firestore ghi thành công
                        return createdUser.sendEmailVerification().continueWith(verifyTask -> {
                            secondaryAuth.signOut();
                            notifyListeners();
                            return new AdminUserItem(
                                    newUid, name.trim(), code.trim(), email.trim(), phone.trim(),
                                    "GUARD", shift.trim(), scope.trim(), "ACTIVE", System.currentTimeMillis()
                            );
                        });
                    });
                })
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Exception err = task.getException();
                        if (err != null) {
                            Log.e("AdminRepository", "createGuardAccount failed: " + err.getClass().getSimpleName());
                        }
                    }
                    return task;
                });
    }

    private FirebaseAuth getOrCreateSecondaryAuth() {
        FirebaseApp secondaryApp;
        try {
            secondaryApp = FirebaseApp.getInstance(SECONDARY_APP_NAME);
        } catch (IllegalStateException notFound) {
            FirebaseApp defaultApp = FirebaseApp.getInstance();
            secondaryApp = FirebaseApp.initializeApp(context, defaultApp.getOptions(), SECONDARY_APP_NAME);
        }
        return FirebaseAuth.getInstance(secondaryApp);
    }

    /**
     * Đọc cấu hình quy tắc quét lặp trực tiếp từ Firestore systemConfig/scanRules.
     */
    public Task<ScanRuleConfig> getScanRulesAsync() {
        return firestore.collection("systemConfig").document("scanRules").get().continueWith(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                DocumentSnapshot doc = task.getResult();
                Long interval = doc.getLong("duplicateIntervalSeconds");
                Boolean alert = doc.getBoolean("enableDuplicateAlert");
                Boolean overnight = doc.getBoolean("autoFlagOvernight");
                Long maxPerDay = doc.getLong("maxEntriesPerDay");
                return new ScanRuleConfig(
                        interval != null ? interval.intValue() : 60,
                        alert == null || alert,
                        overnight == null || overnight,
                        maxPerDay != null ? maxPerDay.intValue() : 10
                );
            }
            return new ScanRuleConfig(60, true, true, 10);
        });
    }

    /**
     * Lưu cấu hình quy tắc quét lặp lên Firestore systemConfig/scanRules.
     */
    public Task<Boolean> saveScanRules(@NonNull ScanRuleConfig config) {
        FirebaseUser admin = auth.getCurrentUser();
        if (admin == null) {
            return Tasks.forException(new SecurityException("Chưa đăng nhập Quản trị viên"));
        }

        DocumentReference configRef = firestore.collection("systemConfig").document("scanRules");
        Map<String, Object> data = new HashMap<>();
        data.put("duplicateIntervalSeconds", config.getDuplicateIntervalSeconds());
        data.put("enableDuplicateAlert", config.isEnableDuplicateAlert());
        data.put("autoFlagOvernight", config.isAutoFlagOvernight());
        data.put("maxEntriesPerDay", config.getMaxDailyScans());
        data.put("updatedBy", admin.getUid());
        data.put("updatedAt", FieldValue.serverTimestamp());
        return configRef.set(data, SetOptions.merge()).continueWith(t -> {
            if (!t.isSuccessful()) throw Objects.requireNonNull(t.getException());
            return true;
        });
    }

    /**
     * Tải danh sách nhật ký kiểm toán thật từ Firestore collection auditLogs.
     */
    public Task<List<DocumentSnapshot>> getAuditLogs() {
        return firestore.collection("auditLogs").limit(100).get().continueWith(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                return new ArrayList<>();
            }
            List<DocumentSnapshot> docs = new ArrayList<>(task.getResult().getDocuments());
            docs.sort((a, b) -> {
                Date da = a.getDate("at");
                Date db = b.getDate("at");
                return Long.compare(db != null ? db.getTime() : 0L, da != null ? da.getTime() : 0L);
            });
            return docs;
        });
    }

    private void appendAudit(WriteBatch batch, String actorUid, String action,
                             String targetPath, @Nullable String reason) {
        DocumentReference logRef = firestore.collection("auditLogs").document();
        Map<String, Object> log = new HashMap<>();
        log.put("actorUid", actorUid);
        log.put("action", action);
        log.put("targetPath", targetPath);
        if (reason != null && !reason.isEmpty()) log.put("reason", reason);
        log.put("at", FieldValue.serverTimestamp());
        log.put("schemaVersion", 1);
        batch.set(logRef, log);
    }
}
