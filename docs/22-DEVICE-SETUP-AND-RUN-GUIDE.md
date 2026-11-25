# Hướng dẫn kết nối thiết bị, chạy ứng dụng và đọc log qua ADB

Tài liệu này tổng hợp toàn bộ quy trình thực chiến trên hệ điều hành Windows qua terminal PowerShell: từ kết nối điện thoại/máy ảo, biên dịch và cài đặt APK, mở ứng dụng cho đến cách đọc log chi tiết bằng ADB.

---

## 1. Lệnh chạy nhanh 1 bước (Quick Run Oneliner)

Dưới đây là các câu lệnh gộp 1 dòng chạy trực tiếp trên PowerShell thư mục gốc dự án (`D:\Workplace\Project\ParkGuardAI`):

### Cách A: Cài đặt và Mở App ngay lập tức (Dùng khi file APK đã build sẵn)
> Dùng cách này nếu mã nguồn không thay đổi hoặc bạn vừa chạy `assembleDebug` xong:

```powershell
adb install -r -d app\build\outputs\apk\debug\app-debug.apk; adb shell am start -n com.example.parkguardai/.MainActivity
```

### Cách B: Biên dịch mới + Cài đặt + Tự động mở App (Toàn diện nhất)
> Dùng cách này mỗi khi bạn vừa sửa mã nguồn Java / giao diện XML:

```powershell
.\gradlew.bat assembleDebug; adb install -r -d app\build\outputs\apk\debug\app-debug.apk; adb shell am start -n com.example.parkguardai/.MainActivity
```

### Cách C: Gỡ cài đặt cũ và Cài mới hoàn toàn (Xóa sạch cache/session)
> Dùng cách này nếu muốn đưa ứng dụng về trạng thái nguyên bản ban đầu:

```powershell
adb uninstall com.example.parkguardai; adb install app\build\outputs\apk\debug\app-debug.apk; adb shell am start -n com.example.parkguardai/.MainActivity
```

---

## 2. Kết nối và Quản lý Thiết bị

### 2.1. Kiểm tra danh sách thiết bị kết nối

```powershell
adb devices
```

- **Trạng thái hợp lệ (`device`):**
  ```text
  List of devices attached
  lzp7ytz5v87dei5p        device
  ```
  *(Sẵn sàng cài đặt và chạy ứng dụng)*

- **Trạng thái `offline`:** Thiết bị chưa cho phép truyền dữ liệu hoặc đang khóa màn hình.
- **Trạng thái `unauthorized`:** Thiết bị đang chờ bấm chấp nhận hộp thoại gỡ lỗi trên màn hình.

---

### 2.2. Xử lý triệt để khi thiết bị báo `offline` hoặc ngắt kết nối

Khi `adb devices` báo `offline`, thực hiện lần lượt các bước sau:

1. **Mở sáng và mở khóa màn hình điện thoại:**
   - Hệ điều hành Android (đặc biệt là Xiaomi HyperOS / MIUI) sẽ tự động ngắt kết nối ADB khi màn hình tắt hoặc khóa để bảo vệ dữ liệu.
2. **Chạy lệnh tái kết nối thiết bị:**
   ```powershell
   adb reconnect offline
   ```
3. **Khởi động lại ADB Server (nếu vẫn offline):**
   ```powershell
   adb kill-server; adb start-server; adb devices
   ```
4. **Rút cáp và cắm lại:**
   - Khi cắm lại cáp, nếu màn hình điện thoại hiện popup **"Cho phép gỡ lỗi USB từ máy tính này?"**, hãy tích chọn **"Luôn cho phép từ máy tính này"** và bấm **Cho phép (OK)**.

---

### 2.3. Cài đặt quan trọng trên điện thoại Xiaomi / Redmi / POCO (HyperOS / MIUI)

Để tránh lỗi ngắt kết nối `closed` hoặc `INSTALL_FAILED_USER_RESTRICTED` khi cài qua ADB:
1. Vào **Cài đặt** > **Cài đặt bổ sung** > **Tùy chọn nhà phát triển**.
2. Bật **Gỡ lỗi USB** (USB debugging).
3. Bật **Cài đặt qua USB** (Install via USB).
4. Bật **Gỡ lỗi USB (Cài đặt bảo mật)** nếu thiết bị yêu cầu.
5. Khi cài đặt, luôn để màn hình điện thoại sáng để bấm xác nhận nếu có thông báo bảo mật hiện lên.

---

## 3. Khởi chạy và Điều khiển App qua ADB

| Thao tác | Câu lệnh PowerShell |
|---|---|
| **Mở ứng dụng lên màn hình** | `adb shell am start -n com.example.parkguardai/.MainActivity` |
| **Đóng ứng dụng (Force Stop)** | `adb shell am force-stop com.example.parkguardai` |
| **Khởi động lại ứng dụng** | `adb shell am force-stop com.example.parkguardai; adb shell am start -n com.example.parkguardai/.MainActivity` |
| **Chụp ảnh màn hình điện thoại** | `adb shell screencap -p /sdcard/screen.png; adb pull /sdcard/screen.png current_screen.png; adb shell rm /sdcard/screen.png` |

---

## 4. Đọc Log Runtime qua ADB trên PowerShell

### 4.1. Lọc log sự kiện của ParkGuard AI (Xác thực, Phân quyền, Firestore, Guard, Student)

```powershell
adb logcat | Select-String -Pattern "parkguard|Auth|SignIn|Role|Firestore|Student|Guard|Admin"
```

### 4.2. Bắt log lỗi Crash (Ngoại lệ nghiêm trọng)

Khi ứng dụng bị dừng đột ngột hoặc gặp lỗi văng app:

```powershell
adb logcat -s AndroidRuntime:E System.err:W
```

### 4.3. Xóa sạch bộ đệm log cũ trước khi kiểm thử

Trước khi bấm nút Đăng nhập hoặc thực hiện thao tác kiểm tra mới, nên dọn sạch log cũ:

```powershell
adb logcat -c
```

### 4.4. Dừng đọc log

Nhấn tổ hợp phím **`Ctrl + C`** trong cửa sổ terminal để thoát khỏi chế độ theo dõi logcat.

---

## 5. Bảng tra cứu các lỗi thực tế và cách xử lý

| Lỗi gặp phải | Nguyên nhân | Cách khắc phục |
|---|---|---|
| `lzp7ytz5v87dei5p offline` | Màn hình điện thoại tắt, máy đang khóa hoặc ADB daemon bị treo. | Mở khóa màn hình điện thoại, chạy `adb reconnect offline` hoặc rút cáp cắm lại. |
| `lzp7ytz5v87dei5p unauthorized` | Chưa bấm xác nhận cấp quyền tin cậy máy tính trên điện thoại. | Mở khóa điện thoại, tích chọn "Luôn cho phép" và bấm OK. |
| `INSTALL_FAILED_USER_RESTRICTED` | Xiaomi HyperOS/MIUI chặn cài ứng dụng tự động qua cáp USB. | Vào Tùy chọn nhà phát triển > Bật "Cài đặt qua USB" (Install via USB) và giữ màn hình luôn sáng khi cài. |
| `failed to run abb_exec. Error: closed` | Cáp USB tiếp xúc kém hoặc máy vừa bị ngắt kết nối trong lúc truyền file. | Mở khóa màn hình, cắm lại cáp vào cổng USB ổn định trên máy tính và chạy lại lệnh cài đặt. |
| `logcat: pid '' out of range` | PowerShell Windows bị dính ký tự xuống dòng ẩn khi lấy PID. | Sử dụng bộ lọc chuỗi: `adb logcat \| Select-String -Pattern "parkguard"`. |
