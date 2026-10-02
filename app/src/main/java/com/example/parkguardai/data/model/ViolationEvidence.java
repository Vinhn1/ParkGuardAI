package com.example.parkguardai.data.model;

import java.util.Date;

/**
 * Lưu metadata của một ảnh bằng chứng thuộc hồ sơ vi phạm.
 * Ảnh gốc nằm ở Cloud Storage, còn document evidence chỉ giữ khóa
 * đối tượng và vị trí tại lúc chụp. Ảnh phụ không thay vị trí ảnh chính.
 */
public class ViolationEvidence {

    /**
     * ID ảnh bằng chứng.
     */
    private String id;

    /**
     * ID hồ sơ chứa ảnh; cần kiểm quan hệ trước khi lưu.
     */
    private String violationId;

    /**
     * Loại ảnh như hiện trường gốc, ảnh phụ hoặc vùng biển số cắt.
     */
    private String kind;

    /**
     * Đánh dấu ảnh hiện trường chính; mỗi hồ sơ chỉ được có một ảnh chính.
     */
    private Boolean isPrimary;

    /**
     * Khóa đối tượng trong Storage, không phải URL truy cập công khai.
     */
    private String objectKey;

    /**
     * Thời điểm chụp ảnh, khác với thời điểm upload hoặc lưu hồ sơ.
     */
    private Date capturedAt;

    /**
     * Mã băm nội dung để kiểm tra tính toàn vẹn ảnh.
     */
    private String sha256;

    /**
     * Kiểu nội dung ảnh phải được kiểm tra trước upload.
     */
    private String mimeType;

    /**
     * Kích thước ảnh tính bằng byte, cần giới hạn ở tầng lưu trữ.
     */
    private Long sizeBytes;

    /**
     * UID của người tạo ảnh trong quy trình ghi nhận.
     */
    private String createdBy;

    /**
     * ID ảnh gốc nếu đây là ảnh crop hoặc dẫn xuất.
     */
    private String sourceEvidenceId;

    /**
     * Snapshot vị trí của chính ảnh này; có thể thiếu tọa độ nhưng phải có trạng thái.
     */
    private LocationSnapshot location;

    /**
     * Phiên bản cấu trúc tài liệu.
     */
    private Integer schemaVersion;

    /**
     * Constructor rỗng để khởi tạo model và hỗ trợ ánh xạ dữ liệu sau này.
     */
    public ViolationEvidence() {
    }

    /**
     * Khởi tạo bản ghi với các trường dữ liệu tương ứng. Việc kiểm quyền,
     * kiểm tra quan hệ và tính hợp lệ phải thực hiện tại repository/backend.
     */
    public ViolationEvidence(
            String id,
            String violationId,
            String kind,
            Boolean isPrimary,
            String objectKey,
            Date capturedAt,
            String sha256,
            String mimeType,
            Long sizeBytes,
            String createdBy,
            String sourceEvidenceId,
            LocationSnapshot location,
            Integer schemaVersion
    ) {
        this.id = id;
        this.violationId = violationId;
        this.kind = kind;
        this.isPrimary = isPrimary;
        this.objectKey = objectKey;
        this.capturedAt = capturedAt;
        this.sha256 = sha256;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.createdBy = createdBy;
        this.sourceEvidenceId = sourceEvidenceId;
        this.location = location;
        this.schemaVersion = schemaVersion;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getViolationId() {
        return violationId;
    }

    public void setViolationId(String violationId) {
        this.violationId = violationId;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Boolean getIsPrimary() {
        return isPrimary;
    }

    public void setIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public Date getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(Date capturedAt) {
        this.capturedAt = capturedAt;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getSourceEvidenceId() {
        return sourceEvidenceId;
    }

    public void setSourceEvidenceId(String sourceEvidenceId) {
        this.sourceEvidenceId = sourceEvidenceId;
    }

    public LocationSnapshot getLocation() {
        return location;
    }

    public void setLocation(LocationSnapshot location) {
        this.location = location;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

}
