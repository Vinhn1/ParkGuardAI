package com.example.parkguardai.data.model;

import java.util.Date;

/**
 * Biểu diễn metadata hồ sơ tại violations/{violationId}.
 * Bằng chứng ảnh nằm ở ViolationEvidence và Cloud Storage; các trường vị trí
 * dưới đây là bản sao của ảnh chính để truy vấn, phải khớp snapshot ảnh chính.
 * Model không tự kết luận vi phạm hoặc xác định chủ xe từ biển số.
 */
public class ViolationRecord {

    /**
     * ID hồ sơ vi phạm.
     */
    private String id;

    /**
     * ID xe được liên kết sau kiểm tra đáng tin cậy; có thể rỗng với xe chưa đăng ký.
     */
    private String vehicleId;

    /**
     * UID bảo vệ ghi nhận, phải lấy từ phiên đã xác thực ở nguồn dữ liệu.
     */
    private String guardUid;

    /**
     * Phạm vi khu vực hoặc ca dùng để kiểm quyền bảo vệ.
     */
    private String scopeId;

    /**
     * ID ảnh hiện trường chính; ảnh phụ không thay vị trí hồ sơ.
     */
    private String primaryEvidenceId;

    /**
     * Chuỗi OCR gợi ý trước khi bảo vệ kiểm tra hoặc sửa.
     */
    private String plateOcrRaw;

    /**
     * Biển số cuối cùng do bảo vệ xác nhận; không tự chứng minh chủ xe.
     */
    private String plateFinal;

    /**
     * Nguồn kết quả biển số, ví dụ OCR hoặc nhập/chỉnh thủ công.
     */
    private String recognitionSource;

    /**
     * Mã loại vi phạm tại thời điểm ghi nhận.
     */
    private String typeCode;

    /**
     * Phiên bản danh mục loại vi phạm để giữ nguyên nghĩa hồ sơ cũ.
     */
    private Integer typeVersion;

    /**
     * Trạng thái xử lý; chuyển trạng thái phải được kiểm quyền và ghi audit.
     */
    private ViolationStatus status;

    /**
     * Ghi chú nghiệp vụ của người ghi nhận, không chứa dữ liệu cá nhân thừa.
     */
    private String note;

    /**
     * Thời điểm hành vi được người ghi nhận quan sát theo nghiệp vụ.
     */
    private Date occurredAt;

    /**
     * Thời điểm chụp ảnh chính, khác với thời điểm lưu hồ sơ.
     */
    private Date capturedAt;

    /**
     * Trạng thái đánh giá vị trí ảnh chính; danh mục cuối chờ DEC-008A.
     */
    private String locationStatus;

    /**
     * Nguồn vị trí ảnh chính, phân biệt tự động và thủ công.
     */
    private String locationSource;

    /**
     * Vĩ độ ảnh chính; rỗng khi không có vị trí hợp lệ.
     */
    private Double latitude;

    /**
     * Kinh độ ảnh chính; rỗng khi không có vị trí hợp lệ.
     */
    private Double longitude;

    /**
     * Bán kính sai số ngang ước tính theo mét; rỗng nếu không đo được.
     */
    private Double locationAccuracy;

    /**
     * Thời điểm thiết bị đo vị trí gắn với ảnh chính.
     */
    private Date locationTimestamp;

    /**
     * Mã khu vực khi sử dụng phương án vị trí thủ công được duyệt.
     */
    private String areaCode;

    /**
     * Thời điểm hồ sơ được nguồn đáng tin cậy tạo trên server.
     */
    private Date createdAt;

    /**
     * Khóa chống tạo hồ sơ trùng khi gửi lại cùng một yêu cầu.
     */
    private String idempotencyKey;

    /**
     * Phiên bản cấu trúc tài liệu.
     */
    private Integer schemaVersion;

    /**
     * Constructor rỗng cho khởi tạo và ánh xạ dữ liệu.
     */
    public ViolationRecord() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getGuardUid() {
        return guardUid;
    }

    public void setGuardUid(String guardUid) {
        this.guardUid = guardUid;
    }

    public String getScopeId() {
        return scopeId;
    }

    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
    }

    public String getPrimaryEvidenceId() {
        return primaryEvidenceId;
    }

    public void setPrimaryEvidenceId(String primaryEvidenceId) {
        this.primaryEvidenceId = primaryEvidenceId;
    }

    public String getPlateOcrRaw() {
        return plateOcrRaw;
    }

    public void setPlateOcrRaw(String plateOcrRaw) {
        this.plateOcrRaw = plateOcrRaw;
    }

    public String getPlateFinal() {
        return plateFinal;
    }

    public void setPlateFinal(String plateFinal) {
        this.plateFinal = plateFinal;
    }

    public String getRecognitionSource() {
        return recognitionSource;
    }

    public void setRecognitionSource(String recognitionSource) {
        this.recognitionSource = recognitionSource;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public Integer getTypeVersion() {
        return typeVersion;
    }

    public void setTypeVersion(Integer typeVersion) {
        this.typeVersion = typeVersion;
    }

    public ViolationStatus getStatus() {
        return status;
    }

    public void setStatus(ViolationStatus status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Date getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Date occurredAt) {
        this.occurredAt = occurredAt;
    }

    public Date getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(Date capturedAt) {
        this.capturedAt = capturedAt;
    }

    public String getLocationStatus() {
        return locationStatus;
    }

    public void setLocationStatus(String locationStatus) {
        this.locationStatus = locationStatus;
    }

    public String getLocationSource() {
        return locationSource;
    }

    public void setLocationSource(String locationSource) {
        this.locationSource = locationSource;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLocationAccuracy() {
        return locationAccuracy;
    }

    public void setLocationAccuracy(Double locationAccuracy) {
        this.locationAccuracy = locationAccuracy;
    }

    public Date getLocationTimestamp() {
        return locationTimestamp;
    }

    public void setLocationTimestamp(Date locationTimestamp) {
        this.locationTimestamp = locationTimestamp;
    }

    public String getAreaCode() {
        return areaCode;
    }

    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

}
