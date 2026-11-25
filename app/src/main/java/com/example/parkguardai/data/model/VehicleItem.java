package com.example.parkguardai.data.model;

import java.io.Serializable;

/**
 * Biểu diễn thông tin phương tiện thực tế của sinh viên được đồng bộ từ Cloud Firestore.
 */
public class VehicleItem implements Serializable {
    public final String id;
    public final String plateNumber;
    public final String plateCanonical;
    public final String vehicleType;
    public final String model;
    public final String color;
    public final String status; // VERIFIED, APPROVED, PENDING, REJECTED, BLOCKED
    public final String stampNumber;
    public final String submissionDate;
    public final String ownerName;
    public final String ownerId;
    public final String registrationCardUrl;
    public final String vehiclePhotoUrl;

    public VehicleItem(String id, String plateNumber, String plateCanonical, String vehicleType,
                       String model, String color, String status, String stampNumber,
                       String submissionDate, String ownerName, String ownerId,
                       String registrationCardUrl, String vehiclePhotoUrl) {
        this.id = id != null ? id : "";
        this.plateNumber = plateNumber != null ? plateNumber : "";
        this.plateCanonical = plateCanonical != null ? plateCanonical : "";
        this.vehicleType = vehicleType != null ? vehicleType : "MOTORBIKE";
        this.model = model != null ? model : "";
        this.color = color != null ? color : "";
        this.status = status != null ? status : "PENDING";
        this.stampNumber = stampNumber != null ? stampNumber : "";
        this.submissionDate = submissionDate != null ? submissionDate : "";
        this.ownerName = ownerName != null ? ownerName : "";
        this.ownerId = ownerId != null ? ownerId : "";
        this.registrationCardUrl = registrationCardUrl != null ? registrationCardUrl : "";
        this.vehiclePhotoUrl = vehiclePhotoUrl != null ? vehiclePhotoUrl : "";
    }

    public boolean isApproved() {
        return "VERIFIED".equalsIgnoreCase(status) || "APPROVED".equalsIgnoreCase(status);
    }

    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(status);
    }

    public boolean isRejectedOrBlocked() {
        return "REJECTED".equalsIgnoreCase(status) || "BLOCKED".equalsIgnoreCase(status);
    }
}
