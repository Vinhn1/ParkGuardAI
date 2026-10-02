package com.example.parkguardai.data.model;

/**
 * Xác định trạng thái liên kết giữa một tài khoản và một phương tiện.
 * Chỉ VERIFIED trong khoảng hiệu lực đã được xét duyệt mới có thể
 * tham gia kiểm tra quyền xem hồ sơ vi phạm tại backend.
 */
public enum VehicleLinkStatus {
    /** Yêu cầu liên kết đang chờ xét duyệt. */
    PENDING,
    /** Người có thẩm quyền đã xác minh liên kết. */
    VERIFIED,
    /** Yêu cầu liên kết đã bị từ chối và không cấp quyền xem. */
    REJECTED,
    /** Liên kết từng có hiệu lực nhưng nay đã kết thúc. */
    INACTIVE
}
