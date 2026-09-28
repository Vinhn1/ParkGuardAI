package com.example.parkguardai.data.model;

/**
 * Xác định loại vi phạm được ghi nhận trong hệ thống ParkGuard.
 */

public enum ViolationType {

    /**
     * Phương tiện đỗ sai khu vực quy định (ví dụ: sinh viên đỗ vào khu vực giảng viên).
     */
    PARKING_WRONG_AREA,

    /**
     * Phương tiện đỗ chặn lối ra vào hoặc gây cản trở giao thông.
     */
    BLOCKING_ENTRANCE,

    /**
     * Phương tiện đỗ tại khu vực có biển báo cấm đỗ.
     */
    NO_PARKING_ZONE,

    /**
     * Sử dụng khi vi phạm không thuộc các loại được định nghĩa sẵn.
     * Nội dung cụ thể có thể được mô tả trong ghi chú của vi phạm.
     */
    OTHER
}
