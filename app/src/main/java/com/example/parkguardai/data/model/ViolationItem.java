package com.example.parkguardai.data.model;

import java.io.Serializable;

/**
 * Biểu diễn thông tin hồ sơ vi phạm thực tế được đồng bộ từ Cloud Firestore.
 */
public class ViolationItem implements Serializable {
    public final String id;
    public final String plateNumber;
    public final String plateCanonical;
    public final String typeCode;
    public final String violationName;
    public final String location;
    public final String timestamp;
    public final long capturedAtMillis;
    public final String penalty;
    public final String status; // PENDING, PENDING_PAYMENT, APPEALED, CONFIRMED, RESOLVED, PAID, CANCELLED
    public final String guardUid;
    public final String guardName;
    public final String note;
    public final String complaintReason;
    public final Double latitude;
    public final Double longitude;
    public final Double locationAccuracy;
    public final String primaryEvidenceUrl;
    public final String cropEvidenceUrl;

    public ViolationItem(String id, String plateNumber, String plateCanonical,
                         String typeCode, String violationName, String location,
                         String timestamp, long capturedAtMillis, String penalty,
                         String status, String guardUid, String guardName,
                         String note, String complaintReason,
                         Double latitude, Double longitude, Double locationAccuracy,
                         String primaryEvidenceUrl, String cropEvidenceUrl) {
        this.id = id != null ? id : "";
        this.plateNumber = plateNumber != null ? plateNumber : "";
        this.plateCanonical = plateCanonical != null ? plateCanonical : "";
        this.typeCode = typeCode != null ? typeCode : "";
        this.violationName = violationName != null ? violationName : "";
        this.location = location != null ? location : "";
        this.timestamp = timestamp != null ? timestamp : "";
        this.capturedAtMillis = capturedAtMillis;
        this.penalty = penalty != null ? penalty : "";
        this.status = status != null ? status : "PENDING";
        this.guardUid = guardUid != null ? guardUid : "";
        this.guardName = guardName != null ? guardName : "";
        this.note = note != null ? note : "";
        this.complaintReason = complaintReason != null ? complaintReason : "";
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationAccuracy = locationAccuracy;
        this.primaryEvidenceUrl = primaryEvidenceUrl != null ? primaryEvidenceUrl : "";
        this.cropEvidenceUrl = cropEvidenceUrl != null ? cropEvidenceUrl : "";
    }

    public boolean isUnprocessed() {
        return "PENDING".equalsIgnoreCase(status) || "PENDING_PAYMENT".equalsIgnoreCase(status)
                || "CONFIRMED".equalsIgnoreCase(status);
    }

    public boolean isAppealed() {
        return "APPEALED".equalsIgnoreCase(status) || "REVIEWING".equalsIgnoreCase(status);
    }

    public boolean isResolved() {
        return "RESOLVED".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status);
    }

    public boolean isCancelled() {
        return "CANCELLED".equalsIgnoreCase(status);
    }

    public boolean isProcessed() {
        return isResolved() || isCancelled();
    }
}
