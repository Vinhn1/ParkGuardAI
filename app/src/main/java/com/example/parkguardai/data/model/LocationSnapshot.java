package com.example.parkguardai.data.model;

import java.util.Date;

/**
 * Lưu vị trí liên kết với một ảnh tại thời điểm chụp.
 * Tọa độ có thể rỗng nếu không lấy được fix hợp lệ; không dùng 0,0
 * làm giá trị thay thế. Ngưỡng chấp nhận và luồng thủ công chờ DEC-008A.
 */
public class LocationSnapshot {

    /**
     * Vĩ độ theo độ; rỗng nếu vị trí không hợp lệ hoặc chưa có.
     */
    private Double latitude;

    /**
     * Kinh độ theo độ; rỗng nếu vị trí không hợp lệ hoặc chưa có.
     */
    private Double longitude;

    /**
     * Bán kính sai số ngang ước tính, đơn vị mét; vị trí thủ công có thể không có.
     */
    private Double locationAccuracy;

    /**
     * Thời điểm thiết bị đo vị trí, không phải thời điểm callback trả về.
     */
    private Date locationTimestamp;

    /**
     * Thời điểm chụp ảnh mà snapshot này được gắn vào.
     */
    private Date capturedAt;

    /**
     * Nguồn vị trí tự động hay thủ công; không gọi Fused là GPS vệ tinh khi chưa kiểm chứng.
     */
    private String locationSource;

    /**
     * Trạng thái đánh giá fix hoặc ngoại lệ; danh mục cuối chờ phê duyệt.
     */
    private String locationStatus;

    /**
     * Mã khu vực khi chính sách cho phép dùng vị trí theo khu vực.
     */
    private String areaCode;

    /**
     * Lý do thiếu vị trí hoặc đề xuất vị trí thủ công.
     */
    private String reason;

    /**
     * UID người duyệt ngoại lệ vị trí thủ công.
     */
    private String reviewerUid;

    /**
     * Thời điểm người có thẩm quyền duyệt ngoại lệ.
     */
    private Date reviewedAt;

    /**
     * Constructor rỗng để khởi tạo model và hỗ trợ ánh xạ dữ liệu sau này.
     */
    public LocationSnapshot() {
    }

    /**
     * Khởi tạo bản ghi với các trường dữ liệu tương ứng. Việc kiểm quyền,
     * kiểm tra quan hệ và tính hợp lệ phải thực hiện tại repository/backend.
     */
    public LocationSnapshot(
            Double latitude,
            Double longitude,
            Double locationAccuracy,
            Date locationTimestamp,
            Date capturedAt,
            String locationSource,
            String locationStatus,
            String areaCode,
            String reason,
            String reviewerUid,
            Date reviewedAt
    ) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationAccuracy = locationAccuracy;
        this.locationTimestamp = locationTimestamp;
        this.capturedAt = capturedAt;
        this.locationSource = locationSource;
        this.locationStatus = locationStatus;
        this.areaCode = areaCode;
        this.reason = reason;
        this.reviewerUid = reviewerUid;
        this.reviewedAt = reviewedAt;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLocationAccuracy() {
        return locationAccuracy;
    }

    public void setLocationAccuracy(Double locationAccuracy) {
        this.locationAccuracy = locationAccuracy;
    }

    public Date getLocationTimestamp() {
        return locationTimestamp;
    }

    public void setLocationTimestamp(Date locationTimestamp) {
        this.locationTimestamp = locationTimestamp;
    }

    public Date getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(Date capturedAt) {
        this.capturedAt = capturedAt;
    }

    public String getLocationSource() {
        return locationSource;
    }

    public void setLocationSource(String locationSource) {
        this.locationSource = locationSource;
    }

    public String getLocationStatus() {
        return locationStatus;
    }

    public void setLocationStatus(String locationStatus) {
        this.locationStatus = locationStatus;
    }

    public String getAreaCode() {
        return areaCode;
    }

    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getReviewerUid() {
        return reviewerUid;
    }

    public void setReviewerUid(String reviewerUid) {
        this.reviewerUid = reviewerUid;
    }

    public Date getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Date reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

}
