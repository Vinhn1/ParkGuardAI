# 11 — Chiến lược kiểm thử

> Hiện chỉ có test mẫu [`ExampleUnitTest.java`](../app/src/test/java/com/example/parkguardai/ExampleUnitTest.java) và [`ExampleInstrumentedTest.java`](../app/src/androidTest/java/com/example/parkguardai/ExampleInstrumentedTest.java). Không có test nghiệp vụ; build chưa chạy vì môi trường khảo sát thiếu Java/SDK. Ma trận dưới đây là `PLANNED` theo ADR-001..005 và GPS, chưa có kết quả chạy.

## Tầng kiểm thử

| Tầng | Phạm vi ưu tiên | Dữ liệu và tiêu chí |
|---|---|---|
| Unit Java | Chuẩn hóa/kiểm tra biển số, email/phone theo policy, khoảng hiệu lực VehicleLink, chuyển trạng thái, idempotency, phân loại lỗi | Case bảng có giá trị hợp lệ/sai/xung đột/ranh giới thời gian; không mirror getter/setter |
| Integration | Repository ↔ datasource, upload ảnh ↔ metadata, retry, transaction, pagination, phục hồi phiên | Fake/Emulator, lỗi có chủ đích, xác minh không tạo trùng hoặc hồ sơ nửa vời |
| Authorization | S-A không xem S-B, G ngoài phạm vi, role giả từ client, URL ảnh, token bị thu, xe chuyển chủ | Test cả list/chi tiết/file/sửa; deny tại backend/rules, không chỉ UI |
| UI/instrumented | Đăng ký lỗi trường, camera permission denied, nhiều biển số, OCR sai và sửa tay, thoát bản nháp, offline | Thiết bị/emulator phù hợp, chụp màn hình kiểm các trạng thái, TalkBack/font lớn |
| AI/model | I/O tensor, tiền xử lý, decode/NMS, box/crop, OCR, chất lượng thực địa, performance | Bộ ảnh gắn nhãn tách training; báo metric theo điều kiện và phiên bản model |
| Security/privacy | Logcat không PII, cache/backup, direct URL, file export, consent, deletion, audit | Kiểm thử truy cập trái phép, restore thiết bị, request giả/sai payload |

## Bộ dữ liệu AI và đo đạc

Thiết lập tập thực địa có quyền sử dụng ảnh và nhãn người kiểm, tách biệt khỏi dữ liệu huấn luyện. Chia theo biển xe máy một/hai dòng, ô tô, ngày/đêm, bóng phản chiếu, góc nghiêng, khoảng cách, độ mờ, nhiều xe/biển số, không có biển, vật thể giống biển. Không chỉ lấy ảnh dễ. Quản lý version dữ liệu và loại bỏ/ẩn danh người xung quanh khi phù hợp chính sách.

Đo detector: precision/recall theo box và mAP; crop: tỷ lệ cắt đủ biển; **ML Kit OCR**: character error rate, full-plate accuracy, tỷ lệ cần sửa, thứ tự hai hàng và lỗi từng ký tự; end-to-end: tỷ lệ hồ sơ hoàn tất đúng, sai gán phương tiện, thời gian xử lý. Báo metric **riêng cho biển xe máy một hàng/hai hàng** và điều kiện ngày/đêm/góc/mờ; so sánh raw OCR với chuỗi sau chuẩn hóa và chuỗi guard xác nhận, không tự sửa ký tự không chắc chắn. Báo confidence interval hoặc kích thước mẫu và điều kiện thu; không áp số liệu 99% của tập test huấn luyện cho trường. Benchmark trên thiết bị Android mục tiêu: cold/warm inference, p50/p95, RAM, pin/nhiệt, thời gian camera mở, FPS và crash; kiểm CPU/delegate nếu dùng. Ngưỡng chấp nhận cần chốt dựa pilot.

## Kiểm thử tình huống nghiệp vụ bắt buộc

| ID | Tình huống | Kỳ vọng |
|---|---|---|
| T-01 | Hai sinh viên đăng ký một biển số cùng lúc | Không tự xác minh cả hai; có xung đột và quy trình xét duyệt, không lộ thông tin đối phương. |
| T-02 | Xe chuyển liên kết ngày T, vi phạm ngày T-1/T+1 | Quyền xem theo chính sách và khoảng thời gian; chủ mới không tự thấy lịch sử cũ. |
| T-03 | OCR đọc sai một ký tự | Bảo vệ chỉnh tay, lưu raw/final và audit, không tự gán sai chủ. |
| T-04 | Ảnh có nhiều biển số/không có biển số | Chọn đúng vùng hoặc nhập tay; không lấy mặc định kết quả AI. |
| T-05 | GPS bị từ chối/không có mạng | Hiện đường thay thế hợp lệ, không lưu `0,0` như tọa độ thật, không báo đã gửi khi offline. |
| T-06 | Gửi hồ sơ hai lần sau timeout | Một hồ sơ theo idempotency key; trạng thái ảnh/metadata nhất quán. |
| T-07 | Sửa role/ownerId/status trong request | Backend/rules từ chối, audit nếu phù hợp, không đổi quyền. |
| T-08 | Sinh viên đoán ID/URL hồ sơ khác | Trả lỗi quyền; ảnh không mở được qua URL public dài hạn. |
| T-09 | Thay phone liên hệ hoặc yêu cầu reset | Số mới vẫn gắn nhãn chưa xác minh; số cũ không còn là liên hệ; reset chỉ theo email Auth, không chiếm tài khoản bằng số chuyển chủ. |
| T-10 | App bị dừng/khôi phục khi chụp | Không leak camera/ảnh, bản nháp nhất quán và xóa theo policy. |
| T-11 | Đăng ký trên Spark có/không nhập phone | Cả hai trường hợp được tiếp tục sau xác minh email; UI không gửi SMS OTP, phone nhập vào không thành định danh đã xác minh. |
| T-12 | Firebase Auth tạo tài khoản nhưng gửi email xác minh hoặc ghi profile lỗi | Giữ trạng thái dở dang có đường gửi lại/tiếp tục, không tạo tài khoản thứ hai hoặc báo onboarding hoàn tất. |

### Ma trận GPS gắn ảnh chính — bắt buộc trước pilot

Thiết lập ngưỡng đo (`maxAccuracyMeters`, `maxFixAgeMillis`, `maxCaptureDeltaMillis`, `locationTimeoutMillis`) từ DEC-008A, rồi kiểm các ca ở hai phía ranh giới; không hardcode con số chưa duyệt vào test. Dùng đồng hồ giả/fix giả cho unit test, emulator và thiết bị thật cho permission/settings/độ trễ; kiểm thời điểm chụp là thời điểm sự kiện ảnh, không phải callback lưu file.

| ID | Tình huống | Kết quả cần chứng minh |
|---|---|---|
| GPS-01 | Quyền và dịch vụ bật, fix mới/đủ chính xác | `primaryEvidenceId`, `capturedAt`, `locationTimestamp`, tọa độ, sai số, `FUSED_AUTO`, `AUTO_ACCEPTED` được lưu/hiển thị nhất quán; không yêu cầu nhập tay. |
| GPS-02 | Người dùng từ chối quyền hoặc chỉ cấp approximate không đạt ngưỡng | Hiện quyền/trạng thái đúng, cho thử lại/chụp lại/thủ công đã duyệt; không lưu như GPS hợp lệ. |
| GPS-03 | Dịch vụ định vị tắt | Hướng dẫn bật dịch vụ và thử lại; không lấy cached fix thay thế. |
| GPS-04 | Fix có sai số lớn hoặc thiếu `hasAccuracy()` | `REJECTED_INACCURATE`/trạng thái tương đương; không hiển thị độ chính xác giả, không báo thành công. |
| GPS-05 | Fix đo quá lâu trước `capturedAt` hoặc lệch quá ngưỡng sau chụp | `REJECTED_STALE`/trạng thái tương đương; không dùng `getLastLocation()` hoặc vị trí lúc lưu hồ sơ. |
| GPS-06 | Không nhận fix trước timeout/null/error | Báo rõ, cho thử lại/chụp lại/thủ công đã duyệt; không treo UI hoặc gửi hồ sơ như có tọa độ hợp lệ. |
| GPS-07 | Bảo vệ di chuyển sau khi chụp, fix đến muộn | Vị trí sau di chuyển không gắn cho ảnh cũ nếu vượt ngưỡng; hồ sơ giữ lỗi/ngoại lệ hoặc chụp lại. |
| GPS-08 | Chụp nhiều ảnh trong cùng hồ sơ | Ảnh phụ có metadata riêng, không ghi đè vị trí ảnh chính; đổi ảnh chính tường minh thì rà lại snapshot và audit. |
| GPS-09 | Giờ thiết bị đổi, callback lưu ảnh chậm, process bị dừng | So tuổi fix trên đồng hồ đơn điệu cùng boot, persisted UTC/ID ảnh nhất quán; không dùng callback time làm `capturedAt`. |
| GPS-10 | Guard giả request sửa nguồn/trạng thái hoặc xem tọa độ ngoài phạm vi | Backend/rules từ chối; thủ công chỉ được duyệt theo quyền, ảnh/vị trí không rò qua log/URL. |

### Firebase và quyền truy cập — bắt buộc trước kết nối môi trường thật

Dùng Firebase Emulator Suite với **Firestore Rules và Storage Rules đã nạp rõ ràng**, Auth test users và dữ liệu giả; test cả thành công và từ chối. Backend Admin SDK chạy trong test riêng vì server SDK bỏ qua Rules. [Firebase Rules unit tests](https://firebase.google.com/docs/rules/unit-tests), [Firestore rules/query](https://firebase.google.com/docs/firestore/security/rules-query).

| ID | Tình huống | Kỳ vọng |
|---|---|---|
| FB-01 | Khách hoặc S-A đọc/sửa `users/S-B`, xe/link/request của S-B | Từ chối ở Rules, kể cả get theo ID và list/query. |
| FB-02 | S gửi profile/request chứa `role=ADMIN`, `VERIFIED`, `reviewerUid`, `validFrom` | Từ chối hoặc trường bị backend kiểm loại, không cấp quyền. |
| FB-03 | G ngoài `guardScopes` tạo/đọc/sửa violation hoặc xem ảnh | Từ chối Firestore và Storage; G trong scope chỉ có đúng thao tác được giao. |
| FB-04 | A không có grant duyệt/đọc audit/export | UI có thể ẩn nhưng Rules/backend vẫn từ chối. |
| FB-05 | Hai ADMIN duyệt đồng thời cùng `plateCanonical` cho hai UID | Chỉ một transaction `plateClaim` active thành công; request kia xung đột, không lộ chủ khác; audit nhất quán. |
| FB-06 | Chuyển nhượng xe; vi phạm trước/sau `validFrom` | Chủ mới không tự xem hồ sơ trước liên kết; projection/quyền ảnh được cập nhật, chủ cũ xử lý theo policy. |
| FB-07 | S/G đoán path Storage hoặc dùng download URL/metadata của hồ sơ khác | Không tải được ảnh; upload sai MIME/size/path bị từ chối; không ghi đè ảnh gốc. |
| FB-08 | Client sửa `primaryEvidenceId`, `locationStatus`, trạng thái vi phạm hoặc audit log | Từ chối nếu không qua nghiệp vụ đặc quyền; ảnh chính/snapshot không lệch. |
| FB-09 | Mất mạng sau upload ảnh/trước ghi metadata, hoặc Firestore cache phát local write | Không báo hồ sơ thành công; ảnh mồ côi có xử lý; retry idempotent không nhân đôi. |
| FB-10 | Thu quyền ADMIN/GUARD khi token cũ còn hiệu lực | Theo cơ chế refresh/active grant đã thiết kế, không tiếp tục tác vụ đặc quyền trái quyền; có test thời điểm thu quyền. |

## Cổng chất lượng theo giai đoạn

Trước merge/triển khai: test unit nghiệp vụ và quyền pass; integration Firebase/Auth/Firestore/Storage và backend đặc quyền pass; UI test luồng trọng yếu; Firestore/Storage Rules deny trái quyền; chạy trên thiết bị thật đối với CameraX/YOLO/ML Kit/GPS; kiểm privacy/backup; review diff/secret. Nếu thiếu thiết bị/SDK/mạng, ghi cổng **chưa xác minh** và người chịu trách nhiệm. [Android CameraX ImageAnalysis](https://developer.android.com/media/camera/camerax/analyze) làm cơ sở kiểm close/lifecycle. Chưa tạo môi trường Firebase thật trong nhiệm vụ tài liệu.
