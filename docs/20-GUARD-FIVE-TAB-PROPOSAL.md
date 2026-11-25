# Đề xuất năm tab cho vai trò bảo vệ

> Ngày 2026-10-06. Wireframe năm tab, Quét nhanh chỉ đọc tại chỗ và MapLibre trong app đã được người dùng duyệt theo DEC-025. ML Kit Latin bundled và style demo MapLibre cho pilot đã được duyệt theo DEC-026. Xem [quy tắc UI/UX](07-UI-UX-GUIDELINES.md).

## Hiện trạng trước khi duyệt wireframe

- `GuardHomeFragment` hiện điều khiển ba panel trong một layout: Tổng quan, Hồ sơ và Cá nhân. `guard_bottom_nav.xml` có ba mục; nút chụp vi phạm từ Tổng quan mở `GuardCameraFragment`.
- Sổ vi phạm đã có dữ liệu nền: bản nháp trong `GuardDraftStore`, tối đa 50 hồ sơ do chính bảo vệ tạo trong phạm vi grant từ `ViolationRepository`, và màn chi tiết. Chưa có phân trang, lọc hay tìm kiếm.
- Model `parkguard_yolo11n_best.tflite` đã có trong assets. Đã đọc trực tiếp cấu trúc input `[1,3,640,640]` và output `[1,5,8400]` đều `FLOAT32`; chưa xác minh chuẩn hóa, ý nghĩa output, độ trễ hoặc OCR chạy trong luồng bảo vệ. Cần đo các phần này bằng ảnh mẫu và thiết bị trước khi nối vào UI; model theo đặc tả chỉ phát hiện vùng biển số.
- Hồ sơ vi phạm đã lưu có tọa độ của ảnh chính. Chưa có SDK bản đồ, API key hoặc nhà cung cấp bản đồ được duyệt. Truy vấn hiện tại chỉ trả hồ sơ do guard hiện tại tạo, có scope hợp lệ, tối đa 50 bản ghi.

## Wireframe đề xuất

```text
┌──────────────────────────────────────┐
│ Nội dung tab đang chọn                │
│                                      │
│ Trang chủ: phạm vi, tác vụ chụp ảnh, │
│ số bản nháp, hồ sơ gần đây            │
│                                      │
│ Sổ vi phạm: bản nháp trên máy,        │
│ hồ sơ đã lưu máy chủ, tải lại,        │
│ mở chi tiết hoặc xóa bản nháp         │
│                                      │
│ Quét nhanh: camera → vùng biển số AI │
│ gợi ý → chữ OCR gợi ý → người sửa;    │
│ không tự kết luận vi phạm/chủ xe      │
│                                      │
│ Bản đồ: vị trí các hồ sơ bảo vệ được  │
│ phép xem → chọn điểm → mở chi tiết    │
│                                      │
│ Cá nhân: email, grant/phạm vi,       │
│ đổi mật khẩu, đăng xuất               │
├──────────────────────────────────────┤
│ Trang chủ | Sổ vi phạm | Quét nhanh   │
│ Bản đồ    | Cá nhân                  │
└──────────────────────────────────────┘
```

Thanh dưới dùng năm mục có icon và nhãn tiếng Việt; [BottomNavigationView hỗ trợ tối đa năm mục](https://github.com/material-components/material-components-android/blob/master/docs/components/BottomNavigation.md). Nếu bề ngang nhỏ, chế độ nhãn tự động có thể chỉ hiện nhãn của mục đang chọn, nhưng TalkBack vẫn đọc tên mọi mục. Nút chụp vi phạm hiện tại vẫn nằm ở Trang chủ và tiếp tục dùng luồng CameraX/GPS đã duyệt. Sổ vi phạm kế thừa dữ liệu đang có, không tự đổi quyền truy cập.

## Hai quyết định cần chốt cho tính năng mới

| Vấn đề | Phương án | Lợi ích | Rủi ro/chi phí | Điều kiện nghiệm thu |
|---|---|---|---|---|
| Quét nhanh A (đề xuất) | Chỉ đọc/gợi ý biển số tại chỗ; không lưu hoặc upload ảnh, không tạo hồ sơ | Phân biệt rõ với chụp vi phạm; ít dữ liệu nhạy cảm hơn | Cần đo tensor và chọn bản ML Kit trước khi tích hợp | Camera hiển thị vùng gợi ý, OCR có nhãn “AI gợi ý”, người sửa được, ảnh không lưu khi thoát |
| Quét nhanh B | Quét rồi chuyển thành bản nháp vi phạm | Dùng lại luồng hiện có | Cần gắn ảnh chính với GPS đúng thời điểm quét, tránh biến ảnh quét thành bằng chứng thiếu vị trí | Không tạo hồ sơ nếu thiếu ảnh chính/vị trí đạt DEC-008A; rà soát trước gửi |
| Bản đồ A | Google Maps SDK trong app | Bản đồ nền quen thuộc, marker tương tác | Cần API key, cấu hình dự án/billing, dependency và kiểm quyền API | Chỉ hiện hồ sơ thuộc grant/scope; chọn marker mở đúng chi tiết; lỗi mạng/quota rõ |
| Bản đồ B | MapLibre với nguồn tile được phép dùng | Không buộc Google Maps API key; vẫn có bản đồ trong app | Cần dependency, chọn nguồn tile/điều khoản/giới hạn sử dụng và kiểm tra kích thước app | Như A; attribution và điều khoản nguồn tile đầy đủ |
| Bản đồ C | Danh sách vị trí trong app; nút mở ứng dụng bản đồ ngoài bằng `geo:` Intent | Không thêm SDK/key; triển khai nhanh cho pilot | Không có bản đồ nền ngay trong tab, rời app khi xem điểm | Danh sách chỉ có hồ sơ được quyền; tọa độ/độ chính xác rõ; ứng dụng thiếu trình xử lý có lỗi phù hợp |

Các phương án bản đồ chỉ dùng tọa độ gắn ảnh chính; không dùng GPS hiện tại của bảo vệ để thay thế. Nếu chọn A hoặc B, cần duyệt công nghệ, chi phí và quyền truy cập dịch vụ trước khi thêm dependency/cấu hình. [Google Maps SDK yêu cầu billing và API key](https://developers.google.com/maps/documentation/android-sdk/usage-and-billing); [MapLibre có SDK Android](https://maplibre.org/maplibre-native/android/examples/getting-started/) nhưng vẫn cần nguồn tile, ví dụ nguồn OSM có [chính sách sử dụng và ghi công](https://operations.osmfoundation.org/policies/tiles/). Nếu chọn C, tab nên có tiêu đề “Vị trí hồ sơ” để diễn đạt đúng khả năng.

## Thư mục rỗng và hướng dùng

Các thư mục `ai/detection`, `ai/model`, `ai/ocr`, `ai/processing`, `camera`, `location`, `ui/guard/{capture,history,home,violation}` là khung chưa có mã. Khi tính năng được duyệt, đặt lớp suy luận/tiền xử lý thực sự vào `ai/*`, adapter CameraX vào `camera` nếu được tách khỏi Fragment, và màn/lớp lịch sử vào `ui/guard/history` nếu tách Sổ vi phạm. Các thư mục `ui/student/*`, `ui/shell`, `ui/common`, `util`, `domain/sevice` và `src/debug/*` không có nghiệp vụ được duyệt trong yêu cầu này; không tạo lớp rỗng hoặc chức năng giả chỉ để lấp thư mục.

## Cổng triển khai

Sau khi duyệt wireframe và chọn hai phương án: cập nhật Decision Log; sửa navbar và màn hình theo Java/XML/ViewBinding; kiểm tra quyền ở datasource/Rules; đo tensor/normalization/output/latency trước AI; thử trên thiết bị thật với camera, GPS và grant. Nếu thiếu Java/SDK/thiết bị, kết quả build/runtime ghi `UNKNOWN`.

## Cập nhật triển khai ngày 2026-10-06

- Năm tab đã nối vào `GuardHomeFragment`: Trang chủ, Sổ vi phạm, Quét nhanh, Bản đồ, Cá nhân. Sổ vi phạm dùng lại bản nháp/hồ sơ/chi tiết đang có; theo DEC-027, giao diện đã thêm tìm kiếm và lọc trên tập bản ghi đã tải, không phải truy vấn toàn bộ lịch sử máy chủ.
- Quét nhanh chụp JPEG CameraX vào bộ nhớ, dùng asset TFLite để gợi ý tối đa ba vùng biển, ML Kit Latin bundled đọc chữ trên vùng đã chọn hoặc toàn ảnh, cho người bảo vệ sửa. Ảnh quét không ghi file, không upload và không tạo hồ sơ; dữ liệu ảnh bị bỏ khi rời tab. Đây là gợi ý, chưa có kết quả đo với ảnh biển số thật.
- Theo DEC-028, sau khi quét người bảo vệ có thể mở luồng chụp ảnh chính mới. Biển số hợp lệ chỉ là gợi ý đi kèm đến màn rà soát; ảnh Quét nhanh không trở thành bằng chứng. Văn bản ghi nhận xem trước xuất hiện sau khi bảo vệ rà ảnh, chọn hành vi và kiểm vị trí ảnh chính, trước khi gửi hồ sơ.
- Bản đồ dùng MapLibre và style demo theo DEC-026, chỉ đặt marker cho hồ sơ trả về từ truy vấn grant/scope có `locationStatus=AUTO_ACCEPTED` và tọa độ hợp lệ. Chọn điểm hoặc hàng mở chi tiết. Giới hạn truy vấn hiện tại là 50 hồ sơ; nguồn demo chỉ dùng pilot.
- Đã sử dụng thực tế các thư mục `ai/detection`, `ai/model`, `ai/ocr`, `camera`, `ui/guard/capture`. Các thư mục còn rỗng vẫn không có chức năng được duyệt riêng; không thêm lớp giả.
- `:app:assembleDebug` và `:app:lintDebug` chạy thành công trên Windows JDK/Android SDK ngày 2026-10-06; lint có 0 lỗi, 21 cảnh báo, gồm native x86_64 LiteRT chưa căn lề 16 KB. Chưa chạy app trên thiết bị thật, nên camera, bản đồ, OCR, quyền grant và hiệu năng Android vẫn `UNKNOWN`.
