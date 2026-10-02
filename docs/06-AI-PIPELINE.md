# 06 — Pipeline Camera, YOLO11n và OCR

> `PARTIAL`: repository có [`parkguard_yolo11n_best.tflite`](../app/src/main/assets/parkguard_yolo11n_best.tflite) (10.626.664 byte, đầu file `TFL3`) và dependency TensorFlow Lite trong [`app/build.gradle.kts`](../app/build.gradle.kts). ADR-002 đã chọn **Google ML Kit Text Recognition để thử nghiệm OCR**, nhưng chưa có CameraX, lớp suy luận, ML Kit hoặc test model trong code. Tên file và thông tin huấn luyện do dự án cung cấp **không đủ** xác định kiểu/shape tensor hay phép hậu xử lý.

## Phạm vi AI và hợp đồng dữ liệu

Mô hình YOLO11n được mô tả là **phát hiện vùng chứa biển số**. Nó không trả chuỗi ký tự, không xác định chủ xe và không phân loại hành vi đỗ sai. Pipeline mục tiêu: **CameraX chụp ảnh chính + ghi `capturedAt` + yêu cầu fix vị trí gắn ảnh chính** → xoay và scale ảnh → tensor đúng metadata → YOLO11n detect → decode + NMS theo output thực tế → crop đúng tọa độ ảnh gốc → **ML Kit Text Recognition** đọc ký tự → chuẩn hóa/gợi ý → **bảo vệ đối chiếu và có quyền chỉnh biển số, vị trí trước xác nhận** → hồ sơ. GPS không phải output của YOLO/OCR; pipeline vị trí có trạng thái lỗi riêng và không chờ đến lúc lưu hồ sơ. Lưu `plateOcrRaw`, `plateFinal`, nguồn chỉnh sửa và version model khi phù hợp, tránh dùng confidence như bằng chứng duy nhất.

## Kiểm tra model trước khi lập trình hậu xử lý

1. Dùng công cụ đọc TFLite FlatBuffer/Interpreter để ghi **số lượng input/output, tên, shape, dtype, quantization scale/zero point, signature, metadata, op cần thiết**. So khớp nguồn export/model gốc và checksum. Đừng mặc định `[1,640,640,3]` chỉ vì kích thước huấn luyện 640 × 640.
2. Kiểm tra màu RGB/BGR, dải đầu vào, letterbox hay resize trực tiếp, interpolation và cách đảo tọa độ. Ghi version phép tiền xử lý cùng test ảnh vàng.
3. Dùng ảnh mẫu có nhãn để xác định output là tọa độ dạng gì, thứ tự chiều, score, NMS đã gộp hay chưa. Nếu có quantization, giải lượng tử bằng tham số đọc được; không đoán `float32`.
4. Kiểm tra model license, nguồn dữ liệu và quyền sử dụng theo mục đích dự án trước phân phối. Thông tin precision/recall/mAP do dự án cung cấp chỉ trên tập test báo cáo, chưa được repository xác minh.

## CameraX và xử lý ảnh mục tiêu

- Camera preview và chụp ảnh tĩnh gắn lifecycle; chỉ xin quyền `CAMERA` khi mở chức năng. Nếu dùng `ImageAnalysis`, chọn backpressure phù hợp và đóng **`ImageProxy.close()`** ở mọi đường thoát; không đóng `Media.Image` bọc bên trong. [Hướng dẫn ImageAnalysis của Android](https://developer.android.com/media/camera/camerax/analyze).
- Ghi nhận rotation, kích thước frame và phép biến đổi từ preview → tensor → ảnh gốc. Bounding box phải được clamp trong ảnh trước crop; vùng quá nhỏ/ngoài ảnh là lỗi có thể phục hồi.
- Giới hạn số frame suy luận, tái sử dụng buffer hợp lý, tránh giữ bitmap toàn ảnh trong ViewModel; giải phóng `Interpreter`, image buffer, executor và camera binding theo lifecycle. Suy luận/crop/OCR không chạy trên main thread.
- Chỉ lưu ảnh gốc cần làm bằng chứng và crop nếu có mục đích; xóa ảnh tạm theo chính sách. Ảnh gốc có metadata thời gian/nguồn, hash sau upload; crop là dẫn xuất, không thay thế ảnh hiện trường.
- Ảnh chính phải có `capturedAt` và snapshot vị trí từ lần chụp đó; ảnh bổ sung không được ghi đè. Nếu camera trả callback lưu file muộn, không coi thời điểm callback là thời điểm chụp. Nguồn timestamp chụp và độ trễ phải được kiểm thử với [CameraX ImageCapture](https://developer.android.com/media/camera/camerax/take-photo); xem [kiến trúc](05-SYSTEM-ARCHITECTURE.md) cho kiểm tuổi fix.

## ML Kit OCR đã chọn để thử nghiệm; chi tiết còn mở

| Biến thể/kế hoạch | Lợi ích | Rủi ro/chi phí | Điều kiện đánh giá |
|---|---|---|---|
| ML Kit Text Recognition v2 Latin **bundled** | Chạy on-device, sẵn ngay kể cả offline; phù hợp pilot ổn định | Tăng kích thước app; chưa có bằng chứng chính xác trên biển xe máy Việt Nam | So sánh app size, latency và full-plate accuracy; lựa chọn bundled/unbundled còn mở |
| ML Kit Latin **unbundled** | App nhỏ hơn | Cần tải model ban đầu, camera có thể chưa đọc được OCR khi chưa tải xong | Chỉ chọn nếu chấp nhận bước chuẩn bị/tải model và thông báo trạng thái |
| OCR tùy biến hoặc server | **Phương án dự phòng nghiên cứu**, chưa được duyệt thay ML Kit | Thêm dữ liệu huấn luyện, hạ tầng/PII, chi phí và quyết định mới | Chỉ trình lại nếu pilot ML Kit không đạt tiêu chí đã duyệt |

[ML Kit Text Recognition v2 Android](https://developers.google.com/ml-kit/vision/text-recognition/v2/android) nêu khác biệt bundled/unbundled; [danh sách ngôn ngữ](https://developers.google.com/ml-kit/vision/text-recognition/v2/languages) có tiếng Việt trong nhóm Latin, nhưng **đó không phải chứng nhận độ chính xác trên biển số xe Việt Nam**. Thiết kế tập pilot có biển xe máy **hai hàng** tách nhóm: crop đủ hai hàng, thứ tự dòng OCR, ký tự ngắt dòng, ký tự dễ nhầm `0/O`, `1/I`, `5/S`, `8/B`, góc nghiêng, phản sáng, mờ, đêm. Lưu `rawText` và chuỗi sau chuẩn hóa để đối chiếu; quy tắc chuẩn hóa chỉ loại khoảng trắng/ký tự phân cách đã được duyệt, **không tự đổi ký tự không chắc chắn**. Bảo vệ luôn xem crop và chỉnh tay, AI không kết luận vi phạm. Test theo [11](11-TESTING-STRATEGY.md).

## Kịch bản lỗi và ngưỡng kiểm thử

| Tình huống | Xử lý |
|---|---|
| Không có bounding box | Chụp lại hoặc nhập tay kèm nguồn/lý do; không tạo box giả. |
| Nhiều box | Hiển thị từng vùng, cho bảo vệ chọn; không coi box score cao nhất là xe vi phạm. |
| Crop mờ/quá nhỏ/ngoài ảnh | Chụp lại; giữ ảnh gốc để kiểm tra; không gửi ảnh hỏng như bằng chứng. |
| OCR rỗng/không hợp lệ | Cho nhập/chỉnh tay; lưu phân biệt OCR raw và giá trị cuối. |
| Model không tương thích/thiếu op | Hiện lỗi chức năng AI, mở đường nhập tay; thu thông tin kỹ thuật không chứa PII. |
| Thiết bị nóng/thiếu RAM | Giảm tần suất/độ phân giải theo phép thử đã được duyệt, tránh crash, đo lại chất lượng. |

Đo detection precision/recall theo box và mAP trên bộ thực địa tách khỏi training; OCR dùng character error rate, full-plate accuracy, tỷ lệ cần sửa; đo tỷ lệ hoàn tất hồ sơ, latency p50/p95, RAM đỉnh, pin/nhiệt và failure theo loại thiết bị. Chưa đặt mục tiêu số vì chưa có thiết bị/tập thực địa được duyệt. Báo cả sai sót theo ánh sáng, loại biển và độ phân giải. Xem [Testing Strategy](11-TESTING-STRATEGY.md).
