package com.example.parkguardai.data.model;

/** Bản nháp ảnh và hồ sơ ở vùng riêng của ứng dụng, chưa phải bản ghi máy chủ. */
public final class GuardDraft {
    public String id;
    public String ownerUid;
    public long capturedAtMillis;
    public long locationAtMillis;
    public double latitude;
    public double longitude;
    public float accuracyMeters;
    public boolean locationAccepted;
    public String originalName;
    public String cropName = "";
    public String plate = "";
    public boolean isBatch = false;
    public java.util.List<String> batchPlates = new java.util.ArrayList<>();
    public String typeCode = "";
    public String note = "";
    public String scopeId = "";
    public String penaltyAmount = "100.000 đ";
    public EvidenceImage originalUpload;
    public EvidenceImage cropUpload;
    public boolean sent;
}
