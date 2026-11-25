package com.example.parkguardai.data.model;

/**
 * Kết quả tra cứu thông tin phương tiện từ biển số xe phục vụ phân hệ Bảo vệ.
 */
public final class VehicleLookupResult {
    public enum RegistrationStatus {
        VERIFIED,       // Đã đăng ký và duyệt hợp lệ
        PENDING,        // Đang chờ duyệt
        UNREGISTERED,   // Chưa đăng ký trong hệ thống
        VIOLATING       // Có biên bản vi phạm chưa xử lý
    }

    public final String plate;
    public final String ownerName;
    public final String studentId;
    public final String vehicleModel;
    public final String color;
    public final RegistrationStatus status;
    public final int violationCount;

    public VehicleLookupResult(String plate, String ownerName, String studentId,
                               String vehicleModel, String color, RegistrationStatus status,
                               int violationCount) {
        this.plate = plate;
        this.ownerName = ownerName;
        this.studentId = studentId;
        this.vehicleModel = vehicleModel;
        this.color = color;
        this.status = status;
        this.violationCount = violationCount;
    }

    public boolean isValid() {
        return status == RegistrationStatus.VERIFIED && violationCount == 0;
    }
}
