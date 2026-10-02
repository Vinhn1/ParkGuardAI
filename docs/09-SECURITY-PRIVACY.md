# 09 — Bảo mật và quyền riêng tư

> ADR-001 đã chọn Firebase Authentication, Cloud Firestore và Firebase Cloud Storage; repository đã có Firebase Auth SDK/datasource email nhưng **chưa có** Firebase Project, Firestore/Storage SDK, Security Rules hoặc kho ảnh. [`AndroidManifest.xml`](../app/src/main/AndroidManifest.xml) đang bật backup với [`backup_rules.xml`](../app/src/main/res/xml/backup_rules.xml) và [`data_extraction_rules.xml`](../app/src/main/res/xml/data_extraction_rules.xml) còn là mẫu. Quy định pháp lý cần được người phụ trách pháp chế rà soát trước vận hành.

## Dữ liệu, tác nhân và rủi ro

| Tài sản | Rủi ro chính | Kiểm soát mục tiêu |
|---|---|---|
| Tài khoản/email/số điện thoại/mã sinh viên | Dò tài khoản, chiếm phiên, gán sai người | Xác minh, rate limit, khôi phục an toàn, giới hạn hiển thị, không log |
| Biển số và lịch sử liên kết | Suy diễn lịch sử di chuyển/chủ xe, tranh chấp | Chuẩn hóa, xác minh quyền sử dụng, khoảng thời gian, không tra cứu công khai |
| Ảnh hiện trường/biển số | URL rò rỉ, tải hàng loạt, lộ người xung quanh | Kho riêng, quyền trên từng hồ sơ/file, URL ngắn hạn nếu dùng, watermark/export policy, audit |
| Vị trí/thời gian | Theo dõi cá nhân không cần thiết, gán vị trí cũ/sai cho ảnh, tiết lộ tuyến di chuyển | Lấy foreground tại lúc chụp ảnh chính, kiểm tuổi fix/sai số, gắn ảnh theo ID, phân quyền hiển thị, khu vực thủ công chỉ theo quy trình duyệt, thời hạn giữ |
| Hồ sơ và audit | Sửa/xóa âm thầm, sai trạng thái, retry trùng | Quyền theo tác vụ, sự kiện bất biến, idempotency, bảo vệ bản gốc |
| Bản nháp/cache/backup | Lộ từ thiết bị mất hoặc bản sao lưu | Lưu nội bộ, mã hóa phù hợp, TTL, xóa khi đăng xuất, kiểm quy tắc backup |

Tác nhân cần xét: người chưa đăng nhập, sinh viên khác, bảo vệ ngoài phạm vi, tài khoản quản trị bị chiếm, ứng dụng bị sửa request, người có URL ảnh, thiết bị bị mất, dịch vụ bên thứ ba. Mọi quyền phải kiểm trên dữ liệu thực và phiên xác thực, không chỉ ở UI.

## Xác thực và phân quyền

- Firebase Authentication quản lý đăng ký/đăng nhập/danh tính bằng email/password trên Spark. App và Firestore **không lưu password thuần**. Cần xác minh email, chính sách reset và thu hồi phiên; kiểm tra chống lạm dụng và thông báo không tiết lộ sự tồn tại tài khoản. Phone chỉ là liên hệ tùy chọn chưa xác minh, không dùng đăng nhập/khôi phục hoặc thông báo chứa dữ liệu nhạy cảm; thay số không tự tạo quyền. [Firebase Authentication email/password](https://firebase.google.com/docs/auth/android/password-auth), [quản lý tài khoản](https://firebase.google.com/docs/auth/android/manage-users). Chưa tạo Firebase Project/cấu hình thật.
- Phân quyền theo [ma trận](02-USER-ROLES-PERMISSIONS.md): nguồn tin cậy cấp role/scope; request client không được tự đặt `STUDENT → ADMIN`, `guardId`, `ownerId`, `VERIFIED` hoặc xem file qua ID đoán được. Kiểm cả list và file, trạng thái/hiệu lực liên kết, phạm vi bảo vệ và quyền xem của quản trị. Tránh quyền quản trị bao trùm PII nếu không cần.
- Security Rules cho **Firestore và Storage** phải mặc định từ chối, kiểm owner/scope/trạng thái, chống client sửa role/status, giới hạn cấu trúc dữ liệu và file; **không dùng `allow read, write: if true` khi triển khai**. Test bằng Emulator với phiên ẩn danh, S-A/S-B, G ngoài phạm vi, A thiếu quyền và URL ảnh. [Firebase Rules basics](https://firebase.google.com/docs/rules/basics), [Rules unit tests](https://firebase.google.com/docs/rules/unit-tests). Server SDK có thể bỏ qua Firestore Rules nên chức năng server phải kiểm IAM/quyền riêng; [Firebase field access](https://firebase.google.com/docs/firestore/security/rules-fields) cũng lưu ý Rules không che từng field trong một document, vì vậy tách PII vào document riêng nếu cần.

### Chính sách Firebase theo collection và thao tác

| Tài nguyên | Client được phép | Nguồn đáng tin/kiểm tra bắt buộc |
|---|---|---|
| `users/{uid}` | S đọc/sửa trường profile của `auth.uid`, G/A chỉ dữ liệu tối thiểu theo quyền | Không cho client ghi role, phone/email verified hoặc accountStatus đặc quyền; password không có ở Firestore |
| `accessGrants/{uid}`, custom claims | Không có quyền tự cấp/sửa | Chỉ Admin SDK môi trường đặc quyền ghi; thu quyền xử lý token cũ/refresh và kiểm active/scope |
| `vehicles`, `vehicleVerificationRequests` | S tạo xe/yêu cầu `PENDING` cho chính mình, đọc xe/yêu cầu của mình | ADMIN/nhân sự có quyền duyệt thủ công; client không tự đặt VERIFIED/reviewer/validFrom |
| `vehicleLinks`, `plateClaims` | S chỉ đọc link của mình; không đọc/ghi `plateClaims` | Backend transaction cấp claim một plate active, đóng link cũ khi chuyển nhượng; audit |
| `violations`, evidence metadata | G tạo trong scope, S chỉ đọc hồ sơ đã được liên kết hợp lệ; A theo grant | Không tin `guardUid`, `vehicleId`, `primaryEvidenceId`, locationStatus/approval do client tự đặt; bất biến và chuyển trạng thái do Rules/backend kiểm |
| Storage ảnh | Upload/read theo UID, hồ sơ và loại ảnh; không dùng URL công khai dài hạn | Rules kiểm path, MIME/size, quyền hồ sơ; ảnh gốc không ghi đè, dọn upload mồ côi |
| `auditLogs`, projection sinh viên | Client không ghi audit/projection; S đọc projection của mình | Backend ghi/thu hồi theo link VERIFIED và thời gian; audit append-only theo khả năng dịch vụ |

Custom claims là phương án role thô do [Admin SDK phía server](https://firebase.google.com/docs/auth/admin/custom-claims) đặt, không từ Android; scope chi tiết có thể ở `accessGrants` và được kiểm thêm. Quyền danh sách phải tương thích truy vấn vì [Firestore Rules không lọc kết quả](https://firebase.google.com/docs/firestore/security/rules-query). Các thao tác phức tạp như cấp vai trò, duyệt xe/claim plate, cập nhật projection quyền xem và audit cần backend tin cậy nếu Rules không bảo đảm đủ bất biến; chọn Cloud Functions hay môi trường khác còn mở. Firestore và Storage có Rules riêng, phải test cả hai. Đừng đưa service account JSON vào ứng dụng/Git.

## Dữ liệu tại thiết bị và truyền tải

HTTPS/TLS cho mọi kết nối; không cấu hình cleartext tùy tiện. Token/khóa ký dùng kho an toàn theo nền tảng, không ghi Logcat/Crash report hoặc commit. API key cấu hình phía client cần giới hạn theo ứng dụng/dịch vụ; secret phía server không đóng gói APK. Không để ảnh bằng chứng ở thư mục công khai; nếu cần chia sẻ tạm, dùng cơ chế cấp quyền giới hạn thời gian. Ảnh gốc và crop nên có content hash, MIME/size check và kiểm tra truy cập. Theo [Android data storage](https://developer.android.com/training/data-storage), dữ liệu riêng nên ở app internal storage hoặc nguồn thích hợp theo độ nhạy cảm.

Backup phải được xem xét trước khi lưu bản nháp, token hoặc ảnh: quyết định dữ liệu nào được/không được sao lưu cho Android 11 trở xuống và 12 trở lên, thử restore trên thiết bị khác; [Android backup security](https://developer.android.com/privacy-and-security/risks/backup-best-practices) nêu cách loại dữ liệu nhạy cảm. Không tự tắt toàn bộ backup nếu chưa phân tích nhu cầu khôi phục.

## Quyền riêng tư và vòng đời dữ liệu

Thông báo cho sinh viên/bảo vệ mục đích thu thập từng nhóm dữ liệu, nguồn lưu, ai xem, thời hạn giữ, cách yêu cầu truy cập/sửa/xóa và đầu mối liên hệ. Thu tối thiểu: không yêu cầu CCCD, địa chỉ nhà hoặc thông tin nhạy cảm khi chưa có mục đích. Số điện thoại là liên hệ tùy chọn và chưa xác minh theo DEC-007; mã sinh viên chỉ thu theo quyết định DEC-007B. Yêu cầu mới chỉ cho phép lấy vị trí khi bảo vệ **đang ghi nhận ảnh vi phạm**, không theo dõi liên tục hoặc truy cập vị trí nền. Xin quyền foreground khi cần, giải thích mục đích và xử lý khi người dùng chỉ cấp vị trí gần đúng; nếu fix không đạt ngưỡng đã duyệt, không âm thầm dùng nó. [Android runtime location permissions](https://developer.android.com/develop/sensors-and-location/location/permissions/runtime) nêu người dùng Android 12+ có thể chỉ cấp approximate ngay cả khi ứng dụng xin precise.

Lưu `locationAccuracy`, `locationTimestamp`, `capturedAt`, nguồn/trạng thái và liên kết ảnh chính để kiểm tra, nhưng chỉ hiển thị tọa độ chính xác cho vai trò cần thiết; quyền của sinh viên đối với vị trí chi tiết phải theo [ma trận quyền](02-USER-ROLES-PERMISSIONS.md) và chính sách được duyệt. Không đặt tọa độ trong Logcat, analytics, URL công khai hoặc tên file ảnh. Nếu địa chỉ được suy từ tọa độ qua dịch vụ ngoài, phải đánh giá việc gửi tọa độ cho bên thứ ba trước khi bật. Dữ liệu GPS do thiết bị cung cấp có thể sai hoặc bị giả lập; không diễn đạt nó như bằng chứng tuyệt đối. Fix thủ công không được gắn nhãn là GPS tự động, cần lý do/duyệt/audit. Firestore Rules và backend phải chặn guard tự sửa `locationStatus` thành đã duyệt hoặc thay `primaryEvidenceId` ngoài quy trình.

Thiết lập lịch giữ riêng cho tài khoản, link xe, bản nháp, ảnh, hồ sơ đã xác nhận, audit và backup; có cơ chế xóa/ẩn danh theo chính sách, giữ những dữ liệu phải bảo tồn theo nghĩa vụ được duyệt. Khi yêu cầu xóa tài khoản, xác minh người yêu cầu, chặn truy cập mới, xử lý dữ liệu từng loại và ghi kết quả; không xóa âm thầm bằng chứng hồ sơ đang cần giải quyết. Lưu dấu vết quyền đồng ý và phiên bản thông báo, không coi checkbox là đồng ý cho mọi mục đích sau này.

**Nguồn pháp lý cần rà soát:** [Luật Bảo vệ dữ liệu cá nhân số 91/2025/QH15](https://vanban.chinhphu.vn/?docid=214590&pageid=27160) có hiệu lực 01-01-2026; [Nghị định 13/2023/NĐ-CP](https://vanban.chinhphu.vn/default.aspx?docid=207759&pageid=27160) có hiệu lực 01-07-2023. Tài liệu này không thay thế ý kiến pháp lý; cần xác nhận nghĩa vụ cụ thể, quan hệ giữa văn bản, chuyển dữ liệu ra nước ngoài và hợp đồng với nhà cung cấp trước vận hành.

## Offline, audit và ứng phó sự cố

Phiên bản đầu **online-first**: kiểm kết nối, hiển thị offline và chỉ báo thành công khi Firestore/Storage đã có xác nhận từ server phù hợp; snapshot từ cache hoặc ghi local chưa đồng bộ không là hồ sơ chính thức. [Firestore offline persistence](https://firebase.google.com/docs/firestore/manage-data/enable-offline) có cache Android và đồng bộ local; [transaction](https://firebase.google.com/docs/firestore/manage-data/transactions) không hoàn tất offline. Thiết kế retry idempotent, ảnh mồ côi, và xóa cache nhạy cảm khi đăng xuất/thu quyền. Queue offline nâng cao, mã hóa bản nháp và conflict resolution thuộc giai đoạn sau. Audit tạo/sửa/duyệt/xuất/xóa với actor đáng tin, thời gian, lý do, thay đổi tối thiểu; log kỹ thuật không chứa PII. Xây dựng quy trình thu hồi token, khóa tài khoản, cô lập ảnh, kiểm tra lịch sử và thông báo theo chính sách khi có sự cố. Chi tiết giữ/xóa chờ DEC-006.
