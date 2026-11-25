package com.example.parkguardai.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.parkguardai.data.model.DuplicateScanCheckResult;
import com.example.parkguardai.data.model.GuardDraft;
import com.example.parkguardai.data.model.GuardSecurityAlert;
import com.example.parkguardai.data.model.ScanTimelineItem;
import com.example.parkguardai.data.model.VehicleLookupResult;
import com.example.parkguardai.data.model.ViolationRecord;
import com.example.parkguardai.data.model.ViolationStatus;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Repository trung tâm quản lý dữ liệu phân hệ Bảo vệ (Guard Portal) kết nối 100% Cloud Firestore.
 */
public final class GuardRepository {

    private static volatile GuardRepository INSTANCE;

    private final Context context;
    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final ViolationRepository violationRepository;

    private final List<GuardSecurityAlert> firestoreAlerts = new CopyOnWriteArrayList<>();
    private final List<ViolationRecord> firestoreViolations = new CopyOnWriteArrayList<>();

    public interface ViolationListener {
        void onViolationsChanged(List<ViolationRecord> records);
    }

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    public interface AlertListener {
        void onAlertsChanged(List<GuardSecurityAlert> alerts);
    }

    private final List<ViolationListener> violationListeners = new CopyOnWriteArrayList<>();
    private final List<AlertListener> alertListeners = new CopyOnWriteArrayList<>();
    private ListenerRegistration violationsRegistration;
    private ListenerRegistration alertsRegistration;

    public static GuardRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (GuardRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new GuardRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    private GuardRepository(Context context) {
        this.context = context;
        this.auth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
        this.violationRepository = new ViolationRepository(context);
    }

    /**
     * Tra cứu phương tiện trực tiếp từ Cloud Firestore theo biển số chuẩn hóa (plateCanonical).
     */
    public Task<VehicleLookupResult> lookupVehicle(@Nullable String rawPlate) {
        if (rawPlate == null || rawPlate.trim().isEmpty()) {
            return Tasks.forResult(new VehicleLookupResult(
                    "", "Chưa rõ", "", "Không rõ", "",
                    VehicleLookupResult.RegistrationStatus.UNREGISTERED, 0
            ));
        }

        String clean = normalizePlate(rawPlate);
        return firestore.collection("vehicles")
                .whereEqualTo("plateCanonical", clean)
                .limit(1)
                .get()
                .continueWithTask(vTask -> {
                    if (!vTask.isSuccessful() || vTask.getResult() == null || vTask.getResult().isEmpty()) {
                        return Tasks.forResult(new VehicleLookupResult(
                                rawPlate.trim(),
                                "Chưa đăng ký",
                                "",
                                "Phương tiện chưa đăng ký",
                                "",
                                VehicleLookupResult.RegistrationStatus.UNREGISTERED,
                                0
                        ));
                    }

                    DocumentSnapshot vDoc = vTask.getResult().getDocuments().get(0);
                    String vehicleId = vDoc.getId();
                    String plateDisplay = vDoc.getString("plateRaw");
                    if (plateDisplay == null || plateDisplay.isEmpty()) plateDisplay = rawPlate.trim();
                    String brand = vDoc.getString("brand");
                    String type = vDoc.getString("type");
                    String color = vDoc.getString("color");
                    String createdBy = vDoc.getString("createdBy");
                    String modelDisplay = (brand != null && !brand.isEmpty())
                            ? brand : StudentRepository.vehicleTypeLabel(type);

                    Task<DocumentSnapshot> ownerTask = (createdBy != null && !createdBy.isEmpty())
                            ? firestore.collection("users").document(createdBy).get()
                            : Tasks.forResult(null);
                    Task<QuerySnapshot> reqTask = firestore.collection("vehicleVerificationRequests")
                            .whereEqualTo("vehicleId", vehicleId)
                            .limit(5)
                            .get();
                    Task<QuerySnapshot> vioTask = firestore.collection("violations")
                            .whereEqualTo("plateCanonical", clean)
                            .limit(20)
                            .get();

                    final String finalPlate = plateDisplay;
                    return Tasks.whenAllComplete(ownerTask, reqTask, vioTask).continueWith(allTask -> {
                        String ownerName = "Sinh viên";
                        String studentCode = "";
                        if (ownerTask.isSuccessful() && ownerTask.getResult() != null && ownerTask.getResult().exists()) {
                            String fn = ownerTask.getResult().getString("fullName");
                            String sc = ownerTask.getResult().getString("studentCode");
                            if (fn != null && !fn.isEmpty()) ownerName = fn;
                            if (sc != null) studentCode = sc;
                        }

                        VehicleLookupResult.RegistrationStatus regStatus = VehicleLookupResult.RegistrationStatus.PENDING;
                        if (reqTask.isSuccessful() && reqTask.getResult() != null && !reqTask.getResult().isEmpty()) {
                            for (DocumentSnapshot rDoc : reqTask.getResult().getDocuments()) {
                                String st = rDoc.getString("status");
                                if ("APPROVED".equalsIgnoreCase(st) || "VERIFIED".equalsIgnoreCase(st)) {
                                    regStatus = VehicleLookupResult.RegistrationStatus.VERIFIED;
                                    break;
                                } else if ("REJECTED".equalsIgnoreCase(st) || "BLOCKED".equalsIgnoreCase(st)) {
                                    regStatus = VehicleLookupResult.RegistrationStatus.UNREGISTERED;
                                }
                            }
                        }

                        int violationCount = (vioTask.isSuccessful() && vioTask.getResult() != null)
                                ? vioTask.getResult().size() : 0;

                        return new VehicleLookupResult(
                                finalPlate,
                                ownerName,
                                studentCode,
                                modelDisplay,
                                color != null ? color : "",
                                regStatus,
                                violationCount
                        );
                    });
                });
    }

    /**
     * Kiểm tra quét lặp trong ngày trên Cloud Firestore (violations + parkingHistory).
     */
    public Task<DuplicateScanCheckResult> checkDuplicateScan(@Nullable String rawPlate) {
        if (rawPlate == null || rawPlate.trim().isEmpty()) {
            return Tasks.forResult(DuplicateScanCheckResult.notDuplicate("", "", ""));
        }

        String clean = normalizePlate(rawPlate);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfDayMillis = cal.getTimeInMillis();

        Task<QuerySnapshot> vioTask = firestore.collection("violations")
                .whereEqualTo("plateCanonical", clean)
                .limit(20)
                .get();
        Task<QuerySnapshot> parkTask = firestore.collection("parkingHistory")
                .whereEqualTo("plateCanonical", clean)
                .limit(20)
                .get();

        return Tasks.whenAllComplete(vioTask, parkTask).continueWith(allTask -> {
            SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", new Locale("vi", "VN"));
            List<ScanTimelineItem> items = new ArrayList<>();
            int matchCount = 0;
            String lastTime = "";
            String lastLoc = "";
            String lastVioId = null;
            String lastVioType = null;

            if (vioTask.isSuccessful() && vioTask.getResult() != null) {
                for (DocumentSnapshot doc : vioTask.getResult().getDocuments()) {
                    Date d = doc.getDate("capturedAt");
                    if (d == null) d = doc.getDate("createdAt");
                    if (d == null || d.getTime() < startOfDayMillis) continue;
                    matchCount++;
                    String t = timeFmt.format(d);
                    String loc = doc.getString("scopeId");
                    if (loc == null || loc.isEmpty()) loc = "Hiện trường";
                    items.add(new ScanTimelineItem(t, "Vi phạm", loc, "Vi phạm", false));
                    lastTime = t;
                    lastLoc = loc;
                    lastVioId = doc.getId();
                    lastVioType = doc.getString("typeCode");
                }
            }

            if (parkTask.isSuccessful() && parkTask.getResult() != null) {
                for (DocumentSnapshot doc : parkTask.getResult().getDocuments()) {
                    Date d = doc.getDate("timestamp");
                    if (d == null) d = doc.getDate("createdAt");
                    if (d == null || d.getTime() < startOfDayMillis) continue;
                    matchCount++;
                    String t = timeFmt.format(d);
                    String action = "EXIT".equalsIgnoreCase(doc.getString("actionType")) ? "Ra bãi" : "Vào bãi";
                    String gate = doc.getString("gateName");
                    if (gate == null || gate.isEmpty()) gate = "Cổng bãi xe";
                    items.add(new ScanTimelineItem(t, action, gate, "Hợp lệ", false));
                    if (lastTime.isEmpty()) {
                        lastTime = t;
                        lastLoc = gate;
                    }
                }
            }

            String nowStr = timeFmt.format(new Date());
            if (matchCount > 0) {
                items.add(new ScanTimelineItem(nowStr, "Lần quét này", "Hiện trường", "Cần xác nhận", true));
                return new DuplicateScanCheckResult(
                        true,
                        matchCount + 1,
                        rawPlate.trim(),
                        "Chủ phương tiện",
                        "Phương tiện",
                        lastTime,
                        lastLoc.isEmpty() ? "Hiện trường" : lastLoc,
                        lastVioId,
                        lastVioType,
                        items
                );
            }
            return DuplicateScanCheckResult.notDuplicate(rawPlate.trim(), "Hợp lệ", "Phương tiện");
        });
    }

    /**
     * Ghi nhận lượt xe vào/ra bãi thật vào Firestore collection parkingHistory.
     */
    public Task<Void> recordParkingEvent(@NonNull String rawPlate,
                                         @NonNull String actionType,
                                         @NonNull String gateName) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return Tasks.forException(new SecurityException("Chưa đăng nhập bảo vệ"));
        }
        String clean = normalizePlate(rawPlate);
        return firestore.collection("vehicles")
                .whereEqualTo("plateCanonical", clean)
                .limit(1)
                .get()
                .continueWithTask(vTask -> {
                    String ownerUid = null;
                    if (vTask.isSuccessful() && vTask.getResult() != null && !vTask.getResult().isEmpty()) {
                        ownerUid = vTask.getResult().getDocuments().get(0).getString("createdBy");
                    }
                    Map<String, Object> data = new HashMap<>();
                    data.put("plateNumber", rawPlate.trim());
                    data.put("plateCanonical", clean);
                    if (ownerUid != null && !ownerUid.isEmpty()) {
                        data.put("ownerUid", ownerUid);
                    }
                    data.put("actionType", actionType);
                    data.put("gateName", gateName);
                    data.put("status", "ENTRY".equalsIgnoreCase(actionType) ? "IN_LOT" : "EXITED");
                    data.put("guardUid", user.getUid());
                    data.put("timestamp", FieldValue.serverTimestamp());
                    data.put("createdAt", FieldValue.serverTimestamp());
                    return firestore.collection("parkingHistory").add(data).continueWith(t -> {
                        if (!t.isSuccessful()) throw t.getException();
                        return null;
                    });
                });
    }

    public void addAlertListener(AlertListener listener) {
        if (listener == null) return;
        if (!alertListeners.contains(listener)) {
            alertListeners.add(listener);
        }
        listener.onAlertsChanged(getAllSecurityAlerts());
        setupFirestoreAlertsRealtimeListener();
    }

    public void removeAlertListener(AlertListener listener) {
        if (listener != null) {
            alertListeners.remove(listener);
        }
        if (alertListeners.isEmpty() && alertsRegistration != null) {
            alertsRegistration.remove();
            alertsRegistration = null;
        }
    }

    public void notifyAlertsUpdated() {
        List<GuardSecurityAlert> current = getAllSecurityAlerts();
        for (AlertListener l : alertListeners) {
            try { l.onAlertsChanged(current); } catch (Exception ignored) { }
        }
    }

    public List<GuardSecurityAlert> getAllSecurityAlerts() {
        return new ArrayList<>(firestoreAlerts);
    }

    /**
     * Ghi nhận cảnh báo an ninh mới trực tiếp lên Firestore collection security_alerts.
     */
    public void recordSecurityAlert(GuardSecurityAlert alert) {
        if (alert == null || auth.getCurrentUser() == null) return;
        for (GuardSecurityAlert existing : firestoreAlerts) {
            if (existing.plateNumber.equalsIgnoreCase(alert.plateNumber)
                    && existing.alertType == alert.alertType
                    && existing.status == GuardSecurityAlert.Status.UNHANDLED) {
                return;
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("plateNumber", alert.plateNumber);
        data.put("alertType", alert.alertType.name());
        data.put("title", alert.title);
        data.put("description", alert.description);
        data.put("severity", alert.severity.name());
        data.put("location", alert.location);
        data.put("status", "UNHANDLED");
        data.put("createdByUid", auth.getCurrentUser().getUid());
        data.put("createdAt", FieldValue.serverTimestamp());
        firestore.collection("security_alerts").add(data);
    }

    /**
     * Đánh dấu cảnh báo đã giải quyết trực tiếp trên Cloud Firestore.
     */
    public void resolveAlert(String alertId, String actionNote, @Nullable Callback<Void> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onError(new SecurityException("Chưa đăng nhập"));
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "RESOLVED");
        updates.put("resolutionNote", (actionNote != null && !actionNote.isEmpty()) ? actionNote : "Đã xử lý an toàn");
        updates.put("resolvedByUid", user.getUid());
        updates.put("resolvedAt", FieldValue.serverTimestamp());

        firestore.collection("security_alerts").document(alertId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    if (callback != null) callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }

    private void setupFirestoreAlertsRealtimeListener() {
        if (alertsRegistration != null || auth.getCurrentUser() == null) return;
        alertsRegistration = firestore.collection("security_alerts")
                .limit(50)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    List<GuardSecurityAlert> fresh = new ArrayList<>();
                    SimpleDateFormat fmt = new SimpleDateFormat("HH:mm - dd/MM", new Locale("vi", "VN"));
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String id = doc.getId();
                        String plate = doc.getString("plateNumber");
                        String title = doc.getString("title");
                        String desc = doc.getString("description");
                        String typeStr = doc.getString("alertType");
                        String sevStr = doc.getString("severity");
                        String statusStr = doc.getString("status");
                        String loc = doc.getString("location");
                        String resNote = doc.getString("resolutionNote");
                        Date createdAt = doc.getDate("createdAt");

                        GuardSecurityAlert.AlertType type = GuardSecurityAlert.AlertType.UNKNOWN_VEHICLE;
                        try { if (typeStr != null) type = GuardSecurityAlert.AlertType.valueOf(typeStr); } catch (Exception ignored) {}

                        GuardSecurityAlert.Severity sev = GuardSecurityAlert.Severity.MEDIUM;
                        try { if (sevStr != null) sev = GuardSecurityAlert.Severity.valueOf(sevStr); } catch (Exception ignored) {}

                        GuardSecurityAlert.Status st = "RESOLVED".equalsIgnoreCase(statusStr)
                                ? GuardSecurityAlert.Status.RESOLVED : GuardSecurityAlert.Status.UNHANDLED;

                        String timeStr = createdAt != null ? fmt.format(createdAt) : "Vừa xong";
                        fresh.add(new GuardSecurityAlert(
                                id,
                                plate != null ? plate : "",
                                type,
                                title != null ? title : "Cảnh báo an ninh",
                                desc != null ? desc : "",
                                sev,
                                timeStr,
                                loc != null ? loc : "Bãi giữ xe",
                                st,
                                resNote,
                                createdAt != null ? createdAt.getTime() : 0L
                        ));
                    }
                    fresh.sort((a, b) -> Long.compare(b.createdAtMillis, a.createdAtMillis));
                    firestoreAlerts.clear();
                    firestoreAlerts.addAll(fresh);
                    notifyAlertsUpdated();
                });
    }

    public void addViolationListener(ViolationListener listener) {
        if (listener == null) return;
        if (!violationListeners.contains(listener)) {
            violationListeners.add(listener);
        }
        listener.onViolationsChanged(getAllCurrentRecords());
        setupFirestoreRealtimeListener();
    }

    public void removeViolationListener(ViolationListener listener) {
        if (listener != null) {
            violationListeners.remove(listener);
        }
        if (violationListeners.isEmpty() && violationsRegistration != null) {
            violationsRegistration.remove();
            violationsRegistration = null;
        }
    }

    public void notifyViolationsUpdated() {
        List<ViolationRecord> current = getAllCurrentRecords();
        for (ViolationListener l : violationListeners) {
            try { l.onViolationsChanged(current); } catch (Exception ignored) { }
        }
    }

    @NonNull
    public List<ViolationRecord> getAllCurrentRecords() {
        List<ViolationRecord> list = new ArrayList<>(firestoreViolations);
        list.sort((r1, r2) -> {
            Date d1 = r1.getCapturedAt() != null ? r1.getCapturedAt() : new Date(0);
            Date d2 = r2.getCapturedAt() != null ? r2.getCapturedAt() : new Date(0);
            return d2.compareTo(d1);
        });
        return list;
    }

    private void setupFirestoreRealtimeListener() {
        FirebaseUser user = auth.getCurrentUser();
        if (violationsRegistration != null || user == null) return;

        violationsRegistration = firestore.collection("violations")
                .whereEqualTo("guardUid", user.getUid())
                .limit(50)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    List<ViolationRecord> fresh = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        ViolationRecord rec = new ViolationRecord();
                        rec.setId(doc.getId());
                        rec.setGuardUid(doc.getString("guardUid"));
                        rec.setPlateFinal(doc.getString("plateFinal"));
                        rec.setTypeCode(doc.getString("typeCode"));
                        rec.setScopeId(doc.getString("scopeId"));
                        rec.setNote(doc.getString("note"));
                        Date cap = doc.getDate("capturedAt");
                        if (cap == null) cap = doc.getDate("createdAt");
                        rec.setCapturedAt(cap);
                        rec.setLatitude(doc.getDouble("latitude"));
                        rec.setLongitude(doc.getDouble("longitude"));
                        rec.setLocationAccuracy(doc.getDouble("locationAccuracy"));
                        String st = doc.getString("status");
                        if ("RESOLVED".equalsIgnoreCase(st) || "PAID".equalsIgnoreCase(st)) {
                            rec.setStatus(ViolationStatus.RESOLVED);
                        } else if ("CONFIRMED".equalsIgnoreCase(st)) {
                            rec.setStatus(ViolationStatus.CONFIRMED);
                        } else {
                            rec.setStatus(ViolationStatus.PENDING);
                        }
                        fresh.add(rec);
                    }
                    fresh.sort((r1, r2) -> {
                        Date d1 = r1.getCapturedAt() != null ? r1.getCapturedAt() : new Date(0);
                        Date d2 = r2.getCapturedAt() != null ? r2.getCapturedAt() : new Date(0);
                        return d2.compareTo(d1);
                    });
                    firestoreViolations.clear();
                    firestoreViolations.addAll(fresh);
                    notifyViolationsUpdated();
                });
    }

    public void getGuardRecords(Callback<List<ViolationRecord>> callback) {
        if (callback != null) {
            callback.onSuccess(getAllCurrentRecords());
        }
    }

    /**
     * Thông tin ca trực thực tế của bảo vệ đang đăng nhập.
     */
    public static final class GuardShiftInfo {
        public final String officerName;
        public final String officerCode;
        public final String gateLocation;
        public final String gateName;
        public final String shiftName;
        public final String remainingTime;
        public final int scansInShift;
        public final int totalScans;
        public final int validScans;
        public final int unregisteredScans;
        public final int violationsInShift;
        public final int violationScans;

        public GuardShiftInfo(String officerName, String officerCode, String gateLocation,
                              String shiftName, String remainingTime, int totalScans,
                              int validScans, int unregisteredScans, int violationScans) {
            this.officerName = officerName;
            this.officerCode = officerCode;
            this.gateLocation = gateLocation;
            this.gateName = gateLocation;
            this.shiftName = shiftName;
            this.remainingTime = remainingTime;
            this.scansInShift = totalScans;
            this.totalScans = totalScans;
            this.validScans = validScans;
            this.unregisteredScans = unregisteredScans;
            this.violationsInShift = violationScans;
            this.violationScans = violationScans;
        }
    }

    /**
     * Tải hồ sơ nhân sự, khu vực phân công và số liệu thực tế trong ngày của bảo vệ từ Firestore.
     */
    public void getGuardShiftInfo(@Nullable Callback<GuardShiftInfo> callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onError(new SecurityException("Chưa đăng nhập bảo vệ"));
            return;
        }
        String uid = user.getUid();

        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);

        String defaultShift;
        int endHour;
        if (hour >= 6 && hour < 14) {
            defaultShift = "Ca Sáng (06:00 - 14:00)";
            endHour = 14;
        } else if (hour >= 14 && hour < 22) {
            defaultShift = "Ca Chiều (14:00 - 22:00)";
            endHour = 22;
        } else {
            defaultShift = "Ca Đêm (22:00 - 06:00)";
            endHour = hour >= 22 ? 30 : 6;
        }
        int remainingMinutes = Math.max(0, endHour * 60 - (hour * 60 + minute));
        String remainingTime = (remainingMinutes / 60) > 0
                ? "Còn " + (remainingMinutes / 60) + " giờ " + (remainingMinutes % 60) + " phút"
                : "Còn " + (remainingMinutes % 60) + " phút";

        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfDay = cal.getTimeInMillis();

        Task<DocumentSnapshot> userTask = firestore.collection("users").document(uid).get();
        Task<DocumentSnapshot> grantTask = firestore.collection("accessGrants").document(uid).get();
        Task<QuerySnapshot> vioTask = firestore.collection("violations")
                .whereEqualTo("guardUid", uid).limit(50).get();
        Task<QuerySnapshot> parkTask = firestore.collection("parkingHistory")
                .whereEqualTo("guardUid", uid).limit(100).get();
        Task<QuerySnapshot> alertTask = firestore.collection("security_alerts")
                .whereEqualTo("createdByUid", uid).limit(50).get();

        Tasks.whenAllComplete(userTask, grantTask, vioTask, parkTask, alertTask).addOnCompleteListener(all -> {
            String officerName = user.getEmail() != null ? user.getEmail() : "Bảo vệ";
            String officerCode = uid.substring(0, Math.min(8, uid.length())).toUpperCase(Locale.ROOT);
            String shiftName = defaultShift;
            String gateLocation = "Chưa phân công";

            if (userTask.isSuccessful() && userTask.getResult() != null && userTask.getResult().exists()) {
                DocumentSnapshot uDoc = userTask.getResult();
                String fn = uDoc.getString("fullName");
                String sc = uDoc.getString("staffCode");
                String sh = uDoc.getString("shift");
                if (fn != null && !fn.isEmpty()) officerName = fn;
                if (sc != null && !sc.isEmpty()) officerCode = sc;
                if (sh != null && !sh.isEmpty()) shiftName = sh;
            }

            if (grantTask.isSuccessful() && grantTask.getResult() != null && grantTask.getResult().exists()) {
                Object rawScopes = grantTask.getResult().get("guardScopes");
                if (rawScopes instanceof List) {
                    List<String> scopes = new ArrayList<>();
                    for (Object o : (List<?>) rawScopes) {
                        if (o instanceof String && !((String) o).isEmpty()) scopes.add((String) o);
                    }
                    if (!scopes.isEmpty()) {
                        gateLocation = String.join(", ", scopes);
                    }
                }
            }

            int violationsToday = 0;
            if (vioTask.isSuccessful() && vioTask.getResult() != null) {
                for (DocumentSnapshot d : vioTask.getResult().getDocuments()) {
                    Date cap = d.getDate("capturedAt");
                    if (cap == null) cap = d.getDate("createdAt");
                    if (cap != null && cap.getTime() >= startOfDay) violationsToday++;
                }
            }

            int validToday = 0;
            if (parkTask.isSuccessful() && parkTask.getResult() != null) {
                for (DocumentSnapshot d : parkTask.getResult().getDocuments()) {
                    Date ts = d.getDate("timestamp");
                    if (ts == null) ts = d.getDate("createdAt");
                    if (ts != null && ts.getTime() >= startOfDay) validToday++;
                }
            }

            int unregisteredToday = 0;
            if (alertTask.isSuccessful() && alertTask.getResult() != null) {
                for (DocumentSnapshot d : alertTask.getResult().getDocuments()) {
                    Date ts = d.getDate("createdAt");
                    if (ts != null && ts.getTime() >= startOfDay
                            && "UNKNOWN_VEHICLE".equalsIgnoreCase(d.getString("alertType"))) {
                        unregisteredToday++;
                    }
                }
            }

            int totalToday = validToday + unregisteredToday + violationsToday;
            GuardShiftInfo info = new GuardShiftInfo(
                    officerName, officerCode, gateLocation, shiftName, remainingTime,
                    totalToday, validToday, unregisteredToday, violationsToday
            );
            if (callback != null) callback.onSuccess(info);
        });
    }

    /**
     * Gửi hồ sơ vi phạm mới lên Cloudinary + Cloud Firestore.
     */
    public Task<String> submitViolation(GuardDraft draft, @Nullable ViolationRepository.StageListener listener) {
        return violationRepository.submit(draft, listener);
    }

    /**
     * Bổ sung ảnh bằng chứng thật vào hồ sơ vi phạm hiện có.
     */
    public Task<Void> supplementViolationEvidence(String violationId, File newImageFile,
                                                  double lat, double lng, float accuracy) {
        return violationRepository.supplementEvidence(violationId, newImageFile, lat, lng, accuracy);
    }

    public static String normalizePlate(String raw) {
        if (raw == null) return "";
        return raw.toUpperCase(Locale.ROOT).replaceAll("[\\s.\\-]", "");
    }
}
