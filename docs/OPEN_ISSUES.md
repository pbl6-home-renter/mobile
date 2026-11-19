# Rentify – Vấn đề đang mở

## I. Backend

1. **`POST /contracts` trả 500** khi tạo hợp đồng từ landlord web, dù body hợp lệ theo spec; hợp đồng nháp vẫn được lưu (có trong `GET /contracts`).
2. **Hợp đồng không gắn với tài khoản tenant:** tạo bằng tên và số điện thoại (không có `tenantId`) thì `/me/contracts` và `/me/room-hub` không thấy; hợp đồng mới chỉ là `draft`.
3. **Lưu hồ sơ ghép bạn trả 422 `VALIDATION_FAILED`** không nói trường nào sai; nghi server đòi có `occupation`/`bio` dù spec ghi tùy chọn.
4. **Không trả điểm AI:** thiếu `matchScore`, `aiMatchScore`, `aiAdvice`; trả `lifestyle` thay vì `lifestyleTags`.
5. **Thanh toán (`GET /me/payments`) trả 503.** Yêu thích phòng đã chạy (200) sau cập nhật server ngày 18/11.
6. **Ảnh phòng:** `GET /rooms` đã trả `ward`, `buildingName`, `thumbnailUrl`, và `GET /rooms/{id}` đã trả `landlord`, nhưng `thumbnailUrl` còn null và `photoUrls` còn rỗng vì chưa phòng nào có ảnh.
7. **`GET /me/room-hub` vẫn chỉ trả 12 trường cơ bản** (spec ngày 18/11 không đổi). App tạm ghép thêm chủ trọ (`GET /rooms/{id}`), bạn cùng phòng (`GET /contract-members`), số sự cố (`GET /me/issues`) và hóa đơn (`GET /me/invoices`), mỗi hợp đồng tốn thêm 4 lần gọi (`MyRoomHubLoader`); bỏ khi BE đưa các trường vào room-hub.
8. **`preferredDistrict` đang chứa tên phường;** `preferredWard` bị từ chối 422.
9. **`GET /match-requests` chỉ trả ID** (`requesterId`, `targetId`), không có thông tin người gửi/nhận.
10. **`GET /rooms` không có tìm theo từ khóa** (đề nghị thêm `filter[q]`) và `filter[genderPolicy]` khớp chính xác (`male` không gồm `any`); `sort=createdAt` chạy được nhưng không có trong spec.
11. **`GET /issues` (landlord) trả 500** nên màn Sự cố & Bảo trì của chủ trọ không tải được danh sách.
12. **`GET /dashboard` trả 404 và theo spec chỉ dành cho admin**; app landlord đã tự tổng hợp tổng quan từ `GET /me/buildings` và `GET /contracts`.
13. **`GET /conversations` chậm** (1,2 đến 5 giây mỗi lần, gấp đôi các API khác) nên tab Tin nhắn tải lâu.
14. **Link tải ảnh lên (`POST /media/presign-upload`) nghi có lỗi:** URL chứa `x-amz-checksum-crc32=AAAAAA==` (CRC32 của nội dung rỗng) nên có thể bị R2 từ chối khi tải ảnh thật; cần thử lại sau khi app đã sửa lỗi gửi nhầm Authorization.

## II. App

1. **Thẻ hồ sơ ghép bạn lặp tên phường** ("Phường Hóa Châu · Phường Hóa Châu, …").
2. **Mọi lỗi tải hồ sơ bị coi là "chưa có hồ sơ"** nên lỗi mạng cũng hiện banner "Tạo hồ sơ".
3. **Tab Phòng:** lỗi `/me/contracts` bị coi như không có hợp đồng; hợp đồng `draft` bị xếp vào "Đã kết thúc"; ảnh phòng đang là ảnh mẫu vì server chưa trả ảnh thật.
4. **`BuildingWardResolver`:** cache không tự làm mới, gọi `/buildings` lỗi thì ward trống không báo.
5. **Form hồ sơ:** dropdown phường chưa có ô tìm kiếm; nghề nghiệp và giới thiệu đang bắt buộc theo phỏng đoán.
6. **Dialog nhóm chat** vẫn bắt gõ tay User ID.
7. **Tên người gửi/nhận lời mời** đang tra từ danh sách hồ sơ công khai; người ẩn hoặc xóa hồ sơ hiện "Người dùng".
8. **Sau khi chấp nhận lời mời không tự mở chat** (server không tạo hội thoại ở bước chấp nhận, spec ghi rõ); người dùng phải bấm "Nhắn tin" trên thẻ lời mời. Chưa kiểm thử nút "Nhắn tin", "Từ chối", "Rút lời mời".
9. **Tìm kiếm phòng:** khi có từ khóa hoặc chọn giới tính Nam/Nữ, app tải toàn bộ các trang (100 phòng/trang, tối đa 20 trang) rồi lọc ở app; chọn Nam/Nữ nay gồm cả phòng `any`. Chậm dần khi số phòng lớn; chưa kiểm thử trên màn hình.
10. **Sự cố và hợp đồng theo spec:** `POST /issues`, `PATCH /issues/{id}` không nhận ảnh đính kèm hay gắn thẻ nên app đã bỏ các trường này (chưa có cách đính kèm ảnh sự cố); `PATCH /contracts/{id}` chỉ nhận `description` và `vehicleCount`.
11. **App landlord:** đã chạy thử API landlord trên server thật (tòa nhà, phòng, hợp đồng, chat, người dùng đều 200 và khớp DTO); tổng quan, quản lý tài sản, hợp đồng, chi tiết hợp đồng, hóa đơn, điện nước, tin nhắn, cá nhân đều hiển thị. Thanh điều hướng dưới của landlord đã dùng chung `RentifyFloatingBottomBar` với tenant.
12. **Chưa kiểm thử thao tác ghi của landlord:** tạo tòa nhà, tạo phòng, tạo và kích hoạt hợp đồng, trả phòng và quyết toán cọc, ghi điện nước, lập và phát hành hóa đơn, cập nhật sự cố, gửi tin nhắn, đổi mật khẩu.
13. **Chưa kiểm thử trên máy:** tab Phòng với hợp đồng thật, lưu hồ sơ và hộp chọn ngày, giao diện mới của Báo sự cố, dialog nhóm, Đặt lại mật khẩu, OTP, và ba màn đăng nhập/đăng ký/quên mật khẩu sau khi đổi ô nhập.
