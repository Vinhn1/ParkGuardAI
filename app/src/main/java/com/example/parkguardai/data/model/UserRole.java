package com.example.parkguardai.data.model;


/**
 * Xác định vai trò của người dùng trong hệ thống ParkGuard.
 *
 * Vai trò được sử dụng để phân quyền chức năng và điều hướng
 * người dùng đến giao diện phù hợp sau khi đăng nhập.
 */

public enum UserRole {
    /**
     * Sinh viên có quyền xem phương tiện và các vi phạm
     * thuộc phạm vi tài khoản của mình.
     */
    STUDENT,
    /**
     * Bảo vệ có quyền ghi nhận, xác minh và quản lý
     * các vi phạm trong phạm vi được cấp quyền.
     */
    SECURITY_GUARD
}





