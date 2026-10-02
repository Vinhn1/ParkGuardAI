package com.example.parkguardai.data.model;

import java.util.Date;

/**
 * Biểu diễn khóa chiếm dụng một biển số đã xác minh ở backend.
 * Bản ghi plateClaims/{plateKey} chỉ phục vụ transaction chống cấp
 * VERIFIED đồng thời; Android không được đọc hoặc ghi collection này.
 */
public class PlateClaim {

    /**
     * ID xác định của biển số chuẩn hóa do backend tạo, không do Android quyết định.
     */
    private String plateKey;

    /**
     * Biển số chuẩn hóa tương ứng; không tự chứng minh quyền sở hữu.
     */
    private String plateCanonical;

    /**
     * ID phương tiện đang giữ claim có hiệu lực.
     */
    private String vehicleId;

    /**
     * ID liên kết VERIFIED đang được claim bảo vệ.
     */
    private String activeLinkId;

    /**
     * Thời điểm backend xác lập claim.
     */
    private Date claimedAt;

    /**
     * Phiên bản claim phục vụ kiểm tra thay đổi đồng thời.
     */
    private Integer version;

    /**
     * Phiên bản cấu trúc tài liệu.
     */
    private Integer schemaVersion;

    /**
     * Constructor rỗng để khởi tạo model và hỗ trợ ánh xạ dữ liệu sau này.
     */
    public PlateClaim() {
    }

    /**
     * Khởi tạo bản ghi với các trường dữ liệu tương ứng. Việc kiểm quyền,
     * kiểm tra quan hệ và tính hợp lệ phải thực hiện tại repository/backend.
     */
    public PlateClaim(
            String plateKey,
            String plateCanonical,
            String vehicleId,
            String activeLinkId,
            Date claimedAt,
            Integer version,
            Integer schemaVersion
    ) {
        this.plateKey = plateKey;
        this.plateCanonical = plateCanonical;
        this.vehicleId = vehicleId;
        this.activeLinkId = activeLinkId;
        this.claimedAt = claimedAt;
        this.version = version;
        this.schemaVersion = schemaVersion;
    }

    public String getPlateKey() {
        return plateKey;
    }

    public void setPlateKey(String plateKey) {
        this.plateKey = plateKey;
    }

    public String getPlateCanonical() {
        return plateCanonical;
    }

    public void setPlateCanonical(String plateCanonical) {
        this.plateCanonical = plateCanonical;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getActiveLinkId() {
        return activeLinkId;
    }

    public void setActiveLinkId(String activeLinkId) {
        this.activeLinkId = activeLinkId;
    }

    public Date getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(Date claimedAt) {
        this.claimedAt = claimedAt;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

}
