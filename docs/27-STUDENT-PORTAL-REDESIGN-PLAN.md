# Kế hoạch Thiết kế & Triển khai Giao diện Phân hệ Sinh viên (Student Portal Redesign Plan)

> Cập nhật: 2026-10-25  
> Trạng thái: Đang triển khai (Theo quyết định kiến trúc DEC-036)  
> Mục tiêu: Thiết lập toàn bộ các màn hình phân hệ Sinh viên theo đúng 100% mẫu thiết kế Dark Mode Navy/Cyan/Rose, ViewBinding, XML View System, 100% Java Native.

---

## 1. Danh sách 7 Màn hình Phân hệ Sinh viên

### A. 4 Màn hình chính theo ảnh thiết kế 1:
1. **Màn hình 1: Xe của tôi (`StudentHomeFragment` / `fragment_student_home.xml`) - Tab 1 Bottom Nav**:
   - Header: Logo `ParkGuard AI` + Badge vai trò `Sinh viên`.
   - Lời chào: *Xin chào, Nguyễn Văn A*.
   - Thẻ phương tiện chính "Xe đã đăng ký":
     * Biển số trắng to: `69A-123.45` + Badge xanh `Đã duyệt`.
     * Dòng phụ: *Xe máy • Honda Vision • Đen*.
   - 2 Thẻ trạng thái nhanh:
     * Trạng thái bãi (Xanh): Icon xe + `Trong bãi` + *Vào lúc: 07:20*.
     * Trạng thái vi phạm (Đỏ Rose): Icon cảnh báo + `1 vi phạm` + *Chưa xử lý*.
   - Khối "Lịch sử vào/ra gần đây":
     * Hôm nay - Vào: `07:20`
     * Hôm qua - Ra: `11:45`
   - Nút hành động Cyan pill: `Đăng ký xe mới` (chuyển sang màn hình 2).
   - Chạm vào khối Lịch sử -> Chuyển sang màn hình 6 (Lịch sử vào/ra chi tiết).
   - Chạm vào thẻ 1 vi phạm -> Chuyển sang màn hình 5 (Vi phạm của tôi).

2. **Màn hình 2: Đăng ký xe mới (`StudentAddVehicleFragment` / `fragment_student_add_vehicle.xml`)**:
   - Header: `< Đăng ký xe mới`.
   - 3 Segmented Tabs chọn loại xe: `[Xe máy]` (active cyan), `[Xe đạp điện]`, `[Ô tô]`.
   - Ô nhập biển số xe với định dạng chữ hoa nổi bật: Hint *Ví dụ: 69A-123.45*.
   - 2 ô: *Hãng xe* (Honda) và *Màu xe* (Đen).
   - Thẻ upload 1: *Ảnh cà vẹt xe* + "Chạm để chụp hoặc chọn ảnh".
   - Thẻ upload 2: *Ảnh xe và biển số* + "Giúp AI nhận diện chính xác".
   - Nút hành động Cyan pill: `Gửi yêu cầu duyệt`.

3. **Màn hình 3: Chi tiết vi phạm (`StudentViolationDetailFragment` / `fragment_student_violation_detail.xml`)**:
   - Header: `< Chi tiết vi phạm`.
   - Thẻ vi phạm:
     * Biển số: `69A-123.45` + Badge Rose `Chưa xử lý`.
     * Khung hình bằng chứng do AI chụp.
     * Bảng thông tin: Lý do: *Đỗ sai khu vực*, Thời gian: *22/10/2026 08:42*, Vị trí: *Bãi A*.
   - Khung gửi khiếu nại:
     * Tiêu đề: *Nội dung khiếu nại*.
     * Ô nhập: Hint *Trình bày lý do bạn cho rằng nhận diện chưa đúng*.
   - Nút hành động Cyan pill: `Gửi khiếu nại`.

4. **Màn hình 4: Thông báo (`StudentNotificationsFragment` / `fragment_student_notifications.xml`) - Tab 3 Bottom Nav**:
   - Header: `Thông báo` + Badge `3 chưa đọc`.
   - 4 Segmented Filter Tabs: `[Tất cả]` (active), `[Vi phạm]`, `[Xe]`, `[Hệ thống]`.
   - Section Hôm nay:
     * Thông báo 1 (Đỏ Rose): *Vi phạm mới • 08:42 •* - Xe 69A-123.45 bị ghi nhận đỗ sai khu vực + 2 nút: `Xem chi tiết` (chuyển sang màn hình 3) và `Khiếu nại`.
     * Thông báo 2 (Vàng Amber): *Lượt vào bất thường • 14:05 •* - Xe có 2 lượt vào liên tiếp + 2 nút: `Là tôi` và `Không phải tôi`.
     * Thông báo 3 (Xanh lá): *Xe vào bãi • 07:20 •* - Xe 69A-123.45 đã vào cổng chính.
   - Section Hôm qua:
     * Thông báo 4 (Xanh dương): *Đăng ký xe đã được duyệt • 16:30* - Xe 69A-123.45 đã được duyệt và có thể ra vào bãi.

---

### B. 3 Màn hình bổ sung theo ảnh thiết kế 2:
5. **Màn hình 5: Vi phạm của tôi (`StudentViolationsFragment` / `fragment_student_violations.xml`) - Tab 2 Bottom Nav**:
   - Header: `Vi phạm của tôi` + Badge Rose `1 chưa xử lý`.
   - 4 Filter Tabs: `[Tất cả]` (active), `[Chưa xử lý]`, `[Khiếu nại]`, `[Đã xử lý]`.
   - Danh sách thẻ vi phạm:
     * Thẻ 1 (Đỏ Rose): `69A-123.45` • Đỗ sai khu vực - 22/10 | Badge `Chưa xử lý`.
     * Thẻ 2 (Vàng Amber): `69A-123.45` • Quá thời gian đỗ - 15/10 | Badge `Khiếu nại`.
     * Thẻ 3 (Xanh lá): `69A-123.45` • Không có thẻ xe - 02/10 | Badge `Đã xử lý`.
   - Thẻ gợi ý an ninh: Icon chuông Cyan `Mẹo tránh vi phạm` - "Đỗ đúng vạch và ra khỏi bãi trước giờ đóng cổng để tránh bị ghi nhận."

6. **Màn hình 6: Lịch sử vào/ra (`StudentParkingHistoryFragment` / `fragment_student_parking_history.xml`)**:
   - Header: `< Lịch sử vào/ra`.
   - 3 Filter Tabs: `[Hôm nay]` (active), `[7 ngày]`, `[Tháng]`.
   - 2 Thẻ tóm tắt nhanh:
     * *Tổng thời gian đỗ:* `4g 25p`
     * *Số lượt vào:* `2`
   - Section Hôm nay • 22/10:
     * 13:10 • Vào • Cổng chính | `Hợp lệ`
     * 11:45 • Ra • Cổng phụ | `Hợp lệ`
     * 07:20 • Vào • Cổng chính | `Hợp lệ`
   - Section Hôm qua • 21/10:
     * 16:30 • Ra • Cổng chính | `Hợp lệ`
     * 07:35 • Vào • Cổng chính | `Hợp lệ`

7. **Màn hình 7: Hồ sơ sinh viên (`StudentProfileFragment` / `fragment_student_profile.xml`) - Tab 4 Bottom Nav**:
   - Header: `Hồ sơ`.
   - Thẻ thông tin sinh viên: Avatar `NA`, Họ tên *Nguyễn Văn A*, Mã SV: *2100123456*, Lớp: *DH21TIN01*, SĐT: *0901 234 567*.
   - Khối "Xe của tôi": Header + Nút `+ Thêm xe` (Cyan) + Thẻ xe: Biển số `69A-123.45`, *Honda Vision • Đen*, Badge `Đã duyệt`.
   - Nhóm cài đặt tài khoản:
     * `Đổi mật khẩu` (icon khóa + chevron >).
     * `Đăng nhập vân tay / Face ID` (Switch on/off).
     * `Thông báo đẩy` (Switch on/off).
   - Nút Outlined: `Đăng xuất` (đỏ).

---

## 2. Danh mục Task triển khai

* [x] **TASK-STU-01**: Cập nhật Menu 4 tab sinh viên [`student_bottom_nav.xml`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/res/menu/student_bottom_nav.xml): `Xe của tôi`, `Vi phạm`, `Thông báo`, `Hồ sơ`.
* [x] **TASK-STU-02**: Bổ sung chuỗi strings tiếng Việt cho toàn bộ phân hệ Sinh viên vào [`strings.xml`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/res/values/strings.xml).
* [x] **TASK-STU-03**: Thiết kế lại `fragment_student_home.xml` (Màn hình 1 - Xe của tôi) -> Đã nghiệm thu: [student_screen1_home.png](file:///C:/Users/ADMIN/.gemini/antigravity-ide/brain/2c376373-0a9f-4e0a-aac4-90f7d95ce945/student_screen1_home.png).
* [x] **TASK-STU-04**: Thiết kế lại `fragment_student_add_vehicle.xml` (Màn hình 2 - Đăng ký xe mới) -> Đã nghiệm thu: [student_screen2_add_vehicle.png](file:///C:/Users/ADMIN/.gemini/antigravity-ide/brain/2c376373-0a9f-4e0a-aac4-90f7d95ce945/student_screen2_add_vehicle.png).
* [x] **TASK-STU-05**: Thiết kế lại `fragment_student_violation_detail.xml` (Màn hình 3 - Chi tiết vi phạm & Gửi khiếu nại) -> Đã nghiệm thu: [student_screen3_violation_detail.png](file:///C:/Users/ADMIN/.gemini/antigravity-ide/brain/2c376373-0a9f-4e0a-aac4-90f7d95ce945/student_screen3_violation_detail.png).
* [x] **TASK-STU-06**: Xây dựng mới `fragment_student_notifications.xml` và `StudentNotificationsFragment.java` (Màn hình 4 - Thông báo) -> Đã nghiệm thu: [student_screen4_notifications.png](file:///C:/Users/ADMIN/.gemini/antigravity-ide/brain/2c376373-0a9f-4e0a-aac4-90f7d95ce945/student_screen4_notifications.png).
* [x] **TASK-STU-07**: Thiết kế lại `fragment_student_violations.xml` (Màn hình 5 - Vi phạm của tôi) -> Đã nghiệm thu: [student_screen5_violations.png](file:///C:/Users/ADMIN/.gemini/antigravity-ide/brain/2c376373-0a9f-4e0a-aac4-90f7d95ce945/student_screen5_violations.png).
* [x] **TASK-STU-08**: Xây dựng mới `fragment_student_parking_history.xml` và `StudentParkingHistoryFragment.java` (Màn hình 6 - Lịch sử vào/ra) -> Đã nghiệm thu: [student_screen6_history.png](file:///C:/Users/ADMIN/.gemini/antigravity-ide/brain/2c376373-0a9f-4e0a-aac4-90f7d95ce945/student_screen6_history.png).
* [x] **TASK-STU-09**: Thiết kế lại `fragment_student_profile.xml` (Màn hình 7 - Hồ sơ sinh viên & Cài đặt) -> Đã nghiệm thu: [student_screen7_profile.png](file:///C:/Users/ADMIN/.gemini/antigravity-ide/brain/2c376373-0a9f-4e0a-aac4-90f7d95ce945/student_screen7_profile.png).
* [x] **TASK-STU-10**: Cập nhật `StudentNavigation.java` và khai báo các destination vào [`nav_graph.xml`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/res/navigation/nav_graph.xml).
* [x] **TASK-STU-11**: Biên dịch `./gradlew :app:assembleDebug` đạt 0 lỗi (BUILD SUCCESSFUL).
* [x] **TASK-STU-12**: Cài đặt APK lên thiết bị thật `lzp7ytz5v87dei5p` và chụp ảnh màn hình nghiệm thu toàn bộ 7 màn hình sinh viên thành công 100%.
