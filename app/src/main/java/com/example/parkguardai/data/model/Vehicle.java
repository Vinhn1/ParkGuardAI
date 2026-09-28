package com.example.parkguardai.data.model;

/**
 * Đại diện cho một phương tiện được đăng ký
 * trong hệ thống ParkGuard.
 *
 * Mỗi phương tiện thuộc về một người dùng thông qua ownerId.
 * Model này chỉ lưu trữ thông tin của phương tiện và không
 * chịu trách nhiệm xử lý AI, OCR hoặc truy cập cơ sở dữ liệu.
 */

public class Vehicle {
    private String id;
    private String ownerId; // ID của người sở hữu phương tiện
    private String licensePlate; // Biển số xe đã được chuẩn hóa, dùng để nhận diện và tra cứu xe
    private VehicleType type; // Loại phương tiện
    private String brand; // Hãng SX phương tiện
    private String model; // Tên/Dòng xe
    private String color; // Màu sắc của xe


    public Vehicle() {
    }

    public Vehicle(String id, String ownerId, String licensePlate, VehicleType type, String brand, String model, String color) {
        this.id = id;
        this.ownerId = ownerId;
        this.licensePlate = licensePlate;
        this.type = type;
        this.brand = brand;
        this.model = model;
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public VehicleType getType() {
        return type;
    }

    public void setType(VehicleType type) {
        this.type = type;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
