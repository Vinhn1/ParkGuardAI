package com.example.parkguardai.data.model;

import java.util.Date;
import java.util.Map;

/**
 * Biểu diễn bản tóm tắt vi phạm dành cho một sinh viên đã được cấp quyền.
 * Đây là projection đề xuất, chưa phải quyền truy cập thực tế; backend
 * phải tạo/thu hồi theo liên kết xe VERIFIED và khoảng hiệu lực.
 */
public class UserViolationView {

    /**
     * UID của sinh viên được phép thấy bản tóm tắt.
     */
    private String uid;

    /**
     * ID hồ sơ vi phạm được chiếu vào danh sách.
     */
    private String violationId;

    /**
     * Trạng thái xử lý đang được phép công bố.
     */
    private ViolationStatus status;

    /**
     * Thời điểm xảy ra ghi nhận theo hồ sơ.
     */
    private Date occurredAt;

    /**
     * Thông tin tóm tắt đã lọc quyền, không chứa ảnh hoặc dữ liệu cá nhân thừa.
     */
    private Map<String, Object> summary;

    /**
     * Phiên bản cấu trúc tài liệu.
     */
    private Integer schemaVersion;

    /**
     * Constructor rỗng để khởi tạo model và hỗ trợ ánh xạ dữ liệu sau này.
     */
    public UserViolationView() {
    }

    /**
     * Khởi tạo bản ghi với các trường dữ liệu tương ứng. Việc kiểm quyền,
     * kiểm tra quan hệ và tính hợp lệ phải thực hiện tại repository/backend.
     */
    public UserViolationView(
            String uid,
            String violationId,
            ViolationStatus status,
            Date occurredAt,
            Map<String, Object> summary,
            Integer schemaVersion
    ) {
        this.uid = uid;
        this.violationId = violationId;
        this.status = status;
        this.occurredAt = occurredAt;
        this.summary = summary;
        this.schemaVersion = schemaVersion;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getViolationId() {
        return violationId;
    }

    public void setViolationId(String violationId) {
        this.violationId = violationId;
    }

    public ViolationStatus getStatus() {
        return status;
    }

    public void setStatus(ViolationStatus status) {
        this.status = status;
    }

    public Date getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Date occurredAt) {
        this.occurredAt = occurredAt;
    }

    public Map<String, Object> getSummary() {
        return summary;
    }

    public void setSummary(Map<String, Object> summary) {
        this.summary = summary;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

}
