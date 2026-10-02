package com.example.parkguardai.data.model;

import java.util.Date;
import java.util.Map;

/**
 * Lưu dấu vết nghiệp vụ nhạy cảm do backend đáng tin cậy tạo.
 * Ứng dụng Android không được tự ghi, sửa hoặc xóa audit log;
 * nội dung thay đổi phải được tối thiểu hóa để tránh lộ dữ liệu cá nhân.
 */
public class AuditLog {

    /**
     * ID sự kiện do backend tạo.
     */
    private String id;

    /**
     * UID tác nhân thực hiện, hoặc định danh tác vụ hệ thống.
     */
    private String actorUid;

    /**
     * Mã hành động như duyệt xe, đổi quyền hoặc sửa hồ sơ.
     */
    private String action;

    /**
     * Đường dẫn tài nguyên bị tác động, không chứa token hoặc URL ảnh.
     */
    private String targetPath;

    /**
     * Thời điểm sự kiện được ghi bởi nguồn đáng tin cậy.
     */
    private Date at;

    /**
     * Lý do nghiệp vụ khi hành động yêu cầu giải trình.
     */
    private String reason;

    /**
     * ID yêu cầu giúp nhận diện retry và đối chiếu sự kiện.
     */
    private String requestId;

    /**
     * Tóm tắt thay đổi tối thiểu; không lưu mật khẩu, token, ảnh hoặc PII thừa.
     */
    private Map<String, Object> changes;

    /**
     * Phiên bản cấu trúc tài liệu.
     */
    private Integer schemaVersion;

    /**
     * Constructor rỗng để khởi tạo model và hỗ trợ ánh xạ dữ liệu sau này.
     */
    public AuditLog() {
    }

    /**
     * Khởi tạo bản ghi với các trường dữ liệu tương ứng. Việc kiểm quyền,
     * kiểm tra quan hệ và tính hợp lệ phải thực hiện tại repository/backend.
     */
    public AuditLog(
            String id,
            String actorUid,
            String action,
            String targetPath,
            Date at,
            String reason,
            String requestId,
            Map<String, Object> changes,
            Integer schemaVersion
    ) {
        this.id = id;
        this.actorUid = actorUid;
        this.action = action;
        this.targetPath = targetPath;
        this.at = at;
        this.reason = reason;
        this.requestId = requestId;
        this.changes = changes;
        this.schemaVersion = schemaVersion;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getActorUid() {
        return actorUid;
    }

    public void setActorUid(String actorUid) {
        this.actorUid = actorUid;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    public Date getAt() {
        return at;
    }

    public void setAt(Date at) {
        this.at = at;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Map<String, Object> getChanges() {
        return changes;
    }

    public void setChanges(Map<String, Object> changes) {
        this.changes = changes;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

}
