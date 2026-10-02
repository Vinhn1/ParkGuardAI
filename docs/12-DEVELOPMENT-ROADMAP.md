# 12 — Roadmap phát triển theo lát chức năng

> Đây là kế hoạch `PROPOSED`, không phải chỉ thị triển khai ngay. ADR-001..005 đã duyệt Firebase/ML Kit/ADMIN/xác minh thủ công/online-first; GPS ảnh chính đã duyệt ở mức nghiệp vụ. **Chưa có hồ sơ Sprint hoặc bằng chứng Sprint hoàn thành trong repository**; mã mới chỉ ở trạng thái [13](13-CURRENT-PROJECT-STATUS.md), build chưa xác minh. Các mã `R` bên dưới là **mốc giai đoạn**, backlog `IMP` cuối file là task lập trình có đủ trường để xét duyệt. Không tự tạo Firebase Project hoặc cấu hình thật.

## Giai đoạn 0 — Quyết định và nền kiểm thử

| Mốc | Mục tiêu; đầu vào → đầu ra | Phụ thuộc; hoàn thành khi |
|---|---|---|
| R0.1 | Chốt đối tượng pilot, loại vi phạm và khu vực; yêu cầu sản phẩm → tiêu chí nghiệm thu thực địa | Người dùng/trường; có danh sách người duyệt và tiêu chí rõ |
| R0.2 | Thiết kế Firebase Auth/Firestore/Storage và vùng dữ liệu, Rules, môi trường backend đặc quyền; ADR-001 → thiết kế được duyệt, **chưa tạo project thật** | R0.1; có chi phí, test quyền, vận hành và schema/index dự kiến |
| R0.3 | Chốt các quyết định **còn mở**: mã SV (phone Spark đã chốt), chứng cứ xe tối thiểu, ngưỡng GPS, lưu giữ, backend đặc quyền; DEC-006/007B/008A và chi tiết ADR-004 → chính sách | R0.1; ghi [Decision Log](15-DECISION-LOG.md), cập nhật yêu cầu/schema |
| R0.5 | Thiết kế tiêu chuẩn fix ảnh chính: ngưỡng sai số/tuổi/chênh thời gian/timeout, quyền precise/approximate, fallback thủ công; yêu cầu GPS → DEC-008A và bộ test ranh giới | R0.1, tham vấn bảo vệ và thiết bị mục tiêu; có ngưỡng được duyệt, không giả định GPS luôn có |
| R0.4 | Thiết lập môi trường Java 21/Android SDK, build baseline, test mẫu; repo hiện tại → báo cáo build/khởi chạy | Không đổi dependency vô cớ; ghi lệnh, thiết bị, kết quả và lỗi cụ thể |

## Giai đoạn 1 — Tài khoản, quyền và dữ liệu phương tiện

| Mốc | Mục tiêu; đầu vào → đầu ra | Phụ thuộc; hoàn thành khi |
|---|---|---|
| R1.1 | Wireframe đăng ký/đăng nhập/xe; [UI/UX](07-UI-UX-GUIDELINES.md) → luồng duyệt | R0.3; được người dùng duyệt trước code |
| R1.2 | Tạo Firebase Auth, vai trò đáng tin, phục hồi/xác minh email; ADR-001/003 → Rules + UI | R0.2, R1.1; test đăng nhập, trùng, nâng role trái phép, reset |
| R1.3 | Schema `VehicleLink` và ràng buộc trùng plate; [Database](04-DATABASE-DESIGN.md) → migration/rules | R0.2/0.3; test tranh chấp và khoảng hiệu lực |
| R1.4 | UI nhiều xe và hàng chờ duyệt; wireframe + R1.3 → màn hình và nghiệp vụ | R1.1/1.3; test ≥2 xe, trạng thái, quyền đọc và audit |

## Giai đoạn 2 — Camera và hồ sơ vi phạm

| Mốc | Mục tiêu; đầu vào → đầu ra | Phụ thuộc; hoàn thành khi |
|---|---|---|
| R2.1 | Kiểm model asset: tensor/metadata/license và ảnh vàng → báo cáo kỹ thuật | R0.4; không giả định shape, có test tiền/hậu xử lý |
| R2.2 | Pilot Google ML Kit Text Recognition, chọn bundled/unbundled và ngưỡng chấp nhận → cấu hình OCR được duyệt | R2.1 và dữ liệu thực địa; có full-plate accuracy riêng cho biển xe máy hai hàng, lỗi theo điều kiện, app size/latency |
| R2.3 | Wireframe camera, khối vị trí ảnh chính, thử lại/chụp lại/thủ công, rà soát và lỗi/offline → phê duyệt UI | R0.3/0.5, R2.2; hiển thị `capturedAt`, sai số, nguồn/trạng thái và mọi nhánh lỗi |
| R2.4 | CameraX capture và ảnh tạm → adapter lifecycle với `primaryEvidenceId`/`capturedAt` đáng tin | R2.3; test quyền, xoay, ảnh mờ, close tài nguyên, nhiều ảnh không ghi đè ảnh chính |
| R2.4a | Nghiên cứu Fused Location Provider/SettingsClient và quyền foreground; thiết kế adapter vị trí một lần khi chụp ảnh chính → đề xuất dependency/chi phí, chưa cài đặt | R0.5; có lựa chọn kỹ thuật được duyệt, kiểm nguồn timestamp, không yêu cầu background location |
| R2.4b | Sau phê duyệt công nghệ, triển khai lấy/kiểm fix và snapshot theo ảnh chính → dữ liệu vị trí và trạng thái UI | R2.4/2.4a, DEC-008A; test GPS-01..09, từ chối stale/low accuracy/timeout, không dùng vị trí lúc lưu |
| R2.5 | Detector, crop, OCR, sửa tay → kết quả gợi ý | R2.1/2.2/2.4; test nhiều/không biển, I/O tensor, bộ nhớ |
| R2.6 | Lưu hồ sơ/ảnh/snapshot vị trí/audit/idempotency → lịch sử của guard | R0.2/0.3/0.5, R2.3/2.4b/2.5; test quyền, retry, ảnh mồ côi, nhiều ảnh và projection ảnh chính, chưa gửi vs đã gửi; nếu chọn Firestore cần Rules/transaction được duyệt |

## Giai đoạn 3 — Tra cứu sinh viên và quản trị

| Mốc | Mục tiêu; đầu vào → đầu ra | Phụ thuộc; hoàn thành khi |
|---|---|---|
| R3.1 | Truy vấn vi phạm theo link đã duyệt/thời gian → API/rules | R1.3, R2.6; test S-A/S-B, chủ mới/vi phạm cũ, file ảnh |
| R3.2 | UI lịch sử/chi tiết/trạng thái sinh viên → màn hình | R3.1, wireframe duyệt; test loading/empty/error/offline và accessibility |
| R3.3 | Quy trình sửa/duyệt hồ sơ và danh mục → sự kiện audit, phiên bản | DEC-003 và R2.6; không ghi đè ảnh/bản gốc, test quyền |
| R3.4 | Thống kê/cấu hình quản trị tối thiểu → báo cáo tổng hợp | R3.3; export PII cần quyền riêng, có audit |

## Giai đoạn 4 — Pilot, bảo mật và phát hành

| Mốc | Mục tiêu; đầu vào → đầu ra | Phụ thuộc; hoàn thành khi |
|---|---|---|
| R4.1 | Kiểm thử thực địa trên thiết bị mục tiêu → báo cáo detector/OCR/end-to-end/hiệu năng | R2.6/3.2; bộ ảnh đại diện, chỉ số theo nhóm, ngưỡng được duyệt |
| R4.2 | Kiểm Firestore/Storage Rules, backend đặc quyền, backup, xóa dữ liệu, audit, secret, vận hành → biên bản privacy/security | R0.3/3.3; test trái quyền và quy trình sự cố pass |
| R4.3 | Hướng dẫn sử dụng/đào tạo bảo vệ và phản hồi pilot → sửa lỗi có ưu tiên | R4.1/4.2; hoàn tất các lỗi P0, có người chịu trách nhiệm |
| R4.4 | Quyết định phát hành → checklist và phê duyệt người dùng | R4.1–4.3; không tự publish/deploy |

## Sau pilot, chỉ khi được chọn

`EXT-01` yêu cầu xem xét lại hồ sơ, OCR tùy biến, tối ưu delegate phần cứng, phân tích thống kê nâng cao. Mỗi hạng mục phải có yêu cầu, chi phí và quyết định riêng. Không mở rộng chỉ vì có công nghệ sẵn.

## Backlog task chi tiết để phê duyệt trước lập trình

Các đường dẫn chưa tồn tại dưới đây là **file dự kiến**, không phải file đã tạo. Chỉ bắt đầu một lát kỹ thuật sau khi lát trước có kết quả và UI liên quan được duyệt. Mỗi commit message là **đề xuất**, không cho phép tự `git add/commit/push`.

### IMP-00 — Xác minh baseline và Sprint hiện tại

- **Mục tiêu:** biết build/khởi chạy hiện tại và cổng chất lượng trước khi thêm Firebase.
- **File dự kiến:** đọc `app/build.gradle.kts`, `gradle/libs.versions.toml`, `app/src/main/res/navigation/nav_graph.xml`; cập nhật `docs/13-CURRENT-PROJECT-STATUS.md` nếu có kết quả, không cần sửa app nếu baseline chạy.
- **Công nghệ:** Java 21/Gradle 9.3.1/Android SDK, Gradle test và emulator/thiết bị.
- **Tiên quyết:** môi trường có Java/SDK; xác nhận working tree đang có thay đổi của người khác.
- **Bước:** ghi status/diff → chạy build/test tối thiểu → mở app và kiểm NavHost → ghi lỗi cụ thể/điều kiện tái hiện.
- **Chấp nhận:** có báo cáo build/launch với lệnh, phiên bản, thiết bị; không đánh dấu Sprint hoàn thành khi không có bằng chứng.
- **Test:** `assembleDebug`, unit test mẫu, smoke launch nếu môi trường cho phép.
- **Rủi ro:** graph rỗng có thể làm app không chạy; Gradle/SDK thiếu hoặc thay đổi sẵn gây nhiễu.
- **Commit đề xuất:** `docs(status): record Android baseline verification` (chỉ nếu tài liệu thay đổi và được duyệt).

### IMP-01 — Sửa điểm vào điều hướng tối thiểu

- **Mục tiêu:** có start destination/luồng khởi động hợp lệ trước màn hình theo vai trò.
- **File dự kiến:** `app/src/main/res/navigation/nav_graph.xml`, `app/src/main/java/com/example/parkguardai/ui/LaunchFragment.java`, `app/src/main/res/layout/fragment_launch.xml`, `app/src/main/res/values/strings.xml`, test điều hướng.
- **Công nghệ:** Java, XML, Navigation, ViewBinding.
- **Tiên quyết:** IMP-00 chỉ ra nhu cầu sửa; wireframe màn hình đầu được người dùng duyệt.
- **Bước:** xác định trạng thái chưa đăng nhập → thêm destination và layout tối thiểu → bind lifecycle → test quay lại/khởi động.
- **Chấp nhận:** app mở ổn định và không đi vào màn hình có dữ liệu khi chưa xác thực; không tự xây toàn bộ UI.
- **Test:** smoke launch, rotation/process recreation cơ bản, back navigation.
- **Rủi ro:** sửa graph đè thay đổi đang stage; cần đối chiếu diff trước sửa.
- **Commit đề xuất:** `fix(navigation): add valid launch destination`.

### IMP-02 — Nền Firebase cục bộ và Rules mặc định từ chối

- **Mục tiêu:** chuẩn bị tích hợp Firebase Auth/Firestore/Storage và bộ test Rules **trên emulator**, chưa tạo Firebase Project thật.
- **File dự kiến:** `app/build.gradle.kts`, `gradle/libs.versions.toml`, `firebase.json`, `firestore.rules`, `storage.rules`, `tests/firebase-rules/*` (đều là dự kiến); tài liệu cấu hình nếu cần.
- **Công nghệ:** Firebase Android SDK, Firebase Emulator Suite, Security Rules.
- **Tiên quyết:** IMP-00, ADR-001; người dùng duyệt dependency/phiên bản và phạm vi code; chưa cần file cấu hình thật.
- **Bước:** đề xuất phiên bản/chi phí → tạo cấu hình local giả → Rules deny-by-default → test khách/role giả bị từ chối → ghi cách chạy.
- **Chấp nhận:** Rules test deny mặc định pass, không có service account/secret/project thật trong repo.
- **Test:** FB-01, FB-02, FB-07 mức skeleton; kiểm secret/diff.
- **Rủi ro:** emulator thiếu môi trường, Rules chưa đủ schema; không nới quyền để làm test pass.
- **Commit đề xuất:** `chore(firebase): add local emulator and closed rules baseline`.

### IMP-03 — Đăng ký/đăng nhập và xác minh email sinh viên

- **Mục tiêu:** hoàn thiện luồng Firebase Auth email/mật khẩu cho STUDENT và profile Firestore riêng không chứa password; datasource email đã có, chưa có Firebase Project/UI/Rules.
- **File dự kiến:** đối chiếu `data/model/UserProfile.java` và `data/datasource/FirebaseEmailAuthDataSource.java` đã có, `app/src/main/java/com/example/parkguardai/ui/auth/*`, `data/repository/AuthRepository.java`, layout auth, `strings.xml`, `nav_graph.xml`, `firestore.rules`, test.
- **Công nghệ:** Firebase Authentication, Firestore, Java/XML/ViewBinding.
- **Tiên quyết:** IMP-02, wireframe auth duyệt; phone là liên hệ tùy chọn chưa xác minh theo DEC-007, mã SV còn DEC-007B.
- **Bước:** form/validation → nối datasource Auth create/login/reset/verify email → tạo profile idempotent → xử lý Auth thành công nhưng gửi email/profile lỗi → UI trạng thái; không thêm Phone Auth/SMS trên Spark.
- **Chấp nhận:** S không thể tự chọn G/A; email chưa xác minh chưa qua bước thêm xe; đăng xuất xóa quyền dữ liệu/cache theo policy.
- **Test:** AUTH-01..03, FB-01/02, lỗi mạng và profile dở.
- **Rủi ro:** hai hệ Auth/Firestore không có transaction chung; phải có phục hồi an toàn.
- **Commit đề xuất:** `feat(auth): add student email authentication flow`.

### IMP-04 — Vai trò ADMIN/GUARD đáng tin và điều hướng riêng

- **Mục tiêu:** có ba role sản phẩm, menu riêng, quyền từ nguồn tin cậy; Android không tự cấp ADMIN.
- **File dự kiến:** kiểm tra `data/model/UserRole.java` đã có ADMIN, `ui/admin/*`, `ui/guard/*`, `nav_graph.xml`, `accessGrants` Rules/test, `backend/<runtime-duyet>/roles/*` (dự kiến).
- **Công nghệ:** Firebase Auth custom claims hoặc grant tài liệu, Firestore Rules, Admin SDK trong backend được chọn.
- **Tiên quyết:** IMP-02/03, duyệt môi trường backend đặc quyền và quy trình cấp quyền; wireframe role duyệt.
- **Bước:** đối chiếu enum ADMIN và role/scope đã có trong POJO → cấp/thu quyền chỉ backend → điều hướng theo role → kiểm lại quyền mọi request.
- **Chấp nhận:** S sửa request/profile không thành ADMIN; G ngoài scope và A thiếu grant bị từ chối; token cũ sau thu quyền được xử lý theo policy.
- **Test:** FB-02..04, FB-10, UI role navigation.
- **Rủi ro:** custom claim có độ trễ refresh, Admin SDK bypass Rules; cần kiểm IAM và grant active.
- **Commit đề xuất:** `feat(auth): add trusted role access and admin navigation`.

### IMP-05 — Đăng ký nhiều xe và yêu cầu xác minh

- **Mục tiêu:** sinh viên sau xác minh email tạo ít nhất một xe, thêm nhiều xe và theo dõi `PENDING`.
- **File dự kiến:** đối chiếu `data/model/VehicleRecord.java` và `data/model/VehicleVerificationRequest.java` đã có, `ui/vehicle/*`, layout xe, `strings.xml`, `firestore.rules`, test.
- **Công nghệ:** Firestore, Java/XML/ViewBinding, chuẩn hóa plate ở domain.
- **Tiên quyết:** IMP-03/04, schema và wireframe xe duyệt; chứng cứ tối thiểu còn cần chốt.
- **Bước:** chuẩn hóa plate có version → form thêm xe → ghi `vehicles`/request `PENDING` → hiện trạng thái/xung đột không lộ người khác.
- **Chấp nhận:** ≥1 xe để hoàn tất onboarding, thêm xe tiếp theo được; S không tự ghi `VERIFIED` hay đọc request của người khác.
- **Test:** VEH-01/03/06, T-01, FB-01/02.
- **Rủi ro:** plate format biến thể và dữ liệu tự khai sai; không dùng plate làm chứng cứ sở hữu.
- **Commit đề xuất:** `feat(vehicle): add registration and verification requests`.

### IMP-06 — Duyệt xe thủ công và claim biển số nguyên tử

- **Mục tiêu:** ADMIN/nhân sự được cấp quyền duyệt/từ chối; không có hai link VERIFIED active cùng plate.
- **File dự kiến:** `ui/admin/VehicleVerificationFragment.java`, layout duyệt, `backend/<runtime-duyet>/vehicleVerification/*`, `firestore.rules`, `storage.rules`, Rules/integration test.
- **Công nghệ:** Firestore transaction, Admin SDK, Storage chứng cứ nếu cần.
- **Tiên quyết:** IMP-04/05; chốt backend đặc quyền, quy trình chứng cứ/chuyển nhượng và quyền duyệt.
- **Bước:** đọc request/plateKey → transaction claim/link/request/audit → trả xung đột có lý do → đóng link cũ khi chuyển nhượng → cập nhật UI trạng thái.
- **Chấp nhận:** chỉ một claim active, request kia không VERIFIED; hồ sơ cũ không tự cấp chủ mới; audit và quyền duyệt đúng.
- **Test:** FB-05/06, T-01/02, concurrent approval/retry.
- **Rủi ro:** contention, HMAC key/PII trong path, transaction retry, bằng chứng nhạy cảm.
- **Commit đề xuất:** `feat(vehicle): add manual verification with atomic plate claims`.

### IMP-07 — CameraX và ảnh chính có thời điểm chụp

- **Mục tiêu:** chụp ảnh hiện trường, chọn ảnh chính và giữ `capturedAt` đáng tin cho từng ảnh.
- **File dự kiến:** `ui/guard/CameraFragment.java`, `camera/CameraCaptureManager.java`, layout camera, `AndroidManifest.xml`, `strings.xml`, UI test.
- **Công nghệ:** CameraX, Android Runtime Permissions, Java/XML/ViewBinding.
- **Tiên quyết:** IMP-01/04, wireframe camera duyệt, quyền camera và nguồn timestamp được kiểm thử.
- **Bước:** xin camera foreground → bind lifecycle → chụp ảnh chính/ảnh phụ → gắn ID/time riêng → xử lý chụp lại/đổi ảnh chính.
- **Chấp nhận:** ảnh phụ không ghi đè ảnh chính/time; từ chối quyền có đường phục hồi; tài nguyên được đóng.
- **Test:** GUA-01/08, GPS-08/09, camera rotation/lifecycle.
- **Rủi ro:** callback lưu file chậm không phải thời điểm phơi sáng, memory leak ảnh lớn.
- **Commit đề xuất:** `feat(camera): capture primary evidence with timestamps`.

### IMP-08 — Vị trí foreground gắn ảnh chính

- **Mục tiêu:** lấy/kiểm fix tại thời điểm chụp, cảnh báo và fallback theo DEC-008A.
- **File dự kiến:** `location/CaptureLocationProvider.java`, `ui/guard/ViolationFormFragment.java`, layout khối vị trí, `AndroidManifest.xml`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, test.
- **Công nghệ:** Fused Location Provider + SettingsClient **nếu được duyệt**, runtime foreground permission.
- **Tiên quyết:** IMP-07, DEC-008A chốt ngưỡng/timeout/manual, dependency được duyệt; không xin background location.
- **Bước:** kiểm permission/settings → request current fix → kiểm thời điểm/sai số với `capturedAt` → gắn snapshot ID ảnh → UI thử lại/chụp lại/manual đã duyệt.
- **Chấp nhận:** không dùng fix cũ/lúc lưu, ảnh phụ không ghi đè, tọa độ thủ công có nguồn/trạng thái riêng.
- **Test:** GPS-01..10, permission approximate/deny, thiết bị thật ngoài trời/trong nhà.
- **Rủi ro:** Fused có cache, độ chính xác không ổn định, timestamp thiết bị bị đổi.
- **Commit đề xuất:** `feat(location): attach validated capture location to evidence`.

### IMP-09 — Đọc metadata và tích hợp detector YOLO11n

- **Mục tiêu:** xác định input/output tensor thực tế và phát hiện box/crop đúng trên ảnh chính.
- **File dự kiến:** `ai/PlateDetector.java`, `ai/ModelMetadataInspector.java` hoặc tài liệu kiểm model, `ai/PlatePreprocessor.java`, `ai/PlatePostprocessor.java`, test ảnh vàng.
- **Công nghệ:** TensorFlow Lite dependency hiện có, Java, xử lý ảnh.
- **Tiên quyết:** IMP-07, model asset/license/tensor được kiểm; không giả định shape 640.
- **Bước:** ghi metadata → tiền xử lý đúng dtype/scale → chạy Interpreter ngoài UI → decode/NMS → crop/đảo tọa độ → đo tài nguyên.
- **Chấp nhận:** box/crop trùng nhãn test trong sai số đã duyệt, lỗi không có/nhiều box có đường thao tác, không leak Interpreter.
- **Test:** R2.1, T-04, bộ ảnh vàng/benchmark thiết bị.
- **Rủi ro:** model thiếu metadata/op, hậu xử lý sai, hiệu năng thấp.
- **Commit đề xuất:** `feat(ai): add verified plate detection pipeline`.

### IMP-10 — Thử Google ML Kit OCR và sửa biển số

- **Mục tiêu:** đọc crop biển số xe máy Việt Nam và cho guard xác nhận/chỉnh ký tự.
- **File dự kiến:** `ai/PlateTextRecognizer.java`, `domain/PlateNormalizer.java`, `ui/guard/PlateReviewFragment.java`, layout review, `libs.versions.toml`, `app/build.gradle.kts`, test/dữ liệu thử ẩn danh.
- **Công nghệ:** ML Kit Text Recognition v2 Latin (bundled/unbundled **chờ chọn**), Java/XML.
- **Tiên quyết:** IMP-09, quyết định biến thể ML Kit và bộ ảnh pilot có quyền sử dụng.
- **Bước:** chạy OCR trên crop → giữ rawText/thứ tự dòng → chuẩn hóa không tự đổi ký tự mơ hồ → hiển thị crop và field sửa → đo metric biển một/hai hàng.
- **Chấp nhận:** guard sửa được mọi ký tự, không tự kết luận vi phạm, báo riêng full-plate accuracy hai hàng và tỷ lệ sửa.
- **Test:** T-03/04, tập hai hàng ngày/đêm/mờ/góc; latency/app size.
- **Rủi ro:** OCR đảo thứ tự hai dòng, nhầm `0/O`, model tải chậm nếu unbundled.
- **Commit đề xuất:** `feat(ocr): add ML Kit plate text review`.

### IMP-11 — Lưu hồ sơ/ảnh với xác nhận server

- **Mục tiêu:** lưu bằng chứng Storage và hồ sơ Firestore nhất quán, idempotent, online-first.
- **File dự kiến:** `data/repository/ViolationRepository.java`, `data/datasource/FirebaseViolationDataSource.java`, `ui/guard/ViolationReviewFragment.java`, `firestore.rules`, `storage.rules`, backend finalize nếu cần, test.
- **Công nghệ:** Cloud Firestore, Firebase Cloud Storage, transaction/backend đáng tin.
- **Tiên quyết:** IMP-08/09/10, schema/rules và policy ảnh/vị trí được duyệt.
- **Bước:** tạo idempotency key → upload ảnh riêng → ghi metadata/snapshot → finalize hồ sơ khi đủ → chỉ báo thành công khi server xác nhận → dọn upload mồ côi.
- **Chấp nhận:** offline/cache không được báo thành công; retry không nhân đôi; `primaryEvidenceId`/GPS khớp ảnh chính; ảnh có quyền đọc đúng.
- **Test:** GUA-03/04/05/06/08, FB-07..09, T-06.
- **Rủi ro:** Storage và Firestore không cùng transaction, lỗi giữa hai bước, chi phí ảnh.
- **Commit đề xuất:** `feat(violation): persist reviewed evidence with server confirmation`.

### IMP-12 — Lịch sử bảo vệ và màn hình quản trị hồ sơ

- **Mục tiêu:** G xem hồ sơ trong scope; A rà/xử lý hồ sơ với audit.
- **File dự kiến:** `ui/guard/ViolationHistoryFragment.java`, `ui/admin/ViolationManagementFragment.java`, layout list/detail, `firestore.rules`, backend chuyển trạng thái/test.
- **Công nghệ:** Firestore query/index, Navigation/ViewBinding, nguồn đáng tin cho trạng thái.
- **Tiên quyết:** IMP-04/11, wireframe ADMIN/G duyệt và chính sách trạng thái.
- **Bước:** query phân trang theo scope → chi tiết/ảnh quyền riêng → chuyển trạng thái có lý do → ghi audit.
- **Chấp nhận:** G ngoài scope bị chặn; A thiếu grant không xem/sửa; bản gốc không bị ghi đè.
- **Test:** FB-03/04/08, T-07/08, offline/empty/error.
- **Rủi ro:** Rules không lọc hộ query, index thiếu, role token cũ.
- **Commit đề xuất:** `feat(violation): add scoped guard and admin history`.

### IMP-13 — Tra cứu sinh viên theo liên kết đã duyệt

- **Mục tiêu:** S chỉ xem hồ sơ/ảnh liên quan xe VERIFIED và khoảng hiệu lực.
- **File dự kiến:** `ui/student/ViolationListFragment.java`, `ui/student/ViolationDetailFragment.java`, backend projection `userViolationViews` nếu duyệt, `firestore.rules`, `storage.rules`, test.
- **Công nghệ:** Firestore projection hoặc backend query tin cậy, Storage Rules, Java/XML.
- **Tiên quyết:** IMP-06/11, chốt chính sách xem hồ sơ quá khứ/thu hồi quyền và projection.
- **Bước:** xác lập quyền theo link/time → tạo/thu projection → query S an toàn → kiểm ảnh chi tiết → UI trạng thái.
- **Chấp nhận:** chủ mới không tự xem hồ sơ cũ; S-A không xem S-B bằng list/ID/URL; cache không lộ sau thu quyền theo policy.
- **Test:** T-02/08, FB-01/06/07/10.
- **Rủi ro:** projection lệch khi chuyển nhượng, Rules không đủ cho join/time.
- **Commit đề xuất:** `feat(student): show authorized violation history`.

### IMP-14 — Danh mục, thống kê và audit ADMIN

- **Mục tiêu:** hoàn thiện chức năng quản trị còn lại theo grant riêng, ưu tiên dữ liệu tổng hợp.
- **File dự kiến:** `ui/admin/ViolationTypeFragment.java`, `ui/admin/StatisticsFragment.java`, `ui/admin/AuditLogFragment.java`, layout tương ứng, `firestore.rules`, backend báo cáo/test.
- **Công nghệ:** Firestore query/index, backend tổng hợp khi cần, Java/XML.
- **Tiên quyết:** IMP-04/06/11/12, chính sách export/retention và wireframe duyệt.
- **Bước:** version danh mục → query tổng hợp → list audit chỉ quyền riêng → giới hạn export và ghi audit.
- **Chấp nhận:** không mặc định xuất PII; sửa danh mục không đổi nghĩa hồ sơ cũ; audit không sửa từ client.
- **Test:** FB-04/08, danh mục version, dữ liệu lớn/pagination.
- **Rủi ro:** chi phí query và lộ dữ liệu trong báo cáo.
- **Commit đề xuất:** `feat(admin): add catalog statistics and audit views`.

### IMP-15 — Kiểm thử tích hợp, bảo mật và pilot thực địa

- **Mục tiêu:** xác nhận ứng dụng hoạt động trên thiết bị thật và quyền dữ liệu trước đề xuất phát hành.
- **File dự kiến:** test Java/androidTest, `tests/firebase-rules/*`, `docs/11-TESTING-STRATEGY.md`, `docs/13-CURRENT-PROJECT-STATUS.md`, tài liệu vận hành; không tự sửa nghiệp vụ ngoài lỗi được giao.
- **Công nghệ:** Firebase Emulator, Android instrumentation, camera/ML Kit/GPS benchmark.
- **Tiên quyết:** IMP-01..14 theo phạm vi phát hành, chính sách dữ liệu và thiết bị pilot.
- **Bước:** chạy test Rules/Storage → test field camera/GPS/OCR hai hàng → đo hiệu năng/cost → rà backup/secret → tổng hợp lỗi P0 và báo cáo.
- **Chấp nhận:** không còn lỗi P0, có metric và giới hạn mẫu, quyết định phát hành riêng của người dùng.
- **Test:** toàn bộ FB/GPS/T theo [11](11-TESTING-STRATEGY.md), kiểm thử thực địa.
- **Rủi ro:** dữ liệu pilot nhạy cảm, thiết bị không đại diện, chi phí Firebase; không tự publish.
- **Commit đề xuất:** `test(pilot): add end-to-end security and field validation`.
