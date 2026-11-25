package com.example.parkguardai.data.model;

import java.io.Serializable;

/**
 * Biểu diễn thông tin một người dùng hoặc nhân sự bảo vệ trong danh sách quản lý của Quản trị viên.
 */
public class AdminUserItem implements Serializable {
    private String uid;
    private String name;
    private String code;
    private String email;
    private String phone;
    private String role; // "GUARD", "STUDENT", "ADMIN"
    private String shift;
    private String scope;
    private String status; // "ACTIVE", "PENDING", "LOCKED", "REJECTED"
    private long createdAt;
    private String avatarUrl;

    public AdminUserItem(String uid, String name, String code, String email, String phone,
                         String role, String shift, String scope, String status, long createdAt) {
        this.uid = uid;
        this.name = name;
        this.code = code;
        this.email = email;
        this.phone = phone;
        this.role = role != null ? role : "STUDENT";
        this.shift = shift;
        this.scope = scope;
        this.status = status != null ? status : "ACTIVE";
        this.createdAt = createdAt;
    }

    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getRole() {
        return role;
    }

    public String getShift() {
        return shift;
    }

    public String getScope() {
        return scope;
    }

    public String getStatus() {
        return status;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setShift(String shift) {
        this.shift = shift;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public boolean isGuard() {
        return "GUARD".equalsIgnoreCase(role) || "SECURITY_GUARD".equalsIgnoreCase(role);
    }

    public boolean isStudent() {
        return "STUDENT".equalsIgnoreCase(role);
    }

    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(status) || "PENDING_APPROVAL".equalsIgnoreCase(status);
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status) || "APPROVED".equalsIgnoreCase(status);
    }

    public boolean isLocked() {
        return "LOCKED".equalsIgnoreCase(status);
    }

    public boolean isRejected() {
        return "REJECTED".equalsIgnoreCase(status);
    }
}
