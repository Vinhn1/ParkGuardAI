# 19 — Thiết kế và triển khai vai trò bảo vệ

## Cập nhật triển khai 2026-10-05

**Chạy thử tài khoản bảo vệ:** người dùng đã tạo Auth account và xác minh email. Người dùng xác nhận `accessGrants/{UID}` và/hoặc Rules chưa hoàn tất; lỗi “chưa kiểm tra được quyền” là lỗi đọc grant, không phải bằng chứng mất mạng. Để vào role bảo vệ cần **cả hai**: Publish toàn bộ `firestore.rules` trên đúng Firebase project và tạo document grant theo đúng UID từ Authentication. Sau đó đăng xuất/đăng nhập lại hoặc bấm thử lại; bản mã mới làm mới ID token và hiện thông báo riêng khi Firestore từ chối đọc quyền. Không tạo grant từ Android client.

DEC-022, DEC-008A, DEC-024, DEC-025/026, DEC-027 và DEC-006A đã được duyệt. Working tree hiện có UI bảo vệ năm tab, CameraX chụp JPEG, vị trí gắn lần bấm chụp theo ngưỡng pilot, bản nháp riêng của app, crop thủ công, Cloudinary unsigned upload ảnh gốc/crop, Firestore hồ sơ `PENDING` và metadata ảnh, thông tin hồ sơ trong app. Chức năng xuất PDF đã được gỡ theo DEC-027. Giao diện dùng cấu trúc ảnh mẫu và palette pilot riêng; build/lint sau lần làm mới UI đã đạt, nhưng thử trên thiết bị thật cho phần bảo vệ vẫn `UNKNOWN`. Các đoạn “chưa có” trong phần thiết kế lịch sử bên dưới mô tả thời điểm viết wireframe, không phải hiện trạng mới nhất.

**Sửa luồng GPS ngày 2026-10-06:** nguồn vị trí tiếp tục nhận các fix trong tối đa 15 giây, bỏ qua fix chưa đạt ngưỡng thay vì kết thúc ngay ở fix đầu. Nếu hết cửa sổ chênh 5 giây so với lần bấm chụp, màn camera ẩn thao tác lấy lại vị trí cho ảnh đó và yêu cầu chụp lại; nút tiếp tục chỉ mở khi fix hợp lệ đã được lưu cùng bản nháp. `git diff --check`, phân tích XML và build/lint đã đạt; hành vi trên thiết bị thật vẫn `UNKNOWN`.

Tài khoản bảo vệ thử chưa được tạo. Người quản trị Firebase Console cần: tạo email/mật khẩu trong Authentication, xác minh email, lấy UID; tạo `accessGrants/{UID}` với `role = SECURITY_GUARD` (string), `active = true` (boolean), `guardScopes = ["PILOT_GATE_A"]` (array string, thay bằng mã phạm vi thực), `schemaVersion = 1` (number); triển khai `firestore.rules` và `firestore.indexes.json` vào đúng project. Không ghi mật khẩu vào app/Git. Android chỉ đọc grant từ server, không tự cấp quyền. UID/email/phạm vi thật chưa có trong repository.

Cloudinary dùng `cloud_name = ds1g5ryn1`, unsigned preset `parkguard_violation_upload`, cấu hình `secure=true`, không có API Secret. Firestore chỉ lưu metadata `secureUrl`, `publicId`, `uploadedAt`, `imageType`, `isPrimary`, không lưu binary/Base64. Cần kiểm preset trong Console là unsigned, chỉ image, có giới hạn kích thước/quota. `secure_url` bảo đảm HTTPS nhưng URL CDN vẫn truy cập được bởi ai có URL; Firestore Rules không che ảnh Cloudinary. Trước dữ liệu nhạy cảm cần backend ký upload/URL và chính sách truy cập. Nếu Cloudinary thành công nhưng Firestore lỗi, ảnh có thể mồ côi vì app unsigned không được xóa đối tượng.

Bản nháp ở `noBackupFilesDir`, chỉ xóa bằng thao tác xóa hoặc chụp lại của người dùng; không tự dọn theo thời gian. Ảnh đã gửi vẫn có bản sao cục bộ và nút xóa riêng; xóa bản sao cục bộ không xóa Cloudinary/Firestore. Chi tiết trong app hiển thị hồ sơ `PENDING`, chưa tự gửi tới sinh viên hoặc suy chủ xe từ biển số. Lịch sử hiện tối đa 50 hồ sơ của guard đúng scope, chưa có phân trang/duyệt vi phạm. AI/OCR chạy ở **Quét nhanh riêng**; theo DEC-028, biển số hợp lệ có thể chuyển làm gợi ý để rà soát ảnh chính mới, nhưng ảnh quét không thành bằng chứng và model không chọn hành vi vi phạm.

Kiểm trên **thiết bị thật**: đăng nhập/thu hồi grant; từ chối và cấp camera/vị trí precise; GPS lỗi/ngưỡng đạt; chụp lại, crop, mở lại/xóa bản nháp; ngắt mạng upload và retry; so Firestore `PENDING`/metadata sau callback; thử tài khoản khác đọc hồ sơ; mở ảnh và chi tiết trong app. Không dùng emulator theo yêu cầu. [Hiện trạng](13-CURRENT-PROJECT-STATUS.md) ghi giới hạn build/runtime.

## Wireframe và phương án lịch sử trước triển khai

> Cập nhật 2026-10-05. Người dùng yêu cầu ưu tiên hoàn thiện vai trò bảo vệ và dùng một tài khoản cố định để thử trên thiết bị thật. Wireframe được duyệt theo DEC-022; ngưỡng GPS pilot được duyệt theo DEC-008A. Chưa có bằng chứng các chức năng camera/GPS/ảnh đã chạy.

## Hiện trạng và đích nghiệm thu

Đã có Firebase Auth email/mật khẩu, luồng sinh viên và Firestore cho hồ sơ/xe `PENDING`; người dùng báo đã thử thành công đăng ký trên thiết bị thật. Chưa có tài khoản bảo vệ được cấp quyền, điều hướng theo role, CameraX, GPS, Storage, hồ sơ vi phạm, PDF/thông báo hoặc Rules cho các tài nguyên đó. Không dùng emulator theo yêu cầu người dùng. Bản đầu vẫn online-first; ảnh trong bản nháp phải có nhãn rõ là ở thiết bị cho đến khi máy chủ xác nhận.

Luồng mục tiêu: đăng nhập bằng tài khoản thử cố định → kiểm tra quyền bảo vệ tại nguồn dữ liệu → Trang chủ → Chụp ảnh chính + thời điểm + vị trí cùng sự kiện → Xem ảnh gốc và crop biển số → Nhập/rà thông tin → Gửi hồ sơ → Lịch sử/chi tiết → Xuất thông báo theo định dạng được duyệt. Hồ sơ cá nhân bảo vệ hiển thị tài khoản, phạm vi và đăng xuất. AI chỉ gợi ý vùng biển số; không tự gán người/xe hoặc kết luận hành vi.

## Nghiên cứu mẫu giao diện và lựa chọn đề xuất

| Hướng mẫu | Giá trị cho tác vụ bảo vệ | Rủi ro/chi phí | Đánh giá |
|---|---|---|---|
| **Material 3 cho Android Views**: navigation bar, card, text field, bottom sheet, danh sách; dùng thư viện Material đang có | Đồng bộ với form đăng nhập; 3–5 đích điều hướng, nhãn rõ; dễ làm Java/XML, hỗ trợ accessibility | Cần tự thiết kế bố cục và token; không sao chép được layout Compose | **Đề xuất** làm nền; không nâng dependency chỉ để lấy style mới |
| Material 3 Expressive cho Views | Nhiều biến thể màu/chuyển động, cảm giác mới hơn | Phải kiểm phiên bản component và theme, tăng công kiểm trên thiết bị; animation có thể gây xao nhãng khi chụp | Chỉ tham khảo cách dùng hình khối và active indicator; chưa đổi theme/phiên bản |
| UI kit cộng đồng/Figma | Nhiều màn hình đẹp để lấy ý tưởng bố cục | Quyền sử dụng asset và chất lượng accessibility không thống nhất; nhiều kit dùng Compose/iOS/web | Chỉ dùng làm tham chiếu thị giác sau khi kiểm license, không chép ảnh/icon/logo |

Nguồn: [Material navigation bar cho Views](https://github.com/material-components/material-components-android/blob/master/docs/components/BottomNavigation.md), [Material components cho Views](https://github.com/material-components/material-components-android/blob/master/docs/getting-started.md), [Android layout/navigation patterns](https://developer.android.com/design/ui/mobile/guides/layout-and-content/layout-and-nav-patterns), [Motion cho Views](https://developer.android.com/develop/ui/views/animations/overview). Lựa chọn trên là **đề xuất thiết kế**, không phải tuyên bố template bên ngoài đã được cấp phép.

### Phong cách đề xuất

- Giao diện tác nghiệp sáng, nền `surface` trung tính, một màu nhấn hiện có của app; ảnh và dữ liệu quan trọng nổi bật hơn trang trí. Trạng thái luôn có chữ và biểu tượng, không chỉ màu. Màu/logo trường chờ DEC-009.
- Thanh dưới 3 đích: **Tổng quan · Hồ sơ · Cá nhân**. Nút **Chụp vi phạm** lớn ở Tổng quan và hành động nổi dễ bấm; màn hình camera ẩn thanh dưới để dành chỗ cho khung hình. Tên “Hồ sơ” ở đây là lịch sử vi phạm, tránh nhầm với “Cá nhân”.
- Card nhiệm vụ trong ca, số bản nháp trên thiết bị và hồ sơ đã xác nhận máy chủ tách nhãn. Dòng lịch sử có ảnh thu nhỏ, loại, giờ, trạng thái; chi tiết phân nhóm bằng chứng, vị trí, diễn biến.
- Chuyển cảnh 180–250 ms giữa trang tổng quan/chi tiết, phản hồi chụp ngắn và chuyển từ ảnh camera sang rà soát; tôn trọng `ValueAnimator.areAnimatorsEnabled()` và không chặn thao tác bởi animation. Không animate tọa độ hay trạng thái gửi theo cách gây hiểu lầm.
- Chữ Việt trong `strings.xml`, kích thước chạm tối thiểu 48 dp, hỗ trợ font lớn/TalkBack, trạng thái loading/empty/error/offline, chế độ sáng/tối.

## Wireframe cần duyệt trước khi viết XML

```text
Đăng nhập (màn hiện có)
  → kiểm tra email xác minh và accessGrants/{uid} từ server
  → có SECURITY_GUARD + active + phạm vi → Tổng quan
  → thiếu quyền/thu hồi → màn báo quyền và đăng xuất

TỔNG QUAN                               [Cá nhân]
Xin chào, Bảo vệ                         Ca/phạm vi: …
[ Chụp vi phạm ]                         tác vụ chính
[Bản nháp trên thiết bị: n] [Đã gửi: n]  nhãn nguồn dữ liệu
Hồ sơ gần đây                            xem tất cả →
Thanh dưới: Tổng quan | Hồ sơ | Cá nhân

CHỤP ẢNH (toàn màn hình)
[Trở về] [Đèn flash]
┌───────────────────────────────────────┐
│ Khung camera; gợi ý chụp cả xe/biển số│
└───────────────────────────────────────┘
Quyền camera/vị trí và chất lượng vị trí: …
              [ Nút chụp ảnh chính ]
Sau chụp: ảnh gốc + giờ chụp + trạng thái fix riêng của ảnh
[Chụp lại] [Tiếp tục]

RÀ SOÁT ẢNH
Ảnh chính (không sửa byte gốc) [Xem lớn]
Crop biển số (ảnh dẫn xuất) [Cắt lại] [Nhập tay khi cần]
Biển số đã rà* · Loại vi phạm* · Ghi chú tùy chọn
Vị trí ảnh chính: tọa độ, sai số, lúc đo, nguồn, trạng thái
Ảnh phụ (nếu có; không thay vị trí ảnh chính)
[Lưu bản nháp] [Rà soát hồ sơ]

RÀ SOÁT HỒ SƠ → GỬI
Ảnh gốc · crop · biển số cuối · loại · thời gian · vị trí
Nhãn: Bản nháp trên thiết bị / Đang gửi / Đã xác nhận máy chủ
[Quay lại sửa] [Gửi hồ sơ]
Nếu upload lỗi: giữ bản nháp, nêu bước thất bại, thử lại cùng ID

HỒ SƠ                           [Lọc trạng thái]
Danh sách theo thời gian/phạm vi; chỉ hồ sơ được phép
→ Chi tiết: ảnh gốc, crop, loại, biển số, vị trí, lịch sử
→ [Xuất thông báo] theo định dạng và người nhận được duyệt

CÁ NHÂN
Email · trạng thái xác minh · vai trò · phạm vi được cấp
Quyền camera/vị trí hiện tại · thông tin ứng dụng
[Đăng xuất]
```

**Đối chiếu UI/UX:** mỗi bước có một hành động chính; vị trí gắn ảnh chính và có trạng thái riêng; crop giữ ảnh gốc; không dùng màu duy nhất cho trạng thái; camera có đường chụp lại; thao tác gửi có màn rà soát; quyền dữ liệu vẫn do Rules/backend kiểm. Đây là màn hình tác nghiệp thật, không dùng dữ liệu demo.

## Tài khoản thử cố định

Người dùng đã chọn **một tài khoản bảo vệ cố định tạm thời** để thử nghiệm, sau đó thay bằng cấp/duyệt tài khoản từ ADMIN. Cách triển khai an toàn: tạo email/mật khẩu thử trong Firebase Authentication bằng Firebase Console; lưu `accessGrants/{uid}` cho đúng UID qua Console/quyền quản trị, gồm `role = SECURITY_GUARD`, `active = true`, `guardScopes = [mã phạm vi thử]`, thời điểm và người cấp; Firestore Rules chỉ cho chủ tài khoản **đọc** grant của mình, không cho Android ghi. Không điền mật khẩu hoặc service account vào app/repository. UID thực, email và phạm vi đang chờ người dùng cung cấp; chưa tạo được tài khoản chỉ từ mã nguồn. Khi ADMIN cấp quyền về sau, giữ hợp đồng grant và thay thao tác nhập thủ công bằng nghiệp vụ đặc quyền có audit. [Firebase Admin user management](https://firebase.google.com/docs/auth/admin/manage-users), [custom claims](https://firebase.google.com/docs/auth/admin/custom-claims).

## Các quyết định còn cần để hoàn thiện chức năng

| Quyết định | Phương án đề xuất, lợi ích | Rủi ro/chi phí, điều kiện nghiệm thu |
|---|---|---|
| DEC-008A: vị trí ảnh chính | **Đã duyệt cho pilot:** chỉ nhận precise; sai số ngang ≤30 m, tuổi fix ≤10 giây, lệch lúc chụp ≤5 giây, chờ tối đa 15 giây. Nếu lỗi: chụp lại/thử lấy vị trí, chưa cho nhập thủ công. | Ngưỡng có thể khó đạt trong nhà, làm chậm tác vụ; phải đo thực địa rồi duyệt/sửa. Không chấp nhận fix cũ hoặc lấy lúc lưu để thế ảnh. Dữ liệu GPS do thiết bị cung cấp không chứng minh tuyệt đối vị trí. |
| DEC-016: kho ảnh | CameraX lưu bản nháp ở vùng riêng của ứng dụng; khi Cloud Storage Blaze, vùng bucket và ngân sách được duyệt, upload bản gốc/crop và finalize hồ sơ. | Bản nháp có thể mất khi gỡ app; Storage cần Blaze và Rules; phải xử lý upload mồ côi, backup và lưu giữ. [Firebase Storage billing](https://firebase.google.com/docs/storage/faqs-storage-changes-announced-sept-2024). |
| DEC-010/011: quyền & backend | Một grant thử do người quản trị nhập thủ công; Rules mặc định từ chối, kiểm grant/scope cho mỗi tài nguyên. Sau pilot dùng backend đặc quyền cho cấp/thu và audit. | Console thao tác thủ công dễ sai; cần UID/phạm vi, tài khoản quản trị, kiểm quyền chéo và thu hồi. Không tin role trong APK. |
| DEC-006: vòng đời ảnh/GPS | Chọn thời gian giữ bản nháp, ảnh chính/crop, hồ sơ, audit và backup trước khi lưu dữ liệu thật. | Thiếu hạn giữ làm khó dọn và xử lý yêu cầu dữ liệu; tiêu chí: chính sách, thao tác xóa/thu hồi, thông báo cho người dùng. |
| Định dạng thông báo vi phạm | PDF xem trước rồi chia sẻ/xuất theo vai trò được duyệt, hoặc bản ghi trong app, hoặc cả hai. | PDF chứa PII và tọa độ; cần chốt người nhận, trường hiển thị, quyền xuất và audit; không tự gửi tới sinh viên theo biển số. |

Nguồn kỹ thuật: [CameraX ImageCapture](https://developer.android.com/media/camera/camerax/take-photo), [Android app-specific storage](https://developer.android.com/training/data-storage/app-specific), [Fused location](https://developers.google.com/android/reference/com/google/android/gms/location/FusedLocationProviderClient), [Firestore Rules](https://firebase.google.com/docs/firestore/security/get-started), [Storage Rules](https://firebase.google.com/docs/storage/security). Ngưỡng GPS ở bảng đã được duyệt **cho pilot**, cần đo thực địa để xét lại.

## Thứ tự triển khai và thử trên thiết bị thật

1. Duyệt wireframe và chốt UID/phạm vi tài khoản thử. Tạo Auth + grant bằng người quản trị, kiểm tra đọc grant từ server, route role và thu quyền. Sinh viên không vào role bảo vệ.
2. CameraX preview/chụp file riêng của app, ảnh gốc bất biến, chụp lại và xem lớn; đo độ trễ thời điểm chụp trên thiết bị. Xin quyền camera/vị trí khi dùng, hiển thị lý do từ chối.
3. Chốt DEC-008A rồi lấy vị trí foreground theo sự kiện ảnh chính; lưu thời điểm đo, accuracy, nguồn và trạng thái theo `evidenceId`. Kiểm xoay màn hình, app nền, quyền approximate, GPS tắt, fix cũ, nhiều ảnh.
4. Cắt ảnh dẫn xuất từ ảnh gốc bằng vùng người dùng chọn; sau khi đo tensor và duyệt pilot AI/OCR mới thêm gợi ý tự động. Người bảo vệ luôn sửa được biển số, không tự gán chủ xe.
5. Chốt Blaze/bucket/Rules/hạn giữ, rồi đồng bộ bản gốc/crop và metadata, xác nhận từ server, retry cùng ID, lịch sử trong phạm vi. Thử tài khoản sinh viên, guard khác và URL ảnh trực tiếp.
6. Chốt đầu ra thông báo; làm mẫu xem trước/xuất và audit. Kiểm tra trên thiết bị thật, không chạy emulator theo yêu cầu.

Không đánh dấu bất kỳ bước nào là hoàn tất chỉ từ wireframe; trạng thái mã và bằng chứng kiểm tra cập nhật tại [13](13-CURRENT-PROJECT-STATUS.md).
