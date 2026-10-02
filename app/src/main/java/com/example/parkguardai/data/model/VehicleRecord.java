package com.example.parkguardai.data.model;

import java.util.Date;

/**
 * Biểu diễn phương tiện tại vehicles/{vehicleId} trong thiết kế Firestore.
 * Phương tiện là dữ liệu đăng ký; quyền sử dụng và lịch sử chủ tài khoản
 * nằm ở VehicleLink, không suy ra từ biển số hoặc người tạo bản ghi.
 */
public class VehicleRecord {

    /**
     * ID phương tiện không suy ra từ biển số.
     */
    private String id;

    /**
     * Biển số đúng như sinh viên đã nhập để có thể đối chiếu khi xét duyệt.
     */
    private String plateRaw;

    /**
     * Biển số chuẩn hóa phục vụ phát hiện ứng viên trùng; không là khóa quyền truy cập.
     */
    private String plateCanonical;

    /**
     * Phiên bản quy tắc chuẩn hóa biển số đã áp dụng.
     */
    private Integer plateNormVersion;

    /**
     * Loại phương tiện do người đăng ký chọn.
     */
    private VehicleType type;

    /**
     * Hãng xe nếu được thu thập theo chính sách.
     */
    private String brand;

    /**
     * Dòng xe nếu cần cho xét duyệt.
     */
    private String model;

    /**
     * Màu xe nếu cần cho nhận diện thủ công.
     */
    private String color;

    /**
     * Khóa ảnh phương tiện trong Storage, không phải URL công khai.
     */
    private String photoObjectKey;

    /**
     * UID tài khoản đã tạo bản ghi; không đồng nghĩa là chủ đã xác minh.
     */
    private String createdBy;

    /**
     * Thời điểm bản ghi được tạo ở nguồn đáng tin cậy.
     */
    private Date createdAt;

    /**
     * Phiên bản cấu trúc tài liệu.
     */
    private Integer schemaVersion;

    /**
     * Constructor rỗng cho khởi tạo và ánh xạ dữ liệu.
     */
    public VehicleRecord() {
    }

    /**
     * Khởi tạo các trường của bản ghi; repository/backend phải kiểm tra
     * tính hợp lệ, quan hệ và quyền trước khi lưu.
     */
    public VehicleRecord(
            String id,
            String plateRaw,
            String plateCanonical,
            Integer plateNormVersion,
            VehicleType type,
            String brand,
            String model,
            String color,
            String photoObjectKey,
            String createdBy,
            Date createdAt,
            Integer schemaVersion
    ) {
        this.id = id;
        this.plateRaw = plateRaw;
        this.plateCanonical = plateCanonical;
        this.plateNormVersion = plateNormVersion;
        this.type = type;
        this.brand = brand;
        this.model = model;
        this.color = color;
        this.photoObjectKey = photoObjectKey;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.schemaVersion = schemaVersion;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPlateRaw() {
        return plateRaw;
    }

    public void setPlateRaw(String plateRaw) {
        this.plateRaw = plateRaw;
    }

    public String getPlateCanonical() {
        return plateCanonical;
    }

    public void setPlateCanonical(String plateCanonical) {
        this.plateCanonical = plateCanonical;
    }

    public Integer getPlateNormVersion() {
        return plateNormVersion;
    }

    public void setPlateNormVersion(Integer plateNormVersion) {
        this.plateNormVersion = plateNormVersion;
    }

    public VehicleType getType() {
        return type;
    }

    public void setType(VehicleType type) {
        this.type = type;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getPhotoObjectKey() {
        return photoObjectKey;
    }

    public void setPhotoObjectKey(String photoObjectKey) {
        this.photoObjectKey = photoObjectKey;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

}
