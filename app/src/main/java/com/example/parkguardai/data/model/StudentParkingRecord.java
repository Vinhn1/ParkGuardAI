package com.example.parkguardai.data.model;

import java.io.Serializable;

/**
 * Model biểu diễn một lượt xe vào hoặc ra bãi giữ xe của sinh viên.
 */
public class StudentParkingRecord implements Serializable {
    private String id;
    private String plateNumber;
    private String actionType; // ENTRY (Vào) hoặc EXIT (Ra)
    private String dateTitle;  // "Hôm nay", "Hôm qua", v.v.
    private String timeFormatted;
    private String locationName;
    private String status;     // IN_LOT, EXITED
    private long timestamp;

    public StudentParkingRecord() {
    }

    public StudentParkingRecord(String id, String plateNumber, String actionType,
                                String dateTitle, String timeFormatted, String locationName,
                                String status, long timestamp) {
        this.id = id;
        this.plateNumber = plateNumber;
        this.actionType = actionType;
        this.dateTitle = dateTitle;
        this.timeFormatted = timeFormatted;
        this.locationName = locationName;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getDateTitle() {
        return dateTitle;
    }

    public void setDateTitle(String dateTitle) {
        this.dateTitle = dateTitle;
    }

    public String getTimeFormatted() {
        return timeFormatted;
    }

    public void setTimeFormatted(String timeFormatted) {
        this.timeFormatted = timeFormatted;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
