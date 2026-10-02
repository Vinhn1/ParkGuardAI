package com.example.parkguardai.data.model;

import java.util.Date;

/**
 * Biểu diễn một yêu cầu xác minh hoặc thay đổi quyền sử dụng phương tiện.
 * Sinh viên có thể tạo yêu cầu PENDING; kết quả và thông tin người duyệt
 * phải được ghi qua nghiệp vụ có kiểm quyền, không qua setter ở UI.
 */
public class VehicleVerificationRequest {

    /**
     * ID yêu cầu xác minh.
     */
    private String id;

    /**
     * UID của người gửi yêu cầu.
     */
    private String requesterUid;

    /**
     * ID phương tiện cần xác minh.
     */
    private String vehicleId;

    /**
     * Loại yêu cầu như đăng ký mới hoặc chuyển nhượng; danh mục chờ chính sách duyệt.
     */
    private String requestType;

    /**
     * Trạng thái xử lý yêu cầu.
     */
    private VehicleVerificationRequestStatus status;

    /**
     * Thời điểm gửi yêu cầu.
     */
    private Date requestedAt;

    /**
     * Thời điểm duyệt hoặc từ chối; rỗng khi còn chờ.
     */
    private Date reviewedAt;

    /**
     * UID người được phân quyền xét duyệt.
     */
    private String reviewerUid;

    /**
     * Lý do duyệt, từ chối hoặc ghi chú xử lý theo chính sách.
     */
    private String reason;

    /**
     * Khóa ảnh/chứng cứ trong Storage, không phải URL công khai.
     */
    private String evidenceObjectKey;

    /**
     * Phiên bản cấu trúc tài liệu.
     */
    private Integer schemaVersion;

    /**
     * Constructor rỗng để khởi tạo model và hỗ trợ ánh xạ dữ liệu sau này.
     */
    public VehicleVerificationRequest() {
    }

    /**
     * Khởi tạo bản ghi với các trường dữ liệu tương ứng. Việc kiểm quyền,
     * kiểm tra quan hệ và tính hợp lệ phải thực hiện tại repository/backend.
     */
    public VehicleVerificationRequest(
            String id,
            String requesterUid,
            String vehicleId,
            String requestType,
            VehicleVerificationRequestStatus status,
            Date requestedAt,
            Date reviewedAt,
            String reviewerUid,
            String reason,
            String evidenceObjectKey,
            Integer schemaVersion
    ) {
        this.id = id;
        this.requesterUid = requesterUid;
        this.vehicleId = vehicleId;
        this.requestType = requestType;
        this.status = status;
        this.requestedAt = requestedAt;
        this.reviewedAt = reviewedAt;
        this.reviewerUid = reviewerUid;
        this.reason = reason;
        this.evidenceObjectKey = evidenceObjectKey;
        this.schemaVersion = schemaVersion;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRequesterUid() {
        return requesterUid;
    }

    public void setRequesterUid(String requesterUid) {
        this.requesterUid = requesterUid;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public VehicleVerificationRequestStatus getStatus() {
        return status;
    }

    public void setStatus(VehicleVerificationRequestStatus status) {
        this.status = status;
    }

    public Date getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Date requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Date getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Date reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewerUid() {
        return reviewerUid;
    }

    public void setReviewerUid(String reviewerUid) {
        this.reviewerUid = reviewerUid;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getEvidenceObjectKey() {
        return evidenceObjectKey;
    }

    public void setEvidenceObjectKey(String evidenceObjectKey) {
        this.evidenceObjectKey = evidenceObjectKey;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

}
