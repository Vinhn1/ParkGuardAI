# CHƯƠNG 4: HIỆN THỰC HỆ THỐNG

Sau khi hoàn thành giai đoạn phân tích yêu cầu và thiết kế hệ thống ở Chương 3, chương này trình bày chi tiết các kết quả hiện thực hóa ứng dụng ParkGuard AI trên nền tảng Android. Nội dung chương tập trung vào việc mô tả cấu hình môi trường phát triển, trình bày giao diện thực tế của các chức năng, cấu trúc cơ sở dữ liệu lưu trữ, các dạng báo cáo thống kê và phân tích một số đoạn mã nguồn cốt lõi trong chương trình.

---

## 4.1 Cấu hình và triển khai hệ thống

Hệ thống ParkGuard AI được xây dựng dựa trên sự kết hợp giữa nền tảng ứng dụng di động Android SDK, công cụ biên dịch mô hình trí tuệ nhân tạo TensorFlow Lite chạy trực tiếp trên thiết bị, và các dịch vụ đám mây của Google Firebase cùng Cloudinary để quản lý tài khoản và lưu trữ hình ảnh hiện trường.

### 4.1.1 Yêu cầu môi trường và công cụ phát triển

Quá trình xây dựng ứng dụng di động đòi hỏi máy tính có cấu hình phần cứng phù hợp để phục vụ việc biên dịch mã nguồn và chạy các trình giả lập thiết bị. Ứng dụng được lập trình bằng ngôn ngữ Java trên nền hệ điều hành Android, sử dụng bộ công cụ Android Studio chính thức do Google cung cấp. Bảng 4.1 tổng hợp các tiêu chuẩn về phần cứng và phần mềm được sử dụng trong suốt quá trình phát triển và thử nghiệm chương trình.

Bảng 4.1. Yêu cầu môi trường phần cứng và phần mềm phát triển
| Thành phần | Cấu hình tối thiểu | Cấu hình sử dụng thực tế |
| :--- | :--- | :--- |
| Bộ vi xử lý (CPU) | Intel Core i5 thế hệ 8 hoặc tương đương | Intel Core i7 thế hệ 11 trở lên hoặc AMD Ryzen 7 |
| Bộ nhớ trong (RAM) | 8 GB RAM | 16 GB đến 32 GB RAM |
| Ổ cứng lưu trữ | Trống tối thiểu 15 GB dung lượng | Ổ cứng SSD còn trống trên 30 GB |
| Hệ điều hành máy tính | Windows 10 64-bit | Windows 11 64-bit hoặc macOS |
| Môi trường lập trình (IDE) | Android Studio Hedgehog | Android Studio Ladybug (phiên bản 2024.2) |
| Bộ công cụ Java (JDK) | OpenJDK 11 | Java Development Kit (JDK) 17 LTS |
| Thiết bị kiểm thử | Máy ảo Android API 29 | Điện thoại Android thật (hệ điều hành 10.0 trở lên) |

Các thông số kỹ thuật bên trong dự án được thiết lập trong tệp cấu hình Gradle nhằm đảm bảo phần mềm hoạt động ổn định trên nhiều dòng điện thoại khác nhau. Bảng 4.2 liệt kê các phiên bản SDK và những thư viện chính được tích hợp vào dự án.

Bảng 4.2. Các thông số cấu hình và thư viện chính trong dự án
| Tên thành phần | Phiên bản thiết lập | Mục đích sử dụng trong ứng dụng |
| :--- | :--- | :--- |
| Compile SDK | 36 (Android 15) | Biên dịch mã nguồn với các hàm mới nhất của Android |
| Min SDK | 29 (Android 10) | Phiên bản hệ điều hành thấp nhất có thể cài đặt app |
| Target SDK | 36 | Phiên bản Android mà ứng dụng hướng tới tối ưu |
| ViewBinding | Bật (true) | Giúp kết nối giao diện XML với mã Java an toàn |
| TensorFlow Lite | 2.14.0 | Nạp và chạy mô hình AI nhận diện biển số trên điện thoại |
| ML Kit Text Recognition | 16.0.1 | Đọc các ký tự chữ và số trên vùng ảnh biển số xe |
| CameraX | 1.3.4 | Điều khiển camera chụp ảnh hiện trường và bật đèn flash |
| Firebase BOM | 33.1.2 | Đồng bộ các dịch vụ tài khoản và dữ liệu trực tuyến |
| Play Services Location | 21.3.0 | Lấy tọa độ vị trí GPS gắn liền vào biên bản vi phạm |

### 4.1.2 Cấu hình dịch vụ nền tảng (Firebase và Cloudinary)

Hệ thống ParkGuard AI lựa chọn hệ sinh thái Google Firebase làm nền tảng xử lý dữ liệu đám mây theo kiến trúc trực tuyến thời gian thực. Việc kết nối giữa ứng dụng Android và máy chủ Firebase được thực hiện thông qua tệp cấu hình `google-services.json` đặt trong thư mục gốc của module ứng dụng. Đồng thời, mã vân tay chứng chỉ ký số SHA-1 và SHA-256 của môi trường phát triển được khai báo đồng bộ trên bảng điều khiển Firebase để đảm bảo tính an toàn cho các phiên trao đổi dữ liệu.

Thành phần Firebase Authentication đảm nhận vai trò quản lý tài khoản người dùng, sử dụng phương thức đăng nhập bằng thư điện tử và mật khẩu kết hợp kiểm tra trạng thái xác minh email. Cơ sở dữ liệu Cloud Firestore được khởi tạo ở chế độ bảo mật nghiêm ngặt (Production Mode) và đặt tại trung tâm dữ liệu khu vực Đông Nam Á. Vị trí đặt máy chủ này mang lại độ trễ kết nối rất thấp, phù hợp với điều kiện mạng di động thực tế tại khuôn viên trường học.

Đối với việc lưu trữ hình ảnh hiện trường xe vi phạm, ứng dụng kết nối trực tiếp với dịch vụ lưu trữ đám mây Cloudinary nhằm giảm tải dung lượng cho cơ sở dữ liệu chính. Các thông số định danh tài nguyên bao gồm Cloud Name, API Key và API Secret được cấu hình cục bộ thông qua tệp thuộc tính `local.properties`. Cách tổ chức cấu hình tách biệt này giúp ứng dụng truyền nhận ảnh an toàn mà không làm lộ các khóa bảo mật quan trọng trong mã nguồn dự án.

### 4.1.3 Tích hợp mô hình AI và tài nguyên On-Device

Nhằm đảm bảo tốc độ phản hồi nhanh khi bảo vệ tác nghiệp ngoài hiện trường, mô hình học sâu YOLO11n sau khi hoàn tất huấn luyện được xuất sang định dạng TensorFlow Lite mang tên `parkguard_yolo11n_best.tflite` với dung lượng khoảng 10,1 MB. Tệp mô hình này được nhúng trực tiếp vào thư mục tài nguyên `assets/` của ứng dụng Android, cho phép thiết bị thực thi suy luận trực tiếp trên phần cứng di động mà không cần phụ thuộc vào đường truyền mạng Internet.

Trong tệp kịch bản đóng gói Gradle, dự án được cấu hình tính năng không nén đối với các tệp có đuôi mở rộng `.tflite`. Cấu hình này cho phép bộ thông dịch nạp nhanh tệp mô hình vào bộ nhớ mà không phát sinh độ trễ giải nén. Bên cạnh đó, thư viện nhận dạng chữ Google ML Kit Text Recognition cũng được tích hợp sẵn sàng trong tệp khai báo `AndroidManifest.xml`, giúp ứng dụng tự động kiểm tra và tải các mô-đun đọc chữ quang học ngay khi cài đặt vào điện thoại.

### 4.1.4 Quy trình biên dịch và triển khai ứng dụng

Dự án ParkGuard AI được quản lý mã nguồn tập trung thông qua kho lưu trữ Git và phát triển trực tiếp trên môi trường Android Studio. Khi mở dự án trong môi trường phát triển, hệ thống Gradle tự động phân giải cấu trúc thư mục, tải các thư viện thành phần và thiết lập cây điều hướng giao diện. Quy trình này đảm bảo toàn bộ mã nguồn Java và các tệp giao diện XML luôn đồng bộ trước khi tạo bản dựng thử nghiệm.

Ứng dụng được biên dịch và đóng gói thành tệp cài đặt APK thông qua công cụ Gradle Build ở chế độ gỡ lỗi (Debug Mode). Chương trình được nạp trực tiếp vào điện thoại Android thử nghiệm thông qua giao thức ADB qua cổng USB có bật chế độ nhà phát triển. Việc thử nghiệm trực tiếp trên phần cứng thật giúp đánh giá chính xác khả năng tương thích của ứng dụng đối với cụm camera góc rộng, độ chính xác của cảm biến định vị GPS và hiệu năng xử lý của mô hình AI khi chụp ảnh ngoài trời.

---

## 4.2 Hiện thực giao diện

Giao diện người dùng của ParkGuard AI được xây dựng theo phong cách nền tối hiện đại, sử dụng tông màu xanh thẫm làm nền chủ đạo kết hợp với màu xanh ngọc sáng để làm nổi bật các nút bấm và thông tin quan trọng. Cách bố trí này giúp lực lượng bảo vệ dễ quan sát màn hình ngoài trời nắng, hạn chế mỏi mắt khi trực ca đêm và tiết kiệm pin cho thiết bị di động. Bảng 4.3 tổng hợp danh mục các màn hình chính được phân chia theo từng nhóm người dùng trong hệ thống.

Bảng 4.3. Danh mục các màn hình giao diện trong ứng dụng ParkGuard AI
| Phân hệ người dùng | Tên màn hình chức năng | Tệp giao diện XML tương ứng | Mục đích sử dụng chính |
| :--- | :--- | :--- | :--- |
| Xác thực tài khoản | Màn hình đăng nhập | `fragment_login.xml` | Nhập email, mật khẩu và tự phân luồng vai trò |
| Xác thực tài khoản | Đăng ký sinh viên | `fragment_register.xml` | Sinh viên điền thông tin và nộp hồ sơ chờ duyệt |
| Nhân sự bảo vệ | Camera tuần tra | `fragment_guard_camera.xml` | Chụp ảnh phương tiện vi phạm tại hiện trường |
| Nhân sự bảo vệ | Rà soát kết quả AI | `fragment_guard_review.xml` | Xem ảnh cắt biển số, sửa chữ và chọn nhiều xe |
| Nhân sự bảo vệ | Lập biên bản vi phạm | `fragment_guard_violation_form.xml` | Chọn lỗi đỗ xe, nhập tiền phạt và gửi hồ sơ |
| Nhân sự bảo vệ | Cảnh báo an ninh | `fragment_guard_alerts.xml` | Báo xe vào nhiều lần hoặc vào 2 lượt liên tiếp |
| Nhân sự bảo vệ | Ca trực bảo vệ | `fragment_guard_profile.xml` | Xem thống kê số xe quét trong ca và bàn giao ca |
| Quản trị viên | Bảng điều khiển KPI | `fragment_admin_dashboard.xml` | Xem tổng số vi phạm và biểu đồ khu vực nóng |
| Quản trị viên | Quản lý vi phạm | `fragment_admin_violations.xml` | Tra cứu, lọc danh sách và xem chi tiết bằng chứng |
| Quản trị viên | Thẩm định xe sinh viên | `fragment_admin_verification_queue.xml` | Xem giấy tờ cà-vẹt và bấm duyệt hoặc từ chối xe |
| Quản trị viên | Quản lý người dùng | `fragment_admin_users.xml` | Cấp tài khoản bảo vệ, sửa ca trực và duyệt sinh viên |
| Sinh viên | Danh sách xe cá nhân | `fragment_student_vehicles.xml` | Xem danh sách các xe đã được nhà trường duyệt |
| Sinh viên | Đăng ký phương tiện | `fragment_student_add_vehicle.xml` | Khai báo thông tin xe mới và chụp ảnh giấy tờ |
| Sinh viên | Chi tiết vi phạm | `fragment_student_violation_detail.xml` | Xem ảnh xe vi phạm, vị trí đỗ sai và số tiền phạt |

### 4.2.1 Phân hệ Đăng nhập và Phân quyền

Màn hình đăng nhập là nơi đầu tiên người dùng tiếp cận khi mở ứng dụng. Hệ thống cung cấp hai ô nhập liệu gồm địa chỉ email và mật khẩu cùng nút bấm ẩn hiện mật khẩu thuận tiện. Khi người dùng nhấn nút đăng nhập, hệ thống sẽ đối chiếu với cơ sở dữ liệu để kiểm tra tài khoản thuộc vai trò nào (Quản trị viên, Bảo vệ hay Sinh viên) rồi tự động chuyển người dùng đến đúng màn hình làm việc tương ứng mà không cần phải chọn vai trò thủ công.

Đối với sinh viên mới, ứng dụng cung cấp màn hình đăng ký tài khoản yêu cầu nhập đầy đủ họ tên, mã số sinh viên, lớp học, số điện thoại và email cá nhân. Sau khi sinh viên bấm gửi thông tin, tài khoản sẽ được đưa vào danh sách chờ duyệt chứ chưa được đăng nhập ngay. Màn hình thông báo sẽ xuất hiện để báo cho sinh viên biết tài khoản đang chờ ban quản trị nhà trường đối chiếu thông tin và kích hoạt.

Hình 4.1: Giao diện màn hình đăng nhập và đăng ký tài khoản sinh viên
*(Mô tả hình ảnh: Bên trái là giao diện đăng nhập với ô nhập tài khoản, mật khẩu và nút bấm đăng nhập màu xanh ngọc; bên phải là giao diện đăng ký tài khoản sinh viên gồm các ô điền mã số sinh viên, lớp học, số điện thoại và thông báo chờ ban quản trị phê duyệt)*

### 4.2.2 Phân hệ Nhân sự Bảo vệ (SECURITY_GUARD)

Giao diện của nhân viên bảo vệ được xây dựng với mục tiêu thao tác thật nhanh tại hiện trường. Màn hình camera tuần tra mở sẵn khung ngắm góc rộng kèm đường viền hướng dẫn canh chỉnh biển số xe vào giữa ảnh. Trên màn hình có nút chụp ảnh lớn dễ bấm bằng một tay, nút bật đèn flash để chụp ban đêm, cùng các nút bấm phụ cho phép quét thêm xe khác hoặc mở bàn phím nhập biển số tay nếu biển số xe quá cũ, mờ hoặc bị che khuất.

Sau khi chụp ảnh, màn hình rà soát kết quả sẽ hiển thị ngay lập tức. Chương trình tự động cắt phần ảnh chứa biển số và dùng công cụ đọc chữ để điền sẵn chuỗi biển số vào ô văn bản. Trường hợp trong một bức ảnh có nhiều xe cùng đỗ sai quy định, hệ thống sẽ nhận diện và liệt kê từng xe thành một danh sách thẻ. Bảo vệ có thể chạm nhẹ vào ô chữ để sửa lại ký tự nếu máy đọc nhầm trước khi chuyển sang bước lập biên bản.

Hình 4.2: Giao diện màn hình camera tuần tra và rà soát kết quả nhận diện AI
*(Mô tả hình ảnh: Bên trái là khung ngắm camera tuần tra có vạch canh góc chụp và nút chụp nổi bật; bên phải là màn hình rà soát hiển thị ảnh chụp hiện trường, ảnh cắt cận cảnh biển số và ô nhập chữ số biển số đã được nhận dạng tự động)*

Tại màn hình lập biên bản vi phạm, bảo vệ chọn hành vi vi phạm từ danh sách có sẵn như đỗ xe chắn lối đi cứu hỏa, đỗ sai vạch sơn hoặc đỗ khu vực có biển cấm. Ứng dụng cung cấp ô nhập số tiền phạt xử lý linh hoạt (ví dụ: 100.000 đ hoặc 200.000 đ) kèm theo các nút chọn nhanh mức phạt theo đúng quy định của trường Đại học Cửu Long. Màn hình cũng hiển thị rõ tọa độ GPS và thời gian chụp ảnh thực tế trước khi bảo vệ bấm nút gửi biên bản lên hệ thống.

Bên cạnh việc lập biên bản, phân hệ bảo vệ còn có màn hình theo dõi lịch sử ra vào cổng và màn hình cảnh báo an ninh. Hệ thống sẽ tự động phát hiện và hiển thị cảnh báo màu vàng cam nếu có phương tiện vào trường 2 lượt liên tiếp nhưng chưa có lượt ra, hoặc những xe lạ quét lại nhiều lần trong ngày. Cuối ca trực, bảo vệ có thể mở màn hình ca trực để xem tổng số lượt xe đã kiểm tra, số xe vi phạm và thực hiện thao tác bàn giao ca cho đồng nghiệp tiếp theo.

Hình 4.3: Giao diện màn hình lập biên bản vi phạm, cảnh báo an ninh và ca trực
*(Mô tả hình ảnh: Màn hình lập biên bản vi phạm với các mục chọn lỗi và ô nhập tiền phạt; màn hình hiển thị danh sách cảnh báo xe vào liên tiếp bất thường; màn hình tổng kết số lượt quét trong ca và nút bàn giao ca trực)*

### 4.2.3 Phân hệ Quản trị viên (ADMIN)

Màn hình bảng điều khiển tổng quan là trung tâm làm việc chính của người quản trị, hiển thị các số liệu thống kê quan trọng nhất trong ngày. Quản trị viên có thể nắm bắt ngay tổng số biên bản vi phạm vừa lập, số hồ sơ xe sinh viên đang chờ xét duyệt và số lượng bảo vệ đang trong ca làm việc. Phía dưới màn hình là biểu đồ trực quan thể hiện tỷ lệ vi phạm theo từng khu vực như bãi xe giảng đường A, sân trường hay lối đi ký túc xá.

Tại màn hình quản lý vi phạm, quản trị viên có thể tra cứu toàn bộ biên bản trong trường, lọc hồ sơ theo từng ngày hoặc theo trạng thái xử lý. Khi bấm vào từng biên bản, màn hình chi tiết sẽ hiển thị bức ảnh gốc chụp toàn cảnh chiếc xe vi phạm, ảnh phóng to biển số, địa điểm trên bản đồ và tên bảo vệ lập biên bản. Quản trị viên có toàn quyền xác nhận biên bản để thông báo phạt sinh viên hoặc hủy bỏ biên bản nếu phát hiện sai sót.

Hình 4.4: Giao diện bảng điều khiển tổng quan và chi tiết biên bản vi phạm của quản trị viên
*(Mô tả hình ảnh: Màn hình dashboard với các thẻ số liệu vi phạm trong ngày, biểu đồ tròn phân chia khu vực vi phạm và màn hình xem chi tiết biên bản vi phạm với ảnh hiện trường sắc nét kèm tọa độ)*

Một chức năng quan trọng khác của quản trị viên là duyệt xe sinh viên và quản lý nhân sự. Trong màn hình duyệt xe, quản trị viên đối chiếu ảnh chụp giấy tờ đăng ký xe với thông tin sinh viên đã khai báo, sau đó bấm nút duyệt nhanh hoặc từ chối kèm lý do. Tại màn hình quản lý người dùng, quản trị viên có thể tạo tài khoản cấp cho nhân viên bảo vệ mới, chỉnh sửa ca trực, phân công cổng gác, và kích hoạt tài khoản cho các sinh viên mới đăng ký vào hệ thống.

Hình 4.5: Giao diện thẩm định duyệt xe sinh viên và quản lý tài khoản người dùng
*(Mô tả hình ảnh: Bên trái là màn hình hàng đợi xét duyệt giấy tờ xe sinh viên; bên phải là màn hình quản lý nhân sự bảo vệ với các nút tạo tài khoản, chỉnh sửa ca trực và phân công khu vực gác)*

### 4.2.4 Phân hệ Sinh viên (STUDENT)

Giao diện dành cho sinh viên được thiết kế đơn giản và dễ hiểu. Ngay tại trang chủ, sinh viên có thể xem danh sách các phương tiện xe máy hoặc xe điện của mình đã được nhà trường công nhận hay đang chờ duyệt. Khi cần gửi thông tin xe mới, sinh viên chỉ việc mở màn hình đăng ký xe, điền biển số, chọn hãng xe, màu sơn và dùng điện thoại chụp ảnh giấy tờ xe tải lên ứng dụng để ban quản trị đối chiếu.

Khi phương tiện của sinh viên không may bị bảo vệ ghi nhận đỗ sai quy định, sinh viên sẽ nhận được thông báo trên điện thoại. Trong màn hình danh sách vi phạm, sinh viên có thể bấm vào xem chi tiết để thấy rõ bức ảnh chụp xe của mình đang đỗ sai vị trí nào, thời điểm vi phạm lúc mấy giờ, số tiền phạt cần nộp và chỉ dẫn đến bàn trực bảo vệ để giải quyết vi phạm một cách rõ ràng, minh bạch.

Hình 4.6: Giao diện danh sách xe đã duyệt và chi tiết vi phạm dành cho sinh viên
*(Mô tả hình ảnh: Màn hình hiển thị danh sách xe máy của sinh viên kèm trạng thái đã duyệt; màn hình chi tiết biên bản vi phạm hiển thị ảnh bằng chứng hiện trường, vị trí xe đỗ sai và hướng dẫn xử lý)*

---

## 4.3 Hiện thực CSDL

Dữ liệu của phần mềm ParkGuard AI được tổ chức trên cơ sở dữ liệu Cloud Firestore theo dạng các bộ sưu tập (Collections) và tài liệu (Documents). Cách lưu trữ này cho phép ứng dụng đọc ghi dữ liệu rất nhanh và tự động cập nhật thông tin mới lên màn hình điện thoại của bảo vệ và quản trị viên ngay khi có thay đổi.

### 4.3.1 Cấu trúc các Collection trong Cloud Firestore

Hệ thống phân chia dữ liệu thành các bảng độc lập nhằm đảm bảo dữ liệu rõ ràng, không bị chồng chéo và thuận tiện cho việc truy vấn. Bảng 4.4 liệt kê danh mục các bảng dữ liệu chính đang hoạt động trong hệ thống.

Bảng 4.4. Danh mục các bảng dữ liệu chính trong Cloud Firestore
| Tên bảng (Collection) | Ý nghĩa nghiệp vụ | Thành phần dữ liệu lưu trữ chính |
| :--- | :--- | :--- |
| `users` | Hồ sơ tài khoản người dùng | Họ tên, email, số điện thoại, vai trò, mã sinh viên, mã bảo vệ |
| `vehicles` | Danh mục phương tiện trong trường | Biển số gốc, biển số chuẩn hóa, loại xe, hãng xe, trạng thái |
| `vehicle_links` | Yêu cầu xác thực quyền sở hữu xe | Mã sinh viên, mã xe, ảnh giấy tờ cà-vẹt, trạng thái phê duyệt |
| `violations` | Danh sách các biên bản vi phạm | Biển số vi phạm, loại lỗi, số tiền phạt, tọa độ GPS, thời gian |
| `evidence` (bảng con) | Bằng chứng hình ảnh đi kèm | Đường dẫn ảnh hiện trường, đường dẫn ảnh cắt biển số |
| `scan_rules` | Quy tắc quét và cảnh báo an ninh | Thời gian gộp lần đọc trùng, quy định vào ra xen kẽ |

Trong các bảng trên, bảng biên bản vi phạm (`violations`) là bảng quan trọng nhất vì nó liên kết trực tiếp giữa nhân viên bảo vệ lập biên bản, chiếc xe vi phạm và sinh viên chủ xe. Bảng 4.5 mô tả chi tiết các trường dữ liệu và kiểu dữ liệu được thiết lập bên trong bảng này.

Bảng 4.5. Chi tiết cấu trúc dữ liệu bảng biên bản vi phạm (`violations`)
| Tên trường dữ liệu | Kiểu dữ liệu | Bắt buộc | Diễn giải ý nghĩa |
| :--- | :--- | :--- | :--- |
| `id` | Chuỗi ký tự (String) | Có | Mã định danh duy nhất của biên bản vi phạm |
| `plateFinal` | Chuỗi ký tự (String) | Có | Biển số xe cuối cùng sau khi bảo vệ đã kiểm tra |
| `guardId` | Chuỗi ký tự (String) | Có | Mã tài khoản của bảo vệ chụp ảnh lập biên bản |
| `guardName` | Chuỗi ký tự (String) | Có | Tên hiển thị của nhân viên bảo vệ trực hiện trường |
| `violationTypeCode`| Chuỗi ký tự (String) | Có | Mã phân loại hành vi vi phạm (ví dụ: DO_SAI_VACH) |
| `violationTitle` | Chuỗi ký tự (String) | Có | Tên gọi hành vi vi phạm hiển thị cho người dùng đọc |
| `penaltyAmount` | Số thực (Number) | Có | Mức tiền phạt xử lý bằng Việt Nam Đồng (VND) |
| `status` | Chuỗi ký tự (String) | Có | Trạng thái biên bản (Chờ xác nhận, Đã nộp, Đã hủy) |
| `latitude` | Số thực (Number) | Có | Tọa độ vĩ độ GPS lấy tại thời điểm chụp ảnh |
| `longitude` | Số thực (Number) | Có | Tọa độ kinh độ GPS lấy tại thời điểm chụp ảnh |
| `capturedAt` | Thời gian (Timestamp) | Có | Thời điểm nhân viên bảo vệ nhấn nút chụp bức ảnh |
| `createdAt` | Thời gian (Timestamp) | Có | Thời điểm máy chủ ghi nhận biên bản vào cơ sở dữ liệu |

### 4.3.2 Hiện thực quy tắc bảo mật (Firestore Security Rules)

Để bảo vệ thông tin cá nhân của sinh viên và ngăn chặn việc chỉnh sửa hồ sơ vi phạm ngoài ý muốn, cơ sở dữ liệu Firestore được thiết lập bộ quy tắc kiểm soát quyền truy cập nghiêm ngặt. Hệ thống áp dụng nguyên tắc kiểm tra quyền từ phía máy chủ chứ không chỉ dựa vào giao diện trên điện thoại. Bảng 4.6 trình bày tóm tắt ma trận quyền hạn tương ứng với từng vai trò người dùng trong hệ thống.

Bảng 4.6. Bảng quy tắc phân quyền truy cập dữ liệu theo vai trò người dùng
| Bảng dữ liệu | Vai trò Sinh viên (STUDENT) | Vai trò Bảo vệ (SECURITY_GUARD) | Vai trò Quản trị viên (ADMIN) |
| :--- | :--- | :--- | :--- |
| Hồ sơ người dùng (`users`) | Chỉ xem và sửa thông tin cá nhân của chính mình | Xem danh sách để phục vụ công tác đối soát | Toàn quyền tạo mới, chỉnh sửa, khóa hoặc xóa |
| Danh mục xe (`vehicles`) | Chỉ xem các xe do mình đăng ký | Tra cứu biển số để xem xe lạ hay xe đã đăng ký | Toàn quyền phê duyệt, khóa hoặc xóa phương tiện |
| Biên bản vi phạm (`violations`)| Chỉ xem biên bản vi phạm của các xe thuộc sở hữu | Có quyền tạo mới biên bản và xem lịch sử vi phạm | Toàn quyền xác nhận, chỉnh sửa mức phạt hoặc hủy |
| Quy tắc an ninh (`scan_rules`)| Không có quyền truy cập | Chỉ đọc các quy tắc để ứng dụng áp dụng cảnh báo | Có quyền thay đổi các thông số cấu hình hệ thống |

Toàn bộ các quy tắc trên được lập trình thông qua tệp `firestore.rules`. Hệ thống yêu cầu người dùng phải đăng nhập tài khoản hợp lệ và đã xác minh email mới được phép đọc ghi dữ liệu. Khi bảo vệ tạo một biên bản mới, hệ thống tự động kiểm tra xem mã bảo vệ gửi lên có trùng khớp với tài khoản đang đăng nhập hay không, qua đó ngăn chặn việc mạo danh nhân sự khác khi lập biên bản.

### 4.3.3 Quản lý lưu trữ ảnh bằng chứng và liên kết dữ liệu

Khi bảo vệ bấm nút chụp ảnh vi phạm, điện thoại sẽ thu được một bức ảnh có độ nét cao. Bức ảnh này ngay lập tức được chia làm hai phần: một bức ảnh toàn cảnh hiện trường giữ nguyên góc rộng để làm bằng chứng bối cảnh, và một bức ảnh nhỏ được cắt riêng vùng biển số xe để lưu lại rõ từng ký tự.

Cả hai bức ảnh được nén nhẹ ở chất lượng 85% để giảm bớt dung lượng truyền qua mạng di động mà vẫn nhìn rất rõ nét, sau đó được tải lên thư mục lưu trữ an toàn trên máy chủ Cloudinary. Máy chủ lưu trữ sẽ trả về các đường dẫn liên kết hình ảnh bảo mật dạng HTTPS. Ứng dụng lấy các liên kết này lưu vào mục bằng chứng của biên bản vi phạm trong cơ sở dữ liệu Firestore cùng với tọa độ vị trí và thời gian chụp ảnh thực tế.

---

## 4.4 Hiện thực các báo cáo (report)

Việc số hóa công tác ghi nhận vi phạm giúp nhà trường dễ dàng theo dõi tình hình trật tự giao thông thông qua các biểu mẫu báo cáo tự động, thay thế hoàn toàn sổ tay ghi chép truyền thống. Bảng 4.7 tóm tắt ba loại báo cáo chính được xây dựng trong ứng dụng ParkGuard AI.

Bảng 4.7. Danh mục các mẫu báo cáo thống kê trong hệ thống ParkGuard AI
| Tên biểu mẫu báo cáo | Đối tượng sử dụng | Chu kỳ kết xuất | Nội dung thông tin chính |
| :--- | :--- | :--- | :--- |
| Báo cáo thống kê ca trực | Nhân sự bảo vệ | Sau mỗi ca làm việc | Tổng lượt xe quét, số xe lạ, số biên bản đã lập và bàn giao ca |
| Báo cáo bảng điều khiển KPI | Quản trị viên | Cập nhật tức thời | Số vụ vi phạm theo ngày/tuần, biểu đồ điểm nóng và tỷ lệ xử lý |
| Biên bản vi phạm điện tử | Bảo vệ và Sinh viên | Xuất khi lập biên bản | Ảnh bằng chứng, biển số xe, lỗi vi phạm, tiền phạt và vị trí GPS |

### 4.4.1 Báo cáo thống kê ca trực của nhân viên bảo vệ

Báo cáo ca trực được cập nhật tự động ngay trên màn hình cá nhân của bảo vệ trong suốt thời gian làm nhiệm vụ. Báo cáo này giúp nhân viên kiểm soát được khối lượng công việc mình đã thực hiện, bao gồm số lượt xe máy ra vào cổng, số lượng xe lạ vãng lai chưa qua đăng ký và số trường hợp đỗ xe sai quy định đã bị lập biên bản.

Khi hết ca làm việc, bảo vệ nhấn nút bàn giao ca trực để phần mềm tạo ra một biểu mẫu tổng kết ca làm việc dạng số. Nhân viên có thể nhập thêm vài dòng ghi chú về tình hình an ninh bãi xe trong ca của mình, sau đó bấm xác nhận bàn giao. Dữ liệu này được lưu lại trên máy chủ giúp tổ trưởng tổ bảo vệ và ban quản trị nhà trường dễ dàng kiểm tra, đánh giá tình hình an ninh giữa các ca trực.

Hình 4.7: Mẫu báo cáo thống kê ca trực và biên bản bàn giao ca của bảo vệ
*(Mô tả hình ảnh: Biểu mẫu hiển thị thẻ tên nhân viên, ca làm việc, bảng số liệu đếm số lượt quét hợp lệ, số xe lạ và ô nhập nội dung bàn giao ca trực)*

### 4.4.2 Báo cáo bảng điều khiển KPI Quản trị viên

Bảng điều khiển dành cho quản trị viên hoạt động như một trung tâm báo cáo điều hành trực tuyến. Thay vì phải chờ nhân viên tổng hợp số liệu vào cuối tuần, người quản trị có thể mở điện thoại ra bất cứ lúc nào để xem biểu đồ phân bố các điểm nóng vi phạm trong trường.

Số liệu báo cáo chỉ ra cụ thể khu vực nào trong trường thường xuyên xảy ra tình trạng đỗ xe sai vị trí, ví dụ như khu vực trước sảnh giảng đường A hoặc đoạn đường gần cổng phụ. Nhờ những con số thống kê chính xác này, ban quản lý nhà trường có thể kịp thời bố trí thêm biển báo chỉ dẫn hoặc tăng cường nhân sự tuần tra vào các khung giờ cao điểm để giữ gìn trật tự chung.

Hình 4.8: Mẫu báo cáo bảng điều khiển KPI và biểu đồ phân bố vi phạm
*(Mô tả hình ảnh: Bảng báo cáo tổng hợp các chỉ số vi phạm theo ngày, tuần, tháng và biểu đồ phân bổ các khu vực vi phạm đỗ xe trong khuôn viên trường)*

### 4.4.3 Biên bản vi phạm điện tử (Digital Violation Notice Report)

Biên bản vi phạm điện tử là chứng từ quan trọng nhất của hệ thống, được tạo ra tự động ngay khi nhân viên bảo vệ hoàn tất việc chụp ảnh và kiểm tra thông tin tại hiện trường. Biên bản này được gửi trực tiếp tới tài khoản của sinh viên vi phạm và hiển thị đầy đủ trên màn hình điện thoại của sinh viên.

Nội dung biên bản bao gồm mã số biên bản duy nhất, biển số xe, tên dòng xe, kèm theo hai bức ảnh bằng chứng chụp toàn cảnh và cận cảnh biển số. Điểm đặc biệt của biên bản là có gắn sẵn tọa độ vị trí thực tế trên bản đồ kèm dấu thời gian chính xác đến từng giây. Sinh viên vi phạm có thể xem rõ lỗi của mình cùng số tiền phạt quy định và biết chính xác mình cần đến đâu để chấp hành nộp phạt một cách thuận tiện, tránh các tranh cãi không đáng có.

Hình 4.9: Mẫu biên bản ghi nhận vi phạm đỗ xe điện tử trong ứng dụng
*(Mô tả hình ảnh: Bản mẫu biên bản vi phạm với mã số hồ sơ, ảnh chụp hiện trường xe vi phạm, ảnh cắt biển số, vị trí bản đồ, tên hành vi vi phạm, số tiền phạt và tên bảo vệ lập biên bản)*

---

## 4.5 Hiện thực code liên quan (chỉ nêu vài code đặc trưng)

Phần này giới thiệu bốn đoạn mã nguồn tiêu biểu đại diện cho các chức năng kỹ thuật then chốt của ứng dụng ParkGuard AI, bao gồm việc chạy mô hình AI nhận diện biển số, giải thuật đọc và ghép biển số xe máy 2 dòng, cơ chế lấy tọa độ GPS gắn với ảnh chụp và việc đồng bộ dữ liệu thời gian thực.

### 4.5.1 Tiền xử lý ảnh Letterbox và suy luận phát hiện biển số (`PlateDetector.java`)

Khi chụp ảnh từ camera, kích thước bức ảnh thường rất lớn và có tỷ lệ khác nhau tùy theo từng dòng máy điện thoại. Để mô hình AI YOLO11n có thể hiểu và tìm đúng vị trí biển số, bức ảnh cần được thu nhỏ về đúng kích thước chuẩn 640×640 pixel nhưng vẫn phải giữ nguyên tỷ lệ chiều rộng và chiều cao của xe, phần khoảng trống thừa được bù thêm màu xám chuẩn. Đoạn mã dưới đây trích từ tệp `PlateDetector.java` thực hiện nhiệm vụ này:

```java
// Đoạn mã trích từ lớp PlateDetector.java
public List<Detection> detect(Bitmap source) {
    if (source == null || source.isRecycled()) {
        throw new IllegalArgumentException("Ảnh không hợp lệ");
    }
    
    // Thu nhỏ ảnh giữ nguyên tỷ lệ để vừa khít khung vuông 640x640
    int size = PlateModelContract.INPUT_SIZE; // Kích thước 640
    float scale = Math.min((float) size / source.getWidth(), (float) size / source.getHeight());
    int scaledWidth = Math.max(1, Math.round(source.getWidth() * scale));
    int scaledHeight = Math.max(1, Math.round(source.getHeight() * scale));
    float padX = (size - scaledWidth) / 2f;
    float padY = (size - scaledHeight) / 2f;

    // Tạo khung ảnh mới 640x640 nền xám và vẽ ảnh thu nhỏ vào giữa
    Bitmap letterbox = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
    try {
        Canvas canvas = new Canvas(letterbox);
        canvas.drawColor(Color.rgb(114, 114, 114)); // Tô màu xám chuẩn YOLO
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(source, null,
                new RectF(padX, padY, padX + scaledWidth, padY + scaledHeight), paint);

        // Lấy mảng pixel màu và chuẩn hóa giá trị về đoạn từ 0.0 đến 1.0
        int[] pixels = new int[size * size];
        letterbox.getPixels(pixels, 0, size, 0, 0, size, size);
        float[][][][] input = new float[1][3][size][size]; // Định dạng tensor đầu vào
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int color = pixels[y * size + x];
                input[0][0][y][x] = Color.red(color) / 255f;
                input[0][1][y][x] = Color.green(color) / 255f;
                input[0][2][y][x] = Color.blue(color) / 255f;
            }
        }

        // Chạy mô hình TensorFlow Lite để tìm vị trí biển số
        float[][][] output = new float[1][PlateModelContract.OUTPUT_CHANNELS]
                [PlateModelContract.OUTPUT_CANDIDATES];
        interpreter.run(input, output);

        // Lọc các kết quả có độ tin cậy cao và tính tọa độ trên ảnh gốc
        return decode(output[0], source.getWidth(), source.getHeight(), scale, padX, padY);
    } finally {
        letterbox.recycle(); // Giải phóng bộ nhớ ảnh tạm
    }
}
```

Sau khi chạy xong đoạn mã trên, bộ thông dịch sẽ trả về danh sách các hình chữ nhật bao quanh biển số xe kèm theo điểm tin cậy. Nhờ đó, ứng dụng biết chính xác biển số nằm ở vị trí nào để tự động cắt ảnh cận cảnh chuyển sang cho bộ phận đọc chữ.

### 4.5.2 Nhận dạng ký tự và chuẩn hóa biển số xe Việt Nam (`PlateParser.java`)

Tại Việt Nam, phần lớn phương tiện sinh viên sử dụng là xe máy có biển số chia làm 2 dòng (dòng trên là mã tỉnh và sê-ri như `64-F1`, dòng dưới là số đăng ký như `123.45`). Công cụ đọc chữ thông thường hay bị nhầm lẫn giữa các chữ cái và chữ số có hình dáng gần giống nhau. Bảng 4.8 liệt kê các cặp ký tự thường xuyên bị đọc nhầm và cách thức chương trình tự động thay thế, sửa lỗi.

Bảng 4.8. Bảng đối chiếu các ký tự thường bị nhận diện nhầm và quy tắc sửa lỗi
| Ký tự máy đọc nhầm | Ký tự chuẩn thực tế | Vị trí xuất hiện trên biển số | Giải thích nguyên nhân |
| :--- | :--- | :--- | :--- |
| Chữ cái `O`, `D`, `Q` | Chữ số `0` | Dòng số đăng ký phía dưới | Nét cong tròn tương đồng với số 0 |
| Chữ cái `I`, `l`, `J` | Chữ số `1` | Dòng số đăng ký phía dưới | Nét thẳng đứng dễ nhầm với số 1 |
| Chữ cái `Z` | Chữ số `2` | Dòng số đăng ký phía dưới | Cấu trúc gãy góc tương tự số 2 |
| Chữ cái `A` | Chữ số `4` | Dòng số đăng ký phía dưới | Nét nhọn phía trên giống số 4 |
| Chữ cái `S` | Chữ số `5` | Dòng số đăng ký phía dưới | Nét uốn lượn dễ nhầm với số 5 |
| Chữ cái `G`, `b` | Chữ số `6` | Dòng số đăng ký phía dưới | Vòng tròn phía dưới giống số 6 |
| Chữ cái `B` | Chữ số `8` | Dòng số đăng ký phía dưới | Hai vòng tròn chồng lên nhau |
| Chữ số `8` | Chữ cái `B` | Vị trí sê-ri ở dòng trên | Cần chuyển thành chữ cái sê-ri xe |

Đoạn mã trích từ tệp `PlateParser.java` dưới đây thực hiện sắp xếp các dòng chữ theo thứ tự từ trên xuống dưới, sau đó tìm dòng trên và dòng dưới để ghép thành một biển số xe máy hoàn chỉnh theo đúng chuẩn Việt Nam:

```java
// Đoạn mã trích từ lớp PlateParser.java
@Nullable
public static Result parse(Text textResult) {
    if (textResult == null) return null;

    List<Text.Line> allLines = new ArrayList<>();
    for (Text.TextBlock block : textResult.getTextBlocks()) {
        allLines.addAll(block.getLines());
    }

    // Sắp xếp các dòng chữ nhận dạng được theo thứ tự từ trên xuống dưới
    Collections.sort(allLines, new Comparator<Text.Line>() {
        @Override
        public int compare(Text.Line l1, Text.Line l2) {
            Rect b1 = l1.getBoundingBox();
            Rect b2 = l2.getBoundingBox();
            int y1 = b1 != null ? b1.top : 0;
            int y2 = b2 != null ? b2.top : 0;
            return Integer.compare(y1, y2);
        }
    });

    // Giải thuật tìm và ghép biển số xe máy 2 dòng của Việt Nam
    for (int i = 0; i < allLines.size(); i++) {
        Text.Line lineTop = allLines.get(i);
        String rawTop = lineTop.getText();
        if (isIgnored(rawTop)) continue; // Bỏ qua tên hãng xe như Honda, Yamaha

        // Kiểm tra và sửa lỗi dòng trên (Mã tỉnh + Sê-ri, ví dụ: 64-F1)
        String correctedTop = correctMotorcycleTop(rawTop);
        if (correctedTop != null) {
            for (int j = 0; j < allLines.size(); j++) {
                if (i == j) continue;
                Text.Line lineBottom = allLines.get(j);
                Rect boxTop = lineTop.getBoundingBox();
                Rect boxBottom = lineBottom.getBoundingBox();

                // Đảm bảo dòng dưới nằm thấp hơn dòng trên trong khung ảnh
                if (boxTop != null && boxBottom != null && boxBottom.top < boxTop.top) {
                    continue;
                }

                String rawBottom = lineBottom.getText();
                if (isIgnored(rawBottom)) continue;

                // Sửa lỗi các ký tự số ở dòng dưới (ví dụ đọc chữ O thành số 0)
                String correctedBottom = correctPlateDigits(rawBottom);
                if (correctedBottom != null && (correctedBottom.length() == 4 || correctedBottom.length() == 5)) {
                    String clean = correctedTop + correctedBottom;
                    String formatted = formatMotorcyclePlate(correctedTop, correctedBottom);
                    return new Result(formatted, clean, combineBoxes(boxTop, boxBottom));
                }
            }
        }
    }
    // Nếu không khớp biển xe máy, chương trình chuyển sang đọc biển ô tô 1 dòng
    return parseSingleLineCandidate(allLines);
}
```

### 4.5.3 Chụp ảnh CameraX và thu thập tọa độ GPS đồng bộ (`CaptureLocationSource.java`)

Để biên bản vi phạm có giá trị bằng chứng thuyết phục, vị trí GPS phải được lấy chính xác ngay tại thời điểm bảo vệ bấm nút chụp ảnh xe, tránh tình trạng lấy vị trí cũ hoặc bảo vệ di chuyển đi nơi khác rồi mới lưu hồ sơ. Đoạn mã dưới đây trong tệp `CaptureLocationSource.java` sử dụng dịch vụ Google Play Services để lấy vị trí có độ chính xác cao và kiểm tra độ chênh lệch thời gian:

```java
// Đoạn mã trích từ lớp CaptureLocationSource.java
public final class CaptureLocationSource {
    private static final long TIMEOUT_MILLIS = 15_000L; // Thời gian chờ tối đa 15 giây
    private static final long MAX_CAPTURE_DIFFERENCE_NANOS = 5_000_000_000L; // Sai lệch lúc chụp tối đa 5 giây
    private static final long MAX_FIX_AGE_NANOS = 10_000_000_000L;          // Tuổi của vị trí tối đa 10 giây

    @SuppressLint("MissingPermission")
    public Task<Result> request(long capturedElapsedNanos) {
        cancel();
        TaskCompletionSource<Result> completion = new TaskCompletionSource<>();
        
        // Kiểm tra quyền truy cập vị trí trên điện thoại
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            completion.setResult(new Result(false, false, null));
            return completion.getTask();
        }

        Session session = new Session(completion, capturedElapsedNanos);
        active = session;
        
        // Yêu cầu lấy vị trí vệ tinh có độ chính xác cao nhất (High Accuracy)
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
                .setMinUpdateIntervalMillis(500L)
                .setMaxUpdateDelayMillis(2_000L)
                .build();

        client.requestLocationUpdates(request, session, Looper.getMainLooper());
        handler.postDelayed(session.timeoutRunnable, TIMEOUT_MILLIS);
        return completion.getTask();
    }

    // Hàm kiểm tra xem vị trí nhận được có khớp với thời điểm chụp ảnh hay không
    private boolean isAcceptedFix(Location fix, long capturedElapsedNanos) {
        if (fix == null) return false;
        long elapsedNow = SystemClock.elapsedRealtimeNanos();
        long fixElapsed = fix.getElapsedRealtimeNanos();
        long fixAge = elapsedNow - fixElapsed;
        long captureDiff = Math.abs(capturedElapsedNanos - fixElapsed);
        
        // Chỉ chấp nhận vị trí nếu mới bắt được trong vòng 10 giây và lệch lúc chụp dưới 5 giây
        return fixAge <= MAX_FIX_AGE_NANOS && captureDiff <= MAX_CAPTURE_DIFFERENCE_NANOS;
    }
}
```

Nhờ cơ chế kiểm tra thời gian nghiêm ngặt này, nếu tín hiệu định vị quá yếu hoặc vị trí lấy được đã quá cũ thì ứng dụng sẽ báo lỗi và yêu cầu thử lại, đảm bảo biên bản vi phạm luôn có tọa độ gắn liền trung thực với bức ảnh chụp hiện trường.

### 4.5.4 Tầng Repository và đồng bộ dữ liệu thời gian thực Firestore (`AdminRepository.java`)

Lớp `AdminRepository` đóng vai trò trung gian xử lý các nghiệp vụ của quản trị viên và gửi dữ liệu lên máy chủ Firestore. Khi quản trị viên nhấn nút duyệt một phương tiện sinh viên, hệ thống cần cập nhật đồng thời trạng thái yêu cầu xác minh và trạng thái của chiếc xe đó trong cùng một lượt giao dịch. Đoạn mã dưới đây minh họa việc thực thi thao tác cập nhật đồng bộ và lắng nghe sự thay đổi của danh sách người dùng:

```java
// Đoạn mã trích từ lớp AdminRepository.java
public final class AdminRepository {
    private static AdminRepository instance;
    private final FirebaseFirestore firestore;

    public static synchronized AdminRepository getInstance() {
        if (instance == null) {
            instance = new AdminRepository();
        }
        return instance;
    }

    private AdminRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    // Hàm duyệt hoặc từ chối hồ sơ đăng ký xe của sinh viên
    public Task<Void> updateVerificationStatus(String requestId, String status, @Nullable String reason) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status); // Trạng thái mới: APPROVED hoặc REJECTED
        updates.put("reviewedAt", FieldValue.serverTimestamp()); // Lưu giờ duyệt của máy chủ
        updates.put("reviewedBy", FirebaseAuth.getInstance().getUid()); // Lưu mã tài khoản admin duyệt
        if (reason != null && !reason.trim().isEmpty()) {
            updates.put("rejectReason", reason.trim()); // Lưu lý do nếu từ chối
        }

        // Dùng WriteBatch để cập nhật cùng lúc hai bảng dữ liệu an toàn
        WriteBatch batch = firestore.batch();
        DocumentReference requestRef = firestore.collection("vehicleVerificationRequests").document(requestId);
        batch.update(requestRef, updates);

        // Nếu quản trị viên bấm duyệt, tự động kích hoạt xe sang trạng thái hợp lệ (VERIFIED)
        if ("APPROVED".equals(status)) {
            DocumentReference vehicleRef = firestore.collection("vehicles").document(requestId);
            batch.update(vehicleRef, "status", "VERIFIED");
        }

        return batch.commit(); // Gửi toàn bộ thay đổi lên máy chủ trong một lần
    }

    // Lắng nghe danh sách người dùng thay đổi theo thời gian thực
    public ListenerRegistration observeUsers(EventListener<QuerySnapshot> listener) {
        return firestore.collection("users")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener(listener);
    }
}
```

Đoạn mã trên sử dụng tính năng `WriteBatch` giúp việc cập nhật hai bảng dữ liệu luôn diễn ra đồng thời, nếu một trong hai thao tác bị lỗi thì toàn bộ quá trình sẽ được hủy bỏ để tránh tình trạng dữ liệu bị sai lệch. Cơ chế `addSnapshotListener` giúp điện thoại của quản trị viên tự động cập nhật danh sách người dùng mới ngay lập tức mà không cần phải bấm nút tải lại trang.

---

## Tóm tắt chương 4

Chương 4 đã trình bày toàn diện quá trình hiện thực hóa hệ thống ParkGuard AI trên nền tảng Android. Từ việc thiết lập công cụ Android Studio, tích hợp dịch vụ Firebase và nhúng mô hình AI dạng tflite, hệ thống đã hoàn thiện giao diện cho cả ba nhóm người dùng: Quản trị viên, Bảo vệ và Sinh viên. Cơ sở dữ liệu Cloud Firestore được xây dựng chặt chẽ kết hợp bộ quy tắc phân quyền rõ ràng giúp bảo vệ an toàn thông tin cá nhân. Bốn đoạn mã nguồn tiêu biểu được phân tích đã làm rõ cách thức ứng dụng xử lý ảnh chụp hiện trường, nhận diện biển số xe máy hai dòng đặc trưng tại Việt Nam, thu thập vị trí định vị trung thực và đồng bộ dữ liệu tức thời, đáp ứng tốt mục tiêu quản lý trật tự giao thông trong khuôn viên trường học.
