# Kế hoạch thiết kế lại Phân hệ Bảo vệ (Guard Portal) — ParkGuard AI

> Ngày tạo: 2026-10-25 | Quyết định liên quan: [DEC-035](15-DECISION-LOG.md)  
> Mục tiêu: Thiết lập toàn bộ phân hệ màn hình cho vai trò Bảo vệ (Security Guard) theo mẫu Dark Mode Navy/Cyan/Rose đã cung cấp, đề xuất và xây dựng các màn hình còn thiếu để đảm bảo quy trình kiểm soát xe khép kín, tập trung 100% vào luồng UI trước khi kết nối sâu backend.

---

## 1. Đối chiếu Màn hình mẫu & Màn hình đề xuất bổ sung

### 5 Màn hình có trong mẫu thiết kế của người dùng:
1. **Màn hình 1: Quét camera AI (`GuardCameraFragment` / `fragment_guard_camera.xml`)**:
   - Khung Viewfinder Camera thời gian thực kèm nhãn `• Cổng chính - Camera 1`.
   - Bounding Box AI màu Cyan phát sáng kèm badge tự tin `Biển số - 98%`.
   - Ô hiển thị biển số nhận diện trực tiếp: `69A-123.45`.
   - Thẻ kết quả: Biển số in đậm + Badge xanh `Hợp lệ` + Thông tin `Chủ xe: Nguyễn Văn A`, `Loại xe: Xe máy`.
   - 2 nút hành động chính: `Cho qua` (Cyan Pill) và `Ghi vi phạm` (Rose/Red Pill).
   - Nút phụ: `Nhập biển số thủ công` (Dark Button).
   - Nút chuyển sang chế độ quét nhiều xe (Multi-vehicle scan).
   - Bottom Navigation 4 tab: `Quét` (Active), `Lịch sử`, `Cảnh báo`, `Hồ sơ`.

2. **Màn hình 2: Ghi nhận vi phạm (`GuardViolationFormFragment` / `fragment_guard_violation_form.xml`)**:
   - Top Bar: Nút back tròn `<` + Tiêu đề `Ghi nhận vi phạm`.
   - Khối Biển số vi phạm: Biển số lớn `69B-678.90` kèm badge vàng `Chưa đăng ký`.
   - Khối Lý do vi phạm: Spinner chọn lý do (`Đỗ sai khu vực`, `Chắn lối thoát hiểm`, `Quá giờ quy định`, `Không có thẻ xe`).
   - Khối Ghi chú: EditText nhiều dòng hint `Mô tả ngắn tình huống`.
   - Khối Ảnh bằng chứng: Khung camera/scan + Nhãn `Khung hình do AI chụp` + Badge xanh `Đã đính kèm`.
   - 2 nút đáy: `Huỷ` (Dark Card Button) và `Lưu vi phạm` (Rose/Red Glow Button).

3. **Màn hình 3: Lịch sử quét (`GuardHistoryFragment` / `fragment_guard_history.xml`)**:
   - Top Bar: Nút back tròn `<` + Tiêu đề `Lịch sử quét`.
   - 3 Segmented Filter Tabs: `[Hôm nay]` (Active Cyan) `[Hôm qua]` `[7 ngày]`.
   - Danh sách thẻ lượt quét xe:
     * `69A-123.45` • Vào - 08:42 - Cổng chính → `Hợp lệ` (xanh)
     * `69B-678.90` • Vào - 08:15 - Cổng chính → `Chưa đăng ký` (vàng)
     * `69H-246.80` • Ra - 07:50 - Cổng phụ → `Hợp lệ` (xanh)
     * `69D-112.33` • Vào - 07:31 - Cổng chính → `Vi phạm` (đỏ)
     * `69C-135.79` • Vào - 07:12 - Cổng phụ → `Hợp lệ` (xanh)
   - Bottom Navigation 4 tab (`Lịch sử` active).

4. **Màn hình 4: Quét nhiều xe (`GuardMultiVehicleScanFragment` / `fragment_guard_multi_vehicle_scan.xml`)**:
   - Top Bar: Nút back tròn `<` + Tiêu đề `Quét nhiều xe` + Badge số lượng `3 xe`.
   - Khung Viewfinder Camera đa đối tượng:
     * 3 Bounding Box nhận diện đồng thời: `69A-123.45` (98%), `69B-678.90` (94%), `69H-246.80` (95%).
   - 3 Thẻ thống kê tóm tắt nhanh: `2 Hợp lệ`, `1 Chưa đăng ký`, `0 Vi phạm`.
   - Danh sách xe trong khung hình kèm thông tin chủ xe và loại xe.
   - 2 nút hành động ở đáy: `Cho qua 2 xe hợp lệ` (Cyan sáng rực) và `Xử lý xe lạ` (Dark Card Button).

5. **Màn hình 5: Xe quét lại trong ngày (`GuardRescanAlertFragment` / `fragment_guard_rescan_alert.xml`)**:
   - Top Bar: Nút back tròn `<` + Tiêu đề `Xe quét lại trong ngày`.
   - Khối thông tin xe: Biển số lớn `69A-123.45` kèm badge `4 lần quét`, Chủ xe `Nguyễn Văn A - Xe máy`.
   - Thẻ Cảnh báo an ninh Amber:
     * Icon tam giác vàng + Tiêu đề: `2 lượt vào liên tiếp`.
     * Nội dung: `Xe vào lúc 13:10 và 14:05 nhưng chưa có lượt ra ở giữa. Cần xác nhận.`
   - Timeline lịch sử quét trong ngày (07:20 Vào, 11:45 Ra, 13:10 Vào - gộp 40s, 14:05 Vào - Cần xác nhận).
   - 3 nút hành động:
     * Nút lớn: `Xe đã ra, bổ sung lượt ra` (Cyan Button lớn).
     * 2 nút phụ: `Gộp, quét trùng` (Dark Card Button) và `Nghi vấn biển số` (Rose/Red Button).

---

### 3 Màn hình đề xuất bổ sung để hoàn chỉnh luồng bảo vệ:
6. **Màn hình 6: Danh sách Cảnh báo an ninh (`GuardAlertsFragment` / `fragment_guard_alerts.xml`)**:
   - Màn hình chính của Tab 3 `Cảnh báo` trên Bottom Navigation.
   - Cảnh báo thời gian thực về an ninh bãi xe:
     * Cảnh báo nghi vấn sao chép biển số (xuất hiện ở 2 cổng gần như cùng lúc).
     * Cảnh báo xe vào 2 lần liên tiếp không có lượt ra (liên kết sang màn hình 5).
     * Cảnh báo xe chưa đăng ký cố tình vượt cổng.
     * Cảnh báo xe gửi qua đêm vượt quy định.
   - 3 Segmented Tabs: `[Tất cả]` `[Chưa xử lý]` `[Đã xác nhận]`.
   - Thao tác nhanh: Xem chi tiết xe, xác nhận an toàn, báo cho Quản trị viên.

7. **Màn hình 7: Hồ sơ ca trực & Bàn giao (`GuardProfileFragment` / `fragment_guard_profile.xml`)**:
   - Màn hình chính của Tab 4 `Hồ sơ` trên Bottom Navigation.
   - Thông tin cá nhân: Họ tên bảo vệ, Mã nhân viên (`BV-01`), Ca trực (`Ca sáng: 06:00 - 14:00`), Cổng phụ trách (`Cổng chính A`).
   - Thống kê ca trực hôm nay: Tổng xe qua cổng (142), Vi phạm đã lập (3), Cảnh báo đã xử lý (5).
   - Nút hành động: `Bàn giao ca trực` (mở dialog bàn giao sổ trực/số lượng xe), `Đổi mật khẩu`, `Đăng xuất`.

8. **Màn hình 8: Hộp thoại Nhập biển số thủ công (`GuardManualEntryBottomSheet` / `bottom_sheet_guard_manual_entry.xml`)**:
   - Mở khi bảo vệ bấm `Nhập biển số thủ công` trên màn hình camera quét (khi biển số mờ, bụi bẩn hoặc góc camera khuất).
   - Nhập biển số dạng chữ to in hoa định dạng chuẩn.
   - Chọn chiều xe: `[Vào cổng]` hoặc `[Ra cổng]`.
   - Chọn loại xe: `Xe máy`, `Ô tô`, `Xe điện`.
   - 2 nút bấm: `Kiểm tra & Cho qua` (Cyan) và `Lập biên bản vi phạm` (Rose/Red).

---

## 2. Danh mục Task triển khai

* [x] **TASK-GRD-01**: Cập nhật Menu Bottom Navigation 4 tab chuẩn: [`guard_bottom_nav.xml`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/res/menu/guard_bottom_nav.xml) (`Quét` -> `Lịch sử` -> `Cảnh báo` -> `Hồ sơ`).
* [x] **TASK-GRD-02**: Bổ sung icon và tài nguyên Drawable cho Guard Portal:
  - Vector icon: `ic_guard_scan.xml`, `ic_guard_history.xml`, `ic_guard_alert.xml`, `ic_guard_user.xml`, `ic_alert_triangle.xml`.
  - Background button Rose/Red: `bg_button_pill_rose.xml` (#F13261).
  - Background badge Amber: `bg_badge_warning_amber.xml` (#FFB525).
  - Background card Camera Viewfinder bo góc & khung quét AI.
* [x] **TASK-GRD-03**: Bổ sung chuỗi hiển thị tiếng Việt vào [`strings.xml`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/res/values/strings.xml).
* [x] **TASK-GRD-04**: Thiết kế lại `fragment_guard_camera.xml` (Màn hình 1 - Quét camera AI).
* [x] **TASK-GRD-05**: Xây dựng `fragment_guard_violation_form.xml` (Màn hình 2 - Ghi nhận vi phạm).
* [x] **TASK-GRD-06**: Xây dựng `fragment_guard_history.xml` (Màn hình 3 - Lịch sử quét).
* [x] **TASK-GRD-07**: Xây dựng `fragment_guard_multi_vehicle_scan.xml` (Màn hình 4 - Quét nhiều xe).
* [x] **TASK-GRD-08**: Xây dựng `fragment_guard_rescan_alert.xml` (Màn hình 5 - Xe quét lại trong ngày).
* [x] **TASK-GRD-09**: Xây dựng `fragment_guard_alerts.xml` (Màn hình 6 - Danh sách Cảnh báo an ninh).
* [x] **TASK-GRD-10**: Thiết kế lại `fragment_guard_profile.xml` (Màn hình 7 - Hồ sơ ca trực & Bàn giao).
* [x] **TASK-GRD-11**: Xây dựng `bottom_sheet_guard_manual_entry.xml` (Màn hình 8 - Nhập biển số thủ công).
* [x] **TASK-GRD-12**: Xây dựng Java Controller & Navigation Graph:
  - Tạo `GuardNavigation.java` điều hướng 4 tab.
  - Cập nhật `GuardCameraFragment.java`, `GuardViolationFormFragment.java`, `GuardHistoryFragment.java`, `GuardMultiVehicleScanFragment.java`, `GuardRescanAlertFragment.java`, `GuardAlertsFragment.java`, `GuardProfileFragment.java`, `GuardManualEntryBottomSheet.java`.
  - Khai báo các đích vào [`nav_graph.xml`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/res/navigation/nav_graph.xml).
* [x] **TASK-GRD-13**: Biên dịch kiểm tra `./gradlew :app:assembleDebug` đảm bảo 0 lỗi (BUILD SUCCESSFUL in 47s).
* [x] **TASK-GRD-14**: Cài đặt APK lên thiết bị thật `lzp7ytz5v87dei5p` và chụp ảnh màn hình nghiệm thu toàn bộ 8 màn hình/dialog bảo vệ.

---

## 3. Bằng chứng nghiệm thu giao diện trên thiết bị thật

| STT | Màn hình | Layout XML | Controller Java | Bằng chứng ảnh thật (Artifact) |
|---|---|---|---|---|
| 1 | Quét camera AI (Mẫu 1) | [`fragment_guard_camera.xml`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/res/layout/fragment_guard_camera.xml) | [`GuardCameraFragment.java`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/java/com/example/parkguardai/ui/guard/GuardCameraFragment.java) | `guard_screen1_camera.png` |
| 2 | Ghi nhận vi phạm (Mẫu 2) | [`fragment_guard_violation_form.xml`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/res/layout/fragment_guard_violation_form.xml) | [`GuardViolationFormFragment.java`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/java/com/example/parkguardai/ui/guard/GuardViolationFormFragment.java) | `guard_screen2_violation_form.png` |
| 3 | Lịch sử quét (Mẫu 3) | [`fragment_guard_history.xml`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/res/layout/fragment_guard_history.xml) | [`GuardHistoryFragment.java`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/java/com/example/parkguardai/ui/guard/GuardHistoryFragment.java) | `guard_screen3_history.png` |
| 4 | Quét nhiều xe (Mẫu 4) | [`fragment_guard_multi_vehicle_scan.xml`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/res/layout/fragment_guard_multi_vehicle_scan.xml) | [`GuardMultiVehicleScanFragment.java`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/java/com/example/parkguardai/ui/guard/GuardMultiVehicleScanFragment.java) | `guard_screen4_multi_scan.png` |
| 5 | Xe quét lại trong ngày (Mẫu 5) | [`fragment_guard_rescan_alert.xml`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/res/layout/fragment_guard_rescan_alert.xml) | [`GuardRescanAlertFragment.java`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/java/com/example/parkguardai/ui/guard/GuardRescanAlertFragment.java) | `guard_screen5_rescan_alert.png` |
| 6 | Cảnh báo an ninh bãi xe (Đề xuất thêm - Tab 3) | [`fragment_guard_alerts.xml`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/res/layout/fragment_guard_alerts.xml) | [`GuardAlertsFragment.java`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/java/com/example/parkguardai/ui/guard/GuardAlertsFragment.java) | `guard_screen6_alerts.png` |
| 7 | Ca trực & Hồ sơ (Đề xuất thêm - Tab 4) | [`fragment_guard_profile.xml`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/res/layout/fragment_guard_profile.xml) | [`GuardProfileFragment.java`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/java/com/example/parkguardai/ui/guard/GuardProfileFragment.java) | `guard_screen7_profile.png` |
| 8 | Nhập biển số thủ công (Đề xuất thêm) | [`bottom_sheet_guard_manual_entry.xml`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/res/layout/bottom_sheet_guard_manual_entry.xml) | [`GuardManualEntryBottomSheet.java`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/java/com/example/parkguardai/ui/guard/GuardManualEntryBottomSheet.java) | `guard_screen8_manual_entry.png` |

