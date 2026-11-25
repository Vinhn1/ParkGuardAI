# 18 — Kế hoạch chụp và lưu ảnh vi phạm

> **Cập nhật 2026-10-05:** kế hoạch lịch sử bên dưới được thay ở phần kho ảnh bởi DEC-024 Cloudinary unsigned; mã CameraX/GPS/crop/upload/Firestore đã được viết trong working tree, chưa build/thử role bảo vệ trên thiết bị thật. Bản nháp chỉ xóa thủ công theo DEC-006A. Xem [hiện trạng](13-CURRENT-PROJECT-STATUS.md) và [triển khai bảo vệ](19-GUARD-ROLE-IMPLEMENTATION.md).

> Ngày 2026-10-05. Đây là **kế hoạch**, chưa có CameraX, quyền camera/vị trí, file ảnh thật, Firebase Storage hay hồ sơ được lưu. Giao diện xem trước cũ đã được gỡ. Các lựa chọn còn mở không được coi là quyết định triển khai.

## Mục tiêu và ranh giới

Bảo vệ đã được cấp quyền trong phạm vi được giao chụp **ảnh chính** của tình huống, ghi thời điểm chụp và yêu cầu vị trí foreground cho chính ảnh đó. Ảnh được giữ ở vùng riêng của ứng dụng khi đang là bản nháp; việc báo **đã gửi hồ sơ** chỉ xảy ra sau khi ảnh và metadata được xác nhận ở máy chủ. Ảnh bổ sung có metadata riêng, không ghi đè vị trí ảnh chính. AI/OCR chỉ gợi ý; bảo vệ rà và xác nhận nội dung.

Không dùng biển số để tự tìm chủ xe, không tự kết luận vi phạm, không dùng fix cũ hoặc vị trí lúc bấm lưu thay cho vị trí của ảnh chính. Không đặt ảnh ở thư viện công khai nếu chưa có nhu cầu/chính sách xuất ảnh.

## Wireframe cho bước triển khai tiếp theo

```text
Bảo vệ / Chụp ảnh
┌───────────────────────────────────────┐
│ Khung xem camera                      │
│ Hướng dẫn: toàn xe + biển số          │
└───────────────────────────────────────┘
[Chụp ảnh chính]
  ↓
Ảnh chính [xem lớn] [chụp lại]
Thời điểm chụp · trạng thái lưu bản nháp
Vị trí của ảnh chính: nguồn · lúc đo · sai số · trạng thái
[Thử lấy lại vị trí] / [quy trình thủ công nếu được duyệt]
  ↓
Biển số gợi ý [sửa tay] · loại vi phạm · ảnh phụ
  ↓
Rà soát toàn bộ [Lưu bản nháp] [Gửi hồ sơ]
  ↓
Đang gửi → Đã lưu trên máy chủ / Lỗi và thử lại
```

Hiện chưa có màn hình bảo vệ thật. Khi hiện thực cần màn hình rà soát và trạng thái lỗi riêng, không ghép mọi thao tác vào một nút.

## Phương án lưu ảnh và quyết định cần chốt

| Phương án | Lợi ích | Rủi ro/chi phí | Điều kiện nghiệm thu |
|---|---|---|---|
| **A. Bản nháp chỉ trên thiết bị** bằng bộ nhớ nội bộ riêng của app | Xây và thử CameraX/chụp lại trước; không cần bucket | Mất khi gỡ app/hỏng máy; không chia sẻ giữa thiết bị; chưa phải hồ sơ chính thức | Ảnh mở lại được trong app, không hiện ở thư viện công khai, xóa bản nháp đúng chính sách, nhãn luôn là “Trên thiết bị” |
| **B. A rồi đồng bộ Cloud Storage + Firestore** theo ADR-001 | Hồ sơ online-first, ảnh và metadata được quản lý theo quyền | Cloud Storage for Firebase hiện yêu cầu **Blaze**; phí tùy dung lượng, truyền tải và vùng; cần Rules, backend và dọn upload mồ côi | Chỉ báo thành công sau xác nhận server; truy cập ảnh/hồ sơ trái quyền bị từ chối; retry không tạo trùng |
| **C. Dịch vụ lưu ảnh khác** | Có thể phù hợp chính sách hạ tầng riêng | Thay ADR-001, thêm vận hành/bảo mật/tích hợp | Cần quyết định kiến trúc mới và kiểm thử quyền tương đương |

Tài liệu Firebase hiện ghi **Cloud Storage for Firebase cần gói Blaze**, kể cả bucket mặc định; Spark có thể trả 402/403 khi truy cập bucket. Blaze có hạn mức miễn phí nhưng là gói trả theo sử dụng. Vì DEC-011 vẫn mở, không thêm SDK Storage, tạo bucket hoặc gửi ảnh thật trong giai đoạn UI. Cần người quản trị dự án chốt gói, vùng, ngân sách, cảnh báo chi phí và quyền quản lý trước phương án B. Xem [Firebase Storage billing FAQ](https://firebase.google.com/docs/storage/faqs-storage-changes-announced-sept-2024), [Firebase pricing](https://firebase.google.com/pricing).

## Các chặng triển khai đề xuất

| Chặng | Việc xây dựng | Kết quả kiểm tra |
|---|---|---|
| 0. Chốt điều kiện | DEC-008A: sai số tối đa, tuổi fix, độ lệch với ảnh, timeout, precise/approximate, nguồn `capturedAt`, xử lý thủ công; DEC-006: hạn giữ/xóa; DEC-010/011: backend, vùng và chi phí; quyền bảo vệ theo phạm vi | Quy tắc vị trí, lưu giữ, phân quyền và chi phí có tiêu chí được duyệt |
| 1. Chụp và bản nháp cục bộ | Thêm CameraX `Preview`/`ImageCapture`, xin `CAMERA` khi mở màn hình, lưu JPEG bằng `OutputFileOptions` vào thư mục riêng của app, tạo ID ảnh ngẫu nhiên và metadata; kiểm kích thước/MIME/hash; lưu bản nháp riêng khỏi ảnh công khai | Chụp, xem, chụp lại và mở lại bản nháp sau khởi động app; lỗi quyền, hết dung lượng, camera bận không làm báo lưu thành công |
| 2. Vị trí và rà soát | Yêu cầu vị trí foreground gắn sự kiện ảnh chính; lưu thời điểm đo, sai số, nguồn và trạng thái; so ngưỡng đã duyệt bằng đồng hồ đơn điệu cùng phiên; hiển thị lý do fix lỗi, thử lại/chụp lại và thủ công nếu được duyệt | Ảnh A không lấy vị trí từ ảnh B hoặc lúc lưu; fix lỗi không thành `AUTO_ACCEPTED`; đổi ảnh chính buộc rà lại |
| 3. Hồ sơ và đồng bộ | Khi B được duyệt: Storage SDK + Firestore, Rules, backend tin cậy cho grant/audit/finalize; upload ảnh gốc không ghi đè, metadata tham chiếu `primaryEvidenceId`, idempotency key và trạng thái upload | Chỉ hồ sơ/ảnh đúng quyền mới đọc được; upload lỗi giữ nhãn bản nháp; server xác nhận mới hiện “Đã lưu”; retry không tạo nhiều hồ sơ |
| 4. AI/OCR và hoàn thiện | Đo tensor model trước; nhận diện vùng biển số và OCR tách bước; người dùng sửa kết quả, thêm ảnh phụ, xử lý ảnh lỗi và dọn bản nháp theo chính sách | Không tự gán chủ xe/vi phạm; ảnh gốc còn nguyên; crop và ảnh phụ không thay vị trí ảnh chính |

**Phân lớp khi triển khai:** Fragment chỉ hiển thị trạng thái và xử lý thao tác; ViewModel quản lý trạng thái màn hình; thành phần camera điều khiển CameraX; thành phần vị trí trả snapshot; repository quản lý bản nháp và đồng bộ; Rules/backend kiểm quyền khi truy cập dữ liệu thật. Chỉ tạo lớp khi chặng tương ứng được xây.

## Trạng thái cần thể hiện trong UI

`Chưa chụp` → `Đang chụp` → `Ảnh chính trên thiết bị` → `Đang kiểm vị trí` → `Cần chụp lại / Cần xử lý vị trí / Sẵn sàng rà soát` → `Đang gửi` → `Đã lưu trên máy chủ` hoặc `Gửi thất bại`. UI không được biến “file đã ghi trên thiết bị” thành “hồ sơ đã lưu”. Lỗi quyền, tắt định vị, fix sai số lớn/cũ, hết thời gian, thiếu mạng, upload một phần, quyền bị thu và đổi ảnh chính cần đường xử lý riêng.

## Bảo mật và kiểm thử tối thiểu

- Ảnh gốc và bản nháp ở vùng riêng của ứng dụng; kiểm backup và xóa theo DEC-006, không log đường dẫn chứa thông tin nhận dạng/biển số/tọa độ, không đưa tọa độ vào tên file.
- Storage Rules kiểm UID/grant/phạm vi, path, MIME và kích thước; Firestore Rules/backend kiểm quan hệ ảnh–hồ sơ, ảnh chính bất biến và trạng thái. Thử đọc file trực tiếp bằng tài khoản khác, guard ngoài phạm vi và người đã bị thu quyền.
- Thử camera trên thiết bị thật với xoay màn hình, app vào nền, thiếu dung lượng, chụp nhanh liên tiếp, quyền bị từ chối, chỉ cấp vị trí gần đúng, fix cũ, không có mạng, retry upload và ảnh phụ.
- Kiểm ảnh mở lại được, đúng chiều, giữ nguyên byte gốc, hash khớp và thời điểm/vị trí đi cùng đúng ID ảnh. Theo yêu cầu người dùng, không chạy Firebase Emulator; kiểm thử quyền trên dự án và tài khoản thử riêng bằng thiết bị thật, gồm cả đường dẫn ảnh trực tiếp.

Nguồn kỹ thuật: [CameraX ImageCapture](https://developer.android.com/media/camera/camerax/take-photo), [Android app-specific storage](https://developer.android.com/training/data-storage/app-specific), [Firebase Storage upload](https://firebase.google.com/docs/storage/android/upload-files), [Firebase Storage Rules](https://firebase.google.com/docs/storage/security). Kế hoạch này dựa trên quy tắc của [yêu cầu sản phẩm](01-PRODUCT-REQUIREMENTS.md), [UI/UX](07-UI-UX-GUIDELINES.md) và [Decision Log](15-DECISION-LOG.md).
