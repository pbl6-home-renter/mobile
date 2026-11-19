# Rentify – Danh sách việc cần Backend làm / xác minh

Cập nhật lần cuối: 2026-11-18. Gateway đang test: `http://3.26.114.197/api/v1/`.
Nguồn đối chiếu: `api-doc/reference/rentify.json` (spec), kết quả gọi thật bằng curl và PROJECT_LOG của app mobile.

**Mức ưu tiên:** `P0` = đang chặn một màn hình chính của app; `P1` = ảnh hưởng chức năng nhưng có cách tạm; `P2` = đề xuất cải thiện, không gấp.
**Trạng thái:** `[MỞ]` chưa xử lý · `[ĐÃ XÁC NHẬN]` BE đã trả lời · `[ĐÃ XONG]` BE đã sửa.
---

## A. Lỗi lệch spec

### A1. Địa chỉ phòng: bỏ `district`, cần có `ward` – P0 – [MỞ]
- **Hiện trạng spec:** `GET /rooms` vẫn khai báo `district` là trường **bắt buộc** (required: `id, buildingId, name, area, rentPrice, genderPolicy, address, district, province, isFavorited`) và **không có `ward`**.
- **Hiện trạng server (2026-11-18):** `GET /rooms` không trả `district` (đúng hướng mới) nhưng **cũng không trả `ward`**. Ví dụ phần tử: `{"id":"…","photoMediaIds":[],"photoUrls":[],"buildingId":"…","name":"201","area":"10","rentPrice":300000,"genderPolicy":"male","address":"345 Đặng Tất","province":"Thành phố Huế","isFavorited":false}`.
- **Ảnh hưởng tới app:** DTO app đang theo spec cũ nên coi `district` là bắt buộc → cả danh sách phòng bị bỏ, trang chủ báo "Không tải được dữ liệu". Ngoài ra, không có `ward` nên không hiển thị được "Phường X, Tỉnh Y".
- **Đề nghị BE:**
  1. **Cập nhật spec:** bỏ `district` khỏi `GET /rooms` (và mọi nơi khác), thêm `ward` (tên xã/phường) vào phần tử danh sách phòng.
  2. **Thêm `ward` và `buildingName` vào server** cho `GET /rooms` (danh sách tòa nhà `GET /buildings` đã có `ward`, nên dữ liệu đã tồn tại). **Đã chốt phương án này (2026-11-18)** thay vì để app tự tra qua `buildingId`. Lý do: (a) chỉ cần 1 lần gọi, app tự tra phải gọi thêm `/buildings` (có thể nhiều trang) trước khi hiển thị được địa chỉ; (b) dữ liệu nhất quán tại thời điểm trả, không phụ thuộc bộ nhớ đệm phía app; (c) logic ghép dữ liệu nằm ở BE, không lặp lại ở mọi màn có danh sách phòng; (d) là điều kiện để lọc/sắp xếp theo phường ở server nếu cần. Mức ưu tiên cao nhưng không chặn app vì app đã có cách tạm bên dưới.
  3. **Chi tiết phòng** `GET /rooms/{id}` hiện không trả `address`, `ward`, `province` (dù spec/app mong có); đề nghị bổ sung `address`, `ward`, `province`.
- **Việc team mobile đã làm (tạm thời):** bỏ `district` khỏi DTO; `ward` là chuỗi không null (mặc định rỗng). Vì server chưa trả `ward` trong phòng, app tự ghép `ward` bằng cách tra `buildingId` qua `GET /buildings` (bộ nhớ đệm trong phiên, lấy từng trang 100) và `GET /buildings/{id}` cho tòa chưa có trong đệm (`BuildingWardResolver`). Khi BE trả `ward` ngay trong `GET /rooms` và `GET /rooms/{id}` thì app sẽ bỏ bước ghép này. Hạn chế đã biết của cách tạm: bộ nhớ đệm không tự làm mới trong suốt vòng đời tiến trình, và nếu gọi `/buildings` lỗi thì ward để trống.

### A2. Ảnh phòng: danh sách trả `photoUrls`, không trả `thumbnailUrl`; hiện không phòng nào có ảnh – P1 – [MỞ]
**Spec nói gì (đã đọc `rentify.json`)**
- Ảnh lưu qua dịch vụ Media (storage R2, upload bằng URL ký sẵn): `POST /media/presign-upload` → `PUT` file lên `uploadUrl` → `POST /media/{id}/complete-upload`. Ảnh phòng dùng `purpose = cover_photo` (1 ảnh đại diện/phòng) và `gallery_photo` (nhiều ảnh), `ownerType = room`, hiển thị **public**, chỉ landlord sở hữu được upload. Khi tạo phòng, `POST /rooms` nhận `photoMediaIds` (danh sách `id` Media đã complete).
- Khi đọc phòng, app **không gọi API Media**: `GET /rooms` trả `thumbnailUrl` (URL ảnh đại diện, nullable) và `GET /rooms/{id}` trả `photoUrls` (danh sách URL ảnh). `GET /media/{id}` và `/download-url` yêu cầu Bearer và mô tả cho media riêng tư (chat, avatar nhóm).

**Server thật (2026-11-18)**
- Các route Media **đã triển khai**: `POST /media/presign-upload`, `POST /media/{id}/complete-upload`, `GET /media/{id}`, `GET /media/{id}/download-url` đều trả 401 khi không có token (route tồn tại).
- `GET /rooms` trả `photoMediaIds` + `photoUrls` (không có `thumbnailUrl` như spec); `GET /rooms/{id}` trả `photoMediaIds` + `photoUrls`. Tuy nhiên cả 4 phòng hiện có đều có **`photoUrls: []` và `photoMediaIds: []`** → chưa có ảnh nào được tải lên, nên chưa kiểm chứng được định dạng URL thật.

**Vì sao chưa có ảnh**
- Chưa có phòng nào được gắn ảnh. App landlord hiện **chưa có chức năng tải ảnh phòng**: `CreateRoomActivity` gửi `photoMediaIds = emptyList()` và app landlord không có luồng presign/upload. App tenant chỉ dùng presign cho ảnh chat.

**Đề nghị BE**
1. Cập nhật spec sang `photoUrls`.
2. Cho một vài phòng có ảnh thật.

**Việc team mobile đã làm:** danh sách phòng lấy ảnh đại diện từ `thumbnailUrl`, nếu trống thì lấy ảnh đầu tiên trong `photoUrls` (`RoomPublicListResponseDto.coverUrl`). Khi không có ảnh, app vẫn dùng ảnh mẫu tạm (`resolveImageUrl`, ghi trong PROJECT_LOG). Việc thêm chức năng tải ảnh phòng cho landlord cần được lên kế hoạch riêng.

### A3. `GET /rooms/{roomId}` trả `landlord: null` – P0 – [MỞ]
- **Mô tả:** Spec đặt `landlord` là trường bắt buộc (`id`, `name`, `phone`) trong chi tiết phòng. Server trả `"landlord": null` cho **cả 4 phòng** khi gọi không có token.
- **Ảnh hưởng tới app:** DTO app coi `landlord` bắt buộc không null → mở chi tiết phòng sẽ lỗi parse (người dùng chưa đăng nhập, và có thể cả người đã đăng nhập nếu server vẫn trả null). Nút gọi chủ trọ và nút nhắn tin dựa vào trường này.
- **Cập nhật (2026-11-18):** đã thử khi **đã đăng nhập** vẫn trả `landlord = null`, nên không phải chuyện ẩn thông tin theo quyền. Khóa `landlord` có trong phản hồi nhưng giá trị rỗng (backend định nghĩa nhưng không điền dữ liệu).
- **Việc team mobile đã làm (tạm thời):** cho `landlord` nhận null trong DTO; màn chi tiết phòng ẩn thẻ "Chủ trọ" khi không có, nút Nhắn tin báo không có thông tin liên hệ. Khi BE trả đủ `landlord` thì team mobile đổi lại không null (backlog mục 24 trong PROJECT_LOG).
- **Cần BE xác nhận:** Cập nhật đầy đủ dữ liệu

### A4. `GET /me/room-hub` thiếu dữ liệu cho màn "Phòng của tôi" – P1 – [MỞ]
- **Hiện trạng:** server đã trả mảng theo spec mới và app đã chuyển theo (2026-11-18). Mới chỉ thấy `data: []` vì tài khoản test chưa có hợp đồng, chưa xem được phần tử thật.
- **Spec mới không có:** chủ trọ (`id, name, phone, avatarUrl`), bạn cùng phòng, số sự cố đang mở, hóa đơn hiện tại (app tạm gọi `/me/invoices?filter[contractId]` cho từng hợp đồng), `contractCode`, `rentalType`, `signedContractUrl`, ảnh phòng.
- **Cần BE xác nhận:** các thông tin này nên lấy từ endpoint nào (thêm vào room-hub, hay dùng endpoint khác như danh sách thành viên hợp đồng)? Nếu không có nguồn nào, UI sẽ ẩn các phần này.
- **Cần dữ liệu test:** một tài khoản tenant có hợp đồng đang hiệu lực để kiểm chứng.

### A5. `GET /me/invoices` thiếu trạng thái thanh toán chi tiết – P1 – [MỞ]
- **Mô tả:** Spec chỉ có `id, code, contractId, roomName, period, totalAmount, issuedAt, invoiceStatus`, và `invoiceStatus` chỉ ví dụ `issued`. App cần thêm `paidAmount`, `remainingAmount`, `dueDate` để hiển thị "còn nợ", "hạn nộp", "quá hạn".
- **Ảnh hưởng tới app:** DTO app có các trường ngoài spec với giá trị mặc định; nếu server không trả thì mọi hóa đơn hiện như chưa có hạn nộp và số còn nợ bằng 0.
- **Đề nghị BE:** bổ sung vào spec và server các trường này (hoặc cho biết app phải lấy từ API chi tiết nào), cùng danh sách đầy đủ giá trị của `invoiceStatus`.

### A6. "Yêu thích phòng" trả 404 `RESOURCE_NOT_FOUND` dù đã đăng nhập – P0 – [MỞ]
- **Hiện tượng:** người dùng bấm lưu phòng nhưng không được.
- **Nhận định:** GET danh sách cũng 404 nên lỗi không nằm ở phòng cụ thể mà ở chính tài nguyên "phòng yêu thích" của người dùng: hoặc chức năng chưa được triển khai ở dịch vụ phía sau, hoặc server yêu cầu một bản ghi (ví dụ hồ sơ người thuê) chưa được tạo cho tài khoản mới. Spec chỉ ghi 404/409 chung chung.
- **Cần BE xác nhận và sửa:** (1) chức năng yêu thích đã triển khai trên server chưa? (2) nếu đã triển khai, cần điều kiện gì để tài khoản tenant mới dùng được (hồ sơ nào cần tạo trước)? (3) `GET /me/favorite-rooms` cho người chưa lưu phòng nào phải trả 200 với `data: []`, không phải 404.

## B. Cần BE xác nhận (chưa rõ, chưa kiểm chứng được)

### B2. Đăng nhập khi email chưa xác minh (403) – P0 – [MỞ]
- **Mô tả:** Spec ghi `403 – Email đăng ký chưa được xác minh` nhưng **không nêu mã lỗi (`code`)**. App đang bắt theo mã HTTP 403.
- **Đề nghị BE:** bổ sung mã lỗi cụ thể vào spec (ví dụ `AUTH_EMAIL_NOT_VERIFIED`) và xác nhận body trả về.
- **Bằng chứng một phần:** gọi `POST /auth/login` với thông tin sai trả đúng `401 {"code":"AUTH_LOGIN_INVALID_CREDENTIALS"}` (khớp spec).

### B3. Liên kết xác minh email dẫn tới trang web, không mở lại app – P1 – [MỞ]
- **Mô tả:** Email có nội dung "Verify email address" (hạn 24 giờ, dùng một lần). Bấm vào sẽ mở **trang web đăng nhập**, không mở app. Người dùng phải tự quay lại app.
- **Câu hỏi cho BE:**
  1. Link trỏ tới domain nào, token nằm ở đâu (query/fragment)? Trang web nhận token rồi gọi `POST /auth/email-verifications` thay cho app đúng không?
  2. Có thể hỗ trợ **App Links/Deep Link** (ví dụ `https://<domain>/verify-email?token=…` mở thẳng app, kèm trang dự phòng khi chưa cài app)? Nếu có, app sẽ bắt token và gọi API xác minh, hiển thị kết quả ngay.
  3. Có API để app kiểm tra tài khoản đã xác minh chưa (ví dụ trạng thái trong phản hồi đăng nhập) để màn nhắc xác minh tự chuyển bước khi người dùng xác minh xong?

### B4. Giới hạn gửi lại email xác minh – P2 – [MỞ]
- **Mô tả:** `POST /auth/email-verification-requests` chỉ ghi các mã 202/400/422/500. App đang đặt hạn 60 giây giữa hai lần bấm "Gửi lại email" ở phía client.
- **Cần BE xác nhận:** server có giới hạn tần suất không (trả `429` kèm `Retry-After`)? Nên đồng bộ thời gian chờ với server và bổ sung mã lỗi vào spec.

### B5. Làm mới token và đăng xuất – P0 – [MỞ]
- **Đính chính (2026-11-18):** ghi chú cũ cho rằng `refreshToken` đi qua cookie HttpOnly là **sai**. Spec hiện hành ghi rõ: *"Refresh token gửi trong body JSON cho cả web và mobile (không dùng cookie)"*, request `{"refreshToken": "..."}` (bắt buộc) cho cả `POST /auth/refresh` và `POST /auth/logout`. Mục này đã sửa lại theo đó.
- **Bằng chứng trên server thật:** `POST /auth/refresh` với `{}` → `422 {"code":"REFRESH_TOKEN_REQUIRED"}`; với token giả → `401 {"code":"INVALID_REFRESH_TOKEN"}`; không có `Set-Cookie` nào. `POST /auth/logout` không token → `401 UNAUTHORIZED`. `POST /auth/login` trả `accessToken`, `refreshToken` và `user` trong body (đúng spec).
- **Lỗi nằm ở app (không phải BE):** `AuthApiService.refreshToken()` và `logout()` gọi không có body, nên server thật luôn trả 422 `REFRESH_TOKEN_REQUIRED`. App cũng chưa có cơ chế tự làm mới token khi access token hết hạn (tối đa 15 phút theo spec). Team mobile sẽ sửa.
- **Cần BE xác nhận:** thời hạn access token (spec: 15 phút) và refresh token (7 ngày nếu `rememberMe`, 1 ngày nếu không); `TOKEN_REUSED` thu hồi cả họ token (app không được gọi refresh song song bằng cùng một token).

### B6. Danh sách "Phòng đã lưu" – P2 – [MỞ]
- **Mô tả:** `GET /me/favorite-rooms` chỉ trả `roomId, name, rentPrice`. Thiết kế màn "Phòng đã lưu" cần thêm **quận/phường, diện tích, ảnh đại diện** (ví dụ "Hải Châu, 20 m²").
- **Đề nghị BE:** bổ sung các trường trên vào phần tử của API (đồng bộ với phần tử của `GET /rooms`).

### B7. Chat realtime (Socket.IO) qua gateway HTTP – P1 – [MỞ]
- **Mô tả:** App kết nối Socket.IO v4 theo đường dẫn `/api/v1/community/socket.io/` bằng WebSocket thuần (`ws://` vì gateway chưa có HTTPS).
- **Cần BE xác nhận:** gateway (Caddy) có chuyển tiếp WebSocket cho đường dẫn này không, có yêu cầu xác thực kiểu nào (token ở query hay ở gói `CONNECT`)? Chưa thử được vì cần token.

### B8. Hồ sơ ghép bạn: `preferredDistrict` (Quận/Huyện) – P1 – [MỞ]
- **Mô tả:** spec hồ sơ ghép bạn (`RoommateProfileSaveRequestDto`, `RoommateProfileListResponseDto`, `RoommateProfileDetailResponseDto`) vẫn có `preferredDistrict` và chưa có `preferredWard`. Với địa chỉ mới (không còn quận/huyện), app cần biết trường thay thế.
- **Quan sát trên server (2026-11-18):** `GET /roommate-profiles` trả `preferredDistrict` nhưng giá trị là **tên phường** (ví dụ `"Phường Hóa Châu"`), `preferredArea` là `"Phường Hóa Châu, Thành phố Huế"`; không có `preferredWard`. Tức BE đang dùng tên trường cũ để chứa phường.
- **Kết quả kiểm chứng (2026-11-18):** `PUT /me/roommate-profile` có thêm `preferredWard` trả `422 UNEXPECTED_FIELD`, nên hiện chỉ có `preferredDistrict` để chứa phường. App đang gửi tên phường vào `preferredDistrict` và `preferredArea` = "Phường X, Tỉnh Y"; form đã đổi sang chọn Tỉnh/Phường bằng dropdown (nhãn "Phường / Xã").
- **Cần BE quyết định:** đổi `preferredDistrict` thành `preferredWard` (xã/phường)? Hay bỏ hẳn và chỉ giữ `preferredProvince` + `preferredArea`? Khi chốt, cập nhật spec để app đổi tên trường gửi đi (một chỗ trong `EditRoommateProfileActivity`) và các màn hiển thị.

---

## C. Đề xuất bổ sung / thống nhất

### C1. Ghép bạn ở cùng (Matching) – danh sách lời mời – P0 – [MỞ]
(Đã ghi từ trước trong PROJECT_LOG, nhắc lại để BE xử lý một lần.)
- **Hiện trạng (2026-11-18, kiểm tra trên server thật):** `GET /match-requests` chỉ trả `id, requesterId, targetId, status, createdAt, respondedAt`, **không có** thông tin người gửi/nhận. App cần tên và ảnh để hiển thị. Tenant không tra theo ID được: `GET /users/{userId}` trả `403 FORBIDDEN` (nhóm admin), `GET /users/directory` chỉ tìm theo từ khóa `q`, `GET /roommate-profiles` không lọc theo `tenantId`.
- **Đề nghị BE (ưu tiên):** trả kèm `requester` và `target` rút gọn trong từng phần tử của `GET /match-requests`:
  `{ "requester": { "id", "name", "avatarUrl" }, "target": { "id", "name", "avatarUrl" } }`.
  Chỉ ba trường này, không trả số điện thoại hay email (đúng với mô tả "no phone" của endpoint). Tenancy đã gọi Identity (`GET /internal/users?ids=`) khi xử lý endpoint này nên lấy tên và ảnh theo danh sách ID được ngay, và app chỉ cần một lần gọi.
- **Phương án phụ (dùng lại cho chat, nhóm):** cho `GET /users/directory` nhận thêm tham số `ids` (danh sách ID, tối đa khoảng 50) và trả `id, name, avatarUrl`.
- **App đang xử lý tạm:** tải các trang `GET /roommate-profiles` để ghép `tenantId` thành tên và ảnh (`RoommateProfileResolver`). Người ẩn hoặc xóa hồ sơ hiện "Người dùng". Sẽ bỏ khi BE trả `requester`/`target`.
- `POST /match-requests/{id}/accept-request` hiện không trả `conversationId`, nên app không biết mở cuộc trò chuyện nào. Cần trả `conversationId` hoặc nói rõ cách lấy.
- Các mã lỗi `DUPLICATE_MATCH_REQUEST` và `SELF_MATCHING_NOT_ALLOWED` không còn trong spec: xác nhận server còn trả không.
- Header `Idempotency-Key` (UUID) đã được app gửi cho `POST /match-requests`: xác nhận server kiểm tra và phản hồi thế nào khi gửi lại cùng khóa.

### C2. Hồ sơ ghép bạn của tôi – P1 – [MỞ]
- `GET /me/roommate-profile` (trạng thái `designing`) chỉ trả `tenantId`, `lifestyle`, `personality`, `isVisible`, trong khi `PUT` nhận đầy đủ (giới tính, năm sinh, nghề nghiệp, ngân sách, khu vực mong muốn, ngày vào ở, giới thiệu…). Form sửa hồ sơ của app không thể điền sẵn dữ liệu cũ.
- **Đề nghị BE:** trả đủ các trường mà `PUT` nhận.
- Trường `lifestyle` là đối tượng tự do: app đang gửi/đọc các khóa `cleanAndTidy`, `earlyBird`, `nightOwl`, `petFriendly`, `smoking`, `cooking`, `guestsAllowed` (giá trị boolean). Cần BE chốt danh sách khóa hợp lệ để app không gửi sai.

### C3. Quy ước URL ảnh và avatar – P1 – [MỞ]
- App ghép URL avatar tương đối bằng cách lấy phần trước `/api/` của địa chỉ gateway. Cần BE cho biết: URL ảnh trả về là tuyệt đối hay tương đối? Ảnh phục vụ qua domain/đường dẫn nào, có cần token không? Hiện toàn bộ `photoUrls` của phòng đang rỗng nên chưa kiểm chứng được.

### C4. Khám phá theo tòa nhà (building) và xem danh sách phòng trong tòa – P1 – [MỞ]
- **Nhu cầu sản phẩm (2026-11-18):** trang Khám phá nên hiển thị **các tòa nhà/dãy trọ**; bấm vào một tòa nhà thì xem chi tiết tòa nhà và **các phòng thuộc tòa đó**. Hiện app đang liệt kê **phòng** ở trang chủ.
- **Spec và server hiện tại chưa đủ để làm đúng như vậy:**
  1. `GET /buildings` (công khai) chỉ có bộ lọc `province`, `ward`, `q`, sắp xếp `createdAt`/`name`; mỗi tòa chỉ trả `id, name, province, ward, address`. **Không có ảnh, giá thấp nhất, số phòng trống.** `GET /buildings/{buildingId}` chỉ thêm `status`.
  2. `GET /rooms` chỉ lọc `province`, `ward`, `minPrice`, `maxPrice`, `genderPolicy`. **Không lọc được theo `buildingId`** (đã thử `filter[buildingId]` → `422 UNEXPECTED_FIELD`). Không có API liệt kê phòng của một tòa nhà.
- **Đề nghị BE:**
  1. Thêm cách lấy phòng theo tòa: `filter[buildingId]` cho `GET /rooms` (hoặc `GET /buildings/{buildingId}/rooms`).
  2. Bổ sung thông tin tóm tắt vào phần tử của `GET /buildings`: ảnh đại diện (`thumbnailUrl`), `minRentPrice`, `availableRoomCount` (và nếu có thể `totalRoomCount`) để vẽ thẻ tòa nhà.
  3. Cho phép `filter[minPrice]`, `filter[maxPrice]`, `filter[genderPolicy]` cũng áp dụng khi tìm tòa nhà, nếu sản phẩm cần lọc theo tòa.
  4. **Ảnh của tòa nhà:** spec Media đã có `purpose = cover_photo`/`gallery_photo` với `ownerType = building` (công khai) nhưng `GET /buildings` và `GET /buildings/{id}` không trả ảnh nào. Đề nghị trả `thumbnailUrl` (cover) và `photoUrls` (gallery) của tòa, để thẻ và màn chi tiết tòa nhà không phải mượn ảnh phòng.
  5. **Câu hỏi cần BE trả lời:** `GET /rooms` công khai chỉ trả phòng `available` hay cả phòng đã có người thuê/đang chuẩn bị? Điều này quyết định `availableRoomCount` và việc ẩn tòa không còn phòng trống (app hiện coi mọi phòng trả về là đang cho thuê). Hiện server có 13 tòa công khai nhưng chỉ 4 phòng công khai.
- **Quyết định và việc team mobile đã làm (2026-11-18, tạm thời):** vì backend chưa có (1) và (2) ở trên, app **nhóm phòng theo `buildingId` ở phía app** để hiển thị tòa nhà:
  - Trang Khám phá có mục **"Tòa nhà nổi bật"** (thẻ ngang) đặt trên mục "Dành cho bạn" (giữ nguyên). Chỉ hiển thị tòa **có ít nhất một phòng công khai**; thẻ có tên tòa, phường/tỉnh, "Từ <giá thấp nhất>" và số phòng.
  - Bấm vào tòa nhà mở **Chi tiết tòa nhà** (`BuildingDetailActivity`): ảnh, tên, địa chỉ đầy đủ, số phòng, giá thấp nhất và danh sách phòng của tòa; bấm phòng mở chi tiết phòng như cũ.
  - Cách lấy dữ liệu: `GET /rooms` (lấy hết các trang, mỗi trang tối đa 100 phòng) rồi nhóm theo `buildingId`; tên/địa chỉ/phường của tòa lấy từ `GET /buildings` (bộ nhớ đệm trong phiên) và `GET /buildings/{id}` cho tòa chưa có trong đệm.
  - Tìm kiếm (lọc giá, giới tính, phường) vẫn theo **phòng** như hiện nay.
- **Hạn chế đã biết của cách làm tạm:** (a) mỗi lần mở Khám phá phải tải toàn bộ phòng chỉ để biết có những tòa nào (1.000 phòng ≈ 10 request); (b) không phân trang được danh sách tòa; (c) ảnh đại diện của tòa lấy từ ảnh phòng rẻ nhất, không phải ảnh riêng của tòa; (d) ảnh đang dùng ảnh mẫu khi phòng chưa có ảnh (xem A2).
- **Khi nào bỏ cách làm tạm:** khi BE bổ sung (1) `filter[buildingId]` hoặc `GET /buildings/{buildingId}/rooms` và (2) `thumbnailUrl`, `minRentPrice`, `availableRoomCount` cho tòa nhà thì team mobile gọi thẳng API, bỏ bước nhóm ở phía app (xem PROJECT_LOG backlog mục 25).

---

## D. Hạ tầng và tài liệu

### D1. HTTPS và domain – P1 – [MỞ]
- Gateway chạy `http://` theo địa chỉ IP, token và mật khẩu đi dạng không mã hóa. App tạm bật `usesCleartextTraffic`. Cần domain có chứng chỉ HTTPS trước khi phát hành và trước khi cho thử nghiệm rộng.

### D2. Tài liệu API chính thức – P2 – [MỞ]
- Server không mở trang tài liệu (`/docs`, `/swagger`, `/openapi.json` đều 404). Đề nghị BE cung cấp (a) bản OpenAPI xuất từ từng service, hoặc (b) danh sách route của gateway, để team mobile phát hiện API có trên server mà chưa có trong spec. Mobile chỉ dùng API có trong `rentify.json` (quy tắc dự án).
- **Đính chính (2026-11-18):** lần thăm dò 125 endpoint bằng request **không token** chỉ cho biết route ở cấp **gateway** có tồn tại (118 trả 401 cần đăng nhập), **không chứng minh dịch vụ phía sau đã triển khai**. Khi gọi bằng token thật, `GET/POST /me/favorite-rooms` trả 404 (xem A6) và `GET /me/roommate-profile` trả 404 `ROOMMATE_PROFILE_NOT_FOUND` (có thể hợp lệ vì tài khoản chưa tạo hồ sơ). Để biết endpoint nào thật sự hoạt động cần kiểm thử với token của từng vai trò.

### D3. Dữ liệu thử nghiệm và tài khoản test – P2 – [MỞ]
- Cần BE cung cấp một bộ tài khoản test cho từng vai trò (tenant đã thuê phòng, tenant chưa thuê, landlord) kèm dữ liệu mẫu đầy đủ (phòng có ảnh, chủ trọ, hợp đồng, hóa đơn quá hạn, sự cố, lời mời ghép bạn) để team mobile kiểm thử toàn bộ luồng. Hiện server chỉ có 4 phòng công khai và 13 tòa nhà, phần lớn là dữ liệu thử.

---

## E. Chi tiết: App Links cho link xác minh email (liên quan B3, D1)

**Mục tiêu:** bấm link trong email thì app Rentify tự mở, tự xác minh email rồi báo kết quả. Người chưa cài app (hoặc mở trên máy tính) vẫn mở trang web và xác minh như hiện nay.

**Luồng mong muốn**
1. Người dùng đăng ký trong app → app hiện màn "Xác minh email của bạn".
2. Email chứa link `https://<domain>/verify-email?token=<token>`.
3. Bấm link trên điện thoại: đã cài app thì Android mở thẳng app; chưa cài app thì mở trang web (web tự xác minh).
4. App lấy `token`, gọi `POST /auth/email-verifications`, rồi hiển thị kết quả (thành công → sang đăng nhập; token đã dùng hoặc hết hạn → cho gửi lại email).

**Việc Backend cần làm**
1. Cấp domain có HTTPS (địa chỉ IP `http://3.26.114.197` không dùng được cho App Links).
2. Đổi link trong email sang dạng `https://<domain>/verify-email?token=<token>`. Hiện tại link dẫn tới trang web; cần báo lại domain, đường dẫn và tên tham số token đang dùng.
3. Phục vụ `https://<domain>/.well-known/assetlinks.json` (HTTPS, không chuyển hướng, `Content-Type: application/json`) với nội dung:
```json
[{
  "relation": ["delegate_permission/common.handle_all_urls"],
  "target": {
    "namespace": "android_app",
    "package_name": "com.rentify.app.tenant",
    "sha256_cert_fingerprints": ["<SHA-256 của khóa ký>"]
  }
}]
```
   - App landlord có package `com.rentify.app.landlord`; nếu hai vai trò dùng chung domain thì thêm một đối tượng nữa vào mảng.
   - SHA-256 khóa **debug** hiện có trên máy phát triển (chỉ dùng để thử): `BC:D8:7B:FF:01:AD:79:1E:3C:50:C7:61:28:04:C0:C4:40:58:5C:A9:40:EB:5B:A8:9E:AB:7D:3B:E8:C1:96:3C`. Khi phát hành phải thêm SHA-256 của khóa ký bản release (hoặc khóa Play App Signing nếu dùng Google Play); team mobile sẽ cung cấp.
4. Giữ trang web làm dự phòng khi chưa cài app.
5. Báo mã lỗi cụ thể khi token đã dùng hoặc hết hạn. Spec hiện ghi `POST /auth/email-verifications` trả 400 (token sai hoặc đã dùng), 409 (token hết hạn) nhưng chưa có `code`.

**Việc team mobile làm sau khi có domain và link mẫu** (sẽ lập kế hoạch để duyệt trước khi làm)
- Thêm `intent-filter` có `autoVerify="true"` (scheme `https`, host `<domain>`, `pathPrefix="/verify-email"`) vào `MainActivity` của tenant (và landlord nếu dùng chung).
- Trong `MainActivity`, đọc `intent.data` ở `onCreate` và `onNewIntent`, lấy `token`, gọi `AuthRepository.verifyEmail(token)` (đã có sẵn), hiển thị "Đang xác minh…" rồi kết quả.
- Kiểm tra bằng `adb shell am start -a android.intent.action.VIEW -d "https://<domain>/verify-email?token=..."` và `adb shell pm get-app-links com.rentify.app.tenant` (cần báo `verified`).

**Lưu ý:** thiếu domain HTTPS hoặc thiếu `assetlinks.json` thì Android không mở app, vẫn mở trình duyệt và không báo lỗi. Nên chốt xong các mục 1–3 trước khi team mobile bắt đầu phần app.

### Mẫu tin nhắn gửi Backend
> Mobile cần App Links cho link xác minh email. Backend giúp:
> 1. Cấp domain HTTPS.
> 2. Đổi link trong email thành `https://<domain>/verify-email?token=<token>`.
> 3. Phục vụ `https://<domain>/.well-known/assetlinks.json` cho package `com.rentify.app.tenant` (và `com.rentify.app.landlord` nếu dùng chung) kèm SHA-256 chữ ký (mobile sẽ gửi).
> 4. Giữ trang web làm dự phòng khi chưa cài app.
>
> Nhờ báo lại domain, đường dẫn, tên tham số token, và mã lỗi (`code`) khi token hết hạn hoặc đã dùng.

---

## F. Kết quả kiểm thử có token (tài khoản tenant, 2026-11-18)
Gọi các `GET` không có tham số đường dẫn, chỉ dùng tham số có trong spec. Chỉ dùng token đang có trên máy test, không dùng refresh token.

| Kết quả | Endpoint |
|---|---|
| **200 hoạt động** | `/me`, `/me/room-hub` (0 mục), `/me/contracts` (0), `/me/invoices` (0), `/me/issues` (0), `/me/notification-preference` (4), `/notifications` (0), `/conversations` (0), `/conversations/unread-summary`, `/match-requests` (0), `/roommate-profiles` (3), `/rooms` (4), `/buildings`, `/administrative-units/provinces` (34), `/users/directory` (5), `/.well-known/jwks.json` |
| **404 – cần BE xem** | `/me/favorite-rooms` (mục A6), `/dashboard`, `/audit-logs` (có thể chỉ dành cho vai trò khác nhưng nên trả 403 thay vì 404) |
| **404 hợp lệ** | `/me/roommate-profile` (`ROOMMATE_PROFILE_NOT_FOUND`: tài khoản chưa tạo hồ sơ) |
| **503 – dịch vụ thanh toán không khả dụng** | `/me/payments`, `/payments` (`UPSTREAM_UNAVAILABLE`) |
| **403 đúng quyền** | các API của chủ trọ/quản trị: `/me/buildings`, `/me/rooms`, `/contracts`, `/invoices`, `/issues`, `/meter-readings`, `/rate-policies`, `/billing-settings`, `/contract-templates`, `/users`, `/admin/buildings`, `/contracts/tenants` |
| **422 vì cần tham số** | `/contract-members` (`CONTRACT_ID_REQUIRED`), `/messages`, `/messages/search` |

**Bẫy đã xác nhận bằng token:** `format=json` làm `GET /me/invoices`, `GET /roommate-profiles`, `GET /match-requests` trả `422 UNEXPECTED_FIELD` (spec các API này không có `format`); bỏ `format` thì trả 200. Đây là lỗi của app (đã ghi chú để sửa), không phải của BE.

**Việc cần BE:** (1) sửa chức năng yêu thích (A6); (2) dịch vụ thanh toán đang `503` (`/me/payments`, `/payments`), cần cho biết khi nào hoạt động; (3) `/dashboard` và `/audit-logs` trả 404, cần xác nhận đã triển khai chưa.

---

## Nhật ký cập nhật file này
- 2026-11-18: C1: ghi kết quả thử tra user theo ID (`/users/{userId}` 403 với tenant), đề nghị BE trả `requester`/`target` rút gọn, phương án phụ `directory?ids=`.
- 2026-11-18: B8: ghi kết quả kiểm chứng `preferredWard` bị `422 UNEXPECTED_FIELD` và cách app đang gửi phường qua `preferredDistrict`.
- 2026-11-18: viết lại A4 (server đã trả mảng, app đã theo; liệt kê dữ liệu còn thiếu và câu hỏi nguồn dữ liệu).
- 2026-11-18: A1: chốt yêu cầu BE trả `ward` và `buildingName` trong `GET /rooms` (không dùng cách app tự tra làm giải pháp lâu dài), ghi rõ lý do và hạn chế của cách tạm.
- 2026-11-18: thêm mục F (kết quả kiểm thử có token) và bổ sung bằng chứng cho A6, B8.
- 2026-11-18: thêm A6 (yêu thích phòng trả 404) và đính chính D2 (401 không chứng minh dịch vụ đã triển khai).
- 2026-11-18: C4: ghi quyết định nhóm phòng theo `buildingId` ở app, hạn chế và điều kiện bỏ.
- 2026-11-18: thêm C4 (Khám phá theo tòa nhà, danh sách phòng của tòa).
- 2026-11-18: cập nhật A3 (đã đăng nhập vẫn null; app tạm cho phép null).
- 2026-11-18: sửa B5 (refreshToken gửi qua body JSON, không phải cookie).
- 2026-11-18: viết lại A2 (ảnh phòng) sau khi đối chiếu spec Media và kiểm tra server.
- 2026-11-18: sửa A1 theo địa chỉ mới (bỏ quận/huyện, cần `ward`); thêm B8 (`preferredDistrict`).
- 2026-11-18: thêm mục E (App Links cho link xác minh email) kèm mẫu tin nhắn gửi Backend.
- 2026-11-18: tạo file; tổng hợp từ kiểm thử gateway thật (A1–A3, B2, D2) và các vấn đề đã ghi trong PROJECT_LOG (A4, A5, B5, B6, C1, C2).
