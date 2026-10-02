# 15 — Nhật ký quyết định

> Chỉ đánh dấu `APPROVED` khi có chấp thuận rõ của người dùng. Yêu cầu sản phẩm đã nêu là ràng buộc đầu vào; phương án công nghệ trong tài liệu khác không tự trở thành quyết định. Khi duyệt, ghi ngày, người duyệt, phương án, lý do, hệ quả, link/tài liệu liên quan; không xóa lịch sử mà bổ sung mục thay thế.

| ID | Trạng thái | Nội dung và căn cứ | Hệ quả |
|---|---|---|---|
| DEC-000 | `APPROVED`, 2026-10-02 | Người dùng duyệt tạo `AGENTS.md` và 16 file Markdown trong `docs/`, không sửa Java/XML/Gradle, không thao tác Git thay đổi lịch sử | Chỉ cho phép nhiệm vụ tài liệu này |
| DEC-001 / ADR-001 | `APPROVED`, 2026-10-02 | Firebase Authentication + Cloud Firestore + Firebase Cloud Storage cho danh tính, nghiệp vụ và ảnh | Thiết kế Rules, schema, Storage và test; không lưu password trong Firestore; **chưa cho phép tạo Firebase Project/cấu hình môi trường thật** |
| DEC-002 / ADR-002 | `APPROVED`, 2026-10-02 | Thử Google ML Kit Text Recognition sau YOLO11n/crop, bảo vệ xác nhận kết quả | Cần pilot OCR biển xe máy hai hàng; bundled/unbundled và ngưỡng chất lượng còn mở |
| DEC-003 / ADR-003 | `APPROVED`, 2026-10-02 | Vai trò ADMIN tích hợp trực tiếp trong app Android cùng STUDENT và SECURITY_GUARD | Enum `UserRole.java` đã thêm ADMIN trong nhiệm vụ model sau này; màn hình/điều hướng/quyền thực thi vẫn chưa có |
| DEC-004 / ADR-004 | `APPROVED`, 2026-10-02 | Xác minh phương tiện thủ công bởi ADMIN/nhân sự được ủy quyền; sinh viên đăng ký ≥1 xe sau xác minh đăng nhập | Cần hàng chờ, xử lý trùng/chuyển nhượng, lịch sử thời gian; chứng cứ tối thiểu/quyền duyệt chi tiết còn mở |
| DEC-005 / ADR-005 | `APPROVED`, 2026-10-02 | Phiên bản đầu ưu tiên online, chưa xây đồng bộ offline nâng cao; báo mất mạng, chỉ báo lưu thành công khi có xác nhận server, chống retry trùng | Cần phân biệt cache Firestore với lưu chính thức; offline nâng cao ở giai đoạn sau |
| DEC-006 | `OPEN` | Chính sách dữ liệu: thời hạn giữ/xóa ảnh, vị trí, hồ sơ, audit; cơ sở xử lý và đầu mối tiếp nhận yêu cầu chủ thể dữ liệu | Cần duyệt trước triển khai và rà soát pháp lý |
| DEC-007 | `APPROVED`, 2026-10-02 | Theo lựa chọn có điều kiện của người dùng: vì SMS OTP của Firebase Phone Auth không có trên Spark, bản đầu dùng Firebase Auth email/mật khẩu và xác minh email; số điện thoại chỉ là liên hệ **tùy chọn, chưa xác minh**, không dùng đăng nhập/khôi phục/gán xe. [Firebase pricing](https://firebase.google.com/pricing), [Auth limits](https://firebase.google.com/docs/auth/limits) | Không thêm Phone Auth/SMS; khi muốn xác minh số thật cần quyết định mới về Blaze/chi phí và quy trình đổi số. Chưa tạo Firebase Project thật |
| DEC-007B | `OPEN` | Mã số sinh viên có thu không và xác minh bằng nguồn dữ liệu nào của trường | Chỉ lưu tự khai nếu được duyệt rõ; không gắn nhãn đã xác minh khi chưa có nguồn đối chiếu |
| DEC-008 | `APPROVED`, 2026-10-02 | Khi chụp ảnh chính, tự yêu cầu vị trí foreground gắn ảnh và thời điểm chụp; fix phải đạt ngưỡng; không âm thầm dùng vị trí cũ/lúc lưu; lỗi cho chụp lại hoặc quy trình thủ công được duyệt | Thiết kế snapshot theo ảnh, UI rà soát và kiểm thử nhiều ảnh; **không duyệt dependency hoặc code** |
| DEC-008A | `OPEN` | Chốt sai số tối đa, tuổi fix, chênh thời gian với ảnh, timeout, yêu cầu precise/approximate, nguồn `capturedAt`, quy trình nhập/duyệt vị trí thủ công | Cần chốt trước lập trình GPS và test ranh giới; Fused Location Provider là phương án nghiên cứu |
| DEC-009 | `OPEN` | Màu/biểu trưng của trường và quyền dùng tài nguyên chính thức | Chưa đưa nhận diện thương hiệu trường vào Design System |
| DEC-010 | `OPEN` | Chọn môi trường backend đặc quyền (Cloud Functions hoặc dịch vụ server khác), cách cấp/thu hồi role và scope, kiểm IAM, vùng triển khai | Cần chốt trước IMP-04/06/11; ứng dụng Android không giữ Admin SDK hay tự cấp quyền |
| DEC-011 | `OPEN` | Firebase Project thật, môi trường dev/staging/prod, vùng dữ liệu, ngân sách và người quản trị | ADR-001 chọn công nghệ nhưng chưa cho phép tạo project, tải cấu hình hoặc bật dịch vụ có chi phí |
| DEC-012 | `OPEN` | ML Kit OCR dạng bundled/unbundled, bộ dữ liệu pilot và ngưỡng chấp nhận cho biển một/hai hàng | ADR-002 chỉ phê duyệt công nghệ thử nghiệm; chọn bản phân phối và tiêu chí phát hành sau đo thực địa |
| DEC-013 | `OPEN` | Chứng cứ tối thiểu để xác minh quyền sử dụng xe; người được ủy quyền duyệt; xử lý trùng biển, xe dùng chung và chuyển nhượng | ADR-004 duyệt xác minh thủ công, chưa chốt chính sách xét duyệt chi tiết |

## Phạm vi, lý do và rủi ro của năm ADR đã duyệt

| ADR | Lý do/phạm vi | Tác động và rủi ro cần kiểm soát |
|---|---|---|
| ADR-001 | Dùng dịch vụ Firebase cho danh tính, dữ liệu nghiệp vụ và ảnh để thống nhất nền tảng triển khai; không bao gồm việc tạo project thật | Firestore không có khóa ngoại/unique kiểu SQL; cần thiết kế document claim + transaction/backend đáng tin cậy, Rules cho Firestore/Storage, chi phí đọc/ghi/ảnh và vùng lưu. Không đưa password vào Firestore. |
| ADR-002 | Dùng ML Kit làm công nghệ OCR thử nghiệm trên thiết bị sau detector; con người xác nhận | Chất lượng với biển xe máy hai dòng chưa được chứng minh; cần benchmark và phương án sửa tay, không tự sửa ký tự không chắc chắn. |
| ADR-003 | ADMIN dùng cùng ứng dụng Android, cần luồng/quyền riêng | Bề mặt truy cập dữ liệu nhạy cảm tăng; quyền phải đến từ nguồn tin cậy, Android không tự cấp vai trò. Enum đã có ADMIN, còn cấp quyền/điều hướng phải triển khai ở giai đoạn sau. |
| ADR-004 | Xác minh thủ công để không coi biển số tự khai là chứng cứ sở hữu | Tốn công xét duyệt; cần kiểm trùng nguyên tử và khoảng hiệu lực để không lộ vi phạm cũ cho tài khoản mới. |
| ADR-005 | Online-first giảm phức tạp đồng bộ bản đầu | Mất mạng làm gián đoạn; cache Firestore có thể hiện dữ liệu cũ/ghi cục bộ, UI chỉ công bố thành công khi có xác nhận server, retry idempotent. |

**Phê duyệt các ADR là phê duyệt hướng kiến trúc và tài liệu, chưa phải lệnh triển khai mã nguồn, thêm dependency hoặc cấu hình Firebase Project.**

## Cách chốt một quyết định

1. Nêu vấn đề, phương án thay thế, lợi ích/rủi ro, chi phí/độ phức tạp, phụ thuộc, tiêu chí chọn và tác động dữ liệu/bảo mật.
2. Ghi nhận phê duyệt cụ thể của người dùng. Nếu chỉ duyệt làm tài liệu hoặc khảo sát, không diễn giải thành duyệt triển khai.
3. Cập nhật trạng thái `APPROVED` hoặc `REJECTED`, ngày và lý do; sau đó cập nhật các yêu cầu, luồng, roadmap bị ảnh hưởng. Nếu quyết định mới thay thế quyết định cũ, ghi liên kết hai ID.

## Ràng buộc đầu vào đã được nêu nhưng chưa phải quyết định công nghệ

Ứng dụng Android dùng Java/XML/View System, ViewBinding, hướng MVVM; YOLO11n dùng để **phát hiện vùng biển số**, ML Kit OCR thử đọc ký tự; AI không tự kết luận vi phạm. Ba vai trò sản phẩm đã được duyệt gồm STUDENT, SECURITY_GUARD, ADMIN; [`UserRole.java`](../app/src/main/java/com/example/parkguardai/data/model/UserRole.java) hiện đã chứa cả ba, nhưng chưa có phân quyền vận hành. Xem [tổng quan](00-PROJECT-OVERVIEW.md) và [hiện trạng](13-CURRENT-PROJECT-STATUS.md).
