# Quy tắc làm việc cho AI Coding Agent — ParkGuard AI

> Cập nhật: 2026-10-02. Tài liệu này là điểm vào; yêu cầu chi tiết nằm trong `docs/`. Các phương án trong tài liệu **chưa phải quyết định triển khai** nếu [Decision Log](docs/15-DECISION-LOG.md) chưa ghi là đã duyệt.

## Trước mỗi nhiệm vụ

1. Đọc [hiện trạng có bằng chứng](docs/13-CURRENT-PROJECT-STATUS.md), [yêu cầu sản phẩm](docs/01-PRODUCT-REQUIREMENTS.md), [Decision Log](docs/15-DECISION-LOG.md) và `git status --short --branch`. Đọc mã nguồn thực tế trước khi khẳng định một chức năng đã có.
2. Chọn tài liệu chuyên đề: [vai trò/quyền](docs/02-USER-ROLES-PERMISSIONS.md), [luồng](docs/03-USER-FLOWS.md), [dữ liệu](docs/04-DATABASE-DESIGN.md), [kiến trúc](docs/05-SYSTEM-ARCHITECTURE.md), [AI](docs/06-AI-PIPELINE.md), [UI/UX](docs/07-UI-UX-GUIDELINES.md), [chuẩn code](docs/08-CODING-STANDARDS.md), [bảo mật](docs/09-SECURITY-PRIVACY.md), [test](docs/11-TESTING-STRATEGY.md).
3. Đối chiếu yêu cầu, bằng chứng và quyết định đã duyệt. Phân biệt `IMPLEMENTED` (có mã và bằng chứng), `PARTIAL` (mới một phần), `PLANNED` (yêu cầu đã nêu), `PROPOSED` (phương án bổ sung) và `UNKNOWN` (chưa xác minh). Không biến `PROPOSED` thành cam kết.
4. Nếu nhiệm vụ phụ thuộc quyết định chưa chốt, trình bày phương án, lợi ích, rủi ro, chi phí và điều kiện nghiệm thu trước khi chọn công nghệ hoặc mô hình dữ liệu. Ghi quyết định được người dùng duyệt vào [Decision Log](docs/15-DECISION-LOG.md).

## Quy tắc bắt buộc

- Android dùng **Java**, giao diện **XML/View System**, ViewBinding; tên định danh bằng tiếng Anh, comment/JavaDoc và tài liệu bằng tiếng Việt; chuỗi hiển thị bằng tiếng Việt qua Android String Resources. Không tự chuyển sang Kotlin/Compose, đổi package, nâng dependency hoặc sửa giao diện đã hoàn thành.
- ADR-001..005 đã duyệt Firebase Authentication + Cloud Firestore + Firebase Cloud Storage, thử Google ML Kit Text Recognition, ADMIN trong app Android, xác minh xe thủ công và bản đầu online-first. Mới có model và datasource email Auth; chưa có Firebase Project thật, Firestore/Storage, Rules hoặc luồng đăng ký hoàn chỉnh. DEC-007 chọn email xác minh trên Spark, số điện thoại liên hệ tùy chọn **chưa xác minh**; không thêm SMS OTP/Phone Auth khi chỉ dùng Spark. Không tự tạo Firebase Project, file cấu hình thật hoặc service account. Xem [Decision Log](docs/15-DECISION-LOG.md).
- Giữ Activity/Fragment tập trung vào UI; ViewModel, repository, datasource, AI, camera và location có trách nhiệm riêng khi có nhu cầu rõ. Không thêm lớp trừu tượng chỉ để đủ sơ đồ. Xem [kiến trúc](docs/05-SYSTEM-ARCHITECTURE.md) và [chuẩn code](docs/08-CODING-STANDARDS.md).
- Mọi truy cập dữ liệu phải được kiểm tra quyền ở backend/tầng dữ liệu; không tin vai trò hay `ownerId` do thiết bị gửi. Không để AI tự kết luận vi phạm hoặc tự xác định chủ xe theo biển số. Sinh viên chỉ xem dữ liệu được xác minh thuộc phạm vi của mình. Xem [ma trận quyền](docs/02-USER-ROLES-PERMISSIONS.md) và [bảo mật](docs/09-SECURITY-PRIVACY.md).
- Khi bảo vệ chụp **ảnh chính** của vi phạm, ghi thời điểm chụp và lấy vị trí tự động gắn với chính ảnh đó; chỉ chấp nhận fix đạt ngưỡng thời gian/độ chính xác đã duyệt. Không dùng vị trí cũ, vị trí lúc lưu hồ sơ hoặc vị trí của ảnh bổ sung để âm thầm thay thế. Thiếu vị trí phải cảnh báo và theo luồng chụp lại/thủ công được duyệt. Xem [yêu cầu](docs/01-PRODUCT-REQUIREMENTS.md), [dữ liệu](docs/04-DATABASE-DESIGN.md), [quyết định](docs/15-DECISION-LOG.md).
- Không lưu mật khẩu thuần, không log dữ liệu cá nhân/biển số/token/ảnh, không đưa bí mật lên Git. Dùng HTTPS, giới hạn quyền truy cập ảnh, kiểm tra backup và audit hành động nhạy cảm.
- Trước khi tích hợp model, đo trực tiếp kiểu/kích thước tensor, phép chuẩn hóa, output và thời gian suy luận; asset hiện có **chỉ phát hiện vùng biển số theo thông tin dự án**, OCR là bước riêng. Xem [AI pipeline](docs/06-AI-PIPELINE.md).
- Chạy kiểm tra phù hợp và báo cáo kết quả thực; nếu môi trường thiếu Java/SDK/thiết bị, ghi `UNKNOWN`, không tuyên bố build thành công. Xem [test](docs/11-TESTING-STRATEGY.md).
- Không tự chạy `git add`, `commit`, `push`, `merge`, `rebase`, `reset`, đổi/xóa nhánh hay force push. Trước đề xuất commit phải xem status, diff, file nhạy cảm và kết quả test; chờ người dùng duyệt. Xem [Git workflow](docs/10-GIT-WORKFLOW.md).

## Quy trình thay đổi

Làm rõ mục tiêu và vai trò → kiểm tra code và tài liệu → nêu phương án khi còn quyết định mở → xác nhận phạm vi cần phê duyệt → sửa đúng phạm vi → kiểm thử → đối chiếu quyền riêng tư/đường dẫn/tài liệu → báo cáo file, kết quả và giới hạn. Với màn hình mới, trình bày wireframe và đối chiếu [UI/UX](docs/07-UI-UX-GUIDELINES.md) trước khi triển khai theo yêu cầu dự án.

[Tổng quan](docs/00-PROJECT-OVERVIEW.md) · [Nghiên cứu](docs/14-RESEARCH-RECOMMENDATIONS.md) · [Roadmap](docs/12-DEVELOPMENT-ROADMAP.md) · [Checklist task A–Z](docs/16-END-TO-END-TASK-PLAN.md)
