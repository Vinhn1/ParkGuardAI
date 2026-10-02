package com.example.parkguardai.data.model;

import java.util.Date;
import java.util.List;

/**
 * Lưu bằng chứng về thông báo và sự đồng ý xử lý dữ liệu cá nhân.
 * Bản ghi thuộc users/{uid}/consents và không thay thế kiểm tra
 * cơ sở xử lý dữ liệu hay chính sách lưu giữ đã được phê duyệt.
 */
public class ConsentRecord {

    /**
     * ID lần ghi nhận đồng ý.
     */
    private String id;

    /**
     * UID của người được thông báo và đưa ra lựa chọn.
     */
    private String userId;

    /**
     * Phiên bản thông báo quyền riêng tư đã hiển thị.
     */
    private String noticeVersion;

    /**
     * Các mục đích xử lý cụ thể được người dùng chấp nhận.
     */
    private List<String> purposeCodes;

    /**
     * Thời điểm ghi nhận sự đồng ý.
     */
    private Date acceptedAt;

    /**
     * Nguồn ghi nhận như biểu mẫu đăng ký; không phải chứng cứ cho mục đích khác.
     */
    private String source;

    /**
     * Thời điểm rút đồng ý nếu chính sách cho phép và yêu cầu được xử lý.
     */
    private Date withdrawnAt;

    /**
     * Phiên bản cấu trúc tài liệu.
     */
    private Integer schemaVersion;

    /**
     * Constructor rỗng để khởi tạo model và hỗ trợ ánh xạ dữ liệu sau này.
     */
    public ConsentRecord() {
    }

    /**
     * Khởi tạo bản ghi với các trường dữ liệu tương ứng. Việc kiểm quyền,
     * kiểm tra quan hệ và tính hợp lệ phải thực hiện tại repository/backend.
     */
    public ConsentRecord(
            String id,
            String userId,
            String noticeVersion,
            List<String> purposeCodes,
            Date acceptedAt,
            String source,
            Date withdrawnAt,
            Integer schemaVersion
    ) {
        this.id = id;
        this.userId = userId;
        this.noticeVersion = noticeVersion;
        this.purposeCodes = purposeCodes;
        this.acceptedAt = acceptedAt;
        this.source = source;
        this.withdrawnAt = withdrawnAt;
        this.schemaVersion = schemaVersion;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNoticeVersion() {
        return noticeVersion;
    }

    public void setNoticeVersion(String noticeVersion) {
        this.noticeVersion = noticeVersion;
    }

    public List<String> getPurposeCodes() {
        return purposeCodes;
    }

    public void setPurposeCodes(List<String> purposeCodes) {
        this.purposeCodes = purposeCodes;
    }

    public Date getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(Date acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Date getWithdrawnAt() {
        return withdrawnAt;
    }

    public void setWithdrawnAt(Date withdrawnAt) {
        this.withdrawnAt = withdrawnAt;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

}
