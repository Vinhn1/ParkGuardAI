# 23 — Theo dõi triển khai giao diện theo thiết kế Canvas

> Tạo ngày: 2026-10-22.  
> Căn cứ theo thiết kế mẫu tại [`.kombai/canvas/parkguard-ai-mobile-ux.canvas`](file:///d:/Workplace/Project/ParkGuardAI/.kombai/canvas/parkguard-ai-mobile-ux.canvas) và tài liệu bàn giao `10 · Developer Handoff`.  
> Quy chuẩn kỹ thuật: Android Native Java, XML Layouts, ViewBinding, Material 3 DayNight, không dùng Jetpack Compose hay Kotlin Coroutines.

---

## 1. Bảng điều khiển tiến độ tổng thể

| Pha | Nội dung chính | Tổng số task | Đã hoàn thành | Trạng thái |
|---|---|---|---|---|
| **Pha 1** | Nền tảng Design System & Tokens (Navy/Cyan) | 3 | 1 | `PARTIAL` |
| **Pha 2** | Giao diện Xác thực & Onboarding (AUTH/01 — 10) | 5 | 0 | `PARTIAL` |
| **Pha 3** | Tinh chỉnh & Chuẩn hóa luồng Bảo vệ (GUARD/01 — 22) | 5 | 0 | `PARTIAL` |
| **Pha 4** | Xây dựng giao diện Sinh viên (STUDENT/01 — 10) | 5 | 0 | `PARTIAL` |
| **Pha 5** | Xây dựng giao diện Quản trị viên (ADMIN/01 — 13) | 6 | 0 | `PARTIAL` |
| **Pha 6** | Màn hình dùng chung (COMMON) & Trạng thái toàn cục | 2 | 0 | `PARTIAL` |
| **Tổng cộng** | Toàn bộ 68 khung màn hình theo Canvas | **26** | **1** | **4%** |

---

## 2. Chi tiết các Task theo từng Pha

### Pha 1: Nền tảng Design System & Color Tokens (Theme & Primitives)

* [x] **Task 1.1: Đồng bộ Color Tokens (Bảng màu Navy / Cyan)**
  * **Mục tiêu:** Cập nhật bảng màu từ Teal/Green sang Navy/Cyan theo chuẩn Node `01 · Design System`.
  * **File chỉnh sửa:** [`app/src/main/res/values/colors.xml`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/res/values/colors.xml), [`app/src/main/res/values-night/colors.xml`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/res/values-night/colors.xml).
  * **Nội dung:** Thêm các màu `bg_navy_950` (`#020618`), `bg_navy_900` (`#081028`), `surface_900` (`#0F172B`), `surface_800` (`#182438`), `outline` (`#263650`), `primary_blue` (`#2259FB`), `accent_cyan` (`#00D3F3`), `success_green` (`#00C98D`), `warn_yellow` (`#FFB525`), `error_red` (`#F13261`).

* [ ] **Task 1.2: Cấu hình Theme Material 3 & Typography**
  * **Mục tiêu:** Đảm bảo theme ứng dụng kế thừa đúng các style Material 3 DayNight, hỗ trợ chế độ tương phản cao và font chữ Roboto/Roboto Mono.
  * **File chỉnh sửa:** [`app/src/main/res/values/themes.xml`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/res/values/themes.xml), [`app/src/main/res/values-night/themes.xml`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/res/values-night/themes.xml).

* [ ] **Task 1.3: Bộ Drawables & Styles Component dùng chung**
  * **Mục tiêu:** Tạo các thẻ card bo góc 16dp có đường viền mảnh, badge trạng thái bo tròn (`PENDING`, `VERIFIED`, `REJECTED`), và kiểu dáng MaterialButton/TextInputLayout.
  * **File tạo mới/chỉnh sửa:** `app/src/main/res/drawable/` (`bg_card_surface.xml`, `bg_badge_status.xml`, `bg_button_primary.xml`).

---

### Pha 2: Chuẩn hóa giao diện Xác thực & Onboarding (AUTH/01 — AUTH/10)

* [ ] **Task 2.1: Làm mới Splash & Welcome (AUTH/01, AUTH/02)**
  * **Mục tiêu:** Khớp với bố cục Canvas: Biểu tượng khiên bảo vệ, slogan sản phẩm, nút Đăng nhập / Đăng ký bo góc lớn với hiệu ứng chuyển cảnh mượt.
  * **File:** [`app/src/main/res/layout/fragment_welcome.xml`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/res/layout/fragment_welcome.xml).

* [ ] **Task 2.2: Làm mới Form Đăng nhập & Đăng ký (AUTH/03, AUTH/05)**
  * **Mục tiêu:** Nâng cấp layout đăng nhập và đăng ký: TextInputLayout nền tối `surface_800`, icon trực quan, thông báo lỗi trường cụ thể.
  * **File:** [`app/src/main/res/layout/fragment_login.xml`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/res/layout/fragment_login.xml), [`app/src/main/res/layout/fragment_register.xml`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/res/layout/fragment_register.xml).

* [ ] **Task 2.3: Xây dựng màn hình Quên mật khẩu & Gửi lại (AUTH/04, AUTH/10)**
  * **Mục tiêu:** Tạo màn hình yêu cầu đặt lại mật khẩu và màn hình xác nhận đã gửi email đặt lại kèm bộ đếm thời gian cho phép gửi lại.
  * **File:** `app/src/main/res/layout/fragment_forgot_password.xml`, `app/src/main/res/layout/fragment_reset_sent.xml`.

* [ ] **Task 2.4: Xây dựng Cổng kiểm soát quyền - Access Gate (AUTH/08)**
  * **Mục tiêu:** Xử lý trường hợp người dùng đã đăng nhập và xác minh email nhưng chưa được người quản trị cấp role/grant hoặc bị khóa quyền.
  * **File:** `app/src/main/res/layout/fragment_access_gate.xml`.
  * **Điều chỉnh theo DEC-032:** Tài khoản sinh viên mới chưa có hồ sơ được vào onboarding; chỉ grant đặc quyền thiếu, sai hoặc bị thu hồi mới vào Access Gate.

* [ ] **Task 2.5: Làm mới Onboarding Sinh viên & Xác nhận đã gửi (AUTH/07, AUTH/09)**
  * **Mục tiêu:** Chuẩn hóa form khai báo thông tin sinh viên và xe đầu tiên sau xác minh email; bổ sung màn hình thông báo hồ sơ đã lưu thành công kèm trạng thái xe `PENDING`.
  * **File:** [`app/src/main/res/layout/fragment_student_onboarding.xml`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/res/layout/fragment_student_onboarding.xml), `app/src/main/res/layout/fragment_registration_submitted.xml`.

---

### Pha 3: Tinh chỉnh & Chuẩn hóa luồng Bảo vệ (GUARD/01 — GUARD/22)

* [ ] **Task 3.1: Nâng cấp Trang chủ Bảo vệ (GUARD/01)**
  * **Mục tiêu:** Cập nhật bố cục thẻ ca trực, phạm vi được phân công, card số lượng bản nháp cục bộ và hồ sơ đã gửi, nút chụp vi phạm lớn.
  * **File:** [`app/src/main/res/layout/fragment_guard_home.xml`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/res/layout/fragment_guard_home.xml).

* [ ] **Task 3.2: Bottom Sheet Chọn loại hành vi vi phạm (GUARD/22)**
  * **Mục tiêu:** Thay thế Spinner cũ bằng BottomSheet chọn nhanh 4 nhóm vi phạm theo chuẩn Material 3.
  * **File:** `app/src/main/res/layout/bottom_sheet_violation_type.xml`.

* [ ] **Task 3.3: Làm mới màn hình Rà soát vi phạm (GUARD/12) & Trạng thái tải (GUARD/13, 14, 15)**
  * **Mục tiêu:** Bố cục ảnh hiện trường sắc nét, khung cắt biển số, khối gợi ý AI, thông số định vị GPS; bổ sung dialog trạng thái đang tải lên Cloudinary/Firestore, báo lỗi và báo lưu thành công.
  * **File:** [`app/src/main/res/layout/fragment_guard_review.xml`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/res/layout/fragment_guard_review.xml), `app/src/main/res/layout/dialog_upload_progress.xml`, `app/src/main/res/layout/dialog_save_success.xml`.

* [ ] **Task 3.4: Bổ sung Hộp thoại Thoát bản nháp (GUARD/21) & Quản lý bản nháp (GUARD/20)**
  * **Mục tiêu:** Dialog bảo vệ chống mất dữ liệu khi đang chụp/rà soát: Lưu bản nháp vào máy / Hủy / Tiếp tục rà soát.
  * **File:** `app/src/main/res/layout/dialog_leave_draft.xml`.

* [ ] **Task 3.5: Làm mới Sổ vi phạm (GUARD/16) & Chi tiết hồ sơ (GUARD/17)**
  * **Mục tiêu:** Bố cục thẻ danh sách hồ sơ (tab Bản nháp trên máy / tab Đã lưu máy chủ) và giao diện xem chi tiết hồ sơ vi phạm.
  * **File:** [`app/src/main/res/layout/fragment_guard_detail.xml`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/res/layout/fragment_guard_detail.xml).

---

### Pha 4: Xây dựng giao diện vai trò Sinh viên (STUDENT/01 — STUDENT/10)

* [ ] **Task 4.1: Navigation & Thanh điều hướng 4 Tab Sinh viên**
  * **Mục tiêu:** Thiết lập thanh điều hướng dưới gồm: Trang chủ, Phương tiện, Vi phạm, Cá nhân; tích hợp vào `nav_graph.xml`.
  * **File:** `app/src/main/res/menu/student_bottom_nav.xml`, [`app/src/main/res/navigation/nav_graph.xml`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/res/navigation/nav_graph.xml).

* [ ] **Task 4.2: Trang chủ Sinh viên (STUDENT/01)**
  * **Mục tiêu:** Tóm tắt tình trạng xe đã duyệt, lối tắt khai báo xe mới, danh sách các thông báo vi phạm mới nhất.
  * **File:** `app/src/main/res/layout/fragment_student_home.xml`.

* [ ] **Task 4.3: Quản lý Phương tiện (STUDENT/02, 03, 04)**
  * **Mục tiêu:** Danh sách phương tiện của sinh viên kèm badge (`PENDING`/`VERIFIED`/`REJECTED`), màn hình đăng ký thêm xe, màn hình chi tiết thông tin xe và lý do từ chối nếu có.
  * **File:** `app/src/main/res/layout/fragment_student_vehicles.xml`, `app/src/main/res/layout/fragment_student_add_vehicle.xml`, `app/src/main/res/layout/fragment_student_vehicle_detail.xml`.

* [ ] **Task 4.4: Lịch sử Vi phạm & Trình xem bằng chứng (STUDENT/05, 06, 07)**
  * **Mục tiêu:** Danh sách các vụ vi phạm của các xe thuộc quyền sở hữu của sinh viên, chi tiết vi phạm, trình xem ảnh bằng chứng toàn màn hình có zoom/fit.
  * **File:** `app/src/main/res/layout/fragment_student_violations.xml`, `app/src/main/res/layout/fragment_student_violation_detail.xml`, `app/src/main/res/layout/fragment_student_evidence_viewer.xml`.

* [ ] **Task 4.5: Cá nhân & Cài đặt Sinh viên (STUDENT/08, 09, 10)**
  * **Mục tiêu:** Màn hình thông tin tài khoản, chỉnh sửa thông tin liên hệ, cài đặt ứng dụng và chuyển hướng đổi mật khẩu.
  * **File:** `app/src/main/res/layout/fragment_student_profile.xml`, `app/src/main/res/layout/fragment_student_edit_profile.xml`, `app/src/main/res/layout/fragment_student_settings.xml`.

---

### Pha 5: Xây dựng giao diện vai trò Quản trị viên (ADMIN/01 — ADMIN/13)

* [ ] **Task 5.1: Navigation & Thanh điều hướng 5 Tab Quản trị**
  * **Mục tiêu:** Thiết lập thanh điều hướng: Tổng quan, Duyệt xe, Vi phạm, Người dùng, Mở rộng.
  * **File:** `app/src/main/res/menu/admin_bottom_nav.xml`.

* [ ] **Task 5.2: Dashboard Quản trị (ADMIN/01)**
  * **Mục tiêu:** Thống kê xe đang chờ duyệt, vi phạm trong ngày, trạng thái hệ thống.
  * **File:** `app/src/main/res/layout/fragment_admin_dashboard.xml`.

* [ ] **Task 5.3: Hàng đợi xét duyệt xe (ADMIN/02, 03, 04, 13)**
  * **Mục tiêu:** Danh sách xe cần xét duyệt, màn hình chi tiết đối chiếu thông tin tự khai, hộp thoại nhập lý do từ chối bắt buộc, màn hình xác nhận kết quả duyệt thành công.
  * **File:** `app/src/main/res/layout/fragment_admin_verification_queue.xml`, `app/src/main/res/layout/fragment_admin_verification_detail.xml`, `app/src/main/res/layout/bottom_sheet_reject_reason.xml`, `app/src/main/res/layout/fragment_admin_verification_success.xml`.

* [ ] **Task 5.4: Quản lý Người dùng & Chi tiết tài khoản (ADMIN/05, 06)**
  * **Mục tiêu:** Danh sách người dùng toàn trường (Sinh viên, Bảo vệ), xem chi tiết quyền hạn, phạm vi gán, khóa/mở khóa tài khoản.
  * **File:** `app/src/main/res/layout/fragment_admin_users.xml`, `app/src/main/res/layout/fragment_admin_user_detail.xml`.

* [ ] **Task 5.5: Quản lý Vi phạm & Danh mục lỗi (ADMIN/07, 08, 09)**
  * **Mục tiêu:** Tra cứu hồ sơ vi phạm toàn trường, xem chi tiết biên bản vi phạm, quản lý danh mục mã vi phạm.
  * **File:** `app/src/main/res/layout/fragment_admin_violations.xml`, `app/src/main/res/layout/fragment_admin_violation_detail.xml`, `app/src/main/res/layout/fragment_admin_violation_types.xml`.

* [ ] **Task 5.6: Báo cáo Thống kê & Nhật ký Kiểm toán Audit Log (ADMIN/10, 11, 12)**
  * **Mục tiêu:** Biểu đồ vi phạm theo khu vực, dòng thời gian nhật ký kiểm toán hành động nhạy cảm, menu cài đặt mở rộng.
  * **File:** `app/src/main/res/layout/fragment_admin_statistics.xml`, `app/src/main/res/layout/fragment_admin_audit_log.xml`, `app/src/main/res/layout/fragment_admin_more.xml`.

---

### Pha 6: Màn hình dùng chung (COMMON) & Trạng thái toàn cục (Global States)

* [ ] **Task 6.1: Tiện ích dùng chung (COMMON/01, 02, 03, 04)**
  * **Mục tiêu:** Đổi mật khẩu, Hộp thoại xác nhận đăng xuất, Thông tin ứng dụng / Giới thiệu, Chính sách bảo mật dữ liệu.
  * **File:** `app/src/main/res/layout/fragment_change_password.xml`, `app/src/main/res/layout/dialog_logout_confirm.xml`, `app/src/main/res/layout/fragment_about.xml`, `app/src/main/res/layout/fragment_privacy.xml`.

* [ ] **Task 6.2: Bộ khung trạng thái toàn cục (08 · Global States)**
  * **Mục tiêu:** Xây dựng các layout trạng thái có thể tái sử dụng vào bất kỳ màn hình nào: Đang tải dữ liệu, Danh sách trống, Mất kết nối mạng, Lỗi hệ thống.
  * **File:** `app/src/main/res/layout/layout_state_empty.xml`, `app/src/main/res/layout/layout_state_loading.xml`, `app/src/main/res/layout/layout_state_error.xml`, `app/src/main/res/layout/layout_state_offline.xml`.

### Ghi chú triển khai ngày 2026-10-22

Task 1.1 đã được đối chiếu trên màn chào sáng/tối và màn đăng nhập tối sau khi build pass. Các task còn lại vẫn ở trạng thái `PARTIAL`: chưa có đối chiếu trực quan đầy đủ cho từng node. Các màn hình cần dữ liệu đặc quyền chưa được coi là hoàn thành khi chưa có backend và Rules được duyệt.

| Task | Trạng thái | Phần đã làm và phần còn thiếu |
|---|---|---|
| 1.1 | `DONE` | Các token màu khớp `01 · Design System`; đã build và chụp màn chào sáng/tối, đăng nhập tối trên `Pixel_9_Pro`. |
| 1.2 | `PARTIAL` | Đã cấu hình Material 3 DayNight và kiểu chữ cơ bản; chưa kiểm tra tương phản cao trên toàn ứng dụng. |
| 1.3 | `PARTIAL` | Đã thêm drawable/style dùng chung; chưa áp dụng đồng đều cho mọi thành phần và trạng thái badge. |
| 2.1 | `PARTIAL` | Đã làm mới Welcome và chụp emulator; Splash và biểu tượng còn khác canvas. |
| 2.2 | `PARTIAL` | Form hiện có dùng bảng màu mới; còn thiếu một số chi tiết và trạng thái minh họa của canvas. |
| 2.3 | `PARTIAL` | Đã có form gửi email, trang đã gửi và bộ đếm; chưa kiểm chứng email thật hoặc trang đã gửi trên emulator. |
| 2.4 | `PARTIAL` | Đã có Access Gate và kiểm tra grant từ máy chủ; chưa kiểm chứng với tài khoản có grant thật. |
| 2.5 | `PARTIAL` | Đã có trang xác nhận hồ sơ chờ duyệt; chưa đối chiếu toàn bộ onboarding với canvas. |
| 3.1 | `PARTIAL` | Trang chủ Guard đã đổi token; thẻ ca trực còn thiếu dữ liệu và bố cục canvas. |
| 3.2 | `PARTIAL` | Spinner loại vi phạm đã đổi sang bottom sheet; chưa chụp màn hình với tài khoản Guard. |
| 3.3 | `PARTIAL` | Đã có khung trạng thái tải/lưu; dialog tiến độ và thành công chưa nối vào luồng nộp. |
| 3.4 | `PARTIAL` | Đã nối dialog lưu/xóa bản nháp khi rời màn rà soát; chưa thử bằng bản nháp thực. |
| 3.5 | `PARTIAL` | Sổ/chi tiết dùng token mới; chưa khớp toàn bộ node GUARD/16–17. |
| 4.1 | `PARTIAL` | Đã có bốn tab và điều hướng; chưa thử với tài khoản Student thật. |
| 4.2 | `PARTIAL` | Đã có khung trang chủ và trạng thái chờ; chưa có nguồn dữ liệu xe/thông báo đã xác minh. |
| 4.3 | `PARTIAL` | Đã có khung danh sách, thêm xe, chi tiết; lưu xe thứ hai và dữ liệu duyệt chưa có backend. |
| 4.4 | `PARTIAL` | Đã có khung lịch sử/chi tiết/ảnh; chưa đọc hồ sơ được xác minh theo quyền Student. |
| 4.5 | `PARTIAL` | Đã có cá nhân/cài đặt; chỉnh sửa liên hệ chưa nối lưu server. |
| 5.1 | `PARTIAL` | Đã có năm tab Admin và điều hướng; chưa thử với grant Admin thật. |
| 5.2 | `PARTIAL` | Dashboard có trạng thái không có dữ liệu; thống kê thật chưa có backend. |
| 5.3 | `PARTIAL` | Đã có khung hàng đợi/chi tiết/lý do từ chối/kết quả; thao tác duyệt cần DEC-010 và backend/Rules. |
| 5.4 | `PARTIAL` | Đã có khung người dùng/chi tiết; thao tác khóa quyền chưa có backend/Rules. |
| 5.5 | `PARTIAL` | Đã có khung hồ sơ/danh mục; tìm kiếm và sửa danh mục chưa có backend/Rules. |
| 5.6 | `PARTIAL` | Đã có khung thống kê/audit/menu; dữ liệu tổng hợp và audit đáng tin chưa có backend. |
| 6.1 | `PARTIAL` | Đã có đổi mật khẩu qua email, Giới thiệu, Quyền riêng tư và dialog đăng xuất nối ở Student/Guard/Admin; chưa thử với phiên thật ở từng vai trò. |
| 6.2 | `PARTIAL` | Đã có bốn layout trạng thái; chưa áp dụng và kiểm tra trên toàn bộ màn hình. |

---

## 3. Quy trình Kiểm thử Trực quan trên Máy ảo Android (Live UI Verification)

Sau mỗi task hoàn thành:
1. **Khởi chạy máy ảo:** `Pixel_9_Pro` qua công cụ Emulator.
2. **Biên dịch & Cài đặt APK:** `.\gradlew.bat installDebug`
3. **Mở màn hình cần kiểm tra:** `adb shell am start -n com.example.parkguardai/.MainActivity`
4. **Chụp ảnh màn hình kiểm thử UI:** `adb shell screencap -p /sdcard/ui_test.png && adb pull /sdcard/ui_test.png artifacts/`
5. **Đối chiếu:** So sánh trực quan giữa ảnh chụp máy ảo và khung mẫu trên Canvas.
