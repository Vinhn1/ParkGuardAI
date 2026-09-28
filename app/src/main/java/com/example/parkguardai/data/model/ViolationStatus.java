package com.example.parkguardai.data.model;

/**
 * Xác định trạng thái xử lý của một vi phạm trong ParkGuard.
 */

public enum ViolationStatus {
    /**
     * Vi phạm đang chờ được xác nhận hoặc xử lý.
     */
    PENDING,

    /**
     * Vi phạm đã được bảo vệ kiểm tra và xác nhận.
     */
    CONFIRMED,

    /**
     * Vi phạm đã được xử lý hoàn tất.
     */
    RESOLVED
}
