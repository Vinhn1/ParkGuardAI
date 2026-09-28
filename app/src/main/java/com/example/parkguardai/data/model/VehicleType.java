package com.example.parkguardai.data.model;

/**
 * Xác định các loại phương tiện được hỗ trợ trong ParkGuard.
 *
 * Loại phương tiện được sử dụng để phân loại phương tiện
 * đã đăng ký và hỗ trợ hiển thị thông tin phù hợp trên giao diện.
 *
 * Có thể bổ sung thêm loại phương tiện khi yêu cầu của hệ thống
 * được mở rộng trong tương lai.
 */

public enum VehicleType {
    /**
     * Xe máy.
     */
    MOTORBIKE,

    /**
     * Ô tô.
     */
    CAR
}
