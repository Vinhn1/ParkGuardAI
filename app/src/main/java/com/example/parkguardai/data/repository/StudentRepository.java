package com.example.parkguardai.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.parkguardai.data.datasource.CloudinaryDataSource;
import com.example.parkguardai.data.model.EvidenceImage;
import com.example.parkguardai.data.model.StudentDashboardData;
import com.example.parkguardai.data.model.StudentNotificationItem;
import com.example.parkguardai.data.model.StudentParkingRecord;
import com.example.parkguardai.data.model.UserProfile;
import com.example.parkguardai.data.model.VehicleItem;
import com.example.parkguardai.data.model.ViolationItem;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Repository trung tâm quản lý dữ liệu phân hệ Sinh viên kết nối 100% với Firebase Auth & Cloud Firestore.
 */
public final class StudentRepository {

    private static volatile StudentRepository INSTANCE;

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final CloudinaryDataSource cloudinaryDataSource;

    public static StudentRepository getInstance() {
        if (INSTANCE == null) {
            synchronized (StudentRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new StudentRepository(
                            FirebaseAuth.getInstance(),
                            FirebaseFirestore.getInstance(),
                            new CloudinaryDataSource()
                    );
                }
            }
        }
        return INSTANCE;
    }

    public StudentRepository(FirebaseAuth auth, FirebaseFirestore firestore,
                             CloudinaryDataSource cloudinaryDataSource) {
        this.auth = Objects.requireNonNull(auth, "auth");
        this.firestore = Objects.requireNonNull(firestore, "firestore");
        this.cloudinaryDataSource = Objects.requireNonNull(cloudinaryDataSource, "cloudinaryDataSource");
    }

    /**
     * Tải thông tin hồ sơ của sinh viên đang đăng nhập từ users/{uid}.
     */
    public Task<UserProfile> getMyProfile(@Nullable Context context) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return Tasks.forException(new IllegalStateException("Chưa đăng nhập tài khoản sinh viên"));
        }

        String uid = user.getUid();
        return firestore.collection("users").document(uid).get().continueWith(task -> {
            if (!task.isSuccessful()) {
                throw Objects.requireNonNull(task.getException());
            }
            DocumentSnapshot snapshot = task.getResult();
            UserProfile profile = new UserProfile();
            profile.setUid(uid);
            if (snapshot != null && snapshot.exists()) {
                profile.setFullName(snapshot.getString("fullName"));
                profile.setEmailCanonical(snapshot.getString("emailCanonical"));
                profile.setPhoneCanonical(snapshot.getString("phoneCanonical"));
                profile.setStudentCode(snapshot.getString("studentCode"));
                profile.setStudentClass(snapshot.getString("studentClass"));
                profile.setAvatarUrl(snapshot.getString("avatarUrl"));
                profile.setAccountStatus(snapshot.getString("accountStatus"));
                profile.setCreatedAt(snapshot.getDate("createdAt"));
            } else {
                profile.setFullName(user.getDisplayName() != null ? user.getDisplayName() : user.getEmail());
                profile.setEmailCanonical(user.getEmail());
                profile.setAccountStatus("ONBOARDING_PENDING");
            }
            return profile;
        });
    }

    /**
     * Cập nhật họ tên và số điện thoại liên hệ của sinh viên trên users/{uid}.
     */
    public Task<Boolean> updateMyProfile(@NonNull String fullName, @NonNull String phoneCanonical) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return Tasks.forException(new IllegalStateException("Chưa đăng nhập tài khoản sinh viên"));
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName.trim());
        updates.put("phoneCanonical", phoneCanonical.trim());
        updates.put("updatedAt", FieldValue.serverTimestamp());
        return firestore.collection("users").document(user.getUid())
                .update(updates)
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw Objects.requireNonNull(task.getException());
                    }
                    return true;
                });
    }

    /**
     * Lấy danh sách toàn bộ phương tiện sở hữu của sinh viên từ Firestore (vehicles + vehicleVerificationRequests).
     */
    public Task<List<VehicleItem>> getMyVehicles(@Nullable Context context) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return Tasks.forResult(new ArrayList<>());
        }

        String uid = user.getUid();
        Task<QuerySnapshot> vehiclesQuery = firestore.collection("vehicles")
                .whereEqualTo("createdBy", uid)
                .get();
        Task<QuerySnapshot> requestsQuery = firestore.collection("vehicleVerificationRequests")
                .whereEqualTo("requesterUid", uid)
                .get();
        Task<DocumentSnapshot> profileTask = firestore.collection("users").document(uid).get();

        return Tasks.whenAllComplete(vehiclesQuery, requestsQuery, profileTask).continueWith(allTask -> {
            Map<String, String> reqStatusByVehicleId = new HashMap<>();
            if (requestsQuery.isSuccessful() && requestsQuery.getResult() != null) {
                for (DocumentSnapshot reqDoc : requestsQuery.getResult().getDocuments()) {
                    String vid = reqDoc.getString("vehicleId");
                    String st = reqDoc.getString("status");
                    if (vid != null && st != null) {
                        reqStatusByVehicleId.put(vid, st);
                    }
                }
            }

            String ownerName = user.getEmail() != null ? user.getEmail() : "";
            if (profileTask.isSuccessful() && profileTask.getResult() != null && profileTask.getResult().exists()) {
                String fn = profileTask.getResult().getString("fullName");
                if (fn != null && !fn.isEmpty()) ownerName = fn;
            }

            List<DocumentSnapshot> vehicleDocs = new ArrayList<>();
            if (vehiclesQuery.isSuccessful() && vehiclesQuery.getResult() != null) {
                vehicleDocs.addAll(vehiclesQuery.getResult().getDocuments());
            }

            // Sắp xếp mới nhất lên đầu theo createdAt
            vehicleDocs.sort((a, b) -> {
                Date da = a.getDate("createdAt");
                Date db = b.getDate("createdAt");
                long ta = da != null ? da.getTime() : 0L;
                long tb = db != null ? db.getTime() : 0L;
                return Long.compare(tb, ta);
            });

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"));
            List<VehicleItem> list = new ArrayList<>();
            for (DocumentSnapshot vDoc : vehicleDocs) {
                String vid = vDoc.getId();
                String plateRaw = vDoc.getString("plateRaw");
                String plateCanonical = vDoc.getString("plateCanonical");
                if (plateRaw == null || plateRaw.isEmpty()) plateRaw = plateCanonical != null ? plateCanonical : "";
                if (plateCanonical == null || plateCanonical.isEmpty()) {
                    plateCanonical = normalizePlate(plateRaw);
                }
                String brand = vDoc.getString("brand");
                String modelField = vDoc.getString("model");
                String type = vDoc.getString("type");
                String color = vDoc.getString("color");
                String regUrl = vDoc.getString("registrationCardPhotoKey");
                String photoUrl = vDoc.getString("photoObjectKey");
                Date createdAt = vDoc.getDate("createdAt");

                String status = reqStatusByVehicleId.get(vid);
                if (status == null || status.isEmpty()) {
                    String directStatus = vDoc.getString("status");
                    status = (directStatus != null && !directStatus.isEmpty()) ? directStatus : "PENDING";
                }

                String displayModel;
                if (brand != null && !brand.isEmpty() && modelField != null && !modelField.isEmpty()) {
                    displayModel = brand + " " + modelField;
                } else if (brand != null && !brand.isEmpty()) {
                    displayModel = brand;
                } else if (modelField != null && !modelField.isEmpty()) {
                    displayModel = modelField;
                } else {
                    displayModel = vehicleTypeLabel(type);
                }

                String stamp = ("APPROVED".equalsIgnoreCase(status) || "VERIFIED".equalsIgnoreCase(status))
                        ? "PG-" + vid.substring(0, Math.min(8, vid.length())).toUpperCase(Locale.ROOT)
                        : "Đang chờ duyệt";
                String dateStr = createdAt != null ? sdf.format(createdAt) : "";

                list.add(new VehicleItem(
                        vid,
                        plateRaw,
                        plateCanonical,
                        type != null ? type : "MOTORBIKE",
                        displayModel,
                        color != null ? color : "",
                        status,
                        stamp,
                        dateStr,
                        ownerName,
                        uid,
                        regUrl,
                        photoUrl
                ));
            }
            return list;
        });
    }

    /**
     * Đăng ký phương tiện mới cho sinh viên lên Firestore (và tải ảnh bằng chứng lên Cloudinary nếu có).
     */
    public Task<Boolean> registerVehicle(@Nullable Context context,
                                         @NonNull String plateRaw,
                                         @NonNull String vehicleType,
                                         @Nullable String brand,
                                         @Nullable String color,
                                         @Nullable File registrationCardFile,
                                         @Nullable File vehiclePhotoFile) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return Tasks.forException(new IllegalStateException("Chưa đăng nhập tài khoản sinh viên"));
        }

        String uid = user.getUid();
        String plateCanonical = normalizePlate(plateRaw);
        if (plateCanonical.length() < 5 || plateCanonical.length() > 12) {
            return Tasks.forException(new IllegalArgumentException("Biển số xe không đúng định dạng"));
        }

        Task<EvidenceImage> regUploadTask = registrationCardFile != null
                ? cloudinaryDataSource.uploadImage(registrationCardFile, "REGISTRATION_CARD", false)
                : Tasks.forResult(null);
        Task<EvidenceImage> vehUploadTask = vehiclePhotoFile != null
                ? cloudinaryDataSource.uploadImage(vehiclePhotoFile, "VEHICLE_PHOTO", false)
                : Tasks.forResult(null);

        return Tasks.whenAll(regUploadTask, vehUploadTask).onSuccessTask(ignored -> {
            String regUrl = regUploadTask.getResult() != null ? regUploadTask.getResult().secureUrl : "";
            String photoUrl = vehUploadTask.getResult() != null ? vehUploadTask.getResult().secureUrl : "";

            long now = System.currentTimeMillis();
            String vehicleId = "veh_" + uid + "_" + now;
            String reqId = "req_" + uid + "_" + now;

            DocumentReference vehicleDoc = firestore.collection("vehicles").document(vehicleId);
            DocumentReference requestDoc = firestore.collection("vehicleVerificationRequests").document(reqId);

            Map<String, Object> vData = new HashMap<>();
            vData.put("plateRaw", plateRaw.trim());
            vData.put("plateCanonical", plateCanonical);
            vData.put("plateNormVersion", 1);
            vData.put("type", vehicleType);
            if (brand != null && !brand.trim().isEmpty()) vData.put("brand", brand.trim());
            if (color != null && !color.trim().isEmpty()) vData.put("color", color.trim());
            if (!regUrl.isEmpty()) vData.put("registrationCardPhotoKey", regUrl);
            if (!photoUrl.isEmpty()) vData.put("photoObjectKey", photoUrl);
            vData.put("createdBy", uid);
            vData.put("createdAt", FieldValue.serverTimestamp());
            vData.put("schemaVersion", 1);

            Map<String, Object> rData = new HashMap<>();
            rData.put("requesterUid", uid);
            rData.put("vehicleId", vehicleId);
            rData.put("requestType", "NEW_REGISTRATION");
            rData.put("status", "PENDING");
            rData.put("requestedAt", FieldValue.serverTimestamp());
            rData.put("schemaVersion", 1);

            return firestore.runTransaction(transaction -> {
                transaction.set(vehicleDoc, vData);
                transaction.set(requestDoc, rData);
                return true;
            });
        });
    }

    /**
     * Tải danh sách vi phạm thuộc phạm vi xe đã xác minh của sinh viên từ Firestore.
     */
    public Task<List<ViolationItem>> getMyViolations(@Nullable Context context) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return Tasks.forResult(new ArrayList<>());
        }
        String uid = user.getUid();

        TaskCompletionSource<List<ViolationItem>> tcs = new TaskCompletionSource<>();
        getMyVehicles(context).addOnCompleteListener(vTask -> {
            List<String> verifiedPlates = new ArrayList<>();
            if (vTask.isSuccessful() && vTask.getResult() != null) {
                for (VehicleItem v : vTask.getResult()) {
                    if (v.isApproved()) {
                        if (!v.plateCanonical.isEmpty() && !verifiedPlates.contains(v.plateCanonical)) {
                            verifiedPlates.add(v.plateCanonical);
                        }
                        if (!v.plateNumber.isEmpty() && !verifiedPlates.contains(v.plateNumber)) {
                            verifiedPlates.add(v.plateNumber);
                        }
                    }
                }
            }

            List<Task<QuerySnapshot>> queries = new ArrayList<>();
            // 1. Truy vấn các vi phạm đã gắn trực tiếp ownerUid (đã xác minh chủ xe)
            queries.add(firestore.collection("violations")
                    .whereEqualTo("ownerUid", uid)
                    .limit(50)
                    .get());

            // 2. Truy vấn theo các biển số đã được duyệt (tối đa 10 giá trị mỗi whereIn)
            if (!verifiedPlates.isEmpty()) {
                List<String> subset = verifiedPlates.size() > 10 ? verifiedPlates.subList(0, 10) : verifiedPlates;
                queries.add(firestore.collection("violations")
                        .whereIn("plateCanonical", subset)
                        .limit(50)
                        .get());
                queries.add(firestore.collection("violations")
                        .whereIn("plateFinal", subset)
                        .limit(50)
                        .get());
            }

            Tasks.whenAllComplete(queries).addOnCompleteListener(allTask -> {
                Map<String, ViolationItem> merged = new LinkedHashMap<>();
                for (Task<QuerySnapshot> q : queries) {
                    if (q.isSuccessful() && q.getResult() != null) {
                        for (DocumentSnapshot doc : q.getResult().getDocuments()) {
                            if (!merged.containsKey(doc.getId())) {
                                merged.put(doc.getId(), mapViolation(doc, ""));
                            }
                        }
                    }
                }
                List<ViolationItem> result = new ArrayList<>(merged.values());
                result.sort((a, b) -> Long.compare(b.capturedAtMillis, a.capturedAtMillis));
                tcs.setResult(result);
            });
        });
        return tcs.getTask();
    }

    /**
     * Tải chi tiết một biên bản vi phạm kèm URL ảnh bằng chứng chính từ Firestore.
     */
    public Task<ViolationItem> getViolationById(@NonNull String violationId) {
        DocumentReference vioRef = firestore.collection("violations").document(violationId);
        DocumentReference primaryRef = vioRef.collection("evidence").document(violationId + "_primary");

        Task<DocumentSnapshot> vioTask = vioRef.get();
        Task<DocumentSnapshot> evTask = primaryRef.get();

        return Tasks.whenAllComplete(vioTask, evTask).continueWith(allTask -> {
            if (!vioTask.isSuccessful() || vioTask.getResult() == null || !vioTask.getResult().exists()) {
                throw new IllegalStateException("Không tìm thấy hồ sơ vi phạm " + violationId);
            }
            String evidenceUrl = "";
            if (evTask.isSuccessful() && evTask.getResult() != null && evTask.getResult().exists()) {
                String url = evTask.getResult().getString("secureUrl");
                if (url != null) evidenceUrl = url;
            }
            return mapViolation(vioTask.getResult(), evidenceUrl);
        });
    }

    /**
     * Gửi khiếu nại giải trình cho biên bản vi phạm lên Firestore.
     */
    public Task<Boolean> submitViolationComplaint(@Nullable Context context,
                                                  @NonNull String violationId,
                                                  @NonNull String reason) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return Tasks.forException(new IllegalStateException("Chưa đăng nhập"));
        }

        DocumentReference vioRef = firestore.collection("violations").document(violationId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("complaintReason", reason.trim());
        updates.put("complaintAt", FieldValue.serverTimestamp());
        updates.put("status", "APPEALED");

        return vioRef.update(updates).continueWith(task -> {
            if (!task.isSuccessful()) {
                throw Objects.requireNonNull(task.getException());
            }
            return true;
        });
    }

    /**
     * Tải lịch sử vào/ra bãi xe của sinh viên từ Firestore collection parkingHistory.
     */
    public Task<List<StudentParkingRecord>> getParkingHistory(@Nullable Context context, int filterMode) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return Tasks.forResult(new ArrayList<>());
        }

        long now = System.currentTimeMillis();
        long limitTime;
        if (filterMode == 0) {
            limitTime = now - 24 * 3600 * 1000L;
        } else if (filterMode == 1) {
            limitTime = now - 7L * 24 * 3600 * 1000L;
        } else {
            limitTime = now - 30L * 24 * 3600 * 1000L;
        }

        return firestore.collection("parkingHistory")
                .whereEqualTo("ownerUid", user.getUid())
                .limit(100)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        return new ArrayList<>();
                    }
                    SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", new Locale("vi", "VN"));
                    SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN"));
                    String todayStr = dateFmt.format(new Date(now));
                    String yesterdayStr = dateFmt.format(new Date(now - 24 * 3600 * 1000L));

                    List<StudentParkingRecord> result = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        Date tsDate = doc.getDate("timestamp");
                        if (tsDate == null) tsDate = doc.getDate("createdAt");
                        long ts = tsDate != null ? tsDate.getTime() : 0L;
                        if (ts < limitTime) continue;

                        String plate = doc.getString("plateNumber");
                        if (plate == null) plate = doc.getString("plateCanonical");
                        String actionType = doc.getString("actionType");
                        String gateName = doc.getString("gateName");
                        String status = doc.getString("status");

                        String dateFormatted = tsDate != null ? dateFmt.format(tsDate) : "";
                        String dateTitle = todayStr.equals(dateFormatted) ? "Hôm nay"
                                : yesterdayStr.equals(dateFormatted) ? "Hôm qua" : dateFormatted;
                        String timeFormatted = tsDate != null ? timeFmt.format(tsDate) : "--:--";

                        result.add(new StudentParkingRecord(
                                doc.getId(),
                                plate != null ? plate : "",
                                actionType != null ? actionType : "ENTRY",
                                dateTitle,
                                timeFormatted,
                                gateName != null ? gateName : "Cổng bãi xe",
                                status != null ? status : ("ENTRY".equalsIgnoreCase(actionType) ? "IN_LOT" : "EXITED"),
                                ts
                        ));
                    }
                    result.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                    return result;
                });
    }

    /**
     * Tải danh sách thông báo thật của sinh viên từ Firestore collection notifications.
     */
    public Task<List<StudentNotificationItem>> getNotifications(@Nullable Context context,
                                                                @Nullable String filterType) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return Tasks.forResult(new ArrayList<>());
        }

        return firestore.collection("notifications")
                .whereEqualTo("recipientUid", user.getUid())
                .limit(50)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        return new ArrayList<>();
                    }
                    SimpleDateFormat fmt = new SimpleDateFormat("HH:mm • dd/MM", new Locale("vi", "VN"));
                    List<StudentNotificationItem> result = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        String type = doc.getString("type");
                        if (type == null) type = "SYSTEM";

                        if (filterType != null && !"ALL".equalsIgnoreCase(filterType)) {
                            if ("VIOLATION".equalsIgnoreCase(filterType)
                                    && !"VIOLATION".equalsIgnoreCase(type)
                                    && !"SECURITY_ALERT".equalsIgnoreCase(type)) {
                                continue;
                            } else if (!"VIOLATION".equalsIgnoreCase(filterType)
                                    && !type.equalsIgnoreCase(filterType)) {
                                continue;
                            }
                        }

                        Date createdAt = doc.getDate("createdAt");
                        long ts = createdAt != null ? createdAt.getTime() : 0L;
                        String timeDisplay = createdAt != null ? fmt.format(createdAt) : "";
                        boolean isRead = Boolean.TRUE.equals(doc.getBoolean("read"));
                        boolean requiresAction = Boolean.TRUE.equals(doc.getBoolean("requiresAction"));
                        String relatedVioId = doc.getString("relatedViolationId");
                        String userResponse = doc.getString("userResponse");

                        StudentNotificationItem item = new StudentNotificationItem(
                                doc.getId(),
                                doc.getString("title") != null ? doc.getString("title") : "Thông báo",
                                doc.getString("message") != null ? doc.getString("message") : "",
                                type,
                                timeDisplay,
                                ts,
                                isRead,
                                requiresAction,
                                relatedVioId
                        );
                        if (userResponse != null) {
                            item.setUserResponse(userResponse);
                        }
                        result.add(item);
                    }
                    result.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                    return result;
                });
    }

    /**
     * Phản hồi thông báo cảnh báo xe vào lạ ("Là tôi" / "Không phải tôi") trực tiếp trên Firestore.
     * Nếu "Không phải tôi", tự động tạo cảnh báo khẩn cấp trong security_alerts để Bảo vệ nhận realtime.
     */
    public Task<Boolean> respondToNotification(@Nullable Context context,
                                               @NonNull String notifId,
                                               boolean isMe) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return Tasks.forException(new IllegalStateException("Chưa đăng nhập"));
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("requiresAction", false);
        updates.put("userResponse", isMe ? "IS_ME" : "NOT_ME");
        updates.put("read", true);
        updates.put("respondedAt", FieldValue.serverTimestamp());

        return firestore.collection("notifications").document(notifId)
                .update(updates)
                .onSuccessTask(ignored -> {
                    if (isMe) {
                        return Tasks.forResult(true);
                    }
                    Map<String, Object> alertData = new HashMap<>();
                    alertData.put("alertType", "SUSPICIOUS_ENTRY");
                    alertData.put("severity", "HIGH");
                    alertData.put("status", "UNHANDLED");
                    alertData.put("title", "Sinh viên báo nghi vấn mạo danh biển số");
                    alertData.put("description", "Chủ xe xác nhận lượt vào bãi gần đây không phải do mình thực hiện (Thông báo #" + notifId + ").");
                    alertData.put("reportedByUid", user.getUid());
                    alertData.put("location", "Bãi giữ xe");
                    alertData.put("createdAt", FieldValue.serverTimestamp());
                    return firestore.collection("security_alerts").add(alertData).continueWith(t -> true);
                });
    }

    /**
     * Tổng hợp dữ liệu trang chủ Sinh viên (Xe của tôi) hoàn toàn từ Firestore.
     */
    public Task<StudentDashboardData> getDashboardData(@Nullable Context context) {
        Task<UserProfile> profileTask = getMyProfile(context);
        Task<List<VehicleItem>> vehiclesTask = getMyVehicles(context);
        Task<List<ViolationItem>> violationsTask = getMyViolations(context);
        Task<List<StudentParkingRecord>> historyTask = getParkingHistory(context, 1);

        return Tasks.whenAllComplete(profileTask, vehiclesTask, violationsTask, historyTask)
                .continueWith(task -> {
                    StudentDashboardData data = new StudentDashboardData();

                    if (profileTask.isSuccessful() && profileTask.getResult() != null) {
                        UserProfile p = profileTask.getResult();
                        data.setStudentName(p.getFullName() != null && !p.getFullName().isEmpty()
                                ? p.getFullName() : "Sinh viên");
                        data.setStudentCode(p.getStudentCode() != null ? p.getStudentCode() : "");
                    } else {
                        data.setStudentName("Sinh viên");
                    }

                    if (vehiclesTask.isSuccessful() && vehiclesTask.getResult() != null
                            && !vehiclesTask.getResult().isEmpty()) {
                        VehicleItem mainVeh = vehiclesTask.getResult().get(0);
                        data.setMainPlate(mainVeh.plateNumber);
                        String details = mainVeh.model;
                        if (!mainVeh.color.isEmpty()) {
                            details = details.isEmpty() ? mainVeh.color : details + " • " + mainVeh.color;
                        }
                        data.setVehicleDetails(details);
                        data.setVehicleStatus(mainVeh.status);
                    } else {
                        data.setMainPlate("Chưa đăng ký xe");
                        data.setVehicleDetails("Nhấn nút bên dưới để gửi yêu cầu đăng ký phương tiện");
                        data.setVehicleStatus("NONE");
                    }

                    int unprocCount = 0;
                    if (violationsTask.isSuccessful() && violationsTask.getResult() != null) {
                        for (ViolationItem vio : violationsTask.getResult()) {
                            if (vio.isUnprocessed()) {
                                unprocCount++;
                            }
                        }
                    }
                    data.setUnprocessedViolationsCount(unprocCount);

                    if (historyTask.isSuccessful() && historyTask.getResult() != null
                            && !historyTask.getResult().isEmpty()) {
                        List<StudentParkingRecord> records = historyTask.getResult();
                        data.setRecentHistory(records.size() > 2 ? new ArrayList<>(records.subList(0, 2)) : records);
                        StudentParkingRecord latest = records.get(0);
                        if ("ENTRY".equalsIgnoreCase(latest.getActionType())
                                || "IN_LOT".equalsIgnoreCase(latest.getStatus())) {
                            data.setInLot(true);
                            data.setInLotTime(latest.getDateTitle() + " • " + latest.getTimeFormatted());
                        } else {
                            data.setInLot(false);
                            data.setInLotTime("Ngoài bãi • " + latest.getTimeFormatted());
                        }
                    } else {
                        data.setInLot(false);
                        data.setInLotTime("Chưa ghi nhận lượt gửi");
                    }

                    return data;
                });
    }

    @NonNull
    public static ViolationItem mapViolation(@NonNull DocumentSnapshot doc, @NonNull String evidenceUrl) {
        String id = doc.getId();
        String plate = doc.getString("plateFinal");
        String plateCanonical = doc.getString("plateCanonical");
        if (plate == null) plate = "";
        if (plateCanonical == null || plateCanonical.isEmpty()) {
            plateCanonical = normalizePlate(plate);
        }
        String typeCode = doc.getString("typeCode");
        String violationName = formatViolationType(typeCode);
        String scopeId = doc.getString("scopeId");
        String status = doc.getString("status");
        String note = doc.getString("note");
        String complaintReason = doc.getString("complaintReason");
        String penalty = doc.getString("penaltyAmount");
        String guardUid = doc.getString("guardUid");
        String guardName = doc.getString("guardName");
        String primaryUrl = evidenceUrl.isEmpty() ? doc.getString("primaryEvidenceUrl") : evidenceUrl;
        Date capturedAt = doc.getDate("capturedAt");
        if (capturedAt == null) capturedAt = doc.getDate("createdAt");
        long capturedMillis = capturedAt != null ? capturedAt.getTime() : 0L;
        String timeStr = capturedAt != null
                ? new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN")).format(capturedAt)
                : "";

        return new ViolationItem(
                id,
                plate.isEmpty() ? "Chưa rõ biển số" : plate,
                plateCanonical,
                typeCode != null ? typeCode : "",
                violationName,
                scopeId != null && !scopeId.isEmpty() ? scopeId : "Khu vực bãi xe",
                timeStr,
                capturedMillis,
                penalty != null ? penalty : "",
                status != null ? status : "PENDING",
                guardUid != null ? guardUid : "",
                guardName != null && !guardName.isEmpty() ? guardName : (guardUid != null ? guardUid : "Bảo vệ ca trực"),
                note != null ? note : "",
                complaintReason != null ? complaintReason : "",
                doc.getDouble("latitude"),
                doc.getDouble("longitude"),
                doc.getDouble("locationAccuracy"),
                primaryUrl != null ? primaryUrl : "",
                ""
        );
    }

    public static String formatViolationType(@Nullable String code) {
        if (code == null || code.isEmpty()) return "Vi phạm quy định bãi xe";
        if ("PARKING_WRONG_AREA".equals(code)) return "Đỗ xe sai khu vực quy định";
        if ("BLOCKING_ENTRANCE".equals(code)) return "Chắn lối thoát hiểm & cứu hỏa";
        if ("NO_PARKING_ZONE".equals(code)) return "Đỗ tại khu vực cấm đỗ";
        if ("OTHER".equals(code)) return "Vi phạm quy định khác";
        return code;
    }

    public static String vehicleTypeLabel(@Nullable String type) {
        if ("CAR".equalsIgnoreCase(type)) return "Ô tô";
        if ("ELECTRIC_BIKE".equalsIgnoreCase(type)) return "Xe đạp điện";
        return "Xe máy";
    }

    public static String normalizePlate(@Nullable String raw) {
        if (raw == null) return "";
        return raw.replaceAll("[^a-zA-Z0-9]", "").toUpperCase(Locale.ROOT);
    }
}
