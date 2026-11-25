package com.example.parkguardai.data.datasource;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

/**
 * Làm việc với Firebase Authentication qua email và mật khẩu.
 *
 * Lớp này chỉ tạo phiên và gửi email xác minh; hồ sơ sinh viên và yêu cầu
 * đăng ký xe thuộc datasource/repository riêng. Firebase Spark không gửi
 * SMS OTP cho Phone Auth, nên số điện thoại liên hệ không được đánh dấu
 * đã xác minh hoặc dùng làm thông tin đăng nhập trong luồng này.
 */
public class FirebaseEmailAuthDataSource {

    private final FirebaseAuth auth;

    /**
     * Nhận một FirebaseAuth đã được cấu hình bởi tầng khởi tạo ứng dụng.
     *
     * @param auth đối tượng xác thực; không được null
     */
    public FirebaseEmailAuthDataSource(FirebaseAuth auth) {
        this.auth = Objects.requireNonNull(auth, "auth");
        this.auth.setLanguageCode("vi");
    }

    /**
     * Tạo danh tính bằng email/mật khẩu. Thành công ở đây chưa có nghĩa
     * email đã xác minh hoặc hồ sơ Firestore/xe đầu tiên đã được lưu.
     * Repository phải xử lý trạng thái tạo tài khoản dở dang và gửi email
     * xác minh bằng {@link #sendVerificationEmail()} sau khi có phiên.
     *
     * @param email email sinh viên đã được kiểm tra ở form
     * @param password mật khẩu chỉ truyền cho Firebase Auth, không lưu model/log
     * @return tác vụ tạo danh tính Firebase
     */
    public Task<AuthResult> register(String email, String password) {
        return auth.createUserWithEmailAndPassword(email, password);
    }

    /**
     * Đăng nhập bằng email/mật khẩu; quyền xem dữ liệu còn phải kiểm bằng
     * Security Rules và nguồn cấp quyền đáng tin cậy.
     *
     * @param email email của tài khoản
     * @param password mật khẩu không được lưu hoặc ghi log
     * @return tác vụ đăng nhập Firebase
     */
    public Task<AuthResult> signIn(String email, String password) {
        return auth.signInWithEmailAndPassword(email, password);
    }

    /**
     * Yêu cầu Firebase gửi liên kết đặt lại mật khẩu qua email.
     * Tầng UI phải dùng thông báo an toàn, không tiết lộ tài khoản tồn tại
     * hay không dựa trên lỗi trả về của dịch vụ.
     *
     * @param email địa chỉ người dùng nhập trong luồng khôi phục
     * @return tác vụ yêu cầu gửi liên kết khôi phục
     */
    public Task<Void> sendPasswordResetEmail(String email) {
        return auth.sendPasswordResetEmail(email);
    }

    /**
     * Gửi liên kết xác minh đến email của người dùng đang đăng nhập.
     * Nếu chưa có phiên, tác vụ trả lỗi để UI hướng dẫn đăng nhập lại;
     * không được hiểu lỗi gửi thư là chưa tạo tài khoản.
     *
     * @return tác vụ gửi email xác minh
     */
    public Task<Void> sendVerificationEmail() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return Tasks.forException(new IllegalStateException());
        }
        return user.sendEmailVerification();
    }

    /**
     * Tải lại trạng thái người dùng từ Firebase trước khi kiểm emailVerified.
     * Không dựa vào giá trị phiên cũ khi quyết định cho đăng ký phương tiện.
     *
     * @return tác vụ làm mới; lỗi nếu chưa có phiên
     */
    public Task<Void> reloadCurrentUser() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return Tasks.forException(new IllegalStateException());
        }
        return user.reload();
    }

    /**
     * Đọc trạng thái email sau khi {@link #reloadCurrentUser()} hoàn thành.
     * Giá trị này phục vụ UI; quyền tại backend/Rules phải được kiểm riêng.
     *
     * @return true khi có phiên và Firebase báo email đã xác minh
     */
    public boolean isCurrentEmailVerified() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null && user.isEmailVerified();
    }

    /**
     * Đóng phiên Firebase trên thiết bị hiện tại.
     */
    public void signOut() {
        auth.signOut();
    }
}
