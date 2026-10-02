package com.example.parkguardai.data.model;

import java.util.Date;
import java.util.List;

/**
 * Biểu diễn quyền và phạm vi truy cập do môi trường đáng tin cậy cấp cho một tài khoản.
 * Bản ghi tương ứng với accessGrants/{uid}; ứng dụng Android chỉ được đọc
 * theo quyền, không được tự ghi vai trò hoặc phạm vi.
 */
public class AccessGrant {

    /**
     * UID Firebase Authentication, đồng thời là ID tài liệu quyền.
     */
    private String uid;

    /**
     * Vai trò được cấp từ nguồn tin cậy; giá trị trong model không tự tạo quyền.
     */
    private UserRole role;

    /**
     * Các khu vực hoặc ca mà bảo vệ được phép xử lý.
     */
    private List<String> guardScopes;

    /**
     * Các nghiệp vụ quản trị được cấp riêng, tránh quyền quản trị toàn phần.
     */
    private List<String> adminPermissions;

    /**
     * Cho biết quyền còn hiệu lực; trạng thái phải được kiểm lại ở nguồn dữ liệu.
     */
    private Boolean active;

    /**
     * Thời điểm nguồn đáng tin cậy cập nhật quyền.
     */
    private Date updatedAt;

    /**
     * UID của người hoặc tác vụ đặc quyền cập nhật quyền.
     */
    private String updatedBy;

    /**
     * Phiên bản cấu trúc tài liệu để hỗ trợ thay đổi schema.
     */
    private Integer schemaVersion;

    /**
     * Constructor rỗng để khởi tạo model và hỗ trợ ánh xạ dữ liệu sau này.
     */
    public AccessGrant() {
    }

    /**
     * Khởi tạo bản ghi với các trường dữ liệu tương ứng. Việc kiểm quyền,
     * kiểm tra quan hệ và tính hợp lệ phải thực hiện tại repository/backend.
     */
    public AccessGrant(
            String uid,
            UserRole role,
            List<String> guardScopes,
            List<String> adminPermissions,
            Boolean active,
            Date updatedAt,
            String updatedBy,
            Integer schemaVersion
    ) {
        this.uid = uid;
        this.role = role;
        this.guardScopes = guardScopes;
        this.adminPermissions = adminPermissions;
        this.active = active;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
        this.schemaVersion = schemaVersion;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public List<String> getGuardScopes() {
        return guardScopes;
    }

    public void setGuardScopes(List<String> guardScopes) {
        this.guardScopes = guardScopes;
    }

    public List<String> getAdminPermissions() {
        return adminPermissions;
    }

    public void setAdminPermissions(List<String> adminPermissions) {
        this.adminPermissions = adminPermissions;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

}
