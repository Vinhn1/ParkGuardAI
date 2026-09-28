package com.example.parkguardai.data.model;

/**
 * Đại diện cho một người dùng trong hệ thống ParkGuard.
 *
 * Model này chỉ chịu trách nhiệm lưu trữ dữ liệu cơ bản
 * của người dùng. Logic đăng nhập, phân quyền và truy cập
 * cơ sở dữ liệu được xử lý bởi các thành phần khác.
 */

public class User {
    private String id;
    private String fullname;
    private String email;
    private UserRole role;

    /**
     * Constructor rỗng hỗ trợ việc khởi tạo User trước khi
     * gán dữ liệu và giúp tương thích với các cơ chế ánh xạ
     * dữ liệu có thể được sử dụng sau này.
     */
    public User() {
    }

    /**
     * Khởi tạo người dùng với các thông tin cơ bản.
     */
    public User(String id, String fullname, String email, UserRole role) {
        this.id = id;
        this.fullname = fullname;
        this.email = email;
        this.role = role;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}
