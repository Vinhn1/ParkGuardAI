# 03 — Luồng người dùng và ngoại lệ

> Đây là luồng mục tiêu `PLANNED`; Navigation Graph hiện rỗng tại [`nav_graph.xml`](../app/src/main/res/navigation/nav_graph.xml). Các nhánh phụ thuộc quyết định `OPEN` được nêu rõ. Trạng thái giao diện chung: loading, empty, success, error, offline, permission denied theo [UI/UX](07-UI-UX-GUIDELINES.md).

## Sinh viên: đăng ký và quản lý xe

```mermaid
flowchart TD
  A[Mở đăng ký] --> B[Nhập họ tên, email, mật khẩu và dữ liệu theo chính sách]
  B --> C{Form hợp lệ, đã đồng ý xử lý dữ liệu?}
  C -- Không --> D[Hiện lỗi bên trường, giữ dữ liệu hợp lệ]
  D --> B
  C -- Có --> E[Tạo danh tính STUDENT trong Firebase Auth, profile Firestore không có password]
  E --> F{Trùng hoặc thất bại?}
  F -- Có --> G[Thông báo phù hợp, chống lộ tài khoản]
  F -- Không --> H[Xác minh email]
  H --> I[Đăng ký ít nhất một phương tiện để hoàn tất onboarding]
  I --> J[Gửi vehicleVerificationRequest PENDING vào Firestore]
  J --> K{Biển số xung đột?}
  K -- Có --> L[Chờ xét duyệt, không lộ chủ tài khoản khác]
  K -- Không --> M[Chờ xác minh]
  M --> N[ADMIN/nhân sự được cấp quyền xét duyệt thủ công]
  L --> N
  N --> O[VERIFIED hoặc REJECTED có lý do; thông báo sinh viên]
```

Số điện thoại là liên hệ tùy chọn và **chưa xác minh** trên Firebase Spark: form không có bước SMS OTP; nếu để trống vẫn tiếp tục, nếu cung cấp phải hiện nhãn chưa xác minh và không dùng làm định danh đăng nhập/khôi phục. Email phải được Firebase Auth xác minh trước bước đăng ký xe. Mã sinh viên chỉ thu nếu có nguồn xác minh được duyệt. Khi mất mạng lúc đăng ký, không thông báo đã tạo tài khoản/profile/xe nếu chưa có xác nhận server. Tài khoản Auth tạo xong nhưng profile Firestore chưa hoàn tất cần trạng thái phục hồi, không tạo vai trò ADMIN. Hai yêu cầu cùng biển số có thể cùng `PENDING`; **duyệt `VERIFIED`** phải bảo đảm chỉ một claim hiệu lực bằng transaction/backend đáng tin cậy. Không chuyển hồ sơ cũ cho chủ mới. Xem [Requirements](01-PRODUCT-REQUIREMENTS.md), [Database](04-DATABASE-DESIGN.md), DEC-004/007 tại [Decision Log](15-DECISION-LOG.md).

## Bảo vệ: ghi nhận vi phạm

```mermaid
flowchart TD
  A[Đăng nhập và kiểm tra phạm vi] --> B[Mở CameraX; kiểm quyền camera và vị trí, dịch vụ định vị]
  B --> C{Có quyền camera?}
  C -- Không --> C1[Giải thích, mở cài đặt hoặc rời luồng]
  C -- Có --> D[Hướng dẫn và chụp ảnh chính; ghi capturedAt tại sự kiện chụp]
  D --> E[Yêu cầu fix vị trí foreground gắn với ảnh chính]
  E --> F{Có thời điểm đo, sai số và chênh thời gian đạt ngưỡng?}
  F -- Không --> G[Cảnh báo rõ nguyên nhân; thử lại, chụp lại hoặc vị trí thủ công theo quy trình duyệt]
  G --> H{Có phương án hợp lệ?}
  H -- Không --> G
  H -- Có --> I[Giữ nguồn/trạng thái riêng; không giả là GPS]
  F -- Có --> J[Khóa snapshot vị trí tự động với ảnh chính]
  I --> K[Kiểm tra ảnh mờ; YOLO11n gợi ý vùng biển số]
  J --> K
  K --> L{Số vùng?}
  L -- 0 --> M[Chụp lại hoặc nhập biển số thủ công kèm lý do]
  L -- Nhiều --> N[Chọn vùng đúng, giữ ảnh gốc]
  L -- 1 --> O[Crop vùng biển số]
  N --> O
  O --> P[ML Kit Text Recognition gợi ý ký tự]
  P --> Q[Bảo vệ kiểm tra/chỉnh ký tự]
  M --> R[Chọn loại vi phạm, ảnh bổ sung, ghi chú]
  Q --> R
  R --> S[Xem thời điểm chụp, vị trí, độ chính xác, trạng thái; xác nhận]
  S --> T{Lưu thành công?}
  T -- Có --> U[Xác nhận Firestore/Storage từ server rồi hiện mã hồ sơ]
  T -- Không --> V[Giữ dữ liệu trong phiên theo chính sách; cho thử lại không tạo trùng]
```

Không phát hiện biển số: cho chụp lại/nhập tay, không đặt biển số giả. Nhiều biển số: người dùng chọn vùng, không lấy mặc định confidence cao nhất. OCR không hợp lệ: hiển thị nguyên gợi ý để đối chiếu, đánh dấu nguồn `MANUAL_CORRECTION`. Ảnh mờ: yêu cầu ảnh khác nếu không đủ bằng chứng. Quyền vị trí bị từ chối, dịch vụ tắt, timeout, fix quá cũ hoặc sai số lớn: cảnh báo, không ghi `0,0`, không tự lấy vị trí khi lưu; cho thử lại/chụp lại hoặc chọn khu vực thủ công **nếu DEC-008A duyệt**. Khi thoát trước xác nhận: hỏi lưu/hủy bản nháp, xóa ảnh tạm theo chính sách. Lưu thất bại: trạng thái chưa gửi và mã idempotency, không tạo trùng khi retry.

**Ảnh chính và nhiều ảnh:** ảnh chụp đầu tiên được chọn làm ảnh chính, có `primaryEvidenceId`, `capturedAt` và snapshot vị trí riêng. Ảnh bổ sung có thời điểm/metadata riêng và không thay snapshot vị trí hồ sơ. Nếu đổi ảnh chính, bảo vệ phải chọn tường minh; hệ thống rà lại vị trí của ảnh mới, hiển thị thay đổi, và ghi audit nếu hồ sơ đã lưu. Nếu bảo vệ di chuyển sau ảnh chính, vị trí đo muộn không được gắn ngược vào ảnh cũ khi vượt ngưỡng chênh thời gian; phải chụp lại hoặc theo ngoại lệ thủ công đã duyệt.

## Sinh viên: tra cứu hồ sơ

Đăng nhập → xác minh phiên → xem xe đã liên kết → chọn vi phạm được dịch vụ **phân quyền theo liên kết đã duyệt và thời gian hiệu lực** → xem ảnh/thời gian/khu vực/trạng thái. Xe `PENDING`, `REJECTED`, `INACTIVE`, xung đột biển số hoặc vi phạm trước `validFrom` không cấp quyền tự động. Danh sách rỗng nói rõ “chưa có hồ sơ được phép hiển thị”; lỗi mạng có nút thử lại, cache nếu có phải tuân quyền và chính sách. Đường dẫn trực tiếp tới hồ sơ/ảnh ngoài quyền bị từ chối. Gửi yêu cầu xem xét lại là luồng `PROPOSED`, chỉ xuất hiện sau phê duyệt sản phẩm.

## Quản trị và xử lý tranh chấp

Trong **ứng dụng Android ADMIN**, người có quyền xem hàng chờ xác minh → kiểm tra bằng chứng theo mức tối thiểu → phát hiện trùng biển số → đối chiếu thời gian/quyền sử dụng, yêu cầu bổ sung khi thiếu → duyệt/từ chối có lý do và audit. Khi duyệt, nguồn đáng tin cậy giữ một `plateClaim` hoạt động bằng transaction; request trùng bị từ chối/chuyển hàng tranh chấp, không lộ chủ xe khác. Khi chuyển nhượng: đóng liên kết cũ, tạo liên kết mới; không sửa lịch sử vi phạm cũ. Với hồ sơ vi phạm đã xác nhận: chỉnh sửa bằng phiên bản/sự kiện sửa, không ghi đè ảnh gốc; thay đổi trạng thái cần quyền và lý do.

## Trạng thái hệ thống và phục hồi

| Tình huống | Phản hồi và hành động |
|---|---|
| Camera bị từ chối | Nêu lý do cần quyền, hướng tới cài đặt nếu từ chối lâu dài; không lặp hộp thoại liên tục. |
| Từ chối quyền vị trí/chỉ cấp approximate | Giải thích yêu cầu chất lượng, thử xin quyền foreground phù hợp; nếu không đạt, chụp lại hoặc quy trình thủ công đã duyệt; không nói ảnh có GPS hợp lệ. |
| Dịch vụ định vị tắt | Hướng dẫn bật dịch vụ qua cài đặt hệ thống rồi thử lại; không tự bật hoặc dùng fix cũ. |
| Fix cũ, độ chính xác thấp (sai số lớn), timeout | Hiện `locationStatus` và nguyên nhân cụ thể; thử lấy lại/chụp lại/thủ công theo DEC-008A; không thay bằng vị trí lúc lưu. |
| Nhiều ảnh hoặc bảo vệ di chuyển | Ảnh bổ sung không ghi đè vị trí ảnh chính; đổi ảnh chính cần thao tác rõ và kiểm lại khoảng lệch thời gian. |
| Không Internet | Hiển thị offline; bản đầu online-first không xác nhận nộp hồ sơ khi chưa có xác nhận server. Cache Firestore không đồng nghĩa hồ sơ đã lưu chính thức. |
| Hết phiên/bị thu quyền | Dừng gửi bản nháp, yêu cầu đăng nhập lại, xóa cache nhạy cảm theo chính sách. |
| Upload ảnh xong nhưng ghi metadata lỗi | Đánh dấu giao dịch chưa hoàn tất, thử lại hoặc dọn ảnh mồ côi theo chính sách; không công bố hồ sơ nửa vời. |
| Người dùng rời form | Hỏi lưu bản nháp/hủy; nếu hủy, xử lý ảnh tạm theo thời hạn đã duyệt. |
