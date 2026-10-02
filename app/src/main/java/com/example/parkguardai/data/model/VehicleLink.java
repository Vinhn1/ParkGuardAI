package com.example.parkguardai.data.model;

import java.util.Date;

/**
 * Lưu lịch sử liên kết giữa tài khoản và phương tiện theo khoảng thời gian.
 * Chỉ liên kết VERIFIED còn hiệu lực mới có thể tham gia xét quyền xem
 * hồ sơ; model này không tự xác nhận quyền sử dụng xe.
 */
public class VehicleLink {

    /**
     * ID liên kết, không được thay bằng biển số hoặc UID.
     */
    private String id;

    /**
     * UID của tài khoản yêu cầu hoặc được liên kết.
     */
    private String uid;

    /**
     * ID phương tiện; quan hệ phải được kiểm tra ở nguồn dữ liệu.
     */
    private String vehicleId;

    /**
     * Trạng thái liên kết do quy trình xác minh quyết định.
     */
    private VehicleLinkStatus status;

    /**
     * Mốc bắt đầu hiệu lực sau khi được duyệt; có thể chưa có khi chờ duyệt.
     */
    private Date validFrom;

    /**
     * Mốc kết thúc hiệu lực; rỗng khi liên kết còn mở.
     */
    private Date validTo;

    /**
     * ID yêu cầu xác minh tạo ra hoặc thay đổi liên kết.
     */
    private String verificationRequestId;

    /**
     * UID người xét duyệt; chỉ nguồn đáng tin cậy được ghi.
     */
    private String reviewerUid;

    /**
     * Thời điểm xét duyệt từ nguồn đáng tin cậy.
     */
    private Date reviewedAt;

    /**
     * Phiên bản cấu trúc tài liệu.
     */
    private Integer schemaVersion;

    /**
     * Constructor rỗng để khởi tạo model và hỗ trợ ánh xạ dữ liệu sau này.
     */
    public VehicleLink() {
    }

    /**
     * Khởi tạo bản ghi với các trường dữ liệu tương ứng. Việc kiểm quyền,
     * kiểm tra quan hệ và tính hợp lệ phải thực hiện tại repository/backend.
     */
    public VehicleLink(
            String id,
            String uid,
            String vehicleId,
            VehicleLinkStatus status,
            Date validFrom,
            Date validTo,
            String verificationRequestId,
            String reviewerUid,
            Date reviewedAt,
            Integer schemaVersion
    ) {
        this.id = id;
        this.uid = uid;
        this.vehicleId = vehicleId;
        this.status = status;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.verificationRequestId = verificationRequestId;
        this.reviewerUid = reviewerUid;
        this.reviewedAt = reviewedAt;
        this.schemaVersion = schemaVersion;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public VehicleLinkStatus getStatus() {
        return status;
    }

    public void setStatus(VehicleLinkStatus status) {
        this.status = status;
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    public Date getValidTo() {
        return validTo;
    }

    public void setValidTo(Date validTo) {
        this.validTo = validTo;
    }

    public String getVerificationRequestId() {
        return verificationRequestId;
    }

    public void setVerificationRequestId(String verificationRequestId) {
        this.verificationRequestId = verificationRequestId;
    }

    public String getReviewerUid() {
        return reviewerUid;
    }

    public void setReviewerUid(String reviewerUid) {
        this.reviewerUid = reviewerUid;
    }

    public Date getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Date reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

}
