# 00 — Tổng quan ParkGuard AI

> Cập nhật: 2026-10-02. `IMPLEMENTED/PARTIAL` phản ánh working tree được khảo sát, không phải lời cam kết sản phẩm đã dùng được. Xem [hiện trạng](13-CURRENT-PROJECT-STATUS.md).

## Mục tiêu và phạm vi

ParkGuard AI là ứng dụng Android dự kiến hỗ trợ bảo vệ Trường Đại học Cửu Long ghi nhận trường hợp đỗ xe sai quy định; phát hiện vùng biển số bằng AI, đọc ký tự bằng OCR riêng, lưu bằng chứng và cho sinh viên tra cứu hồ sơ được phép liên kết với mình. **Con người xác nhận hành vi vi phạm và biển số cuối cùng**. Biển số chỉ là dấu hiệu tra cứu, không chứng minh quyền sở hữu hoặc quyền sử dụng xe.

| Nhóm | Nhu cầu chính | Trạng thái |
|---|---|---|
| `STUDENT` | Tài khoản, nhiều phương tiện, xem xác minh và vi phạm hợp lệ | `PLANNED`; enum đã có tên vai trò |
| `SECURITY_GUARD` | Chụp ảnh, rà kết quả AI/OCR, tạo và xem hồ sơ trong phạm vi | `PLANNED`; enum đã có tên vai trò |
| `ADMIN` | Cấp quyền, xác minh, cấu hình, kiểm tra lịch sử trong app Android | `APPROVED` về sản phẩm; enum `UserRole.java` đã có giá trị ADMIN, còn giao diện và phân quyền là `PLANNED` |

Phạm vi mã hiện tại là **một module Android `:app`**, chưa có Firebase Project, backend hoặc cơ sở dữ liệu thật. Nền tảng sản phẩm là Java + XML/View System; kiến trúc hướng MVVM. Theo [ADR-001..005](15-DECISION-LOG.md), kiến trúc mục tiêu đã chọn Firebase Authentication, Cloud Firestore, Firebase Cloud Storage, ML Kit OCR thử nghiệm và ADMIN trực tiếp trong app; bản đầu online-first với xác minh xe thủ công. Đã có enum ADMIN, các POJO dữ liệu và datasource email Firebase Auth ở mức `PARTIAL`; **chưa có luồng đăng ký chạy được hoặc tích hợp Firestore**.

## Công nghệ và bằng chứng hiện tại

| Nội dung | Trạng thái | Bằng chứng |
|---|---|---|
| Java, XML, ViewBinding | `IMPLEMENTED` ở mức khung | [`app/build.gradle.kts`](../app/build.gradle.kts), [`MainActivity.java`](../app/src/main/java/com/example/parkguardai/MainActivity.java), [`activity_main.xml`](../app/src/main/res/layout/activity_main.xml) |
| Navigation Component | `PARTIAL`: có dependency và NavHost, graph rỗng | [`gradle/libs.versions.toml`](../gradle/libs.versions.toml), [`nav_graph.xml`](../app/src/main/res/navigation/nav_graph.xml) |
| Material 3 theme | `PARTIAL`: theme nền; chưa có Design System | [`themes.xml`](../app/src/main/res/values/themes.xml), [`colors.xml`](../app/src/main/res/values/colors.xml) |
| TensorFlow Lite | `PARTIAL`: dependency và asset; không có suy luận | [`app/build.gradle.kts`](../app/build.gradle.kts), [`parkguard_yolo11n_best.tflite`](../app/src/main/assets/parkguard_yolo11n_best.tflite) |
| Firebase Authentication email | `PARTIAL`: có SDK, quyền Internet và datasource tạo/đăng nhập/gửi email xác minh; chưa có Firebase Project/config, UI hoặc kiểm thử tích hợp | [`app/build.gradle.kts`](../app/build.gradle.kts), [`FirebaseEmailAuthDataSource.java`](../app/src/main/java/com/example/parkguardai/data/datasource/FirebaseEmailAuthDataSource.java) |
| CameraX, ML Kit OCR, location, Firestore/Storage | `PLANNED` về mã; lựa chọn ML Kit/Firebase đã `APPROVED` về kiến trúc | Chưa có dependency hoặc lớp triển khai tương ứng tại [`app/build.gradle.kts`](../app/build.gradle.kts) |

Theo thông tin dự án cung cấp, mô hình YOLO11n đã huấn luyện trên bộ Vietnamese License Plate từ Roboflow: 8.357 ảnh (train 5.845, validation 1.680, test 832), 20 epoch, ảnh 640 × 640; precision khoảng 99,14%, recall khoảng 99,06%, mAP@50 khoảng 99,49%, mAP@50–95 khoảng 70,49%. Repository **không có báo cáo đánh giá, mô tả phiên bản dataset, tập test hoặc metadata suy luận đủ để kiểm chứng các số liệu này**. Chúng chỉ mô tả tập kiểm thử được báo cáo, không dự báo hiệu năng tại trường. Xem [AI pipeline](06-AI-PIPELINE.md).

## Ranh giới sản phẩm

- Hồ sơ vi phạm phải có ảnh hiện trường, biển số đã được người ghi nhận xác nhận, loại vi phạm, người ghi nhận và trạng thái xử lý. Với **ảnh chính**, ứng dụng phải ghi thời điểm chụp và yêu cầu vị trí tự động gần thời điểm đó; chỉ chấp nhận fix đạt ngưỡng đã duyệt. Nếu không đạt, cảnh báo và cho chụp lại hoặc xử lý vị trí thủ công theo quy trình được phê duyệt; không thay bằng vị trí khi lưu hồ sơ.
- Sinh viên cần quyền tra cứu có căn cứ từ liên kết phương tiện **đã xác minh theo khoảng thời gian**; hồ sơ cũ không tự chuyển cho chủ tài khoản mới.
- Dữ liệu cá nhân, biển số, ảnh và vị trí cần mục đích xử lý rõ, phân quyền, thời hạn giữ và phương án xóa. Xem [bảo mật và quyền riêng tư](09-SECURITY-PRIVACY.md).
- Sinh viên tạo tài khoản, xác minh email và đăng ký ít nhất một phương tiện; số điện thoại là liên hệ tùy chọn, chưa xác minh trên Spark. Quyền sử dụng xe chỉ được xác minh thủ công bởi người được cấp quyền. Lịch sử vi phạm cũ không tự chuyển cho chủ mới.
- Chức năng gửi yêu cầu xem xét lại là `PROPOSED`, chưa thuộc phạm vi bắt buộc. Firebase/ML Kit/ADMIN trong app đã được duyệt; cách triển khai quyền đặc biệt, biến thể ML Kit bundled/unbundled, ngưỡng GPS và chính sách dữ liệu vẫn cần chốt. Không tự tạo Firebase Project hoặc cấu hình môi trường thật.

## Cách đọc bộ tài liệu

[Yêu cầu](01-PRODUCT-REQUIREMENTS.md) → [vai trò/quyền](02-USER-ROLES-PERMISSIONS.md) → [luồng](03-USER-FLOWS.md) → [dữ liệu](04-DATABASE-DESIGN.md) → [kiến trúc](05-SYSTEM-ARCHITECTURE.md) → [chạy ứng dụng](22-DEVICE-SETUP-AND-RUN-GUIDE.md). Đối chiếu mọi đề xuất với [hiện trạng](13-CURRENT-PROJECT-STATUS.md), [nghiên cứu](14-RESEARCH-RECOMMENDATIONS.md) và [Decision Log](15-DECISION-LOG.md) trước khi viết code.
