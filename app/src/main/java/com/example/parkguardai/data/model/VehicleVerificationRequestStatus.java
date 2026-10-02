package com.example.parkguardai.data.model;

/**
 * Xác định kết quả xử lý một yêu cầu xác minh phương tiện.
 * Trạng thái này khác trạng thái hiệu lực của {@link VehicleLink};
 * chỉ nguồn dữ liệu đáng tin cậy được chuyển sang APPROVED hoặc REJECTED.
 */
public enum VehicleVerificationRequestStatus {
    /** Yêu cầu đã gửi và đang chờ người có thẩm quyền xét duyệt. */
    PENDING,
    /** Yêu cầu được duyệt sau khi kiểm tra biển số và chứng cứ. */
    APPROVED,
    /** Yêu cầu bị từ chối và phải giữ lý do xử lý. */
    REJECTED
}
