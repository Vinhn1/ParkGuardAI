# 21 — Nghiên cứu và đề xuất chức năng quét nhiều xe (Multi-Vehicle Scan)

> Trạng thái: `PROPOSED`, ngày 2026-10-06. Tài liệu khảo sát và thiết kế kỹ thuật phục vụ quyết định sản phẩm tiếp theo. Chưa triển khai vào mã nguồn khi chưa được người dùng phê duyệt vào [Decision Log](15-DECISION-LOG.md).

---

## 1. Bối cảnh và bài toán thực tế

Tại khuôn viên trường đại học, tình trạng vi phạm đỗ xe thường diễn ra theo **cụm hoặc cả dãy phương tiện** (ví dụ: một hàng xe máy cùng đỗ trên lối đi bộ, chắn cửa thoát hiểm, hoặc đỗ ngoài ô vạch quy định).

Nếu sử dụng luồng đơn lẻ hiện tại:
$$\text{Chụp ảnh} \rightarrow \text{Chờ GPS} \rightarrow \text{Chờ AI/OCR} \rightarrow \text{Chọn loại vi phạm} \rightarrow \text{Nhập ghi chú} \rightarrow \text{Gửi từng xe}$$
Bảo vệ sẽ mất 1–2 phút cho mỗi xe. Với một dãy 10–20 xe vi phạm, tổng thời gian thao tác kéo dài 15–30 phút dưới thời tiết ngoài trời, gây quá tải, dễ nản và làm giảm hiệu quả tuần tra.

Do đó, cần một cơ chế **quét/ghi nhận nhiều xe vi phạm** nhanh chóng nhưng vẫn phải bảo đảm các nguyên tắc pháp lý và kỹ thuật cốt lõi của ParkGuard AI.

---

## 2. Các ràng buộc cốt lõi không được vi phạm

Bất kỳ giải pháp quét nhiều xe nào cũng phải tuân thủ nghiêm ngặt các quy tắc đã được duyệt:
1. **Giá trị bằng chứng pháp lý (DEC-008 / DEC-008A):** Mỗi hồ sơ vi phạm của từng xe phải có ảnh bằng chứng rõ nét biển số và bối cảnh vi phạm, gắn liền với thời điểm bấm chụp (`capturedAtMillis`) và tọa độ vị trí thực địa đạt ngưỡng chính xác. Không được dùng một tọa độ đại diện cách xa hoặc vị trí lưu sau đó để gán thay thế.
2. **AI chỉ hỗ trợ gợi ý (AGENTS.md / GEMINI.md):** AI không được tự ý kết luận hành vi vi phạm hay tự động tạo và gửi hồ sơ mà không qua bước rà soát, xác nhận của bảo vệ.
3. **Giới hạn tài nguyên thiết bị Android:** Model TFLite YOLO11n (10.6 MB, input 640×640) và ML Kit OCR tiêu tốn CPU/RAM đáng kể. Không chạy suy luận liên tục trên luồng giao diện hoặc chạy vòng lặp preview gây nghẽn, nóng máy và cạn pin.

---

## 3. Đánh giá và so sánh 3 phương án quét

### Phương án A: Chụp 1 ảnh góc rộng chứa nhiều xe (Multi-box in Single Frame)
- **Cơ chế:** Bảo vệ đứng xa chụp một bức ảnh góc rộng bao quát toàn bộ dãy xe vi phạm. Model YOLO11n phát hiện nhiều bounding box (`List<PlateDetector.Detection>`), tự động cắt từng vùng biển số và chạy OCR.
- **Ưu điểm:**
  - Chỉ bấm máy 1 lần duy nhất cho cả dãy xe.
  - Cùng một thời điểm và một snapshot GPS.
- **Nhược điểm & Rủi ro:**
  - *Chất lượng biển số kém:* Do ảnh chụp từ xa và input model bị nén về 640×640, biển số các xe ở xa sẽ rất nhỏ và mờ, dẫn đến detector bỏ sót box và OCR ML Kit không đọc được ký tự (đặc biệt là biển xe máy 2 dòng của Việt Nam).
  - *Bằng chứng khiếu nại yếu:* Ảnh góc rộng không thể hiện rõ chi tiết vi phạm của từng xe riêng lẻ khi sinh viên mở xem bằng chứng.
  - *Góc chụp bị che khuất:* Các xe đỗ sát nhau sẽ che biển số của nhau.
- **Đánh giá:** **Không phù hợp làm giải pháp chính**.

---

### Phương án B: Quét liên tục thời gian thực dạng Video/Preview (Real-time Continuous Scan)
- **Cơ chế:** Dùng `CameraX ImageAnalysis`. Bảo vệ cầm điện thoại lia máy đi dọc hàng xe. Khi model phát hiện biển số hợp lệ trên khung hình preview, máy rung/bíp và tự tạo bản ghi nháp.
- **Ưu điểm:** Tốc độ quét cực nhanh, trải nghiệm giống quét mã vạch kiểm kê.
- **Nhược điểm & Rủi ro:**
  - *Nóng máy và hao pin:* Chạy YOLO11n (50–80 ms/frame) + ML Kit OCR (100–200 ms) liên tục trên video stream sẽ làm thiết bị nóng nhanh chóng và giật lag giao diện.
  - *Ảnh preview chất lượng thấp:* Khung hình trích xuất từ stream video thường bị nhòe chuyển động (motion blur) và độ phân giải thấp hơn nhiều so với ảnh chụp `ImageCapture` tĩnh chất lượng cao.
  - *Quét nhầm xe không vi phạm:* Rất dễ bắt nhầm biển số của các xe đỗ đúng quy định bên cạnh hoặc xe đang lưu thông qua.
- **Đánh giá:** **Rủi ro kỹ thuật cao, không ổn định trên đa số thiết bị Android phổ thông**.

---

### Phương án C (KHUYẾN NGHỊ): Chụp liên hoàn hàng loạt (Batch / Rapid Capture Mode) kèm Hàng đợi rà soát (Review Queue)
- **Cơ chế:**
  1. **Pha 1 — Chụp nhanh tại hiện trường (Hiện trường):**
     - Bảo vệ kích hoạt chế độ "Chụp liên hoàn".
     - Bảo vệ đi bộ dọc hàng xe vi phạm: hướng camera vào từng xe và bấm chụp (mỗi xe mất ~2 giây).
     - `ImageCapture` chụp ảnh chất lượng cao riêng cho từng xe.
     - Hệ thống lấy snapshot GPS foreground đạt chuẩn DEC-008A và lưu bản nháp cục bộ vào `GuardDraftStore`.
     - Màn hình hiện bộ đếm trực quan: *"Đã ghi nhận N xe trong đợt này"*.
  2. **Pha 2 — Phân tích ngầm tự động (Background AI Processing):**
     - Sau khi bấm "Hoàn tất đợt chụp" (hoặc tự động chạy trên luồng nền `ExecutorService`), hệ thống chạy tuần tự `PlateDetector` và `PlateTextReader` trên từng ảnh gốc đã lưu.
     - Tự động tạo ảnh crop biển số và điền biển số gợi ý vào từng bản nháp.
  3. **Pha 3 — Rà soát và gửi đồng loạt (Bulk Review & Submit):**
     - Bảo vệ chuyển sang màn hình danh sách đợt chụp (dạng danh sách thẻ hoặc Carousel).
     - Bảo vệ có tùy chọn: **"Áp dụng chung một hành vi vi phạm"** (ví dụ: *Đỗ sai khu vực quy định*) cho toàn bộ N xe trong đợt để không phải chọn lại từng lần.
     - Bảo vệ lướt nhanh kiểm tra biển số của từng xe (được phóng to vùng crop biển số), chỉnh sửa nếu cần, rồi bấm **"Xác nhận và gửi tất cả"**.
     - Hệ thống đưa vào hàng đợi tải lên (`ViolationRepository`) chạy ngầm.

- **So sánh hiệu quả:**
  | Tiêu chí | Luồng đơn lẻ hiện tại | Phương án C (Chụp liên hoàn) |
  |---|---|---|
  | Thời gian chụp 10 xe | ~15 – 20 phút | ~1 – 2 phút tại hiện trường |
  | Thời gian rà soát | Rải rác, đứng ngoài trời | Tập trung, có thể ngồi rà soát |
  | Chất lượng bằng chứng | Rõ nét từng xe | Rõ nét từng xe (ảnh gốc riêng) |
  | Độ tin cậy GPS | GPS riêng từng xe | GPS riêng từng xe (chuẩn DEC-008A) |
  | Tiêu thụ tài nguyên | Rất nhẹ | Tối ưu (xử lý tuần tự trên luồng nền) |
  | Tránh ghi đè/nhầm lẫn | Tốt | Tốt (mỗi xe là 1 Draft ID độc lập) |

---

## 4. Kiến trúc và luồng dữ liệu chi tiết cho Phương án C

```
[Bảo vệ bấm Chụp liên hoàn]
            │
            ▼
┌──────────────────────────────────────────────┐
│ Vòng lặp chụp nhanh tại hiện trường:         │
│  - ImageCapture lưu file full-res            │
│  - CaptureLocationSource snapshot GPS        │
│  - Lưu GuardDraft (chưa gửi, pending_ai)     │
│  - Tăng counter trên camera overlay          │
└──────────────────────────────────────────────┘
            │
            ▼ (Bảo vệ bấm "Rà soát đợt này")
┌──────────────────────────────────────────────┐
│ Worker ngầm xử lý tuần tự (Background Queue): │
│  - GuardDraft 1 -> Detect plate -> OCR       │
│  - GuardDraft 2 -> Detect plate -> OCR       │
│  - ...                                       │
└──────────────────────────────────────────────┘
            │
            ▼
┌──────────────────────────────────────────────┐
│ Màn hình Rà soát hàng loạt (Batch Review UI): │
│  - Chọn hành vi vi phạm chung cho cả đợt     │
│  - Danh sách thẻ từng xe: xem crop + biển    │
│  - Bảo vệ sửa tay xe nào AI đọc chưa chuẩn   │
│  - Nút "Xác nhận & Gửi toàn bộ đợt"          │
└──────────────────────────────────────────────┘
            │
            ▼
┌──────────────────────────────────────────────┐
│ Hàng đợi Upload & Firestore (Repository):     │
│  - Upload Cloudinary (song song có giới hạn) │
│  - Firestore batch write violations          │
│  - Cập nhật trạng thái sent trong DraftStore │
└──────────────────────────────────────────────┘
```

---

## 5. Rủi ro kỹ thuật và biện pháp kiểm soát

1. **Rủi ro GPS giữa các lần chụp nhanh:**
   - *Vấn đề:* Khi bấm chụp liên tiếp cách nhau 2–3 giây, bộ định vị Fused Location Provider có thể trả về cùng một tọa độ (do thời gian cập nhật tối thiểu của GPS phần cứng).
   - *Kiểm soát:* Đối với các xe đỗ thành hàng sát nhau trong cùng 1 vị trí sân bãi, tọa độ này hoàn toàn phản ánh trung thực hiện trường vi phạm. Tuy nhiên, thời điểm `capturedAtMillis` và timestamp `locationAtMillis` được lưu độc lập từng ảnh, đảm bảo tính phân biệt.
2. **Dung lượng lưu trữ cục bộ tạm thời:**
   - *Vấn đề:* Chụp liên tiếp 10–20 ảnh có thể chiếm 30–60 MB bộ nhớ tạm `noBackupFilesDir`.
   - *Kiểm soát:* Sau khi upload thành công lên Cloudinary và Firestore, hệ thống tự động dọn dẹp các file JPEG gốc trong đợt chụp hoặc giữ lại ảnh theo chính sách DEC-006A.
3. **Bộ nhớ RAM khi rà soát danh sách nhiều xe:**
   - *Vấn đề:* Nạp cùng lúc 20 Bitmap ảnh chụp vào RecyclerView dễ gây `OutOfMemoryError`.
   - *Kiểm soát:* Chỉ nạp ảnh thumbnail nhỏ (`inSampleSize = 4` hoặc `8`) và ảnh crop biển số nhỏ vào danh sách hiển thị; không giữ đồng thời các Bitmap độ phân giải đầy đủ trong bộ nhớ RAM.

---

## 6. Kế hoạch nghiệm thu và đề xuất quyết định

- **Điều kiện nghiệm thu kỹ thuật:**
  1. Bảo vệ có thể bấm chụp liên tiếp các xe mà camera không bị đơ hoặc trễ quá 1 giây giữa các lần bấm.
  2. Từng xe có đủ 1 ảnh gốc, 1 ảnh crop biển số, 1 snapshot GPS riêng biệt đạt chuẩn sai số DEC-008A.
  3. Giao diện rà soát cho phép áp dụng hàng loạt loại vi phạm chung và sửa từng biển số riêng lẻ.
  4. Quá trình gửi chạy tuần tự/batch trong background, có thanh tiến trình hiển thị rõ tỉ lệ thành công/thất bại.

- **Đề xuất quyết định tiếp theo:**
  Trình người dùng xem xét và phê duyệt theo mục **DEC-030 (OPEN)** trong [Decision Log](15-DECISION-LOG.md) trước khi triển khai các lớp giao diện và bộ xử lý hàng đợi.
