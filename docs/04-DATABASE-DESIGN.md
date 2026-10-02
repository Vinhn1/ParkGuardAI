# 04 — Thiết kế dữ liệu và quan hệ

> **Thiết kế schema vẫn `PROPOSED`**, chưa có database, Firestore SDK hoặc Security Rules trong repository; Firebase Auth SDK mới phục vụ email. `User`, `Vehicle`, `Violation` là POJO cũ; [`UserProfile`](../app/src/main/java/com/example/parkguardai/data/model/UserProfile.java), [`VehicleRecord`](../app/src/main/java/com/example/parkguardai/data/model/VehicleRecord.java), [`ViolationRecord`](../app/src/main/java/com/example/parkguardai/data/model/ViolationRecord.java) và các POJO liên quan biểu diễn tài liệu dữ liệu mục tiêu nhưng **không tạo collection hay thực thi ràng buộc**. ADR-001 đã chọn Firebase; môi trường backend đặc quyền và chi tiết schema còn mở.

## Vấn đề thiết kế phải giải quyết

`Vehicle.ownerId` hiện chỉ lưu một ID và không lưu lịch sử; nếu thay chủ trực tiếp, hồ sơ cũ có thể bị lộ cho chủ mới. `Violation.vehicleId` hiện là chuỗi không có ràng buộc, còn `licensePlate` là kết quả bảo vệ rà; xe chưa đăng ký cần hồ sơ có thể thiếu `vehicleId`. Tọa độ `double` không biểu đạt GPS thiếu. Vì thế thiết kế mục tiêu tách **phương tiện**, **liên kết quyền sử dụng theo thời gian**, **bằng chứng**, **quy trình hồ sơ** và **audit**.

```mermaid
erDiagram
  USER ||--o{ VEHICLE_LINK : yeu_cau
  VEHICLE ||--o{ VEHICLE_LINK : duoc_lien_ket
  USER ||--o{ VIOLATION : ghi_nhan
  VEHICLE o|--o{ VIOLATION : co_the_lien_quan
  VIOLATION ||--|{ EVIDENCE : gom
  EVIDENCE ||--o| LOCATION_SNAPSHOT : vi_tri_luc_chup
  VIOLATION ||--o{ VIOLATION_EVENT : lich_su
  USER ||--o{ VIOLATION_EVENT : tac_nhan
  VIOLATION_TYPE ||--o{ VIOLATION : phan_loai
  USER ||--o{ CONSENT_RECORD : dong_y
  USER {
    string id PK
    string emailCanonical UK
    string phoneCanonical
    string accountStatus
  }
  VEHICLE {
    string id PK
    string plateCanonical
    string type
  }
  VEHICLE_LINK {
    string id PK
    string userId FK
    string vehicleId FK
    string status
    timestamp validFrom
    timestamp validTo
  }
  VIOLATION {
    string id PK
    string vehicleId FK_optional
    string guardId FK
    string primaryEvidenceId FK
    string plateFinal
    timestamp occurredAt
    string status
  }
  EVIDENCE {
    string id PK
    string violationId FK
    string objectKey
    string contentHash
  }
  LOCATION_SNAPSHOT {
    string id PK
    string evidenceId FK
    float latitude
    float longitude
    float locationAccuracy
    timestamp locationTimestamp
    timestamp capturedAt
    string locationSource
    string locationStatus
  }
  VIOLATION_EVENT {
    string id PK
    string violationId FK
    string actorId FK
    timestamp createdAt
  }
```

## Bảng logic và ràng buộc

| Thực thể | Trường cốt lõi đề xuất | Ràng buộc/ghi chú |
|---|---|---|
| `User` | `id`, `fullName`, `emailCanonical`, `phoneCanonical?`, `studentCode?`, `accountStatus`, `createdAt` | `id` từ hệ xác thực; emailVerified lấy từ Firebase Auth. Phone trên Spark chỉ là liên hệ tùy chọn **chưa xác minh**, không unique hay làm định danh; studentCode còn DEC-007B. Role lưu ở nguồn quyền riêng/claim đáng tin. Không có password thuần. |
| `Vehicle` | `id`, `plateRaw`, `plateCanonical`, `type`, `brand?`, `model?`, `color?`, `photoKey?` | Chuẩn hóa biển số có version; lưu raw để đối chiếu. Không suy ra một chủ duy nhất từ `plateCanonical`; cùng biển số phải đưa vào hàng chờ tranh chấp, không tự cấp quyền. |
| `VehicleLink` | `id`, `vehicleId`, `userId`, `status`, `validFrom?`, `validTo?`, `requestedAt`, `reviewedAt?`, `reviewerId?`, `reason?`, `evidenceKey?` | `PENDING`, `VERIFIED`, `REJECTED`, `INACTIVE`; không có hai liên kết `VERIFIED` chồng thời gian cho cùng xe theo chính sách (trường hợp đồng sử dụng cần quyết định riêng). Duyệt có audit. |
| `Violation` | `id`, `vehicleId?`, `guardId`, `primaryEvidenceId`, `plateOcrRaw?`, `plateFinal?`, `recognitionSource`, `typeCode`, `typeVersion`, `status`, `note?`, `occurredAt`, `createdAt`, **projection vị trí ảnh chính** gồm `latitude?`, `longitude?`, `locationAccuracy?`, `locationTimestamp?`, `capturedAt`, `locationSource`, `locationStatus`, `areaCode?`, `idempotencyKey` | `vehicleId` có thể rỗng; `plateFinal` không chứng minh chủ. `guardId` từ phiên; `createdAt` server khác `capturedAt` của ảnh. Projection phải khớp snapshot của ảnh chính, được cập nhật nguyên tử khi đổi ảnh chính; không lấy ảnh bổ sung/lúc lưu làm nguồn thay thế. |
| `Evidence` | `id`, `violationId`, `kind`, `isPrimary`, `objectKey`, `sha256`, `capturedAt`, `mimeType`, `sizeBytes`, `createdBy` | Ảnh gốc bất biến; ảnh crop là dẫn xuất, có liên kết tới ảnh gốc. Mỗi ảnh chụp có thời điểm riêng; chỉ một ảnh hiện trường là ảnh chính. Không lưu URL công khai dài hạn; kiểm quyền khi đọc. |
| `LocationSnapshot` | `id`, `evidenceId`, `latitude?`, `longitude?`, `locationAccuracy?`, `locationTimestamp?`, `capturedAt`, `locationSource`, `locationStatus`, `areaCode?`, `reason?`, `reviewerId?`, `reviewedAt?` | Snapshot logic riêng của từng ảnh, dự kiến nhúng vào `evidence/{evidenceId}.location` trong Firestore; không cần collection riêng nếu kích thước và quyền truy cập phù hợp. Fix tự động chỉ hợp lệ khi có thời điểm đo, sai số ngang và chênh với `capturedAt` đạt ngưỡng đã duyệt. Nhập/chỉnh thủ công có nguồn, lý do và người duyệt riêng. Không dùng giá trị mặc định `0,0` biểu thị thiếu GPS. |
| `ViolationEvent` | `id`, `violationId`, `actorId`, `action`, `before/after` tối thiểu, `reason`, `createdAt`, `requestId` | Append-only theo khả năng backend, không chứa ảnh/token hoặc PII thừa; phục vụ audit. |
| `ViolationType` | `code`, `version`, `labelVi`, `activeFrom`, `activeTo?` | Enum hiện có là mã tạm; khi admin quản lý danh mục, hồ sơ cũ cần giữ version/label tại thời điểm ghi. |
| `ConsentRecord` | `id`, `userId`, `noticeVersion`, `purposeCodes`, `acceptedAt`, `source`, `withdrawnAt?` | Chỉ ghi điều đã thông báo/đồng ý theo chính sách pháp lý; không coi checkbox là giấy phép cho mọi xử lý. |

## Bất biến và truy vấn quyền

1. Sinh viên có thể có `0..n` `VehicleLink`; mỗi yêu cầu duyệt riêng. Onboarding theo ADR-004 chỉ hoàn tất sau khi tạo **ít nhất một yêu cầu xe** sau xác minh đăng nhập. Trạng thái `PENDING/REJECTED` không cấp quyền xem vi phạm; tài khoản có thể tạm chưa có link khi đang dở onboarding.
2. Truy vấn hồ sơ cho sinh viên cần kiểm tra `link.userId = auth.uid`, `link.status = VERIFIED`, `validFrom <= occurredAt < validTo` (nếu `validTo` có), trạng thái hồ sơ được phép công bố và không có tranh chấp đang chặn. Quy tắc sau thời gian thu hồi/quyền xem quá khứ cần chốt ở DEC-004/006; không suy ra tự động từ plate.
3. Ràng buộc biển số, khoảng thời gian và `idempotencyKey` phải được thực thi nguyên tử ở nguồn đáng tin. Firestore đã được chọn nhưng **không có khóa ngoại/unique constraint kiểu SQL**; cần document claim xác định duy nhất + transaction, Rules chặn ghi trái quyền. Email/password thuộc Firebase Authentication, không lưu password trong Firestore. Không hứa unique toàn cục chỉ vì có truy vấn kiểm tra trên UI.
4. Sửa hồ sơ sau xác nhận tạo `ViolationEvent` và bản bằng chứng mới nếu cần; ảnh gốc và dữ liệu gốc không bị ghi đè âm thầm. Trạng thái cho phép chuyển cần bảng chuyển trạng thái được duyệt.
5. Khi chụp ảnh chính, tạo `capturedAt` tại sự kiện chụp và yêu cầu fix foreground. `locationTimestamp` là thời điểm thiết bị đo fix, không phải thời điểm nhận callback hoặc thời điểm lưu hồ sơ. Kiểm tuổi fix/khoảng lệch bằng đồng hồ đơn điệu trên **cùng thiết bị/lần khởi động**, rồi lưu mốc UTC và kết quả kiểm; nếu thiếu thông tin đo/độ chính xác hoặc sai ngưỡng, `locationStatus` không thể là `AUTO_ACCEPTED`. Server lưu `createdAt` riêng và kiểm định dạng/quan hệ; dữ liệu do thiết bị cung cấp là bằng chứng cần rà, không phải chứng nhận tuyệt đối rằng tọa độ không bị giả mạo.
6. `LocationSnapshot.locationSource` đề xuất `FUSED_AUTO`, `MANUAL_AREA` hoặc `MANUAL_COORDINATE` nếu chính sách cho phép. Fused Location Provider có thể kết hợp nhiều nguồn; không gắn nhãn “GPS vệ tinh” khi không kiểm chứng provider. `locationStatus` đề xuất `PENDING`, `AUTO_ACCEPTED`, `REJECTED_STALE`, `REJECTED_INACCURATE`, `UNAVAILABLE`, `MANUAL_PENDING`, `MANUAL_APPROVED`; tên/chuyển trạng thái cuối chờ DEC-008A. Manual không có số đo độ chính xác thiết bị thì `locationAccuracy = null`, không tạo con số giả.
7. Hồ sơ tham chiếu `primaryEvidenceId` và projection vị trí từ `LocationSnapshot` của ảnh chính. Thêm ảnh phụ chỉ thêm `Evidence`/snapshot mới, không ghi đè projection. Đổi ảnh chính yêu cầu thao tác tường minh, kiểm lại điều kiện vị trí và audit nếu hồ sơ đã lưu. Firestore transaction không bao gồm file Storage; cần trạng thái upload/finalize, kiểm quyền và dọn object mồ côi.

## Ánh xạ Firestore đề xuất sau ADR-001

Kiểu: `string`, `boolean`, `number`, `Timestamp`, `map`, `array`, `null`. Dấu `*` là bắt buộc khi tạo; `?` là tùy chọn theo chính sách. ID do Firebase Auth/server hoặc UUID không đoán theo plate. Mọi document có `schemaVersion` và server timestamp phù hợp; profile không chứa password. Tên collection/field là **thiết kế để duyệt trước code**, không phải schema đã tạo.

| Collection/document ID | Trường và kiểu tối thiểu | Quyền, quan hệ, ràng buộc |
|---|---|---|
| `users/{uid}`; ID = Firebase Auth UID | `fullName* : string`, `emailCanonical* : string`, `phoneCanonical? : string`, `studentCode? : string`, `accountStatus* : string`, `createdAt* : Timestamp`, `schemaVersion* : number` | Chỉ profile nghiệp vụ; `emailVerified` phải đọc từ Auth hoặc dữ liệu server đồng bộ tin cậy. `phoneCanonical` là liên hệ tùy chọn chưa xác minh trên Spark, không cấp quyền/khôi phục từ số này. S chỉ sửa trường tự khai của mình; role/grants tách riêng. |
| `accessGrants/{uid}`; ID = Auth UID | `role* : string`, `guardScopes? : array<string>`, `adminPermissions? : array<string>`, `active* : boolean`, `updatedAt* : Timestamp`, `updatedBy* : string` | Chỉ môi trường đặc quyền ghi; không cho client tự cấp ADMIN/GUARD. Có thể kết hợp custom claims cho role thô; phạm vi và thu hồi phải kiểm lại. |
| `vehicles/{vehicleId}`; ID = UUID/server ID | `plateRaw* : string`, `plateCanonical* : string`, `plateNormVersion* : number`, `type* : string`, `createdBy* : uid`, `createdAt* : Timestamp`, `brand?/color?/photoObjectKey?` | Một hồ sơ xe là dữ liệu tự khai cho đến khi có link duyệt. Không dùng `plateCanonical` làm khóa quyền; chỉnh plate tạo yêu cầu mới. |
| `vehicleVerificationRequests/{requestId}`; ID = UUID/server ID | `requesterUid* : string`, `vehicleId* : string`, `requestType* : string`, `status* : PENDING/APPROVED/REJECTED`, `requestedAt* : Timestamp`, `reviewedAt?/reviewerUid?/reason?/evidenceObjectKey?` | S chỉ tạo `PENDING` cho mình; không sửa trường duyệt. A/nhân sự có grant xét duyệt thủ công; kiểm trùng và audit. Có thể có nhiều request PENDING cùng plate. |
| `vehicleLinks/{linkId}`; ID = UUID/server ID | `uid* : string`, `vehicleId* : string`, `status* : PENDING/VERIFIED/REJECTED/INACTIVE`, `validFrom?/validTo? : Timestamp`, `verificationRequestId* : string`, `reviewerUid?/reviewedAt?` | Lịch sử liên kết theo thời gian, không sửa `ownerId` đè lịch sử. Chỉ nguồn đáng tin đặt VERIFIED/INACTIVE; S xem link của mình. |
| `plateClaims/{plateKey}`; ID = khóa xác định từ biển chuẩn hóa **do backend tạo** | `plateCanonical* : string`, `vehicleId* : string`, `activeLinkId* : string`, `claimedAt* : Timestamp`, `version* : number` | Collection riêng, **client không đọc/ghi**. Một document active cho một plate tại thời điểm; cân nhắc HMAC server-side cho ID để không lộ plate trong path. Không coi đây là chứng cứ sở hữu; chỉ khóa cấp quyền đồng thời. |
| `violations/{violationId}`; ID = UUID hoặc idempotency mapping server | `guardUid* : string`, `scopeId* : string`, `primaryEvidenceId* : string`, `plateFinal? : string`, `typeCode* : string`, `typeVersion* : number`, `status* : string`, `capturedAt* : Timestamp`, `locationStatus* : string`, `locationSource* : string`, `latitude?/longitude?/locationAccuracy? : number`, `locationTimestamp? : Timestamp`, `createdAt* : Timestamp`, `idempotencyKey* : string` | `vehicleId?` chỉ sau liên kết đáng tin; projection GPS phải khớp ảnh chính. Client không tự đặt `guardUid`, trạng thái phê duyệt hoặc đổi ảnh chính sau xác nhận. |
| `violations/{id}/evidence/{evidenceId}`; ID = UUID | `kind* : string`, `isPrimary* : boolean`, `objectKey* : string`, `capturedAt* : Timestamp`, `sha256* : string`, `mimeType* : string`, `sizeBytes* : number`, `location* : map` gồm snapshot và trạng thái | Metadata Firestore, bytes nằm ở Storage riêng. Chỉ một ảnh hiện trường chính; `location` lưu cả fix thiếu/trạng thái, không giả tọa độ; ảnh phụ không đè projection hồ sơ. |
| `violationTypes/{code}`; ID = mã loại | `labelVi* : string`, `version* : number`, `active* : boolean`, `effectiveFrom* : Timestamp` | A có quyền danh mục; mã/version trong hồ sơ giữ nghĩa lịch sử, không đổi enum đã lưu âm thầm. |
| `auditLogs/{eventId}`; ID = server ID | `actorUid* : string`, `action* : string`, `targetPath* : string`, `at* : Timestamp`, `reason? : string`, `requestId* : string`, `changes? : map` tối thiểu | Chỉ backend đáng tin ghi; client không sửa/xóa. Tránh token, ảnh hoặc PII thừa. |
| `userViolationViews/{uid}/items/{violationId}`; ID = UID + violation ID | `violationId* : string`, `status* : string`, `occurredAt* : Timestamp`, `summary* : map` tối thiểu | **Projection đề xuất**, do backend tạo/thu hồi sau kiểm link VERIFIED và khoảng hiệu lực để query S an toàn; không thay quyền ảnh/chi tiết. Cách làm cuối cần test Rules và chuyển nhượng. |

`ConsentRecord` logic có thể đặt ở `users/{uid}/consents/{consentId}` (chỉ người dùng đọc của mình, backend ghi kết quả đã kiểm) để tách khỏi profile. Ảnh phương tiện/bằng chứng lưu trong **Firebase Cloud Storage**, Firestore chỉ giữ `objectKey`/metadata; không lưu base64 hoặc URL công khai trong document.

**Ánh xạ model hiện có:** `users/{uid}` ↔ `UserProfile`, `vehicles/{vehicleId}` ↔ `VehicleRecord`, `violations/{violationId}` ↔ `ViolationRecord`. Các class `User`, `Vehicle`, `Violation` cũ chưa được thay thế hoặc tự động chuyển dữ liệu; việc mapping và migration là task riêng. `UserProfile.phoneCanonical` để trống được, không có cờ `phoneVerified` trong phương án Spark; `studentCode` còn chờ DEC-007B. Trạng thái xác minh email lấy từ Firebase Authentication hoặc nguồn server đáng tin, không do client tự ghi. Các giá trị `plateCanonical`, `plateNormVersion`, `primaryEvidenceId` và projection vị trí chỉ là trường dữ liệu; ràng buộc trùng biển, ảnh chính và quyền truy cập cần kiểm tại Rules/backend trước khi ghi.

### Giao dịch, chỉ mục và kiểm tra dữ liệu

**Một plate không VERIFIED đồng thời cho hai tài khoản:** khi ADMIN duyệt, môi trường backend đáng tin cậy chuẩn hóa plate, tính `plateKey`, rồi trong **Firestore transaction** đọc `plateClaims/{plateKey}`, request và link cũ; nếu claim active thuộc link khác thì dừng và tạo xung đột; nếu hợp lệ thì ghi claim + link VERIFIED + request APPROVED + audit theo thiết kế nhất quán. Chuyển nhượng cần đóng link/claim cũ và mở link mới trong giao dịch, giữ `validFrom/validTo`. Không làm kiểm tra bằng truy vấn rồi ghi riêng vì hai người duyệt có thể chạy đồng thời. Transaction có thể retry và thất bại khi offline; handler phải idempotent. [Firestore transactions](https://firebase.google.com/docs/firestore/manage-data/transactions). Backend đặc quyền có thể là Cloud Functions hoặc môi trường server khác dùng Admin SDK; **chưa chọn môi trường triển khai và chưa tạo project**.

**Kiểm tra:** Rules giới hạn field/type/size, owner khi tạo request, không cho client sửa status/quyền; backend kiểm quan hệ UID–vehicle–request, plateNormVersion, trạng thái chuyển, timestamp, tọa độ hợp lệ `[-90,90]/[-180,180]`, sai số không âm, ảnh chính thuộc cùng violation, idempotency. Không coi Rules là khóa ngoại; server SDK bỏ qua Rules nên IAM/validation backend bắt buộc. Truy vấn list phải phù hợp Rules vì [Rules không lọc hộ](https://firebase.google.com/docs/firestore/security/rules-query).

**Index dự kiến, chưa tạo:** `vehicleVerificationRequests(status, requestedAt)` cho hàng chờ; `vehicleLinks(uid, status, validFrom)` cho xe của sinh viên; `violations(guardUid, createdAt desc)` và `(scopeId, status, createdAt desc)` cho guard/admin; `userViolationViews/{uid}/items(status, occurredAt desc)` cho sinh viên; `vehicles(plateCanonical)` chỉ cho truy vấn nội bộ. Kiểm lại index thực tế bằng query/rules và chi phí sau pilot, không tự tạo trong nhiệm vụ tài liệu.

## Chuyển trạng thái đề xuất

| Đối tượng | Chuyển hợp lệ đề xuất | Điều kiện |
|---|---|---|
| VehicleLink | `PENDING → VERIFIED/REJECTED`; `VERIFIED → INACTIVE` | Người được quyền duyệt, lý do, thời gian; yêu cầu sửa biển số tạo vòng duyệt mới. |
| Violation | `PENDING → CONFIRMED → RESOLVED` | `PENDING` là bản đang chờ người có quyền xác nhận; hủy/sửa/kháng nghị cần trạng thái bổ sung nếu được duyệt, không ngầm thêm vào enum hiện có. |

## Chiến lược schema và di chuyển

Ghi version schema/mapping; thêm trường mới với default/nullable có kiểm soát; chạy backfill kiểm tra trước khi bật ràng buộc; thử migration trên dữ liệu giả và bản sao ẩn danh; giữ rollback vận hành, backup được kiểm quyền; không xóa trường/bản ghi cũ khi chưa có kế hoạch. Mapping `User.fullname → fullName`, `Vehicle.ownerId → VehicleLink`, `Violation.originalImageUrl/plateImageUrl → Evidence.objectKey` chỉ là **đề xuất**, không tự sửa model. Firestore đã chọn; thời hạn giữ, môi trường backend đặc quyền và thiết kế offline nâng cao còn mở tại [Decision Log](15-DECISION-LOG.md).
