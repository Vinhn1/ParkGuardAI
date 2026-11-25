# 06 — Pipeline Camera, YOLO11n và OCR

> `PARTIAL`, cập nhật 2026-10-06: repository có [`parkguard_yolo11n_best.tflite`](../app/src/main/assets/parkguard_yolo11n_best.tflite) (10.626.664 byte, đầu file `TFL3`), [`PlateDetector`](../app/src/main/java/com/example/parkguardai/ai/detection/PlateDetector.java) và ML Kit Latin bundled trong [`PlateTextReader`](../app/src/main/java/com/example/parkguardai/ai/ocr/PlateTextReader.java). Quét nhanh đã nối camera → detector → OCR ở bộ nhớ và cho sửa kết quả. Theo DEC-028, chỉ biển số hợp lệ được chuyển làm gợi ý sang luồng chụp ảnh chính mới; ảnh quét không là bằng chứng. Bảo vệ chọn hành vi và xem văn bản ghi nhận trước khi gửi. Build và lint đạt; chất lượng trên ảnh biển thật và hiệu năng Android chưa được đo.

**Đo trực tiếp FlatBuffer ngày 2026-10-06:** asset có SHA-256 `f2735814b03e6df5f0c8c3e195a53bade43b58236f1a343532342016720efb50`, schema `TFL3` version 3, một subgraph, một input `serving_default_args_0` shape `[1,3,640,640]` và một output `serving_default_output_0_output` shape `[1,5,8400]`; cả hai có TensorType `FLOAT32` (enum 0), không có scale/zero point. Đọc theo [schema TensorFlow Lite](https://github.com/tensorflow/tensorflow/blob/master/tensorflow/compiler/mlir/lite/schema/schema_v3b.fbs). Đây là cấu trúc tensor; phần diễn giải output và hiệu năng cần thêm đo đạc bên dưới.

**Đo Interpreter trên máy Linux ngày 2026-10-06:** `ai-edge-litert 2.1.4` tải và chạy model thành công với input/output đúng thông tin trên. Sáu lần suy luận trên ảnh đồng nhất 0 và 1, CPU 2 thread, mất 55,9–80,9 ms; đây **không phải** độ trễ thiết bị Android hay số liệu chất lượng nhận diện. Hai ảnh đồng nhất không tạo score đáng kể (cao nhất khoảng 0,0014). `metadata.json` đính kèm trong asset ghi Ultralytics 8.4.171, task `detect`, một class `license_plate`, `end2end=false`, ảnh 640×640 và license `AGPL-3.0`. Theo [mã export Ultralytics](https://github.com/ultralytics/ultralytics/blob/main/ultralytics/utils/export/engine.py), `_NormalizeCoords` chia 4 kênh box theo kích thước input; kết hợp shape `[1,5,8400]` và một class, **suy luận** output là `(cx, cy, w, h, score)` chuẩn hóa, chưa xác nhận trên ảnh biển số thật. Chưa có số liệu RGB/BGR, phép resize/letterbox và ngưỡng NMS từ chính ảnh mẫu; cần thử trên thiết bị và ảnh mẫu trước khi công bố kết quả AI đáng tin.

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

## ML Kit OCR cho pilot; tiêu chí chất lượng còn mở

| Biến thể/kế hoạch | Lợi ích | Rủi ro/chi phí | Điều kiện đánh giá |
|---|---|---|---|
| ML Kit Text Recognition v2 Latin **bundled** — đã chọn theo DEC-026 | Chạy on-device, sẵn ngay kể cả offline; phù hợp pilot ổn định | Tăng kích thước app; chưa có bằng chứng chính xác trên biển xe máy Việt Nam | Đo app size, latency và full-plate accuracy trên thiết bị, ảnh thực địa |
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

## Lưu ý về bài toán nhận diện nhiều xe vi phạm (Multi-Vehicle Pipeline)
Khi mở rộng sang xử lý cụm nhiều xe vi phạm theo [Nghiên cứu 21](21-MULTI-VEHICLE-SCAN-PROPOSAL.md):
- **Không suy luận liên tục trên luồng preview video:** Tránh quá tải CPU, tụt FPS và quá nhiệt phần cứng.
- **Xử lý tuần tự trên luồng nền (Worker Queue):** Mỗi ảnh chụp trong đợt được xử lý tách biệt lần lượt (`PlateDetector` -> `PlateTextReader`).
- **Bảo toàn bằng chứng pháp lý:** Mỗi xe có ảnh gốc riêng và tọa độ GPS gắn liền tại thời điểm bấm máy của xe đó; ảnh crop biển số chỉ là dẫn xuất hỗ trợ rà soát.

