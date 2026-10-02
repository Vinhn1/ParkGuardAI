# 10 — Quy trình Git và review

> Quy tắc làm việc được người dùng yêu cầu; không có thao tác Git thay đổi lịch sử trong nhiệm vụ tài liệu. Nhánh hiện tại khi khảo sát là `dev`, working tree đã có nhiều sửa đổi sẵn; xem [hiện trạng](13-CURRENT-PROJECT-STATUS.md).

## Trước mọi thay đổi

Chạy `git status --short --branch`, đọc diff liên quan, kiểm tra `.gitignore` và file hiện có. Không ghi đè công việc người khác. Nêu phạm vi file, lý do và tác động. Nếu yêu cầu chỉ là tài liệu, không sửa Java/XML/Gradle/asset. Nếu còn thay đổi chưa rõ tác giả, giữ nguyên, không reset hoặc stage chung.

## Quy ước nhánh đề xuất, chưa tự áp dụng

`main` ổn định; `dev` tích hợp; `feature/...`, `fix/...`, `docs/...` theo một chủ đề. Đây là quy ước **đề xuất**, không tự tạo/đổi/xóa nhánh hoặc merge. Nhánh thực tế và workflow remote phải được kiểm tra mỗi phiên.

## Commit và phê duyệt

Conventional Commits: `feat(auth): add student registration`, `fix(ocr): handle invalid result`, `docs: update project requirements`, `test(vehicle): cover plate conflicts`. Một commit tập trung vào một thay đổi có nghĩa. Trước khi **đề xuất** commit: xem status, toàn bộ diff, file nhạy cảm, file không liên quan, kết quả test; tóm tắt và đề xuất message. **Chờ người dùng phê duyệt trước `git add`, `commit`, `push`, merge, rebase, reset, đổi/xóa nhánh**. Không force push.

## Kiểm tra dữ liệu nhạy cảm

Không commit API key, token, tài khoản, khóa ký, file môi trường, dữ liệu sinh viên, ảnh thật, tập huấn luyện lớn hoặc cấu hình AI cá nhân. [.gitignore](../.gitignore) đã có `local.properties`, `.env*`, `*.jks`, `*.keystore`, `.agents/`, `.gemini/`; chưa có mục riêng `.codex/`, `.aws/`, `google-services.json`. Kiểm tra chúng trước mỗi đề xuất commit; sự thiếu mục ignore không chứng minh có bí mật trong thư mục. Asset TFLite 10,6 MB đang được stage từ trước nhiệm vụ tài liệu; phải kiểm tra license/nguồn và chiến lược lưu binary trước khi đề xuất commit.

## Review và báo cáo

Review mã phải đối chiếu yêu cầu/Decision Log, quyền truy cập dữ liệu, ràng buộc plate/phone/vehicle, lifecycle, tài nguyên, String Resources, test và thay đổi dependency. Review tài liệu phải kiểm tra link, thuật ngữ `IMPLEMENTED/PARTIAL/PLANNED/PROPOSED/UNKNOWN`, mã yêu cầu, nhận định có bằng chứng và các quyết định chưa được duyệt. Báo cáo rõ file đã đổi, cách kiểm tra, lỗi còn lại; không nói build thành công khi chưa chạy được.

### Trình tự review đề xuất

1. Xác định phạm vi thay đổi và người sở hữu phần working tree có sẵn. Không tự gộp file ngoài phạm vi vào thay đổi của mình.
2. Xem `git diff --name-only` và nội dung diff; với file đã stage, xem cả `git diff --cached`. File binary cần kiểm nguồn gốc, kích thước và quyền sử dụng riêng vì diff văn bản không đủ.
3. Đối chiếu từng hunk với mã yêu cầu, quyền dữ liệu và quyết định đã duyệt. Nếu phát hiện quyết định mở, ghi rõ phương án và dừng phần phụ thuộc thay vì tự chọn.
4. Kiểm tra đường dẫn nội bộ tài liệu, resource XML, tên biến/chuỗi hiển thị và phiên bản model/dependency. Kiểm tra secret trên **danh sách file dự kiến** và diff mà không in giá trị nhạy cảm ra báo cáo.
5. Ghi kết quả test với lệnh, thiết bị/môi trường và trạng thái pass/fail/chưa chạy. Lỗi chưa xử lý phải có ảnh hưởng và bước kế tiếp.
6. Đề xuất commit message và phạm vi chính xác để người dùng xét duyệt. Chỉ thực hiện thao tác Git thay đổi repository khi có phê duyệt cụ thể cho thao tác đó.

Nếu phát hiện file được người khác sửa trong lúc làm việc, đọc lại trạng thái và đối chiếu nội dung trước khi tiếp tục. Không dùng `reset`, `checkout`, `restore` hoặc ghi đè để “làm sạch” working tree. Đối với tài liệu trạng thái, ghi ngày khảo sát và tách bằng chứng hiện tại khỏi ý tưởng tương lai để báo cáo không nhanh chóng trở thành khẳng định sai.
