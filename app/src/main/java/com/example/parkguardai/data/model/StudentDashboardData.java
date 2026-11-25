package com.example.parkguardai.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model tổng hợp dữ liệu màn hình chính của Sinh viên (Xe của tôi).
 */
public class StudentDashboardData implements Serializable {
    private String studentName;
    private String studentCode;
    private String mainPlate;
    private String vehicleDetails;
    private String vehicleStatus; // VERIFIED, PENDING, REJECTED, NONE
    private boolean isInLot;
    private String inLotTime;
    private int unprocessedViolationsCount;
    private List<StudentParkingRecord> recentHistory = new ArrayList<>();

    public StudentDashboardData() {
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    public String getMainPlate() {
        return mainPlate;
    }

    public void setMainPlate(String mainPlate) {
        this.mainPlate = mainPlate;
    }

    public String getVehicleDetails() {
        return vehicleDetails;
    }

    public void setVehicleDetails(String vehicleDetails) {
        this.vehicleDetails = vehicleDetails;
    }

    public String getVehicleStatus() {
        return vehicleStatus;
    }

    public void setVehicleStatus(String vehicleStatus) {
        this.vehicleStatus = vehicleStatus;
    }

    public boolean isInLot() {
        return isInLot;
    }

    public void setInLot(boolean inLot) {
        isInLot = inLot;
    }

    public String getInLotTime() {
        return inLotTime;
    }

    public void setInLotTime(String inLotTime) {
        this.inLotTime = inLotTime;
    }

    public int getUnprocessedViolationsCount() {
        return unprocessedViolationsCount;
    }

    public void setUnprocessedViolationsCount(int unprocessedViolationsCount) {
        this.unprocessedViolationsCount = unprocessedViolationsCount;
    }

    public List<StudentParkingRecord> getRecentHistory() {
        return recentHistory;
    }

    public void setRecentHistory(List<StudentParkingRecord> recentHistory) {
        this.recentHistory = recentHistory != null ? recentHistory : new ArrayList<>();
    }
}
