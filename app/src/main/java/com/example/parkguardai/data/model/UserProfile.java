package com.example.parkguardai.data.model;

import java.util.Date;

/**
 * Biểu diễn hồ sơ nghiệp vụ tại users/{uid} trong thiết kế Firestore.
 * Firebase Authentication là nguồn danh tính và trạng thái xác minh email/số
 * điện thoại; vai trò nằm ở AccessGrant, không được lấy từ hồ sơ tự khai.
 * Model này không chứa mật khẩu và không tự cấp quyền.
 */
public class UserProfile {

    /**
     * UID từ Firebase Authentication và ID tài liệu hồ sơ.
     */
    private String uid;

    /**
     * Họ tên do sinh viên khai; không mặc nhiên là tên pháp lý đã xác minh.
     */
    private String fullName;

    /**
     * Email dùng cho liên hệ sau khi được đối chiếu với Firebase Authentication.
     */
    private String emailCanonical;

    /**
     * Số điện thoại liên hệ tùy chọn ở dạng chuẩn hóa. Trong phương án Spark,
     * số này chưa được xác minh bằng OTP và không dùng để đăng nhập, khôi phục
     * tài khoản, xác nhận danh tính hoặc chứng minh quyền sử dụng xe.
     */
    private String phoneCanonical;

    /**
     * Mã số sinh viên nếu có nguồn xác minh phù hợp; dữ liệu tự khai không đủ chứng minh danh tính.
     */
    private String studentCode;

    /**
     * Lớp sinh viên đang theo học (ví dụ: DH21TIN01).
     */
    private String studentClass;

    /**
     * Mã nhân viên (dành cho vai trò Bảo vệ / Quản trị viên).
     */
    private String staffCode;

    /**
     * URL ảnh đại diện (tải lên Cloudinary).
     */
    private String avatarUrl;

    /**
     * Trạng thái hồ sơ nghiệp vụ; danh mục trạng thái chi tiết chờ chính sách được duyệt.
     */
    private String accountStatus;

    /**
     * Thời điểm nguồn đáng tin cậy tạo hồ sơ, không lấy từ đồng hồ thiết bị.
     */
    private Date createdAt;

    /**
     * Thời điểm nguồn đáng tin cậy cập nhật hồ sơ gần nhất.
     */
    private Date updatedAt;

    /**
     * Phiên bản cấu trúc tài liệu để hỗ trợ migration.
     */
    private Integer schemaVersion;

    /**
     * Constructor rỗng cho khởi tạo và ánh xạ dữ liệu.
     */
    public UserProfile() {
    }

    /**
     * Khởi tạo các trường của bản ghi; repository/backend phải kiểm tra
     * tính hợp lệ, quan hệ và quyền trước khi lưu.
     */
    public UserProfile(
            String uid,
            String fullName,
            String emailCanonical,
            String phoneCanonical,
            String studentCode,
            String accountStatus,
            Date createdAt,
            Date updatedAt,
            Integer schemaVersion
    ) {
        this.uid = uid;
        this.fullName = fullName;
        this.emailCanonical = emailCanonical;
        this.phoneCanonical = phoneCanonical;
        this.studentCode = studentCode;
        this.accountStatus = accountStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.schemaVersion = schemaVersion;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmailCanonical() {
        return emailCanonical;
    }

    public void setEmailCanonical(String emailCanonical) {
        this.emailCanonical = emailCanonical;
    }

    public String getPhoneCanonical() {
        return phoneCanonical;
    }

    public void setPhoneCanonical(String phoneCanonical) {
        this.phoneCanonical = phoneCanonical;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    public String getStudentClass() {
        return studentClass;
    }

    public void setStudentClass(String studentClass) {
        this.studentClass = studentClass;
    }

    public String getStaffCode() {
        return staffCode;
    }

    public void setStaffCode(String staffCode) {
        this.staffCode = staffCode;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

}
