# 08 — Chuẩn viết mã Android

> Áp dụng khi có nhiệm vụ triển khai được duyệt. Hiện tại chỉ có khung ứng dụng và model; xem [hiện trạng](13-CURRENT-PROJECT-STATUS.md). Không sửa mã trong nhiệm vụ tạo tài liệu này.

## Ngôn ngữ, tài nguyên và tên

- Java cho Android, XML cho layout/navigation/resource; Gradle Kotlin DSL chỉ là định dạng build hiện có, không phải giấy phép chuyển mã ứng dụng sang Kotlin. Không dùng Jetpack Compose.
- Class/method/variable/field/package bằng tiếng Anh, tên nêu mục đích (`VehicleLink`, `plateCanonical`, `recordViolation`). Tên enum giữ ổn định khi đã lưu bền vững; thay đổi cần migration.
- Comment và JavaDoc tiếng Việt. Mỗi class nêu trách nhiệm, phạm vi, quan hệ quan trọng. Phương thức nghiệp vụ phức tạp nêu mục đích, tham số, giá trị trả về, lỗi/trường hợp đặc biệt. Giải thích **vì sao/quy tắc nghiệp vụ**, không diễn giải lại lệnh. Khi đổi logic, cập nhật comment.
- Tài liệu Markdown tiếng Việt. Chuỗi người dùng qua `strings.xml`/resource bản địa hóa; không hardcode trong Java/XML khi dùng resource được. Không đưa mật khẩu, token, biển số, ảnh hoặc PII vào log/test snapshot công khai.

## Phân tách trách nhiệm

Fragment/Activity render và nhận tương tác; ViewModel nắm `UiState`/điều phối, không giữ View/bitmap lớn; domain chứa nghiệp vụ độc lập khi thực sự cần; repository phối hợp nguồn dữ liệu; datasource làm việc với API/DB; camera/AI/location có adapter riêng. Không tạo nhiều lớp mỏng chỉ để đủ MVVM. Giữ package hiện có, thêm theo lát chức năng và test. Xem [kiến trúc](05-SYSTEM-ARCHITECTURE.md).

## Dữ liệu, lỗi và bảo mật

1. Kiểm tra đầu vào ở UI để hướng dẫn người dùng, **kiểm tra lại ở nguồn dữ liệu/backend** để bảo vệ tính toàn vẹn. Chuẩn hóa biển số có version, giữ dữ liệu gốc để audit; không tự gán chủ xe theo chuỗi plate.
2. Phân loại lỗi validation, quyền, kết nối, timeout, xung đột, lưu ảnh, suy luận AI. Hiện thông báo cụ thể bằng resource; không nuốt exception hoặc hiển thị raw stack trace. Retry có idempotency và giới hạn.
3. Không lưu password thuần trong `User`/DB, không tin role/ownerId từ client, không lưu bí mật trong repo. Mọi request ảnh/hồ sơ cần kiểm quyền backend. Xem [Security](09-SECURITY-PRIVACY.md).
4. Date/time: lưu mốc UTC có định nghĩa rõ, hiển thị múi giờ theo sản phẩm; phân biệt thời điểm chụp, thời điểm server nhận và thời điểm duyệt. Vị trí không có thì dùng trạng thái/nullable, không dùng `0,0` làm sentinel.
5. Khi chụp ảnh chính, gắn `capturedAt` và snapshot vị trí với ID ảnh; đo độ mới bằng đồng hồ đơn điệu trên cùng thiết bị và kiểm sai số theo ngưỡng được duyệt. Không lấy vị trí lúc lưu hồ sơ hoặc `lastLocation` chưa kiểm tuổi để thay thế. Ảnh phụ không ghi đè snapshot của ảnh chính; nguồn thủ công cần trạng thái/lý do riêng. Xem [GPS trong yêu cầu](01-PRODUCT-REQUIREMENTS.md) và [thiết kế dữ liệu](04-DATABASE-DESIGN.md).

## Lifecycle và tài nguyên

Không làm network/AI/crop ảnh trên UI thread. Bind CameraX theo lifecycle, đóng `ImageProxy` trong mọi nhánh, giải phóng `Interpreter`, executor, stream và bitmap đúng lúc. ViewBinding của Fragment chỉ tồn tại từ `onCreateView` đến `onDestroyView`; ViewModel không giữ Activity/Fragment/View context. Callback bất đồng bộ phải kiểm tra màn hình còn sống hoặc sử dụng owner phù hợp. Giới hạn ảnh trong bộ nhớ, xử lý rotation, định dạng ảnh và áp lực bộ nhớ. Tham khảo [CameraX ImageAnalysis](https://developer.android.com/media/camera/camerax/analyze), [ViewModel cho Views](https://developer.android.com/topic/libraries/architecture/views/viewmodel).

## Dependency, kiểm thử và review

Không thêm dependency trùng hoặc tự nâng phiên bản. Trước thư viện mới: nêu nhu cầu, phương án thay thế, license, kích thước app, chi phí, bảo trì và tác động dữ liệu; ghi [Decision Log](15-DECISION-LOG.md) sau duyệt. Viết test cho chuẩn hóa biển số, khoảng hiệu lực, quyền, trạng thái, retry và hậu xử lý AI dựa trên mẫu thật; không viết test chỉ lặp lại implementation. Review phải kiểm tra diff, tài nguyên, lỗi, lifecycle, PII/log, dependency, khả năng tiếp cận và kết quả test theo [chiến lược](11-TESTING-STRATEGY.md).
