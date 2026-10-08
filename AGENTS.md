# Quy Tắc Dự Án (Project Rules)

# Rentify – Quy tắc cho AI

## A. Cách làm việc

1. Chỉ làm đúng phạm vi được giao. Không tự sửa file ngoài phạm vi, không tự refactor code có sẵn.
2. Gặp chỗ không chắc chắn (nghiệp vụ, API, thiết kế) thì hỏi lại, không tự đoán.
3. Ngoại trừ các thay đổi nhỏ như sửa trong 1 file và không đổi logic, ví dụ sửa lỗi chính tả, đổi tên biến, thêm string, chỉnh màu hoặc padding. Ngoài phạm vi đó thì phải có kế hoạch,..., mọi việc đều phải viết kế hoạch trước. Chỉ bắt đầu triển khai sau khi người dùng đã duyệt kế hoạch.
4. Không tự thêm thư viện mới, không sửa file Gradle khi chưa được người dùng đồng ý.
5. Làm xong thì tóm tắt những gì đã thay đổi và các điểm cần người review.
6. Nếu có đề xuất tốt hơn thì cứ nêu ra.
7. Luôn trả lời bằng tiếng Việt.
8. Không comment khi không cần thiết, đặc biệt là comment giải thích những đoạn code đơn giản, dễ hiểu.

## B. Code

8. Không để lại code chết hoặc code cũ bị comment.

## C. API

9. Nguồn đặc tả API duy nhất là file `../rentify-api-spec/reference/rentify.json` trong repo api-dog đặt cạnh project. Không tự thêm endpoint hay field ngoài spec.
10. Khi làm module nào thì chỉ đọc các path có tag tương ứng trong spec.

## D. Bảo mật

11. Không log token, mật khẩu, số CCCD hay số tài khoản ngân hàng. Logging interceptor chỉ bật ở bản debug và phải che header `Authorization`.
12. Không commit secret. Cấu hình riêng tư để trong `local.properties`.

## E. Git

13. Tên nhánh theo dạng `feature/<module>-<description>`, viết bằng tiếng Anh. Commit theo Conventional Commits, phần mô tả bằng tiếng Anh.
14. Không commit thẳng vào `main`. Mọi thay đổi phải đi qua Pull Request có review.
15. Sau mỗi việc (trừ thay đổi nhỏ), AI phải tự đề xuất nội dung cập nhật cho `mobile/docs/PROJECT_LOG.md` theo đúng tiêu chí và 4 phần cố định. Người dùng duyệt rồi mới ghi vào file.

## F. Quy tắc cập nhật Project Log (`mobile/docs/PROJECT_LOG.md`)

16. **Câu hỏi tự kiểm tra duy nhất trước khi đề xuất**: *"2 tháng nữa có người cần thông tin này không, và họ có tự tìm ra được từ code hoặc git log không?"*
17. **Nên ghi**:
    - Các quyết định quan trọng kèm lý do (vì sao chọn cách đó).
    - Những chỗ code đang lệch so me spec hoặc BE, và cách tạm xử lý (workaround).
    - Các vấn đề đang chờ BE hoặc chờ quyết định.
    - Thay đổi ảnh hưởng tới người khác (ví dụ đổi cấu trúc dùng chung, thêm bước setup mới).
    - Bẫy đã từng gặp, để người sau không mắc lại.
18. **Không ghi**:
    - Từng thay đổi nhỏ (vì git log đã có).
    - Giải thích code (vì chỗ đó dành cho comment trong code).
    - Sửa bug thông thường.
    - Nội dung đã có trong spec.
19. **Quy tắc cập nhật bố cục**:
    - **Mục 1 (Trạng thái hiện tại)**: Luôn ghi đè để phản ánh hiện tại.
    - **Mục 2, 3, 4 (Quyết định, Vấn đề & lưu ý, Nhật ký thay đổi lớn)**: Chỉ thêm vào, không xóa. Mỗi mục ngắn gọn 1–4 dòng.
