package com.example.parkguardai.data.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Kết quả kiểm tra xe quét lặp nhiều lần trong cùng một ngày.
 */
public final class DuplicateScanCheckResult {
    public final boolean isDuplicate;
    public final int scanCountToday;
    public final String plate;
    public final String ownerName;
    public final String vehicleType;
    public final String lastScanTime;
    public final String lastLocation;
    public final String previousViolationId;
    public final String previousViolationType;
    public final List<ScanTimelineItem> todayScans;

    public DuplicateScanCheckResult(boolean isDuplicate, int scanCountToday, String plate,
                                    String ownerName, String vehicleType, String lastScanTime,
                                    String lastLocation, String previousViolationId,
                                    String previousViolationType, List<ScanTimelineItem> todayScans) {
        this.isDuplicate = isDuplicate;
        this.scanCountToday = scanCountToday;
        this.plate = plate;
        this.ownerName = ownerName;
        this.vehicleType = vehicleType;
        this.lastScanTime = lastScanTime;
        this.lastLocation = lastLocation;
        this.previousViolationId = previousViolationId;
        this.previousViolationType = previousViolationType;
        this.todayScans = todayScans != null ? todayScans : new ArrayList<>();
    }

    public static DuplicateScanCheckResult notDuplicate(String plate, String ownerName, String vehicleType) {
        return new DuplicateScanCheckResult(false, 1, plate, ownerName, vehicleType,
                "", "", null, null, new ArrayList<>());
    }
}
