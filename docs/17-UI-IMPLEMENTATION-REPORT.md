# 17 — Báo cáo giao diện xác thực đang triển khai

> Cập nhật 2026-10-05 theo DEC-017–020. Mã hiện tại tập trung Splash → trang chào → đăng nhập/đăng ký → hồ sơ sinh viên/xe đầu tiên. Launcher debug và toàn bộ shell/tab role dùng để xem trước đã được xóa theo yêu cầu mới. Người dùng báo đã thử thành công đăng ký email và lưu Firestore trên thiết bị thật; các mục còn lại chưa có bằng chứng nghiệm thu.

## Luồng hiện có

| Bước | Trạng thái mã | Giới hạn |
|---|---|---|
| Splash và trang chào | AndroidX SplashScreen, hero, hai nút đăng nhập/đăng ký, chuyển cảnh ngắn và tôn trọng cài đặt tắt animation | Không kiểm quyền lúc splash |
| Đăng nhập | Email/mật khẩu, lỗi tại trường, trạng thái tải, quên mật khẩu, đăng xuất; sau Auth có gửi lại và kiểm tra email xác minh | Chưa có profile/grant tin cậy và điều hướng theo vai trò |
| Tạo tài khoản Auth | Email/mật khẩu/nhập lại, tạo Firebase Auth, gửi/gửi lại và kiểm tra email xác minh; lỗi gửi thư giữ trạng thái tài khoản đã tạo | Chưa lưu `UserProfile` hoặc yêu cầu xe |
| Onboarding sinh viên sau xác minh | Form họ tên bắt buộc, số liên hệ tùy chọn chưa xác minh, loại xe và biển số bắt buộc; giao dịch Firestore tạo hồ sơ + xe + yêu cầu `PENDING`, có ID ổn định để thử lại; đọc từ server khi mở lại | Chưa có Rules triển khai trên dự án thật hoặc nguồn tin cậy duyệt xe; không được hiển thị thành đã liên kết/đã xác minh |

Không thu mã sinh viên theo DEC-007B. Form Auth không thu tên, phone hoặc biển số trước khi email được xác minh. Phần hồ sơ/xe là bước bắt buộc kế tiếp; giao dịch Firestore thất bại khi offline và không công bố thành công trước khi server xác nhận. Khi mở lại, ứng dụng đọc yêu cầu từ server để tiếp tục hoặc hiển thị đã gửi. `firestore.rules` chỉ cho Auth đã xác minh tạo tài liệu của chính UID, giới hạn field/giá trị, chặn sửa/xóa và mặc định từ chối các đường dẫn khác. Không có role/grant/link `VERIFIED` do client tạo.

## Wireframe chặng kế tiếp

```text
Splash → Chào mừng → Đăng nhập / Tạo tài khoản
Tạo tài khoản: Email* · Mật khẩu* · Nhập lại*
→ Kiểm tra hộp thư · Gửi lại · Tôi đã xác minh
→ Hoàn tất hồ sơ sinh viên
   Họ và tên*
   Số điện thoại liên hệ (tùy chọn, chưa xác minh)
   Xe đầu tiên: loại xe* · biển số*
   Biển số tự khai, cần xét duyệt thủ công
   Thông báo xử lý dữ liệu và đồng ý theo chính sách được duyệt
   [Gửi hồ sơ và yêu cầu xác minh xe]
→ Đã lưu trên máy chủ · Xe PENDING / lỗi và thử lại
```

Biển số không dùng để tìm chủ xe hoặc mở quyền xem vi phạm. Quy tắc chuẩn hóa bản đầu bỏ dấu cách, chấm, gạch nối và chuyển chữ hoa; chỉ kiểm tra ký tự/độ dài ở client/Rules, không khẳng định đúng biển số Việt Nam hay không trùng. Nhân sự duyệt thủ công phải kiểm lại raw/canonical và chứng cứ; kiểm trùng nguyên tử và cấp link `VERIFIED` thuộc backend đặc quyền sau này. Cần chốt thông báo, đồng ý, thời hạn giữ/xóa theo DEC-006 trước vận hành với dữ liệu thật.

## Hướng giao diện

Giao diện dùng Java/XML/ViewBinding và Material 3 TextInputLayout với nhãn nổi, thông báo lỗi ngay dưới trường, hiện/ẩn mật khẩu và vùng chạm tối thiểu 48dp. Splash dùng API hệ thống; trang chào và form dùng fade/rise ngắn qua ViewPropertyAnimator khi animation được bật. Không thêm thư viện motion hay nhận diện trường chưa được duyệt.

Nguồn: [Android SplashScreen](https://developer.android.com/develop/ui/views/launch/splash-screen/migrate), [Material Text Fields](https://github.com/material-components/material-components-android/blob/master/docs/components/TextField.md), [Firebase Auth email/mật khẩu](https://firebase.google.com/docs/auth/android/password-auth), [Firebase xác minh email](https://firebase.google.com/docs/auth/android/manage-users).

## Kiểm tra và giới hạn

- Parse XML/tài nguyên và kiểm đường dẫn sau khi thêm màn hình onboarding: cần chạy lại ở cuối thay đổi.
- `:app:assembleDebug` trong môi trường Linux hiện tại: chưa chạy được vì không có `java`/`JAVA_HOME`. Bản working tree mới chưa có bằng chứng build hoặc chạy trên thiết bị.
- Firebase Auth và ghi Firestore của luồng đăng ký: người dùng báo đã thử thành công trên thiết bị thật; agent chưa quan sát log hay kiểm thử trực tiếp.
- Firestore Rules mới chỉ là file trong repository, chưa có bằng chứng deploy; cần thử quyền chéo, email chưa xác minh, client tự sửa status và retry trên thiết bị/dự án kiểm thử trước khi dùng dữ liệu thật. Theo yêu cầu người dùng, không chạy emulator. Không có CameraX, GPS, Storage Rules, lưu ảnh hoặc màn hình role thật trong đợt này. Kế hoạch camera vẫn ở [18](18-CAMERA-CAPTURE-AND-SAVE-PLAN.md).
