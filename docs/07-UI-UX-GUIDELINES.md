# 07 — Quy tắc UI/UX và Design System

> Giao diện hiện tại chỉ có NavHost và theme Material3 nền; [`colors.xml`](../app/src/main/res/values/colors.xml) mới có đen/trắng, [`strings.xml`](../app/src/main/res/values/strings.xml) mới có tên app. Các token dưới đây là **quy tắc đề xuất**, chưa phải màu/nhận diện chính thức của Trường Đại học Cửu Long. Không thay giao diện đã hoàn thành khi chưa duyệt.

## Nguyên tắc và quy trình duyệt màn hình

Dùng Java + XML/View System, ViewBinding và Navigation Component hiện có. Với từng màn hình: xác định mục tiêu, vai trò, dữ liệu vào/ra và quyền → viết luồng chính/lỗi → wireframe → đối chiếu Design System và khả năng tiếp cận → trình bày phương án → **chỉ triển khai sau phê duyệt**. Tái dùng component/resource có sẵn khi phù hợp; không dùng logo, màu hay ảnh trường như tài nguyên chính thức khi chưa có nguồn/quyền dùng. Ảnh minh họa phải ghi rõ là minh họa.

## Token và thành phần đề xuất

| Thành phần | Quy tắc |
|---|---|
| Màu | Đặt token theo vai trò `primary`, `onPrimary`, `surface`, `onSurface`, `outline`, `error`, `success`, `warning`, trạng thái tối/sáng; chọn palette sau khi có yêu cầu thương hiệu. Không chỉ dựa màu để báo trạng thái. |
| Typography | Dùng hệ thống Material 3 với cấp `headline`, `title`, `body`, `label`; ưu tiên `sp`, font scaling, tiếng Việt có dấu đầy đủ; số biển số dùng kiểu dễ phân biệt ký tự. |
| Spacing | Lưới 4dp; khoảng cách thường 8/16/24dp; không đặt nút sát mép hoặc phần camera che nội dung. Token tập trung trong resource, không rải giá trị tùy ý. |
| Corner/elevation | 8–16dp cho card/dialog tùy vai trò, elevation ít mức; tránh bóng/animation làm mất tập trung khi ghi nhận hiện trường. Giá trị chốt sau wireframe. |
| Iconography | Ưu tiên icon nhất quán kiểu Material; có nhãn hoặc contentDescription khi cần, không dùng biểu tượng độc quyền chưa có phép. |
| Button | Một hành động chính rõ mỗi bước; hành động hủy/xóa có xác nhận; trạng thái disabled/loading rõ, không cho gửi lặp. Vùng chạm tối thiểu 48dp theo [Android Accessibility for Views](https://developer.android.com/guide/topics/ui/accessibility/views/apps-views). |
| Text field | Nhãn luôn hiện, kiểu bàn phím phù hợp, ví dụ định dạng, lỗi cạnh trường và hướng khắc phục; không xóa dữ liệu hợp lệ sau lỗi. Mật khẩu có hiển thị/ẩn, không hiển thị giá trị nhập lại trong log. |
| Card/dialog | Card thể hiện một hồ sơ/xe với status và thời gian; dialog dành cho xác nhận rủi ro hoặc mất bản nháp, không thay màn hình phức tạp. |
| Navigation | Luồng theo vai trò; màn hình chi tiết giữ đường quay lại; điều hướng không được thay thế kiểm quyền server. |

## Khung màn hình theo vai trò

- **Sinh viên:** onboarding/đăng ký → xác minh email → danh sách xe và trạng thái → thêm xe → hồ sơ vi phạm được phép xem → chi tiết/bằng chứng. Số điện thoại tùy chọn nếu nhập phải có nhãn “Chưa xác minh”, không hiển thị nút gửi SMS OTP trên Spark. Trạng thái xe `PENDING/VERIFIED/REJECTED/INACTIVE` phải diễn đạt bằng tiếng Việt, có giải thích lý do/việc tiếp theo.
- **Bảo vệ:** đăng nhập → tác vụ trong ca/phạm vi → CameraX với hướng dẫn “chụp rõ toàn xe và biển số” và giải thích quyền vị trí → chụp **ảnh chính**, tự ghi thời điểm/lấy vị trí → xem trạng thái vị trí → xem vùng gợi ý/OCR và sửa tay → chọn loại vi phạm/ảnh bổ sung → rà soát **vị trí của ảnh chính** → xác nhận → lịch sử. Camera có lối chụp lại/nhập tay biển số; hồ sơ chưa gửi và đã lưu server dùng nhãn khác nhau.

### Khối vị trí trên màn hình lập hồ sơ

Hiển thị tọa độ hoặc địa chỉ **chỉ khi có nguồn địa chỉ đáng tin** (địa chỉ là diễn giải của tọa độ, không thay tọa độ gốc); thời điểm đo `locationTimestamp`, thời điểm chụp `capturedAt`, sai số ngang ước tính bằng mét, `locationSource`, `locationStatus` và nhãn “Vị trí tự động” hoặc “Vị trí thủ công chờ duyệt/đã duyệt”. Không yêu cầu bảo vệ nhập tọa độ khi fix hợp lệ. Nếu chỉ có vị trí gần đúng, sai số lớn, fix cũ, hết thời gian, dịch vụ tắt hoặc thiếu quyền: nêu nguyên nhân và cho “Thử lấy lại vị trí”, “Chụp lại ảnh chính” hoặc “Ghi vị trí thủ công” **nếu quy trình này được duyệt**. Nút lưu không được ngầm xem vị trí lỗi là thành công; thao tác lưu ngoại lệ phải có lý do/trạng thái rõ.

Khi thêm ảnh bằng chứng, giao diện đánh dấu rõ ảnh nào là **ảnh chính** và giữ vị trí của ảnh đó. Đổi ảnh chính yêu cầu xác nhận, hiển thị ảnh/vị trí mới và cảnh báo nếu ảnh mới không có fix hợp lệ. Không hiển thị vị trí tại thời điểm lưu như thể đó là nơi chụp.
- **Quản trị:** ADMIN đã được duyệt **trong ứng dụng Android**; có dashboard, hàng chờ xác minh/hồ sơ/danh mục/thống kê/audit theo quyền riêng; ẩn PII không cần thiết; hành động duyệt/từ chối có lý do. Giao diện riêng không thay Security Rules.

## Danh mục màn hình mục tiêu theo vai trò

Đây là **đặc tả màn hình trước wireframe**, chưa phải giao diện đã triển khai. Mỗi màn hình dùng Java/XML/ViewBinding, Material Design, trạng thái dữ liệu chung và điều hướng theo vai trò; quyền dữ liệu được kiểm tại Firebase Rules/backend. “Lỗi” trong bảng là trạng thái tối thiểu ngoài loading/empty khi có dữ liệu từ mạng.

### STUDENT

| Màn hình | Mục đích và dữ liệu | Quyền | Thành phần UI; lỗi cần thể hiện |
|---|---|---|---|
| Đăng ký | Tạo Firebase Auth bằng email/mật khẩu, tên và phone liên hệ tùy chọn theo DEC-007, đồng ý dữ liệu | Khách | Form, lỗi cạnh trường, checkbox; nhãn phone chưa xác minh, email trùng/kết nối/partial profile |
| Đăng nhập | Vào phiên và điều hướng theo role | Khách | Email/password, quên mật khẩu; sai thông tin/hết mạng |
| Xác minh tài khoản | Xác minh email, trạng thái Auth | S | Hướng dẫn gửi lại/kiểm tra; link hết hạn/chưa xác minh |
| Trang chủ | Tình trạng onboarding, xe và vi phạm được phép | S của chính mình | Card tác vụ, thông báo thiếu xe; offline/cache cũ |
| Hồ sơ cá nhân | Xem/sửa trường profile được phép | S của chính mình | Form, trạng thái xác minh; lỗi quyền/lưu thất bại |
| Danh sách phương tiện | Xem nhiều xe và trạng thái liên kết | S của chính mình | List/card, lọc trạng thái; empty, quyền bị thu |
| Đăng ký phương tiện | Gửi xe đầu tiên hoặc xe bổ sung | S đã xác minh đăng nhập | Form plate/type, ảnh nếu cần; biển số trùng, gửi thất bại |
| Trạng thái xác minh | Theo dõi request PENDING/VERIFIED/REJECTED, lý do | S của chính mình | Timeline/status, nút bổ sung khi có; chờ lâu/từ chối |
| Danh sách vi phạm | Xem hồ sơ được liên kết hợp lệ theo thời gian | S được cấp quyền từng hồ sơ | List phân trang; empty, offline, quyền đã thu |
| Chi tiết vi phạm | Xem ảnh, thời gian, khu vực/vị trí được phép và trạng thái | S được cấp quyền từng hồ sơ/ảnh | Card bằng chứng, timeline; file lỗi/quyền bị từ chối |

#### Wireframe đăng ký email trên Spark — chờ duyệt trước khi viết XML

```text
Đăng ký tài khoản sinh viên
Họ và tên *
Email *
Mật khẩu *                 [Hiện/ẩn]
Nhập lại mật khẩu *        [Hiện/ẩn]
Số điện thoại liên hệ (tùy chọn) — Chưa xác minh
[ ] Tôi đã đọc thông báo xử lý dữ liệu cá nhân *
[Tạo tài khoản]

Sau khi tạo: Kiểm tra email xác minh
[Tôi đã xác minh]  [Gửi lại email]
Sau xác minh: Đăng ký phương tiện đầu tiên → Chờ xét duyệt
```

Lỗi xuất hiện cạnh trường liên quan; lỗi gửi email sau khi Auth đã tạo phải nêu tài khoản đã tồn tại và cho gửi lại, không yêu cầu tạo tài khoản mới. Điện thoại không có nút OTP, không dùng làm cách đăng nhập/khôi phục. Trước khi tạo XML cần duyệt wireframe, nội dung đồng ý và chính sách hiển thị mã sinh viên (DEC-007B). Không yêu cầu biển số trên bước tạo Auth; xe đầu tiên là bước onboarding sau khi email đã xác minh theo ADR-004.

### SECURITY_GUARD

| Màn hình | Mục đích và dữ liệu | Quyền | Thành phần UI; lỗi cần thể hiện |
|---|---|---|---|
| Đăng nhập | Vào phiên guard và nhận phạm vi | G được cấp quyền | Form; sai thông tin/không còn grant |
| Trang chủ | Tác vụ ca/khu vực, lịch sử gần đây | G trong scope | Card tác vụ, trạng thái kết nối; scope rỗng/offline |
| Camera | Chụp ảnh chính và ảnh bổ sung, ghi `capturedAt`/vị trí | G trong scope, quyền camera/location foreground | Preview, hướng dẫn, nút chụp/đổi ảnh chính; quyền từ chối/GPS lỗi |
| Kết quả YOLO và OCR | Xem box, crop, rawText ML Kit | G tạo bản nháp | Ảnh, vùng chọn, gợi ý; không/nhiều box, OCR trống |
| Xác nhận biển số | Rà/chỉnh `plateFinal` | G tạo bản nháp | Text field và ảnh crop; ký tự không hợp lệ/không chắc chắn |
| Lập hồ sơ vi phạm | Chọn type/note/ảnh, kiểm GPS ảnh chính | G trong scope | Form, khối vị trí, thử lại/chụp lại/thủ công đã duyệt; thiếu ảnh/vị trí |
| Xem lại hồ sơ | Rà ảnh, plate, loại, vị trí, thời điểm trước gửi | G tạo bản nháp | Summary, checkbox/xác nhận; dữ liệu thiếu/mất mạng |
| Lịch sử vi phạm | Hồ sơ thuộc phạm vi được cấp | G trong scope | List phân trang/bộ lọc; empty/offline/quyền thu |
| Chi tiết vi phạm | Xem trạng thái/ảnh/audit phù hợp | G được quyền hồ sơ | Ảnh, timeline; file lỗi/ngoài scope |

### ADMIN

| Màn hình | Mục đích và dữ liệu | Quyền | Thành phần UI; lỗi cần thể hiện |
|---|---|---|---|
| Đăng nhập | Vào phiên ADMIN do nguồn tin cậy cấp | A đã cấp grant | Form; sai thông tin/claim chưa cập nhật/đã thu quyền |
| Dashboard | Hàng chờ, chỉ số tổng hợp và tác vụ ưu tiên | A theo grant | Card KPI/tác vụ; dữ liệu trễ/offline |
| Quản lý người dùng | Tìm tài khoản, trạng thái/quyền theo nhiệm vụ | A có quyền nhân sự | List/search, action có xác nhận; quyền thiếu/tài khoản không thấy |
| Quản lý phương tiện | Tìm xe, link lịch sử và xung đột plate | A có quyền xe | List/detail, nhãn VERIFIED/INACTIVE; plate trùng/tranh chấp |
| Xác minh phương tiện | Duyệt/từ chối request thủ công | A/nhân sự có grant duyệt | Hàng chờ, bằng chứng tối thiểu, dialog lý do; transaction xung đột |
| Quản lý hồ sơ vi phạm | Rà/sửa theo phiên bản và trạng thái | A có quyền hồ sơ | List/detail, action có lý do; quyền thiếu/xung đột cập nhật |
| Danh mục vi phạm | Quản lý code/label/version | A có quyền danh mục | List/form/version; mã trùng/đang được dùng |
| Thống kê | Xem báo cáo tổng hợp, export khi có quyền riêng | A có quyền báo cáo | Bộ lọc/chart/bảng; dữ liệu rỗng/quota/export bị từ chối |
| Nhật ký hoạt động | Tra sự kiện audit theo quyền | A có quyền audit | Timeline/search/filter; quyền thiếu/sự kiện không đầy đủ |

Không thiết kế màn hình ADMIN như quyền đọc mọi trường PII. Mọi màn hình dùng trạng thái loading, empty, success, error, offline, permission denied và lỗi form cụ thể. Wireframe từng màn hình vẫn phải được người dùng duyệt trước khi viết XML.

## Trạng thái bắt buộc cho màn hình dữ liệu

| Trạng thái | Nội dung và hành động |
|---|---|
| Loading | Chỉ rõ đang tải/gửi; ngăn gửi lặp; có lối hủy nếu tác vụ lâu. |
| Empty | Nêu nguyên nhân khả dĩ và hành động hợp lệ, ví dụ “Chưa có phương tiện được xác minh”, không hiểu nhầm thành lỗi. |
| Success | Xác nhận ID/trạng thái đã lưu; phân biệt lưu cục bộ và lưu server. |
| Error | Lỗi cụ thể gần nguồn: “Ảnh chưa tải lên, thử lại”; giữ dữ liệu nhập; không đưa stack trace. |
| Offline | Nhãn offline rõ, giới hạn thao tác; thông báo điều gì đã/ chưa đồng bộ theo DEC-005. |
| Permission denied | Giải thích vì sao cần camera/vị trí; cho đường thay thế nếu chính sách cho phép, không yêu cầu lặp liên tục. Với vị trí, phân biệt bị từ chối và chỉ cấp vị trí gần đúng. |

## Khả năng tiếp cận và màn hình khác kích thước

Kiểm tra TalkBack, thứ tự focus, nhãn ảnh/nút, kích thước chữ 200%, chế độ tối, màn hình nhỏ và xoay, bàn phím che form, insets Edge-to-Edge, vùng chạm ≥48dp. Đảm bảo tương phản theo guideline Material/Android qua công cụ kiểm tra, không dựa vào mắt thường. Ảnh bằng chứng phải có mô tả ngắn hợp lý nhưng không đọc to biển số/PII khi không cần. Trạng thái và lỗi có text ngoài màu; thông báo quan trọng được trình đọc màn hình công bố.

## Quy tắc nội dung tiếng Việt

Thông báo nêu việc gì xảy ra và cách khắc phục: “Không đọc rõ biển số. Chụp lại gần hơn hoặc nhập thủ công” thay cho “Có lỗi”. Biển số gợi ý phải đánh dấu “AI gợi ý”; hồ sơ `PENDING` hiển thị “Đang chờ xác nhận”, không gọi là vi phạm đã kết luận. Mọi chuỗi qua Android String Resources, kể cả lỗi form và accessibility label.
