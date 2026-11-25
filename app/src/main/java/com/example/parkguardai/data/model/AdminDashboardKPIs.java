package com.example.parkguardai.data.model;

import java.io.Serializable;

/**
 * Thống kê chỉ số KPI tổng quan bãi xe cho Cổng Quản trị viên (Admin Portal).
 */
public class AdminDashboardKPIs implements Serializable {
    private final int totalVehiclesInLot;
    private final int violationsToday;
    private final String camerasOnline;
    private final int unregisteredVehicles;

    public AdminDashboardKPIs(int totalVehiclesInLot, int violationsToday,
                              String camerasOnline, int unregisteredVehicles) {
        this.totalVehiclesInLot = totalVehiclesInLot;
        this.violationsToday = violationsToday;
        this.camerasOnline = camerasOnline != null ? camerasOnline : "4/4";
        this.unregisteredVehicles = unregisteredVehicles;
    }

    public int getTotalVehiclesInLot() {
        return totalVehiclesInLot;
    }

    public int getViolationsToday() {
        return violationsToday;
    }

    public String getCamerasOnline() {
        return camerasOnline;
    }

    public int getUnregisteredVehicles() {
        return unregisteredVehicles;
    }
}
