package com.example.parkguardai.data.model;

import java.util.Date;

/**
 * Định nghĩa danh mục loại vi phạm có phiên bản do quản trị quản lý.
 * Class này khác enum ViolationType đang có; hồ sơ cũ phải giữ
 * mã và phiên bản đã áp dụng tại thời điểm ghi nhận.
 */
public class ViolationTypeDefinition {

    /**
     * Mã loại vi phạm ổn định dùng làm ID tài liệu.
     */
    private String code;

    /**
     * Phiên bản nội dung danh mục.
     */
    private Integer version;

    /**
     * Nhãn tiếng Việt hiển thị cho người dùng.
     */
    private String labelVi;

    /**
     * Cho biết loại còn được chọn khi lập hồ sơ mới.
     */
    private Boolean active;

    /**
     * Thời điểm bắt đầu áp dụng phiên bản.
     */
    private Date effectiveFrom;

    /**
     * Thời điểm kết thúc áp dụng, nếu đã ngừng dùng.
     */
    private Date effectiveTo;

    /**
     * Phiên bản cấu trúc tài liệu.
     */
    private Integer schemaVersion;

    /**
     * Constructor rỗng để khởi tạo model và hỗ trợ ánh xạ dữ liệu sau này.
     */
    public ViolationTypeDefinition() {
    }

    /**
     * Khởi tạo bản ghi với các trường dữ liệu tương ứng. Việc kiểm quyền,
     * kiểm tra quan hệ và tính hợp lệ phải thực hiện tại repository/backend.
     */
    public ViolationTypeDefinition(
            String code,
            Integer version,
            String labelVi,
            Boolean active,
            Date effectiveFrom,
            Date effectiveTo,
            Integer schemaVersion
    ) {
        this.code = code;
        this.version = version;
        this.labelVi = labelVi;
        this.active = active;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.schemaVersion = schemaVersion;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getLabelVi() {
        return labelVi;
    }

    public void setLabelVi(String labelVi) {
        this.labelVi = labelVi;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Date getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Date effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Date getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(Date effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

}
