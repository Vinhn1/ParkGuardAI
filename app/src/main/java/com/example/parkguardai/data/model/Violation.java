package com.example.parkguardai.data.model;

/**
 * Đại diện cho một lần vi phạm được ghi nhận trong ParkGuard.
 *
 * Model lưu thông tin phương tiện, bảo vệ ghi nhận, loại vi phạm,
 * bằng chứng, vị trí, thời gian và trạng thái xử lý.
 */

public class Violation {

    private String id;

    /**
     * ID của phương tiện liên quan đến vi phạm.
     * Liên kết đến Vehicle.id.
     */
    private String vehicleId;

    /**
     * ID của bảo vệ ghi nhận hoặc xác nhận vi phạm.
     * Liên kết đến User.id.
     */
    private String guardId;

    /**
     * Biển số cuối cùng sau khi bảo vệ đã kiểm tra kết quả nhận diện.
     */
    private String licensePlate;

    private ViolationType type;

    private ViolationStatus status;

    private String note;

    /**
     * Đường dẫn ảnh gốc của phương tiện/hiện trường.
     * Ảnh này được giữ lại làm bằng chứng đầy đủ.
     */
    private String originalImageUrl;

    /**
     * Đường dẫn ảnh biển số được cắt từ ảnh gốc.
     * Ảnh này hỗ trợ kiểm tra kết quả nhận diện biển số.
     */
    private String plateImageUrl;

    /**
     * Tọa độ nơi vi phạm được ghi nhận.
     */
    private double latitude;
    private double longitude;

    /**
     * Thời điểm ghi nhận vi phạm dưới dạng Unix timestamp
     * tính bằng milliseconds.
     */
    private long occurredAt;

    public Violation() {
    }

    public Violation(String id, String vehicleId, String guardId, String licensePlate, ViolationType type, ViolationStatus status, String note, String originalImageUrl, String plateImageUrl, double latitude, double longitude, long occurredAt) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.guardId = guardId;
        this.licensePlate = licensePlate;
        this.type = type;
        this.status = status;
        this.note = note;
        this.originalImageUrl = originalImageUrl;
        this.plateImageUrl = plateImageUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.occurredAt = occurredAt;
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

    public String getGuardId() {
        return guardId;
    }

    public void setGuardId(String guardId) {
        this.guardId = guardId;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public ViolationType getType() {
        return type;
    }

    public void setType(ViolationType type) {
        this.type = type;
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

    public String getOriginalImageUrl() {
        return originalImageUrl;
    }

    public void setOriginalImageUrl(String originalImageUrl) {
        this.originalImageUrl = originalImageUrl;
    }

    public String getPlateImageUrl() {
        return plateImageUrl;
    }

    public void setPlateImageUrl(String plateImageUrl) {
        this.plateImageUrl = plateImageUrl;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public long getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(long occurredAt) {
        this.occurredAt = occurredAt;
    }
}
