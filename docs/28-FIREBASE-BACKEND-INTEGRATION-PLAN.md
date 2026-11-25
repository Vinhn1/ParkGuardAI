# Kế hoạch Kết nối Backend Cloud Firestore & Firebase Auth (Firebase Backend Integration Plan)

> **Mã tài liệu:** `docs/28-FIREBASE-BACKEND-INTEGRATION-PLAN.md`  
> **Cập nhật:** 2026-10-26  
> **Trạng thái:** Dự thảo kế hoạch chờ phê duyệt (Proposal / Pending Approval)  
> **Mục tiêu:** Chuyển đổi toàn bộ ứng dụng ParkGuard AI từ dữ liệu minh họa (Mock/Demo) sang kết nối Firebase Authentication và Cloud Firestore thực tế theo kiến trúc MVVM, đảm bảo an toàn phân quyền, không rò rỉ dữ liệu cá nhân, và 100% Android Native Java.

---

## 1. Hiện trạng & Cơ sở kỹ thuật

1. **Môi trường Firebase hiện có**:
   - Tệp cấu hình [`google-services.json`](file:///d:/Workplace/Project/ParkGuardAI/app/google-services.json) đã được tích hợp với Project ID: `parkguardai-dev`.
   - Các SDK trong [`app/build.gradle.kts`](file:///d:/Workplace/Project/ParkGuardAI/app/build.gradle.kts):
     * `firebase-auth` (Firebase Authentication)
     * `firebase-firestore` (Cloud Firestore)
     * `play-services-location` (FusedLocationProviderClient)
     * `cloudinary-android-core` (Kho lưu trữ ảnh chứng cứ theo DEC-024)
   - Bộ quy tắc an toàn dữ liệu cơ bản đã có tại [`firestore.rules`](file:///d:/Workplace/Project/ParkGuardAI/firestore.rules).
2. **Các Datasource/Repository hiện có**:
   - [`FirebaseEmailAuthDataSource.java`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/java/com/example/parkguardai/data/datasource/FirebaseEmailAuthDataSource.java): Đăng ký, đăng nhập email/mật khẩu, gửi email xác minh, gửi email đặt lại mật khẩu.
   - [`FirestoreAccessGrantDataSource.java`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/java/com/example/parkguardai/data/datasource/FirestoreAccessGrantDataSource.java): Đọc quyền `accessGrants/{uid}` từ server để phân định vai trò `STUDENT`, `SECURITY_GUARD`, `ADMIN`.
   - [`FirestoreStudentOnboardingDataSource.java`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/java/com/example/parkguardai/data/datasource/FirestoreStudentOnboardingDataSource.java): Giao dịch tạo hồ sơ sinh viên `users/{uid}`, xe đầu tiên `vehicles/first_{uid}` và yêu cầu duyệt `vehicleVerificationRequests/first_{uid}`.
   - [`ViolationRepository.java`](file:///d:/Workplace/Project/ParkGuardAI/app/src/main/java/com/example/parkguardai/data/repository/ViolationRepository.java): Quy trình upload ảnh lên Cloudinary và lưu `violations/{violationId}` kèm snapshot GPS.

---

## 2. Kiến trúc Luồng Dữ liệu Mục tiêu (MVVM Pattern)

```
┌────────────────────────────────────────────────────────┐
│                      UI Layer                          │
│  (Fragment + XML ViewBinding + Material 3 Dark Theme)   │
└──────────────────────────┬─────────────────────────────┘
                           │ Lắng nghe LiveData / Task
┌──────────────────────────▼─────────────────────────────┐
│                    ViewModel Layer                     │
│  (Quản lý UI State: Loading / Success / Error / Empty) │
└──────────────────────────┬─────────────────────────────┘
                           │ Gọi nghiệp vụ
┌──────────────────────────▼─────────────────────────────┐
│                   Repository Layer                     │
│  (AuthRepository, StudentRepository, GuardRepository,  │
│   AdminRepository, ViolationRepository)                │
└─────────────┬────────────────────────────┬─────────────┘
              │                            │
┌─────────────▼──────────────┐ ┌───────────▼─────────────┐
│    Firebase Auth SDK       │ │   Cloud Firestore SDK   │
│ (Email/Password, Tokens)   │ │ (Collections & Rules)   │
└────────────────────────────┘ └───────────┬─────────────┘
                                           │
                               ┌───────────▼─────────────┐
                               │ Cloudinary Image Upload │
                               │ (Lưu trữ ảnh chứng cứ)  │
                               └─────────────────────────┘
```

---

## 3. Cấu trúc Collections Cloud Firestore

| Tên Collection | Mục đích | Quyền đọc / ghi (Security Rules) |
|---|---|---|
| `users/{uid}` | Hồ sơ cá nhân người dùng (Sinh viên, Bảo vệ, Admin) | Chủ tài khoản đọc; Sinh viên tạo khi onboarding; Admin đọc/quản lý |
| `accessGrants/{uid}` | Cấp quyền vai trò hệ thống (`STUDENT`, `SECURITY_GUARD`, `ADMIN`, scopes) | Chủ tài khoản đọc; Chỉ Admin tạo/sửa |
| `vehicles/{vehicleId}` | Danh sách phương tiện đăng ký | Sinh viên đọc xe của mình; Sinh viên tạo; Admin đọc/cập nhật trạng thái |
| `vehicleVerificationRequests/{requestId}` | Hàng đợi yêu cầu xác minh xe | Sinh viên tạo yêu cầu; Sinh viên đọc yêu cầu của mình; Admin duyệt/từ chối |
| `violations/{violationId}` | Hồ sơ ghi nhận vi phạm | Bảo vệ tạo trong scope; Bảo vệ đọc ca trực; Sinh viên đọc vi phạm thuộc xe của mình; Admin toàn quyền |
| `violations/{violationId}/evidence/{evidenceId}` | Bằng chứng ảnh & snapshot GPS | Cùng quyền với hồ sơ vi phạm |
| `parkingHistory/{historyId}` | Lịch sử xe vào / ra bãi | Sinh viên đọc lịch sử xe mình; Bảo vệ/Hệ thống ghi nhận |
| `notifications/{notificationId}` | Thông báo an ninh & trạng thái | Người dùng đọc thông báo của chính mình (`recipientUid == auth.uid`) |
| `systemConfig/scanRules` | Cấu hình quy tắc quét lặp & an ninh | Bảo vệ/Sinh viên đọc; Admin sửa |

---

## 4. Danh mục Task Chi tiết theo 5 Giai đoạn

### GIAI ĐOẠN 1: Quản lý Phiên Đăng nhập & Điều hướng Vai trò (Auth & Role Routing)
*Mục tiêu: Đăng nhập thật bằng Firebase Auth, tự động nhận diện Role và chuyển đúng phân hệ.*

- [x] **TASK-BACKEND-01**: Xây dựng `AuthRepository.java` hoàn chỉnh:
  - Tích hợp `FirebaseEmailAuthDataSource` và `FirestoreAccessGrantDataSource`.
  - Hàm `signIn(email, password)`: Đăng nhập -> Kiểm tra `emailVerified` -> Đọc `accessGrants/{uid}` hoặc `users/{uid}` -> Trả về `UserRole` (`ADMIN`, `SECURITY_GUARD`, `STUDENT`).
  - Hàm `register(email, password)`: Tạo Auth -> Gửi email xác minh.
  - Hàm `sendPasswordReset(email)`: Gửi liên kết khôi phục an toàn.
  - Hàm `signOut(context)`: Xóa phiên Firebase Auth và cache demo.
- [x] **TASK-BACKEND-02**: Kết nối `LoginFragment.java` với `AuthRepository`:
  - Hiển thị trạng thái Loading khi nhấn nút Đăng nhập.
  - Bắt lỗi tiếng Việt thân thiện: Sai mật khẩu, tài khoản không tồn tại, email chưa xác minh, lỗi kết nối mạng.
  - Điều hướng chính xác theo vai trò thông qua `viewModel.getNavigationEvent()`:
    * `ADMIN` -> `adminDashboardFragment`
    * `SECURITY_GUARD` -> `guardHomeFragment`
    * `STUDENT` -> Kiểm tra xem đã có hồ sơ/xe chưa: nếu chưa -> `studentOnboardingFragment`, nếu rồi -> `studentHomeFragment`.
- [x] **TASK-BACKEND-03**: Kết nối `RegisterFragment.java` & `ForgotPasswordFragment.java`:
  - Đăng ký tài khoản mới và chuyển sang `registrationSubmittedFragment` sau onboarding.
  - Luồng khôi phục mật khẩu gửi email thực tế qua Firebase Auth.
- [x] **TASK-BACKEND-04**: Kết nối nút **Đăng xuất** trên màn hình Profile của cả 3 phân hệ (`StudentProfileFragment`, `GuardProfileFragment`, `AdminMoreFragment`) để xóa phiên và quay về `WelcomeFragment`.

---

### GIAI ĐOẠN 2: Phân hệ Sinh viên (Student Portal Data Integration)
*Mục tiêu: Đọc và hiển thị dữ liệu xe, vi phạm, lịch sử vào/ra và thông báo thực tế của sinh viên.*

- [x] **TASK-BACKEND-05**: Xây dựng `StudentRepository.java`:
  - `getMyProfile()`: Đọc thông tin sinh viên từ `users/{uid}` và hỗ trợ Demo fallback.
  - `getMyVehicles()`: Đọc `vehicles/first_{uid}` + `vehicleVerificationRequests/first_{uid}` hoặc Demo.
  - `registerVehicle(...)`: Ghi Firestore và Demo với trạng thái `PENDING`.
  - `getMyViolations()`: Query `violations` theo danh sách biển số xe đã đăng ký.
  - `submitViolationComplaint(violationId, reason)`: Cập nhật khiếu nại vi phạm sang `APPEALED`.
  - `getMyParkingHistory()`: Cung cấp nhật ký lượt xe vào/ra bãi theo thời gian.
  - `getMyNotifications()`: Tải thông báo vi phạm, cảnh báo lượt vào bất thường, phê duyệt xe và bảo trì.
- [x] **TASK-BACKEND-06**: Nối dữ liệu vào màn hình Sinh viên:
  - `StudentHomeFragment`: Hiển thị tên thật, biển số thật, trạng thái trong bãi, số vi phạm chưa xử lý, lịch sử gần nhất qua `StudentViewModel`.
  - `StudentAddVehicleFragment`: Nối nút "Gửi yêu cầu duyệt" với `registerVehicle` của ViewModel.
  - `StudentViolationsFragment` & `StudentViolationDetailFragment`: Hiển thị danh sách vi phạm thật và gửi khiếu nại qua ViewModel.
  - `StudentNotificationsFragment`: Hiển thị danh sách thông báo và xử lý nút phản hồi "Là tôi" / "Không phải tôi".
  - `StudentParkingHistoryFragment`: Hiển thị nhật ký vào/ra thực tế theo bộ lọc Hôm nay / 7 ngày / Tháng.
  - `StudentProfileFragment`: Hiển thị thông tin sinh viên thật và danh sách xe sở hữu.

---

### GIAI ĐOẠN 3: Phân hệ Bảo vệ (Guard Portal Data Integration)
*Mục tiêu: Lưu trữ hồ sơ vi phạm hiện trường thật kèm định vị GPS và ảnh Cloudinary; tải sổ vi phạm.*

- [x] **TASK-BACKEND-07**: Nối luồng Ghi nhận vi phạm (`GuardCameraFragment` -> `GuardReviewFragment` -> `GuardViolationFormFragment`):
  - Tích hợp `ViolationRepository.submit`: Upload ảnh gốc + ảnh crop biển số lên Cloudinary -> Lấy snapshot GPS foreground đạt chuẩn DEC-008A (sai số <= 30m, tuổi <= 10s) -> Ghi `violations/{violationId}` và `violations/{violationId}/evidence`.
  - Hiển thị Dialog tiến trình (`dialog_upload_progress.xml`) và Dialog thành công (`dialog_save_success.xml`).
- [x] **TASK-BACKEND-08**: Xây dựng `GuardRepository.java`:
  - `getGuardRecords()`: Tải danh sách vi phạm gần đây do bảo vệ hiện tại ghi nhận (`guardUid == uid`).
  - `checkDuplicateScan(plate)`: Kiểm tra xe quét lặp trong khoảng thời gian quy định.
  - `getSecurityAlerts()`: Lấy danh sách cảnh báo an ninh bãi xe.
- [x] **TASK-BACKEND-09**: Nối dữ liệu vào `GuardHistoryFragment`, `GuardAlertsFragment`, `GuardProfileFragment`.

---

### GIAI ĐOẠN 4: Phân hệ Quản trị viên (Admin Portal Data Integration)
*Mục tiêu: Xét duyệt xe sinh viên, xử lý vi phạm, quản lý ca trực và cấp tài khoản bảo vệ thật.*

- [x] **TASK-BACKEND-10**: Xây dựng `AdminRepository.java`:
  - `getDashboardKPIs()`: Thống kê số xe trong bãi, số vi phạm hôm nay, số camera, số xe chờ duyệt.
  - `getPendingVehicleRequests()`: Lấy danh sách yêu cầu xác minh xe `PENDING`.
  - `reviewVehicleRequest(requestId, isApproved, rejectReason)`: Giao dịch cập nhật trạng thái duyệt/từ chối và kích hoạt `vehicleLinks` `VERIFIED`.
  - `getAllViolations()`: Tải danh mục vi phạm toàn trường và xử lý đóng hồ sơ / khiếu nại.
  - `getUsersAndGuards()`: Tải danh sách người dùng và nhân sự bảo vệ.
  - `createGuardAccount(name, code, phone, email, password, shift, scopes)`: Tạo Auth cho bảo vệ và ghi `accessGrants/{uid}` với role `SECURITY_GUARD`.
  - `getScanRules()` / `saveScanRules(rules)`: Đọc và lưu cấu hình quy tắc quét lặp `systemConfig/scanRules`.
- [x] **TASK-BACKEND-11**: Nối dữ liệu vào các màn hình Admin:
  - `AdminDashboardFragment`: Hiển thị số liệu thật trên các thẻ KPI.
  - `AdminVerificationQueueFragment`: Danh sách xe chờ duyệt -> Bấm Duyệt / Từ chối cập nhật Firestore tức thì.
  - `AdminViolationsFragment`: Danh sách vi phạm thật và bộ lọc trạng thái.
  - `AdminUsersFragment` & `AdminCreateGuardFragment`: Danh sách nhân sự và form tạo tài khoản bảo vệ mới.
  - `AdminMoreFragment`: Nút "Lưu quy tắc" cập nhật Firestore.

---

### GIAI ĐOẠN 5: Rà soát Security Rules, Biên dịch & Nghiệm thu
*Mục tiêu: Đảm bảo Security Rules an toàn tuyệt đối, build 0 lỗi và kiểm thử luồng thực tế.*

- [ ] **TASK-BACKEND-12**: Cập nhật [`firestore.rules`](file:///d:/Workplace/Project/ParkGuardAI/firestore.rules):
  - Bổ sung rules cho `parkingHistory`, `notifications`, `systemConfig/scanRules`.
  - Đảm bảo Admin có quyền duyệt xe (`vehicleVerificationRequests`), sinh viên chỉ đọc xe của mình.
- [ ] **TASK-BACKEND-13**: Biên dịch `./gradlew :app:assembleDebug` đạt 0 lỗi (BUILD SUCCESSFUL).
- [ ] **TASK-BACKEND-14**: Cài đặt APK lên thiết bị thật `lzp7ytz5v87dei5p`, kiểm thử luồng Đăng nhập -> Phân quyền -> Đọc/Ghi dữ liệu Firebase thực tế và báo cáo bằng chứng ảnh chụp.

---

## 5. Tiêu chí Nghiệm thu & Cam kết kỹ thuật

1. **Nguyên tắc kỹ thuật bắt buộc**:
   - 100% Android Native Java, XML View System, Material 3, ViewBinding. Tuyệt đối không dùng Kotlin/Compose.
   - Sử dụng Firebase Android Java Tasks API (`addOnSuccessListener`, `addOnFailureListener`, `Tasks.whenAll`).
   - Mọi chuỗi hiển thị tiếng Việt có dấu qua Android String Resources.
   - Không chứa secret/password trong code, log hay Firestore; mật khẩu thuần chỉ truyền trực tiếp vào Firebase Auth.
2. **Cơ chế Chuyển đổi linh hoạt (Demo Mode Fallback)**:
   - Trong quá trình phát triển, ứng dụng vẫn hỗ trợ nút "Chế độ thử nghiệm giao diện (Debug)" ở màn hình Welcome để xem nhanh UI khi cần mà không làm ảnh hưởng đến luồng Firebase thật.
