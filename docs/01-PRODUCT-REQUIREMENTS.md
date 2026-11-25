# 01 — Yêu cầu sản phẩm

> Đây là đặc tả mục tiêu `PLANNED`, không phải báo cáo tính năng đã triển khai. Hiện trạng tại [13](13-CURRENT-PROJECT-STATUS.md); quyết định mở tại [15](15-DECISION-LOG.md). Mức P0 = cần cho thử nghiệm nghiệp vụ an toàn; P1 = cần trước triển khai rộng; P2 = mở rộng. Chức năng được người dùng đề xuất thêm ghi `PROPOSED`.

## Nguyên tắc nghiệp vụ

- Bảo vệ là người quan sát và xác nhận hành vi đỗ sai. YOLO11n chỉ gợi ý vị trí biển số; OCR chỉ gợi ý ký tự. Hồ sơ chưa được xác nhận không trở thành kết luận cuối.
- Mỗi sinh viên có thể liên kết nhiều phương tiện. Biển số trùng không được cấp quyền xem chéo; liên kết phải có quá trình xác minh và khoảng thời gian hiệu lực. Vi phạm có thể liên quan phương tiện chưa đăng ký và không vì vậy bị gán tự động cho sinh viên.
- Thông tin định danh và ảnh bằng chứng được xử lý theo mục đích rõ ràng, phân quyền và hạn lưu giữ được duyệt. Hệ thống phải hỗ trợ truy nguyên ai đã tạo/sửa/xác nhận hồ sơ.

## Đăng ký tài khoản: loại dữ liệu và kiểm tra

| Trường | Phân loại mục tiêu | Kiểm tra/ghi chú |
|---|---|---|
| Họ và tên | Bắt buộc, tự khai; chưa xác minh danh tính | Không suy ra tên pháp lý; chặn chuỗi rỗng, giới hạn độ dài do thiết kế chi tiết quyết định |
| Email | Bắt buộc, cần xác minh quyền truy cập | Chuẩn hóa để chống trùng theo chính sách; xác minh trước tính năng cá nhân nhạy cảm |
| Số điện thoại | Tùy chọn theo DEC-007, chỉ để liên hệ; **chưa xác minh** trên bản Spark | Nếu cung cấp thì kiểm định dạng trước lưu; hiển thị rõ chưa xác minh, không dùng để đăng nhập, khôi phục tài khoản, chống trùng định danh hay chứng minh quyền xe |
| Mật khẩu + nhập lại | Bắt buộc nếu chọn đăng nhập mật khẩu; chỉ đi qua dịch vụ xác thực | So khớp ở form, chính sách mật khẩu theo nhà cung cấp; không ghi vào model/log/database ứng dụng |
| Mã số sinh viên | Chưa thu ở phiên bản hiện tại theo DEC-007B | Nếu cần sau này, chốt nguồn đối chiếu và quy trình trước khi thêm trường |
| Phương tiện ban đầu | Bắt buộc **ít nhất một yêu cầu đăng ký xe để hoàn tất onboarding** theo ADR-004, sau bước xác minh đăng nhập | Tài khoản có thể tạm thời chưa có xe khi đang dở quy trình; về sau thêm nhiều xe, từng xe xác minh thủ công |
| Đồng ý xử lý dữ liệu | Bắt buộc trước thu thập vượt dữ liệu cần cho xác thực | Ghi phiên bản thông báo, thời điểm, mục đích; thiết kế cách rút/yêu cầu xử lý theo chính sách đã duyệt |

## Yêu cầu chức năng có thể nghiệm thu

| ID | Mô tả; vai trò; ưu tiên | Tiêu chí chấp nhận |
|---|---|---|
| AUTH-01 | Đăng ký sinh viên qua Firebase Authentication; STUDENT; P0 | Form kiểm tra từng trường, mật khẩu trùng, điều khoản; Auth tạo danh tính, Firestore lưu hồ sơ không có password; client không cấp vai trò bảo vệ/admin; lỗi tạo Auth nhưng chưa tạo profile phải có trạng thái phục hồi rõ. |
| AUTH-02 | Đăng nhập/đăng xuất qua Firebase Authentication; mọi vai trò; P0 | Đăng nhập sai báo lỗi an toàn; đăng xuất làm mất quyền đọc dữ liệu cá nhân trên thiết bị/cache theo chính sách; Firestore/Storage Rules và nghiệp vụ đặc quyền kiểm quyền ở nguồn dữ liệu. |
| AUTH-03 | Xác minh email Firebase và phục hồi mật khẩu; STUDENT; P0 | Tài khoản chưa xác minh email chưa qua bước đăng ký xe; link/token hết hạn được xử lý; phục hồi không lộ email có tồn tại hay không nếu có nguy cơ dò tài khoản. Không có SMS OTP trong bản Spark. |
| AUTH-04 | Thay số điện thoại liên hệ tùy chọn; STUDENT; P1 | Chỉ tài khoản đã đăng nhập được yêu cầu thay số; kiểm định dạng, ghi audit và thay kênh liên hệ theo chính sách. Số mới vẫn có nhãn **chưa xác minh**, không mở quyền đăng nhập/khôi phục hay xem hồ sơ. |
| AUTH-05 | Chống trùng và lạm dụng đăng ký; hệ thống; P0 | Email/định danh đã liên kết được xử lý không tạo tài khoản trùng; rate limit và kiểm soát đăng ký hàng loạt có test; thông điệp không tiết lộ dữ liệu người khác. |
| AUTH-06 | Vô hiệu hóa tài khoản và yêu cầu xóa dữ liệu; STUDENT/ADMIN theo quy trình; P1 | Trạng thái chặn phiên mới; xử lý hồ sơ còn nghĩa vụ lưu giữ theo chính sách; có xác nhận và audit, không xóa bằng chứng tùy tiện. |
| VEH-01 | Thêm, xem, cập nhật nhiều phương tiện; STUDENT; P0 | Một sinh viên tạo ít nhất hai bản ghi xe khác nhau; chỉ xem/sửa bản ghi mình được phép; sửa biển số tạo quy trình xác minh lại. |
| VEH-02 | Xác minh quyền sử dụng xe; người phê duyệt được ủy quyền; P0 | Trạng thái `PENDING/VERIFIED/REJECTED/INACTIVE`; có người, thời gian, lý do và bằng chứng phê duyệt; sinh viên không tự đặt `VERIFIED`. |
| VEH-03 | Ràng buộc biển số trùng; hệ thống/người xét duyệt; P0 | Hai yêu cầu cùng biển số được phát hiện; không tự hợp nhất hai tài khoản; xung đột chặn việc cấp quyền xem hồ sơ cho đến khi xét duyệt. |
| VEH-04 | Chuyển nhượng/thay xe; STUDENT/người xét duyệt; P1 | Đóng liên kết cũ bằng `validTo`, tạo liên kết mới với `validFrom`; hồ sơ trước thời điểm mới không tự hiển thị cho người mới; audit giữ nguyên lịch sử. |
| VEH-05 | Biển số chưa đăng ký hoặc OCR sai; GUARD/hệ thống; P0 | Bảo vệ có thể ghi nhận xe chưa xác định với `vehicleId` rỗng và biển số đã rà; không gán chủ chỉ qua OCR; nhập sửa lưu giá trị gợi ý và giá trị cuối để audit. |
| VEH-06 | Hoàn tất onboarding bằng ít nhất một xe; STUDENT; P0 | Sau xác minh đăng nhập, màn hình dẫn đến đăng ký ≥1 xe, tạo `vehicleVerificationRequest` trạng thái `PENDING`; việc có yêu cầu không cấp quyền xem hồ sơ; có thể thêm xe thứ hai trở lên bằng cùng quy trình. |
| GUA-01 | Chụp ảnh hiện trường; SECURITY_GUARD; P0 | Xin quyền camera lúc dùng; từ chối quyền có hướng dẫn và đường đi hợp lệ; ảnh có thể xem lại trước lưu. |
| GUA-02 | YOLO11n, crop, ML Kit Text Recognition, sửa tay; SECURITY_GUARD; P0 | Không có/đa biển số/ảnh mờ/OCR không hợp lệ đều có trạng thái riêng; bảo vệ chọn biển số đúng hoặc nhập tay; không tự sửa ký tự không chắc chắn hoặc kết luận vi phạm. |
| GUA-03 | Tạo hồ sơ vi phạm; SECURITY_GUARD; P0 | Bắt buộc ảnh gốc, loại vi phạm, thời điểm chụp, người ghi nhận, biển số đã rà hoặc lý do không có; vị trí của ảnh chính đạt điều kiện hoặc trạng thái/lý do xử lý ngoại lệ đã duyệt; màn hình rà soát toàn bộ trước xác nhận. |
| GUA-04 | Lưu vào Firestore/Storage và xem lịch sử trong phạm vi; SECURITY_GUARD; P0 | Không báo thành công khi mới có ghi cache hoặc upload ảnh riêng lẻ; chỉ công bố hồ sơ sau xác nhận server phù hợp, thử lại không tạo trùng; Rules/nguồn tin cậy hạn chế guard theo phạm vi. |
| GUA-05 | Online-first khi thoát/mất mạng; SECURITY_GUARD; P0 | Hiện offline, giữ dữ liệu form trong phiên nếu an toàn, không tự xây queue đồng bộ phiên bản đầu; thoát trước xác nhận hỏi hủy/giữ theo chính sách; không báo hồ sơ đã lưu server khi offline. Offline nâng cao là giai đoạn sau. |
| GUA-06 | Tự động gắn vị trí với ảnh chính khi chụp; SECURITY_GUARD; P0 | Khi chụp ảnh chính, ứng dụng ghi `capturedAt` và yêu cầu một vị trí foreground; chỉ nhận fix có thời điểm đo, sai số ngang và khoảng lệch với thời điểm chụp đạt ngưỡng đã duyệt. Lưu `latitude`, `longitude`, `locationAccuracy`, `locationTimestamp`, `locationSource`, `locationStatus` cùng liên kết ảnh/hồ sơ; không dùng vị trí cũ hoặc vị trí lúc lưu để thay thế. |
| GUA-07 | Phục hồi khi không có vị trí hợp lệ; SECURITY_GUARD; P0 | Từ chối quyền, tắt định vị, timeout, fix cũ/sai số lớn đều hiển thị lý do và cho thử lấy lại, chụp lại hoặc vị trí thủ công theo quy trình được duyệt. Vị trí thủ công mang nguồn/trạng thái riêng, không hiển thị là GPS tự động. |
| GUA-08 | Giữ vị trí ảnh chính khi có nhiều ảnh; SECURITY_GUARD; P0 | Hồ sơ có `primaryEvidenceId` rõ ràng; thêm ảnh bằng chứng chỉ tạo metadata riêng, không ghi đè `capturedAt`/vị trí ban đầu. Đổi ảnh chính phải là hành động tường minh, rà lại vị trí và có audit nếu hồ sơ đã lưu. |
| STU-01 | Xem hồ sơ cá nhân/xác minh xe; STUDENT; P0 | Chỉ đọc dữ liệu mình; thể hiện rõ xe đang chờ, bị từ chối, hết hiệu lực; không cho tra cứu hồ sơ của người khác bằng biển số tùy ý. |
| STU-02 | Tra cứu và xem hồ sơ vi phạm liên quan; STUDENT; P0 | Chỉ hiển thị hồ sơ theo quan hệ đã xác minh và thời gian hiệu lực, hoặc quyết định cấp quyền thủ công; gồm ảnh, thời gian, khu vực/vị trí được phép, trạng thái; truy cập trực tiếp URL/ID người khác bị từ chối. |
| STU-03 | Theo dõi trạng thái xử lý; STUDENT; P1 | Hiển thị trạng thái hiện tại và thời điểm cập nhật; khi bị sửa có audit; không trình bày `PENDING` như kết luận đã xác nhận. |
| ADM-01 | Quản lý tài khoản/quyền trong app Android; ADMIN; P0 | Chỉ người được ủy quyền có thể cấp/thu quyền qua nguồn tin cậy; người dùng không tự nâng vai trò; thay đổi có audit và kiểm tra lại quyền phiên; UI ADMIN riêng. |
| ADM-02 | Duyệt phương tiện/hồ sơ trong app Android; ADMIN hoặc nhân sự được ủy quyền; P0 | Xác minh xe thủ công sau kiểm trùng biển số và chứng cứ tối thiểu; lưu người/thời gian/lý do, không cho sinh viên tự đặt `VERIFIED`; hồ sơ cũ không cấp quyền cho chủ mới; danh mục vi phạm có version. |
| ADM-03 | Thống kê/cấu hình; ADMIN; P1 | Báo cáo theo tổng hợp khi có thể, không mặc định xuất PII; thay cấu hình có audit và quyền riêng. |
| EXT-01 | Yêu cầu xem xét lại hồ sơ; STUDENT/nhân sự xử lý; P2 `PROPOSED` | Nếu được duyệt: nộp một yêu cầu có lý do/bằng chứng, trạng thái phản hồi, không sửa bản gốc; thời hạn phản hồi do chính sách quyết định. |

`ADMIN` trong ứng dụng Android đã được duyệt theo ADR-003. `ADM-01` (quyền) và `ADM-02` (duyệt xe/hồ sơ) là năng lực bắt buộc; `ADM-03` (thống kê và cấu hình nâng cao) là mở rộng P1. Mọi quyền quản trị cần thiết kế chi tiết theo [ma trận](02-USER-ROLES-PERMISSIONS.md).

## Yêu cầu phi chức năng

Trong bảng: S = `STUDENT`, G = `SECURITY_GUARD`, A = `ADMIN` dự kiến; “hệ thống” là phần cần được kiểm ở nguồn dữ liệu/backend, không phải một vai trò đăng nhập.

| ID; mô tả | Vai trò | Ưu tiên | Tiêu chí chấp nhận |
|---|---|---|---|
| NFR-01 Bảo mật | Mọi vai trò/hệ thống | P0 | Test truy cập trái quyền cho từng vai trò, bản ghi, ảnh, cả khi sửa request; mặc định từ chối; không có quy tắc đọc/ghi công khai. |
| NFR-02 Riêng tư | Mọi vai trò/hệ thống | P0 | Có thông báo mục đích xử lý, giới hạn dữ liệu thu, thời hạn giữ/xóa được duyệt, không log PII; backup/cache được rà soát. |
| NFR-03 Toàn vẹn | G, A/hệ thống | P0 | Lưu ảnh và hồ sơ có trạng thái nhất quán; không nhân đôi sau retry; audit tạo/sửa/xóa trạng thái; đồng bộ không đổi lịch sử âm thầm. |
| NFR-04 Khả dụng | S, G, A | P0 | Mọi màn hình dữ liệu có loading/empty/error/offline; camera có đường nhập thủ công khi AI thất bại; form báo lỗi tại trường. |
| NFR-05 Hiệu năng | G/hệ thống | P1 | Đo latency, bộ nhớ, pin, nhiệt và tỷ lệ lỗi trên thiết bị mục tiêu; ngưỡng chấp nhận phải chốt từ khảo sát thực địa, không lấy chỉ số test model làm SLA. |
| NFR-06 Khả năng tiếp cận | S, G, A | P1 | Nội dung đọc được khi tăng cỡ chữ, có nhãn TalkBack, tương phản và vùng chạm đạt tiêu chí Design System sau kiểm thử. |
| NFR-07 Bảo trì/kiểm thử | Nhóm phát triển/hệ thống | P1 | Logic ràng buộc biển số, quyền, trạng thái và retry có unit/integration test; pipeline AI có bộ ảnh kiểm định; các test còn lại theo [chiến lược](11-TESTING-STRATEGY.md). |
| NFR-08 Mở rộng | Hệ thống/A | P1 | Thiết kế truy vấn phân trang theo vai trò và thời gian; chính sách chỉ mục, quota/cost, lưu ảnh tách metadata; kiểm thử tải sau khi chọn backend. |
| NFR-09 Chất lượng vị trí | G/hệ thống | P0 | Có cấu hình/ngưỡng được duyệt cho sai số, tuổi fix, chênh thời gian so với ảnh và timeout; test ranh giới, thiết bị di chuyển, nhiều ảnh; không chấp nhận fix không có độ chính xác hoặc thời điểm đo hợp lệ. |

## Điểm chưa quyết định

Chứng cứ tối thiểu/quyền duyệt xe chi tiết; hồ sơ `PENDING` có hiển thị sinh viên không; **ngưỡng sai số, tuổi fix, chênh thời gian, timeout, yêu cầu quyền precise và quy trình vị trí thủ công**; thời hạn giữ ảnh/vị trí/audit; ML Kit bundled hay unbundled; môi trường backend đáng tin cậy cho nghiệp vụ đặc quyền. Email xác minh và phone liên hệ tùy chọn đã được chọn cho bản Spark; mã sinh viên chưa thu theo DEC-007B; Fused Location Provider còn là phương án kỹ thuật GPS. Xem [Decision Log](15-DECISION-LOG.md).
