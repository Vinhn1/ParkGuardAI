package com.example.parkguardai.data.model;

/**
 * Cảnh báo an ninh bãi xe dành cho phân hệ Bảo vệ (Tab Cảnh báo).
 */
public final class GuardSecurityAlert {
    public enum AlertType {
        REPEATED_SCAN,   // Xe quét lại nhiều lần bất thường trong ngày
        OVERNIGHT,       // Xe đỗ qua đêm quá thời hạn
        UNKNOWN_VEHICLE, // Phương tiện chưa đăng ký / xe lạ
        NO_PERMIT        // Không có vé / thẻ xe hợp lệ
    }

    public enum Severity {
        HIGH,
        MEDIUM,
        LOW
    }

    public enum Status {
        UNHANDLED,
        RESOLVED
    }

    public final String id;
    public final String plateNumber;
    public final AlertType alertType;
    public final String title;
    public final String description;
    public final Severity severity;
    public final String timestamp;
    public final String location;
    public Status status;
    public String resolutionNote;
    public long createdAtMillis;

    public GuardSecurityAlert(String id, String plateNumber, AlertType alertType,
                              String title, String description, Severity severity,
                              String timestamp, String location, Status status) {
        this(id, plateNumber, alertType, title, description, severity, timestamp, location, status, null, System.currentTimeMillis());
    }

    public GuardSecurityAlert(String id, String plateNumber, AlertType alertType,
                              String title, String description, Severity severity,
                              String timestamp, String location, Status status,
                              String resolutionNote, long createdAtMillis) {
        this.id = id;
        this.plateNumber = plateNumber;
        this.alertType = alertType;
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.timestamp = timestamp;
        this.location = location;
        this.status = status;
        this.resolutionNote = resolutionNote;
        this.createdAtMillis = createdAtMillis;
    }
}
