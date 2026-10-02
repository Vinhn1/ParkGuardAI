# 02 — Vai trò và phân quyền

> **Thiết kế mục tiêu `PLANNED`**, chưa có RBAC chạy trong repository. ADR-003 đã duyệt `ADMIN` trong app Android cùng `STUDENT` và `SECURITY_GUARD`; [`UserRole.java`](../app/src/main/java/com/example/parkguardai/data/model/UserRole.java) đã có ba giá trị enum nhưng chưa có cấp quyền hoặc điều hướng. Quyền phải thực thi bằng Firebase Security Rules hoặc nghiệp vụ backend đáng tin cậy, không dựa vào việc ẩn nút trên Android.

## Nguyên tắc kiểm soát

Định danh đến từ Firebase Authentication; quyền ADMIN/GUARD và phạm vi phải đến từ nguồn đáng tin (ví dụ custom claims chỉ do Admin SDK phía server đặt và/hoặc access grant riêng), không từ trường `role` do Android tự ghi. Dữ liệu Android gửi (`role`, `ownerId`, `guardId`, `status`) chỉ là yêu cầu, không phải bằng chứng quyền. Mặc định từ chối. Firestore Rules cần kiểm **get/list/create/update/delete**; Storage Rules kiểm **upload/read/delete**; nghiệp vụ đặc quyền như cấp vai trò, claim biển số và duyệt xe cần transaction/backend đáng tin cậy khi Rules không diễn đạt đủ. Ghi audit với tác nhân, mục tiêu, trước/sau, thời gian, lý do và kết quả, tránh lưu token/ảnh trong log. Quản trị chỉ xem PII khi nhiệm vụ cụ thể yêu cầu. [Firebase custom claims](https://firebase.google.com/docs/auth/admin/custom-claims), [Firestore Rules queries](https://firebase.google.com/docs/firestore/security/rules-query).

Ký hiệu: `S` sinh viên, `G` bảo vệ, `A` quản trị được ủy quyền; `—` không quyền. “Duyệt” luôn là quyền riêng với “sửa”. Mọi dòng bên dưới là phương án quyền cần phê duyệt trước triển khai.

| Thao tác/dữ liệu | Ai thực hiện và điều kiện truy cập | Ai sửa | Ai duyệt | Audit |
|---|---|---|---|---|
| Đăng ký tài khoản sinh viên | Khách chưa đăng nhập tạo danh tính Firebase Auth; profile Firestore `users/{auth.uid}` chỉ cho chính mình theo trường được phép, vai trò S cố định ở nguồn quyền | S sửa hồ sơ tự khai sau xác thực; trường xác minh chỉ qua quy trình | Không cấp G/A qua form | Tạo, xác minh, đổi định danh |
| Xem/sửa hồ sơ cá nhân | S chỉ hồ sơ `auth.uid` của mình; G/A chỉ phần tối thiểu để xử lý tác vụ được giao | S sửa tên/liên hệ theo quy trình; A chỉ khi có nhiệm vụ | Đổi vai trò cần người có quyền cấp riêng | Có khi thay đổi |
| Cấp/thu vai trò và phạm vi G/A | A có quyền quản lý nhân sự, kiểm tra người nhận và phạm vi | A được ủy quyền | Người/phòng ban có quyền phê duyệt theo chính sách; không tự duyệt nếu yêu cầu tách nhiệm vụ | Bắt buộc, gồm trước/sau |
| Tạo/yêu cầu liên kết phương tiện | S đã xác minh đăng nhập, đăng ký ≥1 xe để hoàn tất onboarding; tạo yêu cầu `PENDING`; kiểm trùng biển số mà không lộ chủ khác | S sửa thông tin tự khai chưa duyệt; sửa biển số sau duyệt tạo yêu cầu mới | A hoặc nhân sự xác minh xe được ủy quyền | Bắt buộc |
| Xem danh sách/chi tiết phương tiện | S chỉ xe có liên kết tới mình; G chỉ thông tin tối thiểu phục vụ ghi nhận; A theo nhiệm vụ xác minh | S sửa trường cho phép; người duyệt sửa trạng thái/lý do | Người xác minh xe | Có khi sửa/xem nhạy cảm theo chính sách |
| Duyệt/từ chối/chấm dứt liên kết | A trong app Android hoặc nhân sự được cấp quyền và đúng phạm vi; thao tác đặc quyền kiểm ở nguồn tin cậy | Chỉ người được cấp quyền; S không ghi status | Quyền duyệt tách quyền tạo yêu cầu | Bắt buộc |
| Chụp ảnh, chạy AI/OCR cục bộ | G đã đăng nhập và được phân công; ảnh trong bản nháp của G | G chỉnh biển số gợi ý trước gửi | G xác nhận nội dung hồ sơ, AI không duyệt | Ghi khi nộp và khi thay đổi kết quả gợi ý |
| Gắn/điều chỉnh vị trí ảnh chính | G được phân công chỉ khi đang ghi nhận; tọa độ tự động lấy lúc chụp ảnh chính | G chỉ thử lại/chụp lại hoặc đề xuất vị trí thủ công; không sửa tọa độ tự động thành GPS hợp lệ | Người/phạm vi được ủy quyền duyệt ngoại lệ thủ công theo DEC-008A | Bắt buộc khi nhập/chỉnh thủ công, đổi ảnh chính hoặc duyệt ngoại lệ |
| Tạo hồ sơ vi phạm | G trong phạm vi khu vực/ca; loại vi phạm hợp lệ, bằng chứng, thời gian | G sửa bản nháp; sau xác nhận chỉ theo quy trình có lý do | G hoặc người giám sát tùy chính sách còn mở | Bắt buộc |
| Xem lịch sử/chi tiết vi phạm | G chỉ hồ sơ được phân công; S chỉ hồ sơ được liên kết qua xe đã xác minh, đúng khoảng thời gian và trạng thái được công bố; A theo nhiệm vụ | Người có quyền sửa hồ sơ theo quy trình | Người có quyền xác nhận/xử lý | Bắt buộc với sửa; xem PII nhạy cảm theo chính sách |
| Xem ảnh bằng chứng | Cùng quyền hồ sơ và quyền file; URL ngắn hạn hoặc truy cập qua endpoint kiểm quyền | Không sửa ảnh gốc; thay thế chỉ bằng bản mới kèm lý do | Người có quyền rà hồ sơ | Tải/xuất ảnh nhạy cảm nên ghi |
| Đổi trạng thái/xử lý hồ sơ | G/A được cấp quyền, chuyển trạng thái hợp lệ, đúng phạm vi | Người xử lý được phân công | Người có quyền cuối theo chính sách | Bắt buộc |
| Quản lý danh mục vi phạm | A được cấp quyền cấu hình | A | Người phê duyệt cấu hình nếu áp dụng | Bắt buộc; giữ version cũ |
| Thống kê/báo cáo | A có quyền báo cáo; ưu tiên dữ liệu tổng hợp, lọc phạm vi | Không sửa dữ liệu gốc qua báo cáo | Xuất dữ liệu chi tiết cần cấp quyền riêng | Bắt buộc khi export |
| Cấu hình hệ thống/chính sách lưu giữ | A có quyền cấu hình riêng | A theo phân công | Người có quyền chính sách | Bắt buộc |
| Gửi/yêu cầu xem xét lại | S trên hồ sơ được xem; `PROPOSED` | S sửa yêu cầu trước nộp; nhân sự xử lý phản hồi | Người xử lý độc lập theo chính sách | Bắt buộc |
| Xóa/vô hiệu hóa tài khoản, dữ liệu | S yêu cầu đối với bản thân; A xử lý theo chính sách lưu giữ và nghĩa vụ liên quan | Người xử lý dữ liệu được cấp quyền | Cần phê duyệt theo chính sách | Bắt buộc |

## Ánh xạ quyền sang Firebase đã chọn

- `users/{uid}`: sinh viên chỉ đọc/sửa các trường profile tự khai của chính mình; dữ liệu vai trò, `emailVerified`, `phoneVerified` và phạm vi không được tin nếu do client ghi. PII riêng cần tách document để admin không mặc định đọc toàn bộ.
- `vehicleVerificationRequests`: S tạo yêu cầu cho chính mình với `PENDING` cố định, không sửa `reviewerId`, `status`, `reviewedAt`; A/nhân sự có grant chỉ xem trong phạm vi và duyệt qua nghiệp vụ đáng tin cậy. `plateClaims` không cho client đọc/ghi.
- `vehicles` và `vehicleLinks`: S xem phần dữ liệu xe/liên kết của mình; không tự cấp `VERIFIED`, `validFrom` hay đổi chủ. G chỉ xem phần cần cho ghi nhận, không xem hồ sơ sinh viên không liên quan.
- `violations`/`evidence`: G tạo trong phạm vi được giao, `guardId` lấy từ phiên; S chỉ xem qua liên kết `VERIFIED` và thời gian hiệu lực. Truy vấn phải tương thích Rules vì Rules **không lọc hộ**; nếu quan hệ thời gian quá phức tạp, dùng backend trả dữ liệu đã phân quyền hoặc projection được kiểm soát. Storage path và metadata không thay quyền Firestore; test riêng Storage Rules.
- `auditLogs` và `accessGrants`: client không được tự ghi/sửa/xóa dữ liệu quyền hoặc audit tin cậy; A chỉ xem bản ghi được cấp quyền. Custom claims chỉ được đặt từ môi trường đặc quyền, cần xử lý độ trễ refresh token khi thu quyền.

## Ràng buộc quan hệ và thời gian

1. `vehicleId` trong [`Violation.java`](../app/src/main/java/com/example/parkguardai/data/model/Violation.java) là trường dữ liệu hiện có, **chưa có** kiểm tra khóa ngoại hoặc quyền xem. Thiết kế đề xuất dùng bản ghi liên kết `VehicleLink` có `validFrom`, `validTo`, `verificationStatus` và căn cứ duyệt.
2. S chỉ nhận quyền xem hồ sơ khi liên kết đã `VERIFIED`, `occurredAt` thuộc khoảng hiệu lực và trạng thái hồ sơ được phép công bố. Trường hợp xe chưa đăng ký hoặc tranh chấp: chưa cấp quyền tự động; nhân sự được ủy quyền xử lý thủ công với audit.
3. Biển số chuẩn hóa để tìm ứng viên, không dùng như khóa quyền. Kết quả OCR, số điện thoại và mã sinh viên tự khai cũng không chứng minh một mình quyền xem hồ sơ.
4. Khi chuyển nhượng, không đổi `ownerId` trực tiếp rồi làm lịch sử cũ chuyển sang người mới. Cần đóng liên kết cũ và tạo liên kết mới. Quy tắc chính xác phụ thuộc DEC-004.

## Trường hợp kiểm thử bắt buộc

S-A đọc ID hồ sơ/URL ảnh của S-B; S tự sửa `role`, `ownerId`, `VERIFIED`; G ngoài khu vực đọc/sửa hồ sơ; A thiếu quyền xuất xem ảnh nhạy cảm; người dùng chưa đăng nhập đọc danh sách; chủ mới thử xem vi phạm cũ; token cũ sau thu quyền; truy vấn danh sách lọc sai; truy cập file ảnh trực tiếp; request offline được gửi lại sau khi quyền đã bị thu. Mọi trường hợp trái quyền phải bị từ chối tại nguồn dữ liệu, không chỉ ở giao diện. Xem [Testing](11-TESTING-STRATEGY.md), [Security](09-SECURITY-PRIVACY.md).
