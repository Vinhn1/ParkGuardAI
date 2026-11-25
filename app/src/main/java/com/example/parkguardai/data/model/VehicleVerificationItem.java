package com.example.parkguardai.data.model;

import java.io.Serializable;

/**
 * Biểu diễn hồ sơ yêu cầu xác minh phương tiện thực tế từ Cloud Firestore
 * (kết hợp từ vehicleVerificationRequests, vehicles và users).
 */
public class VehicleVerificationItem implements Serializable {
    public final String id;
    public final String vehicleId;
    public final String requesterUid;
    public final String studentName;
    public final String studentCode;
    public final String plateNumber;
    public final String plateCanonical;
    public final String vehicleType;
    public final String vehicleModel;
    public final String color;
    public final String submissionDate;
    public final long requestedAtMillis;
    public final String status; // PENDING, APPROVED, REJECTED, BLOCKED
    public final String reason;
    public final String registrationCardUrl;
    public final String vehiclePhotoUrl;

    public VehicleVerificationItem(String id, String vehicleId, String requesterUid,
                                   String studentName, String studentCode,
                                   String plateNumber, String plateCanonical,
                                   String vehicleType, String vehicleModel, String color,
                                   String submissionDate, long requestedAtMillis,
                                   String status, String reason,
                                   String registrationCardUrl, String vehiclePhotoUrl) {
        this.id = id != null ? id : "";
        this.vehicleId = vehicleId != null ? vehicleId : "";
        this.requesterUid = requesterUid != null ? requesterUid : "";
        this.studentName = studentName != null ? studentName : "";
        this.studentCode = studentCode != null ? studentCode : "";
        this.plateNumber = plateNumber != null ? plateNumber : "";
        this.plateCanonical = plateCanonical != null ? plateCanonical : "";
        this.vehicleType = vehicleType != null ? vehicleType : "MOTORBIKE";
        this.vehicleModel = vehicleModel != null ? vehicleModel : "";
        this.color = color != null ? color : "";
        this.submissionDate = submissionDate != null ? submissionDate : "";
        this.requestedAtMillis = requestedAtMillis;
        this.status = status != null ? status : "PENDING";
        this.reason = reason != null ? reason : "";
        this.registrationCardUrl = registrationCardUrl != null ? registrationCardUrl : "";
        this.vehiclePhotoUrl = vehiclePhotoUrl != null ? vehiclePhotoUrl : "";
    }

    public boolean isApproved() {
        return "APPROVED".equalsIgnoreCase(status) || "VERIFIED".equalsIgnoreCase(status);
    }

    public boolean isBlocked() {
        return "REJECTED".equalsIgnoreCase(status) || "BLOCKED".equalsIgnoreCase(status);
    }

    public boolean isPending() {
        return !isApproved() && !isBlocked();
    }
}
