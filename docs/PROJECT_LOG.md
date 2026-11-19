# Rentify – Project Log

## 1. Trạng thái hiện tại
<!-- Cập nhật đè, luôn phản ánh hiện tại -->
| Module | Trạng thái | Người phụ trách | Ghi chú |
|---|---|---|---|
| Core Network | Hoàn thành | AI & Dev | Đã dựng Retrofit, OkHttp, Moshi DTO wrappers, Auth DTOs, Room DTOs (`RoomCreateResponseDto`, `RoomUpdateResponseDto`), User DTOs, Chat DTOs v2 (`avatarUrl`), `CommunitySocketClient` (Engine.IO v4 / Socket.IO v4 WebSocket), Matching DTOs, Issue DTOs, Contract DTOs, Invoice DTOs, Media DTOs |
| Core Data | Hoàn thành chuẩn hóa (Hướng B) | AI & Dev | Đảm bảo 100% dùng chung: TokenDataStore, AppDataStore, SessionManager, BaseRepository, AuthRepository, UserRepository, AdministrativeUnitsRepository (In-Memory Cache & Snapshot Fallback) |
| Core UI | Hoàn thành | AI & Dev | Đã chuẩn hóa thang đo Spacing 4dp, 50 icon Lucide, `RentifyTextField`, `Widget.Rentify.PillButton`, Card style và Empty/Error State dùng chung |
| App Tenant | Hoàn thành | AI & Dev | Đã chuẩn hóa toàn diện layout, `LoginRequiredSheet`, `TenantLoadingView` (Full-Screen Blur Overlay), Realtime Chat Socket.IO v4, và nâng cấp giao diện Tab Tin nhắn (Search debounce 300ms, Filter Chips, Avatar bảng màu cố định, Skeleton 5 dòng) |
| App Landlord | Đang nâng cấp (Phần 5/6 hoàn thành) | AI & Dev | Đã hoàn thành Phần 1 (`feature/landlord-property-room-detail`), Phần 2 (`feature/landlord-rate-billing-settings`), Phần 3 (`feature/landlord-contracts-upgrade`), Phần 4 (`feature/landlord-meters-invoices-issues`) và Phần 5 (`feature/landlord-chat-sync`); chuẩn bị Phần 6 (`feature/landlord-dashboard-notifications-profile`) |

## 2. Quyết định
<!-- Mới nhất ở trên. Mỗi mục 2–4 dòng -->
### 2026-11-19 – Đồng bộ và Nâng cấp Phân hệ Chat Landlord (Tìm kiếm, Tạo Chat 1:1 & Nhóm, Trả lời, Ảnh Đính kèm, Quản lý Nhóm & Dropdown Bot Nhắc nhở)
- **Lý do:** Đồng bộ giao diện và trải nghiệm Chat của Chủ trọ với `:app-tenant` (`RentifyAvatarView`) và Landlord Web: khắc phục lỗi truyền `currentUserId` rỗng bằng cách đọc từ `TokenDataStore`, hỗ trợ tìm kiếm hội thoại và tạo chat 1:1 / nhóm mới từ `/users/directory`, gửi ảnh đính kèm qua Media API, trích dẫn trả lời tin nhắn, quản lý thành viên/đổi tên/rời nhóm và chọn trực tiếp Hóa đơn/Hợp đồng từ Dropdown khi gửi Bot nhắc nhở.
- **Ảnh hưởng:** `:core-network` cập nhật `UserDirectoryResponseDto.kt`; `:core-ui` thêm `ic_plus.xml`; `:app-landlord` cập nhật `LandlordChatRepository(Impl)`, `LandlordChatFragment`, `LandlordChatViewModel(Factory)`, `LandlordConversationAdapter`, `LandlordChatDetailActivity`, `LandlordChatDetailViewModel(Factory)`, `LandlordMessageAdapter` và thêm `bottom_sheet_new_chat.xml`, `bottom_sheet_landlord_group_info.xml`.
### 2026-11-19 – Nâng cấp Phân hệ Ghi Chỉ số Điện Nước (Chọn Kỳ, Chỉ số cũ, Nhập Chi tiết & Ảnh Công tơ), Bộ lọc Hóa đơn và Báo Sự cố Chủ trọ
- **Lý do:** Đồng bộ đầy đủ các công cụ vận hành hàng tháng với Landlord Web: cho phép chuyển đổi kỳ tháng (`yyyy-MM`) và đối chiếu chỉ số cũ khi chốt số điện nước, hỗ trợ nhập chi tiết từng phòng kèm cờ thay đồng hồ mới (`isBaseline`) và đính kèm ảnh mặt công tơ (`PATCH /meter-readings/{id}/evidence`), bổ sung lọc hóa đơn theo tòa nhà & kỳ, sửa phí khác cho hóa đơn chờ phát hành (`PATCH /invoices/{id}`), và cho phép chủ trọ chủ động tạo báo cáo sự cố mới (`POST /issues`).
- **Ảnh hưởng:** `:core-network` cập nhật `InvoiceDtos.kt`, `InvoiceApiService.kt`; `:app-landlord` cập nhật `LandlordInvoiceRepository(Impl)`, `LandlordIssueRepository(Impl)`, `BillingViewModel`, `IssueViewModel`, `LandlordMeterReadingsActivity`, `BulkMeterAdapter`, `LandlordInvoicesActivity`, `LandlordInvoiceDetailActivity`, `LandlordIssuesActivity` và bổ sung `bottom_sheet_detailed_meter.xml`, `bottom_sheet_create_issue.xml`.
### 2026-11-19 – Hoàn thiện Quy trình 3 Bước Kích hoạt Hợp đồng (Áp mẫu, Sinh PDF Nháp, Upload Bản ký thật), Sửa Hợp đồng & Quyết toán Cọc
- **Lý do:** Thay thế hoàn toàn cơ chế sinh `UUID` giả khi kích hoạt hợp đồng bằng quy trình 3 bước chuẩn trên Landlord Web (`/contract-templates` -> `/generate-draft-document` -> upload file hợp đồng đã ký qua Media API `purpose = signed_contract` -> `/activate-contract`); đồng thời bổ sung chỉnh sửa số xe/ghi chú (`PATCH /contracts/{id}`), lọc hợp đồng theo tòa nhà/từ khóa và sửa lỗi gán cứng `CASH` khi quyết toán tiền cọc chuyển khoản.
- **Ảnh hưởng:** `:core-network` cập nhật `ContractDtos.kt`, `ContractApiService.kt`; `:app-landlord` cập nhật `LandlordContractRepository(Impl)`, `ContractViewModel`, `LandlordContractsActivity`, `LandlordContractDetailActivity`, `bottom_sheet_deposit_settlement.xml` và bổ sung `bottom_sheet_edit_contract.xml`.
### 2026-11-19 – Triển khai Biểu giá Dịch vụ (`/rate-policies`) và Cài đặt Thanh toán & Nhắc nợ (`/billing-settings`) Cấp Tòa nhà & Phòng
- **Lý do:** Theo quy tắc hệ thống (P6-D72), hóa đơn sẽ bị chặn phát hành nếu tòa nhà/phòng chưa có biểu giá điện và nước; đồng thời chủ trọ cần cấu hình ngày thu tiền định kỳ (1–28), ngày nhắc nộp và bật/tắt Bot nhắc nợ tự động ở cả cấp tòa nhà lẫn cấp phòng (hỗ trợ kế thừa tòa nhà).
- **Ảnh hưởng:** `:core-network` bổ sung `RatePolicyDtos.kt`, `BillingSettingDtos.kt`, `RatePolicyApiService.kt`, `BillingSettingApiService.kt`; `:app-landlord` bổ sung `LandlordPolicySettingRepository(Impl)`, `PolicySettingBottomSheetHelper`, `bottom_sheet_rate_policies.xml`, `item_rate_policy_block.xml`, `bottom_sheet_billing_settings.xml`, tích hợp vào `BuildingDetailActivity` và `LandlordRoomDetailActivity`.
### 2026-11-19 – Hoàn thiện Màn hình Chi tiết Phòng, Bộ lọc Tài sản và Tích hợp Danh sách Phòng trong Chi tiết Tòa nhà cho Landlord
- **Lý do:** Đồng bộ đầy đủ tính năng quản lý nhà/phòng với Landlord Web: cho phép chủ trọ tìm kiếm/lọc tòa nhà và phòng theo trạng thái/tầng, quản lý danh sách phòng và đăng ảnh minh chứng sở hữu ngay trong màn Chi tiết tòa nhà, đồng thời xem chi tiết, chỉnh sửa (`PATCH /rooms/{id}`) và xóa phòng (`DELETE /rooms/{id}`).
- **Ảnh hưởng:** `:core-network` cập nhật `BuildingDtos.kt`, `RoomDetailDtos.kt`; `:app-landlord` bổ sung `LandlordMediaRepository(Impl)`, `LandlordRoomDetailActivity`, `bottom_sheet_edit_building.xml`, `bottom_sheet_edit_room.xml`, nâng cấp `PropertiesFragment`, `BuildingDetailActivity`, `CreateRoomActivity`, `BuildingViewModel`, `RoomViewModel`.
### 2026-11-18 – Form hồ sơ ghép bạn: chọn Tỉnh/Phường bằng dropdown, phường gửi qua `preferredDistrict`
- **Lý do:** Địa chỉ mới không còn quận/huyện. Server từ chối `preferredWard` (`422 UNEXPECTED_FIELD`) và dữ liệu thật lưu tên phường trong `preferredDistrict`, `preferredArea` dạng "Phường X, Tỉnh Y". Nhập tay dễ sai và không khớp danh mục, nên form chọn từ `AdministrativeUnitsRepository`; `preferredArea` tự ghép (bỏ ô "Khu vực cụ thể").
- **Ảnh hưởng:** Ngày vào ở dùng hộp chọn ngày (hiển thị `dd/MM/yyyy`, gửi `yyyy-MM-dd`). Nút Lưu chỉ bật khi đủ giới tính, năm sinh, tỉnh, phường; ngân sách, nghề nghiệp, ngày, giới thiệu, lối sống là tùy chọn. Hồ sơ cũ có tỉnh không khớp danh mục phải chọn lại. Chi tiết tên trường chờ BE chốt ở `docs/BACKEND_REQUESTS.md` B8.
### 2026-11-18 – Tab "Phòng" đọc `/me/room-hub` dạng danh sách và dựng lại dữ liệu hub ở phía app
- **Lý do:** Server thật và spec mới trả `/me/room-hub` là mảng gồm thông tin cơ bản của hợp đồng (không có chủ trọ, bạn cùng phòng, số sự cố, hóa đơn). App cũ đọc như một đối tượng nên tab Phòng luôn báo "Không tải được dữ liệu" kể cả khi chưa có phòng.
- **Ảnh hưởng:** `MyRoomHubItemDto` mới, `MyRoomHubLoader` chuyển từng phần tử sang `MyRoomHubResponseDto` và lấy hóa đơn mới nhất qua `/me/invoices?filter[contractId]`. `landlord` nay nullable; `MyRoomFragment`, `MyRoomCardBinder`, `MyRoomViewModel.contactLandlord` xử lý trường hợp không có chủ trọ. `MyRoomViewModel` và `ExploreViewModel` quyết định trạng thái theo số hợp đồng đang hiệu lực.
### 2026-11-18 – Địa chỉ phòng: yêu cầu BE trả `ward`/`buildingName` trong `GET /rooms`, app tự tra qua `buildingId` chỉ là cách tạm
- **Lý do:** Địa chỉ mới chỉ còn xã/phường và tỉnh (bỏ `district`). `GET /rooms` chỉ trả `buildingId`, `ward` nằm ở `GET /buildings`. Đã cân nhắc để app tự ghép lâu dài nhưng chọn BE trả sẵn vì: 1 lần gọi thay vì nhiều trang `/buildings`, dữ liệu luôn nhất quán, không lặp logic ghép ở từng màn, và mở đường lọc theo phường ở server.
- **Ảnh hưởng:** DTO phòng dùng `ward: String = ""` (không null, không còn `district`). `BuildingWardResolver` giữ làm đường dự phòng đến khi BE đáp ứng (xem Mục 5, mục 26). Chi tiết yêu cầu ở `docs/BACKEND_REQUESTS.md` A1.
### 2026-11-18 – Khám phá theo tòa nhà (nhóm phòng theo `buildingId` ở phía app) và màn Chi tiết tòa nhà
- **Lý do:** Mô hình dãy trọ cần Khám phá hiển thị tòa nhà, bấm vào xem các phòng của tòa. Spec chưa hỗ trợ: `GET /buildings` chỉ trả `id, name, province, ward, address` (không ảnh/giá/số phòng) và `GET /rooms` không lọc được theo `buildingId`. Vì `GET /rooms` trả `buildingId`, app tạm nhóm phòng theo tòa ở phía app để làm được ngay.
- **Ảnh hưởng:** `:app-tenant` thêm `TenantBuildingRepository(Impl)`, `BuildingSummary`, `BuildingWardResolver` (nay cache cả thông tin tòa), `TenantRoomRepository.getAllPublicRooms`, mục "Tòa nhà nổi bật" ở `ExploreFragment` (`BuildingCardAdapter`) và `BuildingDetailActivity` (+ViewModel, Factory, layout, đăng ký manifest). Mục "Dành cho bạn" và Tìm kiếm theo phòng giữ nguyên. Chỉ hiển thị tòa có ít nhất một phòng công khai.
### 2026-11-18 – Làm mới token theo spec (refreshToken trong body) và tự làm mới khi gặp 401
- **Lý do:** Spec hiện hành quy định `refreshToken` gửi trong body JSON cho cả `POST /auth/refresh` và `POST /auth/logout` (không dùng cookie); server thật xác nhận (`422 REFRESH_TOKEN_REQUIRED` khi thiếu). App từng gọi hai API này không có body và không có cơ chế tự làm mới, trong khi access token chỉ sống tối đa 15 phút.
- **Ảnh hưởng:** `:core-network` thêm `AuthRefreshRequestDto`, `AuthLogoutRequestDto`, `TokenAuthenticator` (OkHttp Authenticator, thử tối đa 2 lần) và `NetworkClient.tokenRefresher`; `:core-data` thêm `DataStoreTokenRefresher` (khóa chung để không dùng lại cùng một refresh token vì spec thu hồi cả họ token) và sửa `AuthRepositoryImpl`; `:app-tenant`, `:app-landlord` thêm `TenantApplication`/`LandlordApplication` đăng ký bộ làm mới. Mọi `createService` có `tokenProvider` tự được áp dụng.
### 2026-11-18 – Chuyển Base URL từ mock APIDog sang gateway backend thật
- **Lý do:** Backend đã có gateway chạy thật tại `http://3.26.114.197/api/v1/`; đổi `NetworkConfig.DEFAULT_BASE_URL` để app test trực tiếp với dữ liệu thật. Gateway chưa có HTTPS nên dùng HTTP (tenant đã bật `usesCleartextTraffic`).
- **Ảnh hưởng:** `:core-network` (`NetworkConfig.kt`) đổi một hằng số, kéo theo toàn bộ app tenant/landlord gọi server thật, thay cho dữ liệu giả của mock. Cần có dấu `/` cuối vì các `@GET/@POST` dùng đường dẫn tương đối.
### 2026-11-17 – Nâng cấp Toàn diện Giao diện Màn hình Chi tiết Phòng (RoomDetailActivity)
- **Lý do:** Tái thiết kế màn hình Chi tiết phòng theo bố cục tấm bo góc trên 24dp đè lên ảnh (`marginTop = -22dp`), lược bỏ toàn bộ đường kẻ ngăn cách giữa các mục, đồng bộ bề mặt `rentify_soft_surface` (`#F5F6FB`) cho lưới thông số 2x2, chip tiện nghi, bảng điện/nước và thẻ chủ trọ (tích hợp `RentifyAvatarView` 46dp), nâng cấp thanh nổi dưới cùng bo tròn hoàn toàn (giá `vi-VN` + nút `"Nhắn tin"` `Widget.Rentify.PillButton`), và xử lý triệt để lỗi hiển thị nhầm tên người ở dòng địa chỉ cũng như số âm ở sức chứa/tầng.
- **Ảnh hưởng:** `:core-ui` bổ sung các token màu Room Detail trong `colors.xml`; `:core-network` cập nhật `RoomDetailDtos.kt`; `:app-tenant` cập nhật `activity_room_detail.xml`, `RoomDetailActivity.kt`, `strings.xml`, `ExploreFragment.kt`, `SearchResultActivity.kt`.
### 2026-11-17 – Nâng cấp Giao diện Màn hình Chat (Cá nhân & Nhóm), Sheet Thông tin Nhóm và Custom View `RentifyAvatarView`
- **Lý do:** Thống nhất hiển thị avatar trên toàn bộ phân hệ Chat (danh sách tin nhắn, header chat, bong bóng tin nhận, sheet thông tin nhóm) qua một component duy nhất `RentifyAvatarView` (hỗ trợ ghép 2 avatar thành viên chồng chéo cho nhóm chưa có ảnh); nâng cấp khung chat với gom nhóm 5 phút, bo góc động 20dp/6dp, trạng thái đã xem realtime (`conversation:member-read`), phân cách ngày, thanh nhập tin nổi tự giãn và đồng bộ tên nhóm giữa Header & Sheet Thông tin nhóm.
- **Ảnh hưởng:** `:core-ui` bổ sung `RentifyAvatarView.kt`, `attrs.xml` (`avatarSize`), `colors.xml` (`rentify_text_faint`); `:core-network` bổ sung `avatarUrl` vào `ConversationDetailResponseDto`, `ConversationMemberDto` và `senderAvatarUrl` vào `MessageListResponseDto`; `:app-tenant` cập nhật `ChatDetailActivity`, `ChatDetailViewModel`, `MessageAdapter`, `GroupInfoBottomSheetDialog`, `GroupMemberAdapter`, `GroupInfoViewModel` và các layout liên quan; xóa `ChatAvatarHelper.kt`.
### 2026-11-17 – Cập nhật Giao diện Tab Tin nhắn (ChatFragment): Ô Tìm kiếm, Hàng Chip Lọc, Avatar Bảng màu Cố định & Skeleton Loading
- **Lý do:** Nâng cấp trải nghiệm danh sách hội thoại của khách thuê: tinh giản header, bổ sung ô tìm kiếm theo tên & nội dung tin cuối (debounce 300ms), hàng chip lọc ngang ("Tất cả", "Chưa đọc · <số>", "Nhóm"), chuẩn hóa avatar 52dp theo 3 mức ưu tiên (ảnh `avatarUrl` thật -> chữ cái đầu của tên theo 4 bảng màu cố định -> icon nhóm nền xanh ngọc, loại bỏ hoàn toàn mock Unsplash & gradient), huy hiệu chưa đọc màu `rentify_primary` thay cho màu đỏ, đường kẻ `ConversationDividerItemDecoration` lề trái 82dp và 5 dòng skeleton khi đang tải.
- **Ảnh hưởng:** `:core-ui` cập nhật `colors.xml` và `bg_unread_badge.xml`; `:core-network` bổ sung `avatarUrl` vào `ConversationListResponseDto`; `:app-tenant` cập nhật `fragment_chat.xml`, `item_conversation.xml`, `item_conversation_skeleton.xml`, `ChatFragment.kt`, `ConversationAdapter.kt`, `ChatAvatarHelper.kt`, `ConversationDividerItemDecoration.kt`.

### 2026-11-17 – Tích hợp Realtime Chat Socket.IO v4 (Community Socket v1) và Loại bỏ Smart Polling
- **Lý do:** Backend đã hoàn thiện cổng Socket.IO v4 tại `/api/v1/community/socket.io`; việc tiếp tục dùng HTTP polling chu kỳ 3.5s/12s gây tốn tài nguyên và độ trễ. Xây dựng `CommunitySocketClient` trực tiếp trên `okhttp3.WebSocket` theo giao thức Engine.IO v4 / Socket.IO v4 giúp nhận tin nhắn, trạng thái đã đọc và chỉ báo đang gõ (`typing`) tức thì mà không cần thêm thư viện ngoài vào Gradle.
- **Ảnh hưởng:** `:core-network` bổ sung `CommunitySocketClient.kt` và `ChatSocketEvents.kt`; `:app-tenant` (`ChatViewModel`, `ChatDetailViewModel`, `ChatFragment`, `ChatDetailActivity`) và `:app-landlord` (`LandlordChatViewModel`, `LandlordChatDetailViewModel`, `LandlordChatDetailActivity`) xóa bỏ polling loop và chuyển sang lắng nghe sự kiện socket thời gian thực kèm đồng bộ bù khi reconnect.

### 2026-11-17 – Nâng cấp Cơ chế Loading Toàn màn hình Làm mờ Nền (Full-Screen Blur Overlay) trên Tenant
- **Lý do:** Đảm bảo khi màn hình đang tải dữ liệu, toàn bộ giao diện phía dưới được làm mờ (`RenderEffect` blur 10dp trên Android 12+ kết hợp lớp phủ bán trong suốt) và khóa tương tác, chỉ hiển thị hiệu ứng Lottie `loading_ring_ball.json` ở chính giữa màn hình cho đến khi tải xong.
- **Ảnh hưởng:** `:app-tenant` nâng cấp `TenantLoadingView.kt` với `OverlayController` gắn vào `android.R.id.content`, tự động áp dụng đồng bộ cho toàn bộ 22 màn hình có sử dụng `TenantLoadingView` mà không cần sửa từng file layout.

### 2026-11-17 – Đồng bộ DTO Tạo/Cập nhật Phòng và Mã lỗi 409 ROOM_HAS_ACTIVE_CONTRACT (api-doc PR #81)
- **Lý do:** Cập nhật theo đặc tả API mới nhất cho `POST /rooms` và `PATCH /rooms/{roomId}`, tách bạch DTO phản hồi khi tạo/cập nhật phòng và bắt mã lỗi nghiệp vụ khi chủ trọ thao tác trên phòng đang có hợp đồng hiệu lực.
- **Ảnh hưởng:** `:core-network` bổ sung `RoomCreateResponseDto`, `RoomUpdateResponseDto` trong `LandlordRoomDtos.kt` và cập nhật `RoomApiService.kt`; `:app-landlord` cập nhật `LandlordRoomRepository` và `RoomViewModel.kt`.
### 2026-11-15 – Tối ưu hóa Sắp xếp sang Popup Dropdown Menu và Tinh giản Header Tìm kiếm (SearchResultActivity)
- **Lý do:** Khắc phục trải nghiệm mở BottomSheet to chiếm diện tích và chứa mô tả phụ rườm rà cho thao tác sắp xếp đơn giản; tách rời hoàn toàn Sắp xếp khỏi Bộ lọc và chuyển sang Popup Dropdown thả xuống ngay cạnh nút "⇅ Sắp xếp" với các lựa chọn ngắn gọn, trực quan; đưa nút Lọc lên Top Bar (46x46dp) và ẩn thanh chip khi chưa có bộ lọc nhằm tối ưu diện tích hiển thị danh sách phòng.
- **Ảnh hưởng:** 
  - `:app-tenant` bổ sung `bg_popup_sort_menu.xml`, `layout_popup_room_sort.xml`, cập nhật `SearchResultActivity.kt` (`showSortDropdown()` căn lề phải neo dưới `btnSort`, highlight mục active kèm icon tick xanh).
  - Xóa bỏ `SortBottomSheetDialogFragment.kt` và `bottom_sheet_room_sort.xml` để đảm bảo không có code chết.
  - Tăng chiều cao cụm nút chân trang "Áp dụng" / "Đặt lại" lên 54dp trong `bottom_sheet_room_filter.xml` và xóa vạch ngang phân cách giúp giao diện cân đối, thoáng đãng.
### 2026-11-15 – Tối ưu hóa toàn diện giao diện Bộ lọc tìm kiếm (FilterBottomSheetDialogFragment)
- **Lý do:** Khắc phục lỗi nút "Áp dụng" bị thanh điều hướng hệ thống (Navigation Bar) đè lên; tách biệt thanh Action Footer với đường viền 1dp `#EEF0F3`; chuyển nút "Đặt lại" sang dạng Text Action phẳng thanh lịch (chỉ enable khi có thay đổi) và nút "Áp dụng" sang khối chữ nhật bo góc 14dp bề thế (không bị nhầm lẫn với các chip lọc); nâng cấp 2 ô chọn Tỉnh/Phường sang Pill Card 22dp; cập nhật thumb RangeSlider thành 2 vòng tròn rỗng viền xanh `#2563EB` nét 2.5dp; bo góc BottomSheet 28dp và đồng bộ tone màu Primary.
- **Ảnh hưởng:** `:app-tenant` bổ sung `bg_bottom_sheet_filter_rounded.xml`, cập nhật `bottom_sheet_room_filter.xml` và `FilterBottomSheetDialogFragment.kt` (gắn dialog background bo góc 28dp, kiểm tra trạng thái enable cho nút Đặt lại, dropdown menu một chạm).
### 2026-11-15 – Tối ưu hóa UI/UX Màn hình Chi tiết phòng (RoomDetailActivity) & Sticky Bottom Bar
- **Lý do:** Tăng khả năng chuyển đổi và kết nối tức thì giữa khách thuê và chủ trọ theo spec: Bổ sung thanh tương tác cố định ở đáy (Sticky Bottom Bar) gồm giá thuê và 2 nút thao tác nhanh (Gọi điện & Nhắn tin); thêm nút Chia sẻ phòng (Share) trên Top Bar; chuẩn hóa màu giá phòng sang Xanh ngọc `@color/rentify_teal_primary` (`#006876`) và đồng bộ viền mỏng `#EEF0F3`.
- **Ảnh hưởng:** `:core-ui` bổ sung icon Lucide `ic_share_2.xml` và token màu `rentify_border_card`; `:app-tenant` cập nhật `activity_room_detail.xml` (thêm `layoutBottomBar`, cụm nút gọi/nhắn trong landlord card, nút share) và `RoomDetailActivity.kt` (kết nối `callLandlord()`, `contactLandlord()`, chia sẻ thông tin phòng qua `ACTION_SEND`).
### 2026-11-13 – Nâng cấp Bộ lọc Tìm kiếm Phòng trọ (FilterBottomSheetDialogFragment) sang API Đơn vị Hành chính thật và Đưa Repository lên :core-data
- **Lý do:** Loại bỏ hoàn toàn danh sách tĩnh hardcode (6 tỉnh và 7 phường mẫu), đảm bảo khách thuê lọc phòng theo đúng 63 Tỉnh/Thành và Phường/Xã chuẩn backend hỗ trợ; đồng thời chuyển AdministrativeUnitsRepository lên :core-data với In-memory Cache để cả Tenant và Landlord dùng chung một nguồn dữ liệu tối ưu.
- **Ảnh hưởng:** :core-data bổ sung AdministrativeUnitsRepository & AdministrativeUnitsRepositoryImpl (cache static concurrent và snapshot fallback khi APIDog trả 404); :app-landlord cập nhật import; :app-tenant chuyển bố cục Tỉnh/Phường sang dọc thoáng đãng, chống tràn chữ và tự động nạp Phường/Xã theo thời gian thực khi chọn tỉnh.
### 2026-11-13 – Chuẩn hóa Trạng thái Trống (Empty/Error States) huy hiệu tròn pastel và Độ tương phản Typography WCAG AA
- **Lý do:** Khắc phục tình trạng các màn hình danh sách trống dùng icon xám mờ rời rạc thiếu hướng dẫn hành động; nâng cấp độ tương phản chữ từ `#94A3B8` (< 3:1) lên `@color/rentify_text_muted` (`#64748B`, > 4.5:1) đạt chuẩn tiếp cận WCAG AA.
- **Ảnh hưởng:** `:core-ui` cập nhật `view_empty_state.xml` và `view_error_state.xml` với container tròn pastel 72x72dp và icon Lucide; chuẩn hóa hơn 14 màn hình trống trên cả `:app-tenant` và `:app-landlord` (Hợp đồng, Hóa đơn, Sự cố, Yêu thích, Tìm kiếm, Matching, Chat, Tòa nhà, Điện nước).

### 2026-11-13 – Nâng cấp Bề mặt Thẻ (Card Surfaces), Viền mảnh `#E2E8F0`, Thang bo góc và Hiệu ứng Chạm Ripple
- **Lý do:** Loại bỏ bóng đổ nặng nề thiếu tinh tế (`cardElevation` lớn), thay thế bằng ngôn ngữ thẻ phẳng hiện đại có viền vi mô tinh tế (`strokeColor="@color/rentify_divider"` `#E2E8F0`, `cardElevation="0dp/1dp"`), đồng thời sửa lỗi gán `background="?attr/selectableItemBackground"` trên MaterialCardView làm chìm hiệu ứng ripple dưới màu nền thẻ.
- **Ảnh hưởng:** `:core-ui` cập nhật theme gốc `Widget.Rentify.CardView`; chuẩn hóa 44 file layout của Tenant và Landlord: quy đổi corner radius về thang `@dimen/radius_*`, cấu hình `foreground="?attr/selectableItemBackground"` kèm `clickable="true"` và `focusable="true"` cho toàn bộ thẻ bấm được.

### 2026-11-13 – Nâng cấp toàn diện hệ thống Icon dự án sang Lucide Icons chính thức
- **Lý do:** Chuẩn hóa phong cách đồ họa của toàn bộ ứng dụng Rentify theo ngôn ngữ thiết kế Minimalist hiện đại, nét vẽ 2dp thanh thoát bo góc mềm mại, độ phân giải sắc nét 24x24dp và hỗ trợ đổi màu động (tinting) hoàn hảo.
- **Ảnh hưởng:** `:core-ui` cập nhật 50 file icon vector (`ic_*.xml`) bao gồm toàn bộ BottomNav của Tenant & Landlord, các nút thao tác nhanh, icon tiện ích phòng trọ và danh mục phân loại sự cố. Giữ nguyên tên file tài nguyên giúp 102 layout của cả `:app-tenant` và `:app-landlord` tự động áp dụng giao diện mới mà không gây xung đột.
### 2026-11-13 – Chuẩn hóa toàn bộ hệ thống Spacing (Margin & Padding) theo dimens.xml (hệ số 4dp) trên toàn bộ ứng dụng
- **Lý do:** Loại bỏ hoàn toàn các giá trị hardcode `dp` rải rác, thống nhất nhịp điệu thị giác (visual cadence), đảm bảo giao diện đạt độ hoàn thiện cao và chuẩn xác theo nguyên tắc thiết kế 4dp/8dp của Material Design 3 trên cả 2 ứng dụng Tenant và Landlord.
- **Ảnh hưởng:** `:core-ui` mở rộng thang đo `dimens.xml` từ `2dp` (`spacing_2xs`) đến `96dp` (`spacing_bottom_nav`), chuẩn hóa toàn bộ 102 layout XML (`:core-ui`, `:app-tenant`, `:app-landlord`), quy đổi an toàn các khoảng cách lẻ (6dp, 10dp, 14dp) về mốc 4dp tương ứng mà không làm vỡ cấu trúc giao diện.
### 2026-11-13 – Triển khai Phân hệ Chat v2, Lời nhắc tự động Bot Reminders và Hồ sơ Chủ trọ (Giai đoạn 5)
- **Lý do:** Hoàn thành giai đoạn cuối cùng trong kế hoạch xây dựng ứng dụng Chủ trọ (`:app-landlord`): Xây dựng giao diện danh sách hội thoại đa luồng (trực tiếp 1:1 và nhóm tòa nhà/cư dân), màn hình nhắn tin chi tiết thời gian thực hỗ trợ gửi tin nhắn và tính năng đặc thù cho Chủ trọ là gửi lời nhắc tự động Rentify Bot (`POST /conversations/{id}/bot-reminders` theo mẫu hóa đơn/hợp đồng/điện nước), đồng thời hoàn thiện màn hình Hồ sơ cá nhân (`LandlordProfileFragment`) với hiển thị thông tin người dùng, đổi mật khẩu bảo mật (`POST /auth/change-password`) và quy trình đăng xuất an toàn.
- **Ảnh hưởng:** 
  - `:app-landlord` xây dựng `LandlordChatRepository`, `LandlordChatViewModel`, `LandlordConversationAdapter`, `LandlordChatFragment`, `LandlordChatDetailActivity`, `LandlordChatDetailViewModel`, `LandlordMessageAdapter` (hỗ trợ hiển thị 3 loại tin nhắn: gửi đi, nhận về, và card bot reminder), dialog gửi bot reminder, cập nhật `LandlordProfileFragment` kết nối `UserRepositoryImpl` và hoàn thiện luồng đăng xuất.
### 2026-11-13 – Triển khai Phân hệ Quản lý Sự cố & Bảo trì Vận hành cho Landlord (Giai đoạn 4)
- **Lý do:** Hoàn thiện luồng tiếp nhận và xử lý sự cố thiết bị/hạ tầng từ khách thuê (tag Operation) cho ứng dụng Chủ trọ (`:app-landlord`) chuẩn theo API spec: Hỗ trợ duyệt hàng đợi sự cố đa trạng thái (`open`, `in_progress`, `resolved`, `cancelled`), xem chi tiết báo cáo kèm thông tin người gửi & vị trí phòng trọ, quay số gọi nhanh cho khách thuê, phân công thợ/đơn vị sửa chữa, nghiệm thu kết quả có ghi nhận chi phí sửa chữa (`repairCost`) & ghi chú nguyên nhân/khắc phục (`resolutionNote`), và hủy báo cáo sự cố khi không hợp lệ.
- **Ảnh hưởng:** 
  - `:core-network` mở rộng `IssueApiService` và bổ sung `IssueReportListResponseDto`, `IssueReportUpdateRequestDto`, `IssueReportUpdateResponseDto`.
  - `:app-landlord` xây dựng `LandlordIssueRepository`, `IssueViewModel`, `LandlordIssuesActivity` (bộ lọc ChipGroup 5 trạng thái), `LandlordIssueDetailActivity` (giao diện thẻ chi tiết, dialog tiếp nhận/phân công thợ, dialog nghiệm thu chi phí), kết nối trực tiếp Khối 4 trên Tab Vận hành (`OperationsHubFragment`).
### 2026-11-13 – Triển khai Phân hệ Ghi chỉ số Điện Nước, Lập & Phát hành Hóa đơn và Thu tiền mặt cho Landlord (Giai đoạn 3)
- **Lý do:** Hoàn thành toàn diện phân hệ quản lý tài chính & vận hành chỉ số cho ứng dụng Chủ trọ (`:app-landlord`) chuẩn theo API spec: Ghi nhận chỉ số điện/nước hàng loạt theo tòa nhà (`POST /meter-readings/bulk`), tự động lập/tính toán hóa đơn theo kỳ (`POST /invoices/generate`), phát hành hóa đơn hàng loạt hoặc đơn lẻ kèm Idempotency-Key (`POST /invoices/issue-invoice`), theo dõi chi tiết bóc tách chi phí dịch vụ/phí khác/lịch sử giao dịch, ghi nhận thu tiền mặt trực tiếp (`POST /payments/record-cash-payment`), và hủy hóa đơn sai sót có lý do (`POST /invoices/{id}/cancel-invoice`).
- **Ảnh hưởng:** 
  - `:core-network` bổ sung `MeterApiService`, `MeterDtos`, mở rộng `InvoiceApiService` và `InvoiceDtos`.
  - `:app-landlord` xây dựng `LandlordMeterRepository`, `LandlordInvoiceRepository`, `BillingViewModel`, `LandlordMeterReadingsActivity` (chọn tòa nhà, kỳ chốt số, bảng nhập điện nước đa phòng), `LandlordInvoicesActivity` (bộ lọc ChipGroup, thanh tác vụ phát hành hàng loạt, dialog lập hóa đơn theo kỳ), `LandlordInvoiceDetailActivity` (bảng kê tài chính, BottomSheet thu tiền mặt, dialog hủy hóa đơn), kết nối trực tiếp Khối 2 & 3 trên Tab Vận hành (`OperationsHubFragment`).
### 2026-11-13 – Triển khai Phân hệ Quản lý Hợp đồng, Khách thuê & Quyết toán cọc cho Landlord (Giai đoạn 2)
- **Lý do:** Hoàn thiện giai đoạn 2 ứng dụng Chủ trọ (`:app-landlord`) theo API spec (tag Contract, ContractMember): Cho phép chủ trọ tạo hợp đồng mới cho phòng trống, lọc danh sách hợp đồng đa trạng thái (`active`, `signed`, `draft`, `terminated`), quản lý danh sách thành viên ở ghép (`contract-members`), kích hoạt hợp đồng (`activate-contract`), và thực hiện quy trình trả phòng kèm quyết toán tiền cọc (`complete-checkout` & `deposit-settlement`).
- **Ảnh hưởng:** 
  - `:core-network` mở rộng `ContractApiService` và bổ sung đầy đủ DTOs (`ContractListResponseDto`, `ContractCreateRequestDto`, `ContractActivateRequestDto`, `ContractCompleteCheckoutRequestDto`, `DepositSettlementRequestDto`, `ContractMemberListResponseDto`, `ContractMemberAddRequestDto`, `ContractMemberLeaveRequestDto`).
  - `:app-landlord` xây dựng `LandlordContractRepository`, `ContractViewModel`, `LandlordContractsActivity`, `CreateContractActivity`, `LandlordContractDetailActivity`, BottomSheet thêm thành viên & quyết toán cọc, kết nối trực tiếp từ Khối 1 của Tab Vận hành (`OperationsHubFragment`).

### 2026-11-13 – Triển khai Phân hệ Nền tảng Điều hướng, Dashboard & Quản lý Tài sản (Buildings & Rooms) cho Landlord (Giai đoạn 1)
- **Lý do:** Hoàn thiện nền tảng ứng dụng Chủ trọ (`:app-landlord`) chuẩn theo API spec: Xây dựng BottomNavigation 5 tab chuyên biệt, Dashboard KPI tổng hợp, quản lý danh sách & tạo tòa nhà mới gửi yêu cầu phê duyệt tới Admin kèm dữ liệu đơn vị hành chính chuẩn (Tỉnh/Thành, Phường/Xã), cấu hình tài khoản nhận tiền VietQR cho từng tòa nhà, và quản lý danh sách phòng trọ kèm thao tác tạo phòng & đánh dấu sẵn sàng cho thuê (`mark-ready`).
- **Ảnh hưởng:** 
  - `:core-network` bổ sung `AdministrativeUnitsApiService`, `AdministrativeUnitsDtos`, `BuildingApiService`, `BuildingDtos`, mở rộng `RoomApiService` và `LandlordRoomDtos`.
  - `:core-ui` bổ sung các icon điều hướng Landlord (`ic_nav_dashboard`, `ic_nav_properties`, `ic_nav_operations`), drawable `bg_bottom_sheet_rounded`, và token màu `rentify_text_muted`.
  - `:app-landlord` xây dựng `LandlordMainFragment`, `DashboardFragment`, `PropertiesFragment` (2 tab Tòa nhà & Phòng trọ), `CreateBuildingActivity`, `BuildingDetailActivity` (kèm BottomSheet VietQR), `CreateRoomActivity`, cùng toàn bộ Repositories, ViewModels, Adapters liên quan.

### 2026-11-13 – Đồng bộ toàn diện API Spec (api-doc PR #50 - #56): Hóa đơn /me/invoices, Chuỗi hóa Tiền tệ & Chat v2 Reply Quotes
- **Lý do:** Đáp ứng các thay đổi phá vỡ (breaking changes) từ phía backend: đường dẫn `GET /invoices/tenant` bị xóa và thay bằng `GET /me/invoices`; toàn bộ trường số tiền trong hóa đơn được quy chuẩn về String định dạng regex `^(0|[1-9]\d*)(\.\d{1,4})?$`; backend chính thức hỗ trợ quy trình xác thực email (Email verification), Signed Download URL cho Media và tính năng trích dẫn phản hồi tin nhắn (Reply) trong Chat v2.
- **Ảnh hưởng:** `:core-network` cập nhật `InvoiceDtos`, `InvoiceApiService`, `UserCurrentResponseDto`, `AuthEmailVerificationDtos`, `MediaApiService`, `ChatApiService`; `:core-ui` cập nhật `LoginViewModel` bắt lỗi `EMAIL_NOT_VERIFIED`; `:app-tenant` cập nhật `TenantInvoiceRepositoryImpl`, `MyInvoicesAdapter`, `InvoiceDetailActivity`, và hoàn thiện trải nghiệm Chat Reply (nhấn giữ tin nhắn để trích dẫn, preview bar, optimistic quote box).
### 2026-11-05 – Tái thiết kế giao diện Trạng thái Ẩn Hồ sơ Ghép bạn (Matching Hidden Profile View)
- **Lý do:** Khắc phục trải nghiệm kẹp card thông báo vàng lên trên bảng tin bạn cùng phòng khi hồ sơ đang tắt; ẩn hoàn toàn thanh tìm kiếm khu vực và danh sách ứng viên, thay thế bằng Empty State toàn diện khoáng đạt ở giữa màn hình kèm nút "Bật lại hồ sơ" 1 chạm.
- **Ảnh hưởng:** `:app-tenant` cập nhật `fragment_matching.xml` (thêm `layoutSearchArea`, bổ sung `layoutProfileHidden` không dùng card viền), cập nhật `MatchingFragment.kt` (đồng bộ ẩn/hiện search & feed khi `MyProfileStatus.Hidden`).

### 2026-11-05 – Tái cấu trúc giao diện Tab "Phòng của tôi" theo Bố cục 3 Smart Widgets (Phương án 2)
- **Lý do:** Khắc phục tình trạng "Card Fatigue" (quá nhiều thẻ chữ nhật đơn lẻ, người dùng phải cuộn trang dài gây khó theo dõi nội dung). Gom nhóm 6 thẻ rời rạc thành 3 widget thông minh có thứ bậc thị giác rõ nét: (1) Căn hộ & Chủ trọ, (2) Trung tâm tài chính hóa đơn, (3) Danh mục Vận hành & Cư dân (Hợp đồng, Sự cố, Bạn cùng phòng).
- **Ảnh hưởng:** `:app-tenant` cập nhật `fragment_my_room.xml` (rút ngắn hơn 60% chiều dài trang, tích hợp ảnh phòng 96dp bo tròn góc và 2 nút gọi/chat nhanh), nâng cấp `MyRoomFragment.kt` (tích hợp bộ sưu tập ảnh căn hộ/phòng trọ Unsplash sắc nét thay thế placeholder xám).

### 2026-11-05 – Nâng cấp toàn diện Phân hệ Chat: Tin nhắn Ảnh, Trò chuyện Nhóm & Đồng bộ Thời gian thực
- **Lý do:** Hoàn thiện 3 trụ cột thiết yếu của trải nghiệm nhắn tin theo spec API Rentify: (1) Chia sẻ ảnh đính kèm (hóa đơn, phòng trọ) với cơ chế nén ảnh và tải lên R2 storage qua Media API 3 bước; (2) Trò chuyện nhóm giữa các bạn cùng phòng và cư dân (`kind = group`) kèm phân quyền Trưởng nhóm / Thành viên; (3) Tự động cập nhật tin nhắn mới liên tục (Smart Polling chu kỳ 3.5s) khi người dùng đang mở màn hình chat mà không cần thao tác tải lại thủ công.
- **Ảnh hưởng:** `:core-network` bổ sung `MediaDtos.kt` và `MediaApiService.kt`; `:core-ui` bổ sung các vector drawable `ic_group_add`, `ic_group`, `ic_more_vert`; `:app-tenant` bổ sung `TenantMediaRepository`, nâng cấp `ChatDetailActivity`, `ChatDetailViewModel`, `MessageAdapter` (thêm bong bóng chat ảnh Optimistic UI & `PhotoViewerDialogFragment`), xây dựng bộ công cụ Quản lý nhóm (`GroupInfoBottomSheetDialog`, `GroupMemberAdapter`, `CreateGroupChatDialogFragment`), và tích hợp smart sync trên `ChatFragment`.

### 2026-11-05 – Triển khai Phân hệ Quản lý Hóa đơn & Lịch sử Thanh toán (Invoice & Payment Management) cho Tenant
- **Lý do:** Hoàn thiện luồng tài chính tiền phòng (tag Billing) theo spec API Rentify, cho phép Tenant theo dõi danh sách hóa đơn theo từng kỳ thu, kiểm tra chi tiết bóc tách từng dịch vụ (tiền phòng, số điện cũ/mới/tiêu thụ, nước, internet, rác), theo dõi lịch sử các lần chuyển khoản/tiền mặt, và thanh toán nhanh 1 chạm bằng VietQR.
- **Ảnh hưởng:** `:core-network` bổ sung `InvoiceDtos.kt` và `InvoiceApiService.kt`; `:app-tenant` bổ sung `TenantInvoiceRepository`, `MyInvoicesActivity` (giao diện 2 tab Hóa đơn & Giao dịch kèm bộ lọc ChipGroup), `InvoiceDetailActivity` (bảng kê chi tiết dịch vụ & tổng kết tài chính), kết nối trực tiếp sự kiện bấm thẻ `cardCurrentInvoice` và nút "Xem tất cả hóa đơn" trên `MyRoomFragment`.

### 2026-11-05 – Hợp nhất Quản lý Hợp đồng & Lịch sử Thuê vào My Room Hub (Đa phòng & 2 Tab UX)
- **Lý do:** Tối ưu hóa trải nghiệm người dùng theo đúng ngữ cảnh thực tế: loại bỏ mục "Hợp đồng & Lịch sử thuê" khỏi Profile cá nhân để quy về đúng tab "Phòng của tôi" (My Room). Khách thuê có thể sở hữu nhiều phòng thuê cùng lúc. Nếu chỉ có 1 phòng hiệu lực, hệ thống hiển thị thẳng Dashboard chi tiết (kèm nút xem lịch sử); nếu có nhiều hơn 1 phòng hoặc đang xem lịch sử, phân chia thành 2 tab: "Đang hiệu lực" và "Đã kết thúc", chạm vào từng hợp đồng để mở chi tiết cụ thể.
- **Ảnh hưởng:** `:app-tenant` loại bỏ `btnMyContractsRow` khỏi `fragment_profile.xml` và `ProfileFragment.kt`; nâng cấp `MyRoomUiState` (`SingleRoom`, `MultiRooms`), `MyRoomViewModel` (tiêm `TenantContractRepository`), `fragment_my_room.xml` (bổ sung icon `btnViewAllContracts` và container 2 tab `layoutMultiRooms`), `MyRoomFragment.kt` (gắn kết `MyContractsAdapter`, TabLayout, toggle view).
### 2026-11-05 – Triển khai Phân hệ Quản lý Hợp đồng & Lịch sử Thuê (Contract Management & Rental History) cho Tenant
- **Lý do:** Hoàn thiện luồng quản lý hợp đồng thuê phòng của khách thuê (tag Contract) theo spec API Rentify, cho phép Tenant theo dõi danh sách tất cả các hợp đồng thuê nhà từ trước đến nay (lọc theo Đang hiệu lực, Đã ký, Hết hạn, Chấm dứt), và xem chi tiết thỏa thuận, tiền cọc, số lượng xe đăng ký và điều khoản.
- **Ảnh hưởng:** `:core-network` bổ sung `ContractDtos.kt` và `ContractApiService.kt`; `:app-tenant` bổ sung `TenantContractRepository`, `MyContractsActivity`, `ContractDetailActivity`, bổ sung mục "Hợp đồng & Lịch sử thuê" trên `ProfileFragment` và kết nối sự kiện bấm thẻ `cardContract` trên `MyRoomFragment`.

### 2026-11-05 – Triển khai Phân hệ Quản lý Sự cố & Hư hỏng (Issue Reporting & Management) cho Tenant
- **Lý do:** Hoàn thiện luồng vận hành phòng trọ (tag Operation) theo spec API Rentify, cho phép Tenant báo cáo sự cố hư hỏng (nước, điện, gia dụng, kết cấu, an ninh), theo dõi tiến độ xử lý, thông tin thợ sửa chữa, chi phí, và hủy yêu cầu khi cần.
- **Ảnh hưởng:** `:core-network` bổ sung `IssueDtos.kt` và `IssueApiService.kt`; `:app-tenant` bổ sung `TenantIssueRepository`, `CreateIssueActivity`, `MyIssuesActivity`, `IssueDetailActivity`, tích hợp thẻ `cardIssues` và nút `btnReportIssue` trên `MyRoomFragment`.

### 2026-11-05 – Triển khai Màn hình Chi tiết Xem Trọ (RoomDetailActivity) kết nối trực tiếp với Chat & Gọi điện
- **Lý do:** Người dùng từ Explore, Tìm kiếm và Phòng đã lưu cần xem thông tin toàn diện về căn phòng theo spec `GET /rooms/{roomId}`, đồng thời có thể liên hệ ngay với chủ trọ (gọi điện thoại trực tiếp hoặc mở cuộc trò chuyện 1:1 qua Chat).
- **Ảnh hưởng:** `:core-network` bổ sung `RoomDetailDtos.kt` và endpoint `GET /rooms/{roomId}`; `:app-tenant` bổ sung `TenantRoomRepository.getRoomDetail`, `RoomDetailViewModel`, `RoomDetailActivity`, `RoomImageSliderAdapter` (ViewPager2), kết nối điều hướng từ `ExploreFragment`, `SearchResultActivity`, `FavoriteRoomsActivity`.

### 2026-11-05 – Đồng bộ chuỗi hóa Sequence và tích hợp snapshot displayName theo PR #49, PR #51
- **Lý do:** Chuẩn hóa toàn bộ con trỏ `sequence`, `lastReadSequence`, `leftSequence` từ `Long` sang chuỗi thập phân regex `^\d+$` theo đặc tả API mới nhằm chống suy giảm độ chính xác BigInt; đồng thời tận dụng trường `displayName` mới được snapshot từ server để hiển thị chính xác tên người chat trong hội thoại 1:1.
- **Ảnh hưởng:** `:core-network` cập nhật các DTO Chat (`ConversationDtos`, `MessageDtos`), bổ sung `UserDirectoryResponseDto`, `ConversationUnreadSummaryResponseDto` và query filters mới; `:app-tenant` cập nhật `ChatDetailViewModel`, `TenantChatRepository`, `ConversationAdapter` và `ChatFragment`.

### 2026-11-04 – Kiểm soát trạng thái ẩn hồ sơ (Matching Visibility) và ngăn chặn lỗi 403 khi gửi yêu cầu
- **Lý do:** Tránh trường hợp người dùng đang ẩn hồ sơ (`isVisible = false`) gửi lời mời ghép đôi khiến đối phương bấm xem chi tiết bị lỗi `403 PROFILE_NOT_VISIBLE`, đồng thời cung cấp chỉ báo trực quan giúp người dùng luôn biết rõ hồ sơ của mình đang bật hay ẩn.
- **Ảnh hưởng:** `:app-tenant` bổ sung `cardProfileBanner` trên `fragment_matching.xml`, quản lý `MyProfileStatus` trong `MatchingViewModel`, cung cấp cơ chế bật nhanh 1 chạm `quickEnableVisibility()` và Dialog điều hướng tại Feed và `RoommateDetailActivity`.
### 2026-11-04 – Xây dựng phân hệ Ghép bạn ở cùng (Roommate Matching) và điều hướng tức thì sang Chat
- **Lý do:** Tích hợp tính năng AI Match score, tìm kiếm bạn ở ghép theo khu vực, quản lý hồ sơ bạn ở ghép (Roommate Profile) và vòng đời lời mời ghép đôi (Match Request: gửi, chấp nhận, từ chối, thu hồi). Khi chấp nhận lời mời thành công, hệ thống sử dụng ngay `conversationId` trả về từ API để mở trực tiếp `ChatDetailActivity` với đối tác ghép đôi.
- **Ảnh hưởng:** `:core-network` bổ sung `MatchingApiService` và bộ DTOs Matching; `:app-tenant` bổ sung `TenantMatchingRepository`, `MatchingFragment`, `RoommateDetailActivity`, `EditRoommateProfileActivity`, `RoommateFeedAdapter`, `MatchRequestAdapter`.
### 2026-11-04 – Xây dựng Dashboard Phòng của tôi (My Room Hub) với VietQR và phân tách 3 trạng thái
- **Lý do:** Trung tâm hóa toàn bộ dịch vụ liên quan đến phòng đang thuê cho Tenant: hiển thị thông tin phòng, gọi điện/chat trực tiếp với chủ trọ, quản lý hóa đơn kèm BottomSheet VietQR, xem hợp đồng PDF và danh sách bạn cùng phòng.
- **Ảnh hưởng:** `:app-tenant` bổ sung `MyRoomViewModel`, `RoommateAdapter`, `VietQrBottomSheetDialog`, hoàn thiện `MyRoomFragment` và kết nối với `TenantChatRepository`, `TenantRoomRepository`.

### 2026-11-04 – Xây dựng Kiến trúc Chat Tenant với Cursor Pagination và Optimistic UI
- **Lý do:** Chuẩn hóa theo spec API Chat với con trỏ đảo ngược (`cursor`), đảm bảo gửi tin nhắn mượt mà không độ trễ (Optimistic UI) và chống trùng lặp qua `clientMessageId`.
- **Ảnh hưởng:** `:core-network` bổ sung `ChatApiService` và bộ DTOs Chat; `:app-tenant` bổ sung `TenantChatRepository`, `ChatFragment` và `ChatDetailActivity`.

### 2026-11-04 – Tách RoomRepository và DevTweaks khỏi :core-data theo Hướng B
- **Lý do:** Tuân thủ nguyên lý ISP, đảm bảo `:core-data` chỉ chứa 100% tài nguyên dùng chung, loại bỏ nghiệp vụ đặc thù của Tenant khỏi thư viện dùng chung để chuẩn bị phát triển `:app-landlord`.
- **Ảnh hưởng:** `:app-tenant` tự quản lý `TenantRoomRepository` (kế thừa `BaseRepository`) và `TenantDevPreferences`. `:core-data` tinh gọn, không còn code tạm hay nghiệp vụ riêng.

### 2026-10-21 – Tách FavoriteRoomsActivity thành Activity độc lập thay vì Fragment con
- **Lý do:** Cho phép mở màn hình Phòng đã lưu từ nhiều nơi (Profile, Explore, Thông báo hoặc Deeplink) mà không phụ thuộc vào BottomNavigationView của TenantMainFragment, đồng thời có Toolbar BackButton chuẩn hệ thống.
- **Ảnh hưởng:** Module :app-tenant bổ sung FavoriteRoomsActivity, quản lý vòng đời và phân trang danh sách phòng độc lập.

### 2026-10-21 – Tích hợp BottomSheet chỉnh sửa thông tin cá nhân và đổi mật khẩu trong Profile Tenant
- **Lý do:** Tối ưu trải nghiệm người dùng (UX) bằng việc mở form nhập liệu dạng BottomSheet ngay trên màn hình Profile thay vì chuyển sang Activity/Fragment mới, giữ nguyên ngữ cảnh của người dùng.
- **Ảnh hưởng:** Module :app-tenant quản lý trạng thái cập nhật thông tin qua ProfileViewModel, tự động reload và cập nhật đồng bộ các trường tên, SĐT, avatar chữ cái đầu.

### 2026-10-21 – Tích hợp Coil, SwipeRefreshLayout và bổ sung Dev Tweak hasRoom cho Tenant Home
- **Lý do:** Coil giúp tải ảnh thumbnail phòng trọ nhẹ, hỗ trợ cache; SwipeRefreshLayout cho phép kéo làm mới danh sách; Tweak hasRoom trong AppDataStore giúp tester dễ dàng chuyển đổi giữa trạng thái đã có phòng và chưa có phòng ngay trên UI.
- **Ảnh hưởng:** Module :core-ui xuất khẩu coil và swiperefreshlayout qua api(). Giao diện trang chủ phản ánh chuẩn xác mockup thiết kế.

### 2026-10-11 – Tách AppDataStore riêng biệt với TokenDataStore để lưu cờ Onboarding
- **Lý do:** Tránh việc khi người dùng đăng xuất (`clearTokens()`) làm xóa nhầm cờ `onboarding_completed`.
- **Ảnh hưởng:** Đảm bảo Onboarding chỉ xuất hiện 1 lần duy nhất trong toàn bộ vòng đời ứng dụng sau khi cài đặt.

### 2026-10-11 – Đồng bộ toàn bộ luồng Auth qua Interface Callbacks và chuẩn hóa schema API
- **Lý do:** Khắc phục lỗi mất callback khi xoay màn hình/recreate Activity và đảm bảo `role` gửi lên backend luôn khớp enum chữ thường (`tenant`/`landlord`) theo spec Apidog.
- **Ảnh hưởng:** Các màn hình Onboarding, Login, Register, ForgotPassword, VerifyOtp, ResetPassword bảo toàn 100% vòng đời và kết nối thông suốt với ViewModel.

### 2026-10-10 – Xây dựng UI Đăng nhập & Đăng ký trong `:core-ui` với tùy chỉnh màu con trỏ và xóa lỗi tự động
- **Lý do:** Tái sử dụng giao diện xác thực chung cho cả 2 ứng dụng Tenant và Landlord, tùy chỉnh UX ô nhập liệu với con trỏ đồng màu primary text và tự động xóa nhãn báo lỗi khi người dùng gõ tiếp.
- **Ảnh hưởng:** `LoginFragment` và `RegisterFragment` được tích hợp sẵn validation client, floating hint và quản lý trạng thái qua ViewModel state flow.

### 2026-10-10 – Xây dựng AuthRepository & Auth DTOs theo chuẩn API Spec (Rentify API)
- **Lý do:** Khởi tạo tầng mạng và lưu trữ dữ liệu xác thực người dùng cho cả app Tenant và Landlord theo đúng 6 endpoints trong `api-doc/reference/rentify.json`.
- **Ảnh hưởng:** `AuthRepository` tự lưu `accessToken`, `refreshToken`, `userId`, `userRole` vào `TokenDataStore` sau khi đăng nhập thành công và xóa token khi logout hoặc khi refresh token hết hạn (HTTP 401).

### 2026-10-08 – Xây dựng Module core-data với Jetpack DataStore Preferences
- **Lý do:** Cần tầng lưu trữ dữ liệu local an toàn (lưu Access Token/Refresh Token) và quản lý phiên đăng nhập (SessionManager) dùng chung.
- **Ảnh hưởng:** Cung cấp BaseRepository giúp chuyển đổi NetworkResult từ `core-network` thành dữ liệu an toàn cho ViewModel.

### 2026-10-08 – Xây dựng Module core-network với MaskedLoggingInterceptor
- **Lý do:** Cần tầng Network Layer dùng chung cho cả `app-landlord` và `app-tenant` để kết nối APIDog Mock Server.
- **Ảnh hưởng:** Đảm bảo che header `Authorization` khi log debug theo quy tắc D.11.

## 3. Vấn đề & lưu ý
<!-- [MỞ] / [ĐÃ XỬ LÝ] -->
- [MỞ] 2026-11-18 – `GET /me/room-hub` không trả chủ trọ, bạn cùng phòng, số sự cố, hóa đơn hiện tại, `contractCode`, `rentalType`, `signedContractUrl`, ảnh phòng. App ẩn các phần này hoặc dùng giá trị mặc định, hóa đơn lấy tạm từ `/me/invoices`. Cần BE cho biết nguồn dữ liệu; xem `docs/BACKEND_REQUESTS.md` mục A4 và backlog mục 27, 28. Các trạng thái có phòng chưa kiểm thử vì tài khoản test chưa có hợp đồng.
- [ĐÃ XỬ LÝ] 2026-11-18 – Bẫy: màn "Không tải được dữ liệu" ở tab Phòng không đến từ backend mà do app đọc mảng như đối tượng (Moshi/Retrofit báo lỗi cho cả phản hồi). Khi tab Phòng lỗi, xem log `NetworkLog` để đối chiếu dạng dữ liệu thực với DTO trước khi nghi backend.
- [MỞ] 2026-11-18 – **Yêu cầu BE xem xét:** chức năng yêu thích phòng (`GET/POST /me/favorite-rooms`, `DELETE /me/favorite-rooms/{roomId}`) trả `404 RESOURCE_NOT_FOUND` dù tài khoản tenant hợp lệ và các API `/me/*` khác trả 200. Phản hồi giống hệt một đường dẫn không tồn tại (`GET /me/does-not-exist`), nghi dịch vụ phía sau chưa có handler hoặc bản đang chạy chưa chứa chức năng này. App gửi đúng spec, đã báo lỗi rõ theo mã và coi `409 ALREADY_FAVORITED` là thành công. Cách tái hiện và bằng chứng: `docs/BACKEND_REQUESTS.md` mục A6.
- [ĐÃ XỬ LÝ] 2026-11-18 – Server kiểm tra nghiêm tham số truy vấn: tham số không có trong spec của endpoint đó trả `422 UNEXPECTED_FIELD`. App từng gửi thừa `format=json` cho `/roommate-profiles`, `/match-requests`, `/me/invoices`, `filter[status]` cho `/contract-members`, `filter[buildingId]` cho `/meter-readings` (đã bỏ). Chỉ gửi đúng tham số có trong spec của từng endpoint (mỗi endpoint một danh sách riêng, ví dụ `/rooms` và `/me/favorite-rooms` có `format`, `/me/invoices` thì không).
- [MỞ] 2026-11-18 – Khám phá theo tòa nhà đang dùng cách nhóm phòng ở phía app (phải tải toàn bộ phòng, không phân trang được tòa, ảnh tòa lấy từ ảnh phòng). Cần BE thêm `filter[buildingId]` (hoặc `GET /buildings/{id}/rooms`) và `thumbnailUrl`, `minRentPrice`, `availableRoomCount` cho tòa nhà; xem `docs/BACKEND_REQUESTS.md` mục C4 và backlog mục 25.
- [MỞ] 2026-11-18 – `GET /rooms/{id}` trả `"landlord": null` cho mọi phòng, kể cả khi đã đăng nhập, trong khi spec đặt `landlord` bắt buộc (`id`, `name`, `phone`). App tạm cho `landlord` nhận null để màn chi tiết phòng vẫn hiển thị (ẩn thẻ Chủ trọ). Nút Nhắn tin báo không có thông tin liên hệ. Cần BE trả đủ `landlord`; xem `docs/BACKEND_REQUESTS.md` mục A3 và backlog mục 24.
- [ĐÃ XỬ LÝ] 2026-11-18 – Đính chính: các dòng cũ ghi `refreshToken` qua cookie HttpOnly (2026-11-18 và 2026-10-11) là sai theo spec hiện hành; refresh token gửi trong body JSON, không cần `CookieJar`. App đã sửa gửi body và tự làm mới khi 401.
- [MỞ] 2026-11-18 – Chưa kiểm chứng làm mới token với tài khoản thật (cần đăng nhập rồi chờ quá 15 phút hoặc rút ngắn hạn token). Khi refresh token hết hạn/bị thu hồi, `DataStoreTokenRefresher` xóa phiên nhưng app chưa tự chuyển về màn đăng nhập; màn đang mở sẽ báo lỗi cho đến khi đăng nhập lại.
- [MỞ] 2026-11-18 – Ảnh phòng: spec dùng `thumbnailUrl` (`GET /rooms`) và `photoUrls` (`GET /rooms/{id}`), nhưng server thật trả `photoUrls` ở cả hai, không có `thumbnailUrl`, và cả 4 phòng hiện có đều không có ảnh. App landlord chưa có luồng tải ảnh phòng (`CreateRoomActivity` gửi `photoMediaIds` rỗng, chưa có presign/upload). Danh sách phòng của tenant lấy ảnh qua `RoomPublicListResponseDto.coverUrl` (`thumbnailUrl`, nếu trống thì `photoUrls[0]`). Chi tiết: `docs/BACKEND_REQUESTS.md` mục A2.
- [MỞ] 2026-11-18 – Backend thật nhận `refreshToken` qua cookie HttpOnly nhưng app chưa có `CookieJar` (backlog mục 3): luồng làm mới token khi hết hạn và đăng xuất có thể thất bại với gateway thật, cần kiểm tra sớm. Các màn đang bám dữ liệu mock (vd. `requester` của danh sách lời mời) có thể lỗi với dữ liệu thật.
- [MỞ] 2026-11-17 – Code Matching đang lệch spec `match-requests` (api-doc commit `a8d455f`, chưa sửa, chờ BE): (1) `GET /match-requests` không còn trả `requester`/`target`/`matchScore`/`message` nên `MatchRequestListResponseDto` sẽ lỗi parse; (2) `accept-request` không còn trả `conversationId`; (3) mã lỗi `DUPLICATE_MATCH_REQUEST`/`SELF_MATCHING_NOT_ALLOWED` không còn trong spec. Header `Idempotency-Key` của `POST /match-requests` đã bổ sung (`TenantMatchingRepositoryImpl` sinh UUID mỗi lần gửi).
- [MỞ] 2026-11-17 – Cần hỏi BE: tab Lời mời chỉ có `requesterId`/`targetId` nên không hiển thị được tên, avatar. Spec chưa có cách tra theo id cho tenant (`GET /users/{userId}` thuộc nhóm admin, `/users/directory` chỉ tìm theo `q`). Đề xuất BE thêm lại `requester`/`target` (tên, avatar) vào item, hoặc xác nhận tenant được gọi `GET /users/{userId}`.
- [MỞ] 2026-11-17 – Cần hỏi BE: spec `GET /me/roommate-profile` (trạng thái `designing`) chỉ trả `tenantId`, `lifestyle`, `personality`, `isVisible`, trong khi `PUT` nhận đầy đủ gender, birthYear, occupation, budget, moveInOn, bio…; form `EditRoommateProfileActivity` đang đọc các field này để điền sẵn. Do ví dụ spec của `lifestyle` là object tự do (`{sleep: "23h-6h"}`), `lifestyle` ở `RoommateProfileMyResponseDto`/`RoommateProfileSaveRequestDto` đã đổi sang `Map<String, Any>`, form đọc/ghi 7 cờ boolean theo key.
- [ĐÃ XỬ LÝ] 2026-11-13 – Hiệu ứng Ripple trên MaterialCardView: Khi gán `android:background="?attr/selectableItemBackground"` trên `MaterialCardView`, hiệu ứng ripple sẽ bị che khuất bởi màu nền (`cardBackgroundColor`) của thẻ. Luôn dùng `android:foreground="?attr/selectableItemBackground"` kết hợp `android:clickable="true"` và `android:focusable="true"` để hiệu ứng gợn sóng hiển thị đúng trên bề mặt thẻ.
- [ĐÃ XỬ LÝ] 2026-11-13 – Android XML AAPT2 không hỗ trợ cú pháp âm trên Dimension Token: Thuộc tính âm như `-16dp` không thể viết dạng `-@dimen/spacing_md` trong XML layout (gây lỗi biên dịch AAPT2). Những trường hợp cần margin âm (như bù đắp padding container) phải giữ nguyên `-16dp` hoặc định nghĩa token riêng có giá trị âm.
- [MỞ] 2026-11-13 – Ghi nhận thành viên rời phòng (`leave-contract`): Endpoint `/contract-members/{tenantId}/leave-contract` yêu cầu thành viên phải có tài khoản hệ thống (`tenantId` khác null); thành viên không có tài khoản chỉ cập nhật qua ghi chú hợp đồng.
- [MỞ] 2026-11-13 – Quy trình kiểm duyệt Tòa nhà: Chủ trọ chỉ gửi yêu cầu phê duyệt (`POST /buildings` -> `status = pending`), quyền phê duyệt hoặc từ chối thuộc về Admin. Khi bị từ chối (`rejected`), app hiển thị trực quan hộp cảnh báo kèm lý do (`rejectionReason`/`reviewNote`) để chủ trọ chỉnh sửa nộp lại.
- [ĐÃ XỬ LÝ] 2026-11-13 – Chuyển đổi toàn bộ trường số tiền trong phân hệ Hóa đơn sang `String` kèm getter `Long`, xử lý triệt để nguy cơ crash Moshi JSON khi server trả số thực hoặc chuỗi số lớn.
- [ĐÃ XỬ LÝ] 2026-11-13 – Nâng cấp tính năng Trả lời tin nhắn (Chat Reply): Bổ sung cử chỉ nhấn giữ (Long press) trên tin nhắn văn bản và ảnh, thanh xem trước trích dẫn phía trên ô nhập liệu và khung quote trong bong bóng chat.
- [MỞ] 2026-11-05 – Hiệu năng tải tab "Phòng tôi" (MyRoom): Hiện tại MyRoomViewModel đang gọi tuần tự 2 API (getMyContracts và getMyRoomHub) và bị duplicate call giữa init và onResume(), dẫn đến độ trễ 3–5 giây trên môi trường APIDog Mock Server.
- [ĐÃ XỬ LÝ] 2026-11-05 – Triển khai trọn vẹn luồng gửi tin nhắn ảnh dạng Optimistic UI: hiển thị ảnh tạm thời từ URI local trong khi upload binary lên Storage đám mây, sau đó tự động cập nhật mediaId và sequence từ máy chủ khi hoàn tất.
- [ĐÃ XỬ LÝ] 2026-11-05 – Hoàn thiện màn hình Chi tiết xem trọ (`RoomDetailActivity`) thay thế toàn bộ các toast placeholder tại Explore, SearchResult và FavoriteRooms; hỗ trợ đầy đủ cho cả Guest và Tenant đã đăng nhập.
- [ĐÃ XỬ LÝ] 2026-11-05 – Đồng bộ hóa kiểu dữ liệu Sequence (`Long` -> `String`) trên toàn bộ tầng Network và UI Chat, tránh lỗi parse Moshi JSON khi server trả BigInt string.
- [ĐÃ XỬ LÝ] 2026-11-04 – Ngăn chặn lỗi 403 PROFILE_NOT_VISIBLE bằng cách kiểm tra trạng thái hồ sơ cá nhân người gửi trước khi tạo yêu cầu ghép đôi.
- [ĐÃ XỬ LÝ] 2026-11-04 – Hoàn thiện 9 endpoints phân hệ Ghép bạn ở cùng (Matching) theo đúng API Spec, tích hợp điều hướng mượt mà sang ChatDetailActivity khi chấp nhận lời mời ghép đôi.
- [ĐÃ XỬ LÝ] 2026-11-04 – Hoàn thiện giao diện và tương tác phân hệ Phòng của tôi (My Room) theo đúng đặc tả GET /me/room-hub.
- [ĐÃ XỬ LÝ] 2026-11-04 – Tái cấu trúc :core-data, di dời RoomRepository và DevTweaks về nội bộ :app-tenant.
- [MỞ] 2026-10-21 – Workaround Developer Tweaks (hasRoom): Cung cấp toggle giả lập trạng thái đã thuê / chưa thuê phòng cho Tenant. Cần loại bỏ menu và logic Dev Tweaks này khi kết nối hệ thống Backend production thực tế.
- [MỞ] 2026-10-21 – Workaround resolveImageUrl: Do APIDog Mock Server tự sinh tên miền ảo không tồn tại DNS cho trường thumbnailUrl/photoUrls (ví dụ https://tragic-yak.us/), client đang tạm dùng RoomForYouAdapter.resolveImageUrl để fallback sang ảnh phòng Unsplash. Cần xóa bỏ hàm này khi ghép nối Backend production có link ảnh thật.
- [ĐÃ XỬ LÝ] 2026-10-21 – Chuẩn hóa PaginationDto trong ApiListResponse khớp với cấu trúc pagination (pageNo, pageSize, totalItems, totalPages, hasNextPage) từ API spec Apidog.
- [MỞ] 2026-10-11 – API `/auth/refresh` và `/auth/logout` kỳ vọng Cookie `refreshToken` (HttpOnly); cần tích hợp CookieJar khi chạy thực tế với server production.
- [ĐÃ XỬ LÝ] 2026-10-11 – Chuẩn hóa `role` trong `RegisterViewModel` sang lowercase để tránh lỗi 400 Bad Request từ backend validation.
- [ĐÃ XỬ LÝ] 2026-10-10 – Tùy chỉnh `app:cursorColor` trên `TextInputLayout` để ghi đè màu tint con trỏ mặc định của Material Components.
- [ĐÃ XỬ LÝ] 2026-10-10 – Xử lý phản hồi `204 No Content` trong `BaseRepository` (cho endpoint `/auth/logout` và `/auth/password-resets`) bằng cách trả về `NetworkResult.Success(Unit)`.
- [ĐÃ XỬ LÝ] 2026-10-08 – Thêm `consumer-rules.pro` và `proguard-rules.pro` cho `:core-data` và `:core-network` tránh lỗi merge ProGuard AAR.

## 4. Nhật ký thay đổi lớn
<!-- Chỉ thay đổi đáng kể, kèm link PR -->
- 2026-11-19 – Đồng bộ và nâng cấp phân hệ Chat của `:app-landlord` (tìm kiếm, tạo chat 1:1 & nhóm, trả lời tin nhắn, ảnh đính kèm, quản lý thành viên/đổi tên/rời nhóm và Dropdown chọn Hóa đơn/Hợp đồng khi gửi Bot nhắc nhở) trên nhánh `feature/landlord-chat-sync`.
- 2026-11-19 – Nâng cấp phân hệ Điện nước (chọn kỳ tháng, chỉ số cũ, nhập chi tiết & ảnh công tơ), Hóa đơn (lọc theo tòa nhà/kỳ, cập nhật phí khác) và Sự cố (chủ trọ báo sự cố mới) trên nhánh `feature/landlord-meters-invoices-issues`.
- 2026-11-19 – Nâng cấp phân hệ Hợp đồng Landlord (quy trình 3 bước áp mẫu, sinh PDF nháp, upload bản ký thật, sửa số xe/ghi chú, lọc theo tòa nhà và quyết toán cọc chuyển khoản) trên nhánh `feature/landlord-contracts-upgrade`.
- 2026-11-19 – Triển khai Biểu giá dịch vụ (`/rate-policies`) và Cài đặt thanh toán & nhắc nợ (`/billing-settings`) cho Tòa nhà và Phòng trên nhánh `feature/landlord-rate-billing-settings`.
- 2026-11-19 – Triển khai màn hình Chi tiết Phòng (`LandlordRoomDetailActivity`), BottomSheet sửa tòa nhà/phòng, tải ảnh minh chứng sở hữu & ảnh phòng, bộ lọc tìm kiếm Tòa nhà/Phòng theo tầng & trạng thái trên nhánh `feature/landlord-property-room-detail`.
- 2026-11-18 – Form hồ sơ ghép bạn (`EditRoommateProfileActivity`) chuyển địa chỉ sang dropdown Tỉnh/Phường, ngày sang hộp chọn ngày, nút Lưu chỉ bật khi đủ thông tin cần thiết.
- 2026-11-18 – Tab Phòng của Tenant chuyển sang đọc `/me/room-hub` dạng danh sách qua `MyRoomHubLoader`; chủ trọ có thể null, các mục thiếu dữ liệu tự ẩn.
- 2026-11-13 – Tích hợp API Đơn vị Hành chính thực tế (Tỉnh/Thành & Phường/Xã) vào bộ lọc tìm kiếm phòng trọ Tenant, đưa AdministrativeUnitsRepository lên :core-data kèm Fallback snapshot và chuẩn hóa bố cục chống tràn chữ trên nhánh feature/tenant-administrative-units (commits 1d9314c, a8d9942, f99b186).
- 2026-11-13 – Chuẩn hóa bề mặt thẻ, viền mảnh `#E2E8F0`, hiệu ứng ripple foreground và thang bo góc radius trên toàn bộ 44 layout `:app-tenant` và `:app-landlord` trên nhánh `feature/ui-card-surface-polish` (commit `716eeaa`).
- 2026-11-13 – Nâng cấp toàn diện Empty/Error states với huy hiệu tròn pastel 72x72dp & icon Lucide, chuẩn hóa độ tương phản văn bản phụ đạt chuẩn WCAG AA trên `:core-ui`, `:app-tenant` và `:app-landlord` trên nhánh `feature/ui-empty-states-typography` (commit `16b3b52`).
- 2026-11-13 – Nâng cấp toàn diện 50 icon vector sang Lucide Icons chính thức trên nhánh `feature/ui-lucide-icons` (commit `d5a0225`), cả 2 ứng dụng `:app-tenant` và `:app-landlord` đều biên dịch thành công.
- 2026-11-13 – Chuẩn hóa toàn diện 102 file layout XML theo thang đo Spacing bội số 4dp (dimens.xml) trên nhánh `feature/ui-spacing-dimens` (commits `ab5e76d`, `f283923`, `14a3ef2`), cả 2 ứng dụng `:app-tenant` và `:app-landlord` đều biên dịch thành công.
- 2026-11-13 – Hoàn thành triển khai Giai đoạn 2 cho `:app-landlord` trên nhánh `feature/landlord-contracts` (commit `765847b`), biên dịch và đóng gói thành công APK debug.
- 2026-11-13 – Hoàn thành triển khai Giai đoạn 1 cho `:app-landlord` trên nhánh `feature/landlord-properties` gồm 5 commit chuẩn Conventional Commits, biên dịch thành công toàn bộ APK debug.
- 2026-11-13 – Đồng bộ mã nguồn theo API Spec Rentify mới nhất (PR #50 - PR #56): Thay thế endpoint `/invoices/tenant` bằng `/me/invoices`, hỗ trợ `breakdown` dịch vụ hóa đơn, bổ sung luồng email verification, signed download URL cho media, và tính năng trả lời tin nhắn (Message Reply Quotes) trong phân hệ Chat.
- 2026-11-05 – Tái cấu trúc toàn diện giao diện Tab "Phòng của tôi" (MyRoom Hub) trên Tenant theo mô hình 3 Smart Widgets: gộp Nơi ở & Chủ trọ, Hóa đơn tài chính và List Group Vận hành & Cộng đồng.
- 2026-11-05 – Nâng cấp Avatar phân hệ Chat: Tích hợp tải ảnh đại diện bo tròn bằng Coil, cơ chế fallback ảnh chân dung mẫu chất lượng cao, icon nhóm đặc trưng cho group chat và bảng màu Gradient chữ cái đầu sinh động.
- 2026-11-05 – Triển khai gói nâng cấp toàn diện phân hệ Chat Tenant: Hỗ trợ tin nhắn đa phương tiện (Ảnh JPEG & Photo Viewer), Quản lý nhóm trò chuyện cư dân (Tạo nhóm, sửa tên, thêm/xóa thành viên, phân quyền Admin, rời nhóm) và Smart Polling đồng bộ tin nhắn thời gian thực.
- 2026-11-05 – Hoàn thành phân hệ Quản lý Hóa đơn & Lịch sử Thanh toán (Invoice & Payment Management) trên Tenant, tích hợp chi tiết bóc tách dịch vụ và thanh toán nhanh VietQR.
- 2026-11-05 – Tái cấu trúc UX My Room Hub hỗ trợ đa phòng thuê & lịch sử hợp đồng 2 tab ("Đang hiệu lực" & "Đã kết thúc"), dọn dẹp mục hợp đồng khỏi Profile Tenant.
- 2026-11-05 – Hoàn thành phân hệ Quản lý Hợp đồng & Lịch sử Thuê (Contract Management & Rental History) trên Tenant, tích hợp entry points từ Profile và MyRoom Hub.
- 2026-11-05 – Hoàn thành phân hệ Quản lý Sự cố & Hư hỏng (Issue Reporting & Management) trên Tenant, kết nối nút "Báo sự cố" trên MyRoom.
- 2026-11-05 – Triển khai tính năng Chi tiết xem trọ (Room Detail): Xây dựng RoomDetailActivity với ViewPager2 slider ảnh, thông số căn phòng, danh mục tiện ích, thẻ liên hệ chủ trọ (gọi điện & chat trực tiếp), và tích hợp luồng mở từ ExploreFragment, SearchResultActivity, FavoriteRoomsActivity.
- 2026-11-05 – Đồng bộ hóa API Chat & Users theo PR #49, PR #51 từ api-doc: Chuỗi hóa sequence, bổ sung snapshot viewer displayName, thêm endpoint GET /conversations/unread-summary và GET /users/directory.
- 2026-11-04 – Nâng cấp UX phân hệ Ghép bạn: Tích hợp Banner trạng thái hồ sơ (Ẩn / Chưa tạo / Công khai), nút bật nhanh 1 chạm và Dialog bảo vệ luồng gửi yêu cầu kết nối.
- 2026-11-04 – Triển khai toàn diện phân hệ Ghép bạn ở cùng cho Tenant (MatchingFragment gồm 2 tab Khám phá & Lời mời ghép đôi, RoommateDetailActivity, EditRoommateProfileActivity, TenantMatchingRepository, MatchingApiService).
- 2026-11-04 – Triển khai toàn diện phân hệ Phòng của tôi cho Tenant (MyRoomFragment, MyRoomViewModel, VietQrBottomSheetDialog, RoommateAdapter, xử lý 3 trạng thái Guest / No Room / Active Room Hub với 6 thẻ chức năng).
- 2026-11-04 – Triển khai toàn diện tính năng Chat Tenant (ChatApiService, TenantChatRepository, ChatFragment danh sách cuộc trò chuyện, ChatDetailActivity khung chat chi tiết với Cursor pagination, Optimistic UI, MessageAdapter đa viewholder và đánh dấu đã đọc).
- 2026-11-04 – Tái cấu trúc kiến trúc Hướng B: Chuẩn hóa :core-data thành thư viện 100% dùng chung; chuyển RoomRepository sang TenantRoomRepository và DevTweaks sang TenantDevPreferences trong :app-tenant.
- 2026-10-21 – Triển khai màn hình Phòng đã lưu (FavoriteRoomsActivity, FavoriteRoomsViewModel, FavoriteRoomsAdapter, item_favorite_room, layoutEmptyState, phân trang cuộn vô tận và hủy lưu phòng).
- 2026-10-21 – Triển khai hoàn thiện module Profile cho Tenant (UserCurrentUpdateRequestDto, UserChangePasswordRequestDto, UserApiService, UserRepository, ProfileViewModel, ProfileFragment, BottomSheetEditProfile, BottomSheetChangePassword).
- 2026-10-21 – Hoàn thiện giao diện và tầng dữ liệu Trang chủ Tenant (ExploreFragment, RoomPublicListResponseDto, MyRoomHubResponseDto, ExploreViewModel, 2 trạng thái Khách & Đăng nhập).
- 2026-10-21 – Triển khai Auto-login check và cơ chế ghi nhận Onboarding chỉ mở 1 lần qua DataStore.
- 2026-10-11 – Hoàn thiện trọn vẹn UI/UX và logic 6 màn hình thuộc luồng xác thực Auth (Login, Register, Forgot Password, Verify OTP, Reset Password).
- 2026-10-10 – Hoàn thiện UI Auth (`LoginFragment`, `RegisterFragment`, `LoginViewModel`, `RegisterViewModel` & layouts).
- 2026-10-10 – Xây dựng Auth Data Layer (Auth DTOs, AuthApiService, AuthRepository & AuthRepositoryImpl).
- 2026-10-08 – Khởi tạo module `:core-data` (Jetpack DataStore Preferences, SessionManager, BaseRepository).
- 2026-10-08 – Dựng tầng network layer (Retrofit, Moshi, OkHttp Interceptors).

## 5. Danh mục cần dọn dẹp & hoàn thiện sau này (Technical Debt & Cleanup Backlog)
<!-- [CHỜ DỌN DẸP] / [ĐÃ DỌN DẸP] - Ghi rõ vị trí file, nguyên nhân và điều kiện kích hoạt dọn dẹp -->
1. **[CHỜ DỌN DẸP] Developer Tweaks (`hasRoom`)**:
   - **Vị trí:** `mobile/app-tenant/` (`DevTweaksDialogFragment.kt`, `dialog_dev_tweaks.xml`, icon `btnDevTweaks` trên Toolbar Explore, `btnGuestDevTweaks` & `btnDevTweaksRow` trong `ProfileFragment.kt`, logic quan sát `devTweakHasRoom` trong `ExploreViewModel.kt`, `ProfileViewModel.kt` & `MyRoomViewModel.kt`, `TenantDevPreferences.kt`).
   - **Nguyên nhân:** Mock công cụ test nhanh để chuyển đổi trạng thái giao diện Trang chủ Tenant ("chưa có phòng thuê" và "đã có phòng thuê").
   - **Điều kiện dọn dẹp:** Khi hệ thống kết nối Backend thật và luồng kiểm tra hợp đồng thuê phòng thực tế (`/me/room-hub`) hoạt động. Xóa bỏ dialog, layout, preferences và các nút bấm dev khỏi giao diện người dùng.

2. **[CHỜ DỌN DẸP] Fallback ảnh Unsplash (`resolveImageUrl`)**:
   - **Vị trí:** `mobile/app-tenant/src/main/java/com/rentify/app/tenant/ui/explore/RoomForYouAdapter.kt` và `mobile/app-tenant/src/main/java/com/rentify/app/tenant/ui/favoriteroom/FavoriteRoomsAdapter.kt`.
   - **Nguyên nhân:** Mock server APIDog tự động sinh chuỗi link ảnh giả lập có tên miền không tồn tại DNS (ví dụ: `https://tragic-yak.us/`), khiến thư viện Coil bị lỗi không tải được ảnh.
   - **Điều kiện dọn dẹp:** Khi Backend thật đưa vào hoạt động và trả link ảnh hợp lệ từ Cloud Storage / S3 / CDN. Xóa bỏ hàm `resolveImageUrl` và load trực tiếp URL từ DTO. Gateway thật (`http://3.26.114.197/api/v1/`) đã có route Media nhưng chưa phòng nào có ảnh nên chưa xác nhận được định dạng URL; chỉ dọn khi có phòng ảnh thật và đã kiểm tra URL tuyệt đối/tương đối, có cần token hay không.

3. **[ĐÃ DỌN DẸP] Cấu hình CookieJar cho Refresh Token & Logout**:
   - **Vị trí:** `mobile/core-network/src/main/java/com/rentify/app/core/network/client/NetworkClient.kt` và `interceptor/AuthInterceptor.kt`.
   - **Kết quả (2026-11-18):** Không cần `CookieJar`. Spec hiện hành dùng `refreshToken` trong body JSON cho `/auth/refresh` và `/auth/logout`; app đã gửi đúng body và có `TokenAuthenticator` tự làm mới khi 401. Mô tả cũ (Cookie HttpOnly) không còn đúng.

4. **[CHỜ DỌN DẸP] Cấu hình Base URL động môi trường**:
   - **Vị trí:** `mobile/core-network/src/main/java/com/rentify/app/core/network/client/NetworkConfig.kt`.
   - **Nguyên nhân:** Hiện tại `DEFAULT_BASE_URL` đang gắn cứng gateway thật `http://3.26.114.197/api/v1/` (IP dev, giao thức HTTP, không mã hóa).
   - **Điều kiện dọn dẹp:** Đưa cấu hình `BASE_URL` vào `local.properties` và xuất qua `BuildConfig` để dễ dàng đổi giữa Mock, Local BE (10.0.2.2), Staging và Production; khi có domain HTTPS thì chuyển sang HTTPS và tắt `usesCleartextTraffic` ở manifest.

5. **[CHỜ DỌN DẸP] Các nút chức năng đang Toast placeholder trên Profile Tenant**:
   - **Vị trí:** `mobile/app-tenant/src/main/java/com/rentify/app/tenant/ui/profile/ProfileFragment.kt` (`btnGuestTerms`, `btnTermsRow`, `btnNotifications`).
   - **Nguyên nhân:** Giao diện đã có nút bấm nhưng chưa có màn hình chi tiết Điều khoản & Chính sách và Cài đặt thông báo.
   - **Điều kiện dọn dẹp:** Thay thế Toast bằng Navigation Action mở màn hình Activity/Fragment tương ứng khi các màn hình này được thiết kế và triển khai.

6. **[ĐÃ DỌN DẸP] Nút "Báo sự cố" trên màn hình My Room Tenant**:
   - **Vị trí:** `mobile/app-tenant/src/main/java/com/rentify/app/tenant/ui/myroom/MyRoomFragment.kt` (`btnReportIssue`, `cardIssues`).
   - **Kết quả:** Đã kết nối với `CreateIssueActivity` và `MyIssuesActivity` thuộc phân hệ Quản lý Sự cố & Hư hỏng, loại bỏ hoàn toàn Toast tạm thời.

7. **[CHỜ DỌN DẸP] Tải ảnh đính kèm khi Báo cáo sự cố (`issuePhotoMediaIds`)**:
   - **Vị trí:** `mobile/app-tenant/src/main/java/com/rentify/app/tenant/ui/issue/create/CreateIssueViewModel.kt` (`issuePhotoMediaIds = emptyList()`), `activity_create_issue.xml` và `IssueDetailActivity.kt`.
   - **Nguyên nhân:** Backend hiện tại chưa hỗ trợ presigned upload cho mục đích `issue_photo` (API Spec `POST /media/presign-upload` ghi rõ `purpose: issue_photo` trả về lỗi 503 `SERVICE_UNAVAILABLE` do storage cho `issue_report` chưa sẵn sàng). Client tạm thời gửi mảng rỗng `emptyList()` để luồng báo cáo sự cố không bị gián đoạn.
   - **Điều kiện dọn dẹp:** Khi Backend kích hoạt endpoint upload media cho `issue_report`, tiến hành bổ sung giao diện chọn ảnh (Android Photo Picker), RecyclerView xem trước thumbnail ảnh và tích hợp trọn vẹn luồng upload presigned URL (`/media/presign-upload` -> PUT Cloudflare R2 -> `/media/{id}/complete-upload`) để gửi danh sách `mediaId` vào `POST /issues`.

8. **[ĐÃ DỌN DẸP] Tích hợp Socket.IO v4 cho Realtime Chat (thay thế Smart Polling)**:
   - **Vị trí:** `mobile/core-network/src/main/java/com/rentify/app/core/network/socket/CommunitySocketClient.kt`, `ChatSocketEvents.kt`, `ChatViewModel.kt`, `ChatDetailViewModel.kt`, `ChatFragment.kt`, `ChatDetailActivity.kt`, `LandlordChatViewModel.kt`, `LandlordChatDetailViewModel.kt`.
   - **Kết quả:** Đã loại bỏ hoàn toàn vòng lặp polling giả lập (3.5s và 12s) và kết nối trực tiếp tới `/api/v1/community/socket.io` theo chuẩn Engine.IO v4 / Socket.IO v4 WebSocket (`docs/community-socket-v1.md`).

9. **[CHỜ DỌN DẸP] UI tab "Phòng" mới chưa nối logic (danh sách phòng, dòng chân thẻ hóa đơn)**:
   - **Vị trí:** `mobile/app-tenant/` (`MyRoomsAdapter.kt`, `item_my_room.xml`, `MyRoomFragment.kt`, `fragment_my_room.xml`).
   - **Nguyên nhân:** Mới làm giao diện. `MyRoomFooter` có đủ 6 biến thể nhưng chỉ dùng `Ended`; thẻ "Đang thuê" chưa có dòng chân thẻ do `GET /me/invoices` trong spec thiếu trạng thái thanh toán, `dueDate`, số ngày quá hạn (chưa có trạng thái "chờ chủ trọ xác nhận"). Ảnh phòng chỉ có nền xám, dòng phường ẩn vì `GET /me/contracts` không trả ảnh/phường. Chưa có skeleton khi tải, chưa phân loại `draft`/`signed`/`suspended`, chưa xử lý luồng "đúng 1 phòng đang thuê" và nút mở danh sách từ màn chi tiết. `MyContractsActivity` cũ chưa xóa.
   - **Điều kiện dọn dẹp:** Khi chốt logic và BE bổ sung dữ liệu ở trên, nối `MyRoomFooter` với dữ liệu hóa đơn kỳ hiện tại, bổ sung ảnh/phường, thêm skeleton, rồi xóa `MyContractsActivity` cùng adapter, ViewModel, layout liên quan.

9. **[ĐÃ DỌN DẸP] Mock Avatar URL cho Đoạn Chat & Chi tiết Hội thoại**:
   - **Vị trí:** `mobile/app-tenant/src/main/java/com/rentify/app/tenant/ui/chat/util/ChatAvatarHelper.kt`, `ConversationAdapter.kt`, `ChatDetailActivity.kt`.
   - **Kết quả:** Đã xóa bỏ hoàn toàn danh sách `MOCK_USER_AVATARS`, `MOCK_GROUP_AVATARS` và `GRADIENTS`. Tải trực tiếp `avatarUrl` từ API; khi không có ảnh hoặc đang tải/lỗi thì hiển thị chữ cái đầu của tên theo 4 cặp màu cố định (hội thoại 1-1) hoặc icon nhóm 24dp màu `rentify_teal` trên nền `rentify_teal_surface` (hội thoại nhóm).

10. **[CHỜ DỌN DẸP] Mock Chat Media Image Fallback cho Tin nhắn Hình ảnh (MessageAdapter)**:
    - **Vị trí:** `mobile/app-tenant/src/main/java/com/rentify/app/tenant/ui/chat/detail/MessageAdapter.kt` (`MOCK_CHAT_PHOTOS`, `resolveImageUrl`), `PhotoViewerDialogFragment.kt`.
    - **Nguyên nhân:** Mock server APIDog trả về `mediaId` giả lập ngẫu nhiên nhưng endpoint `/media/{id}` trên mock không có file nhị phân thật, dẫn đến việc Coil tải ảnh bị HTTP 404 và rơi vào icon placeholder ngọn núi xám. Client tạm thời cấu hình fallback sang bộ sưu tập 6 ảnh phòng trọ/nội thất Unsplash chất lượng cao theo mã hash của `mediaId` hoặc message `id`.
    - **Điều kiện dọn dẹp:** Khi Backend Production đưa dịch vụ R2 Storage vào hoạt động và phục vụ file nhị phân qua `/media/{id}`, xóa bỏ `MOCK_CHAT_PHOTOS` và chuyển `resolveImageUrl` về trả link `${NetworkConfig.DEFAULT_BASE_URL}media/${item.mediaId}` nguyên bản.

11. **[CHỜ DỌN DẸP] Tối ưu hóa hiệu năng & tốc độ phản hồi tab Phòng tôi (My Room Hub)**:
    - **Vị trí:** `mobile/app-tenant/src/main/java/com/rentify/app/tenant/ui/myroom/MyRoomViewModel.kt`, `MyRoomFragment.kt`.
    - **Nguyên nhân:** Đang gọi tuần tự 2 API (`getMyContracts` rồi mới đến `getMyRoomHub`), bị kích hoạt gọi trùng lặp giữa `init` và `onResume()`, và luôn reset về `MyRoomUiState.Loading` khiến người dùng phải chờ xoay vòng mỗi khi chuyển tab.
    - **Kế hoạch cập nhật tương lai:** Chuyển sang gọi song song 2 API bằng Coroutine `async`/`await`, áp dụng cơ chế Instant Display / Stale-While-Revalidate (giữ dữ liệu cũ hiển thị ngay lập tức, chỉ cập nhật ngầm), khử trùng lặp request giữa vòng đời Fragment và ViewModel, tối ưu fallback ảnh bìa phòng Unsplash.

12. **[CHỜ DỌN DẸP] Fallback Local Snapshot cho Danh mục Đơn vị Hành chính (`AdministrativeUnitsRepositoryImpl`)**:
    - **Vị trí:** `mobile/core-data/src/main/java/com/rentify/app/core/data/repository/administrative/AdministrativeUnitsRepositoryImpl.kt` (`FALLBACK_PROVINCES`, `FALLBACK_WARDS`).
    - **Nguyên nhân:** Mock server APIDog hiện tại chưa cấu hình mock 2 endpoint `/administrative-units/provinces` và `/administrative-units/provinces/{provinceCode}/wards` (trả về lỗi HTTP 404 Not Found), khiến bộ lọc tìm kiếm phòng trọ và màn hình tạo tòa nhà không tải được danh sách Phường/Xã. Client cấu hình fallback tự động sang snapshot danh mục chuẩn gồm 12 Tỉnh/Thành phố lớn và đầy đủ Phường/Xã tương ứng để phục vụ kiểm thử và đảm bảo trải nghiệm người dùng không bị gián đoạn.
    - **Điều kiện dọn dẹp:** Khi APIDog Cloud Mock hoặc Backend Production kích hoạt 2 endpoint này theo đúng spec `rentify.json`, client sẽ tự động nhận dữ liệu từ server mà không kích hoạt nhánh fallback; sau đó có thể loại bỏ `FALLBACK_PROVINCES` và `FALLBACK_WARDS` khỏi Repository.

13. **[CHỜ DỌN DẸP] Bổ sung địa chỉ hành chính chi tiết (`address`, `ward`, `district`, `province`) trong `GET /rooms/{roomId}`**:
    - **Tính năng & Màn hình liên quan:** Dòng địa chỉ kèm icon định vị bên dưới tên phòng trên Màn hình Chi tiết phòng (`RoomDetailActivity` / `activity_room_detail.xml` / `RoomDetailDtos.kt`).
    - **Nguyên nhân & Lỗi dữ liệu phát hiện được:** Trong API Spec `GET /rooms/{roomId}`, backend mới chỉ trả về `buildingName` (và trên APIDog Mock Server trường `buildingName` bị sinh nhầm thành tên người như `"Nguyễn Văn..."`), không trả về các trường địa chỉ (`address`, `ward`, `district`, `province`) như `GET /rooms/public`.
    - **Trạng thái hiện tại trên UI:** Đã dừng lấy `buildingName` làm địa chỉ; `RoomDetailActivity` ưu tiên đọc `address, ward, district, province` từ `RoomDetailResponseDto` và nhận thêm `fallbackAddress` truyền sang từ `RoomPublicListResponseDto` (`ExploreFragment` / `SearchResultActivity`), hoặc hiển thị `"Chưa cập nhật địa chỉ"` nếu cả hai đều trống. Khi Backend bổ sung `address, ward, district, province` vào `GET /rooms/{roomId}`, có thể bỏ `fallbackAddress`.

14. **[ĐÃ DỌN DẸP] Đồng bộ Endpoint Trung tâm Phòng của tôi (`GET /me/room-hub`) theo API Spec PR #70** (2026-11-18: server đã trả mảng, app đã chuyển sang `MyRoomHubItemDto` và `MyRoomHubLoader`; phần dữ liệu còn thiếu xem mục 27, 28):
    - **Vị trí:** `mobile/core-network/src/main/java/com/rentify/app/core/network/api/RoomApiService.kt` (`getMyRoomHub`), `MyRoomHubResponseDto.kt`, `TenantRoomRepository.kt`, và `ExploreViewModel.kt`.
    - **Nguyên nhân:** Backend vừa cập nhật hợp đồng API (PR #70) cho `GET /me/room-hub`: chuyển đổi từ phản hồi đơn lẻ `ApiSingleResponse<MyRoomHubResponseDto>` sang danh sách có phân trang `ApiListResponse<MyRoomHubResponseDto>` (`pageNo`, `pageSize`, `pagination`) và tinh giản các trường dữ liệu trực tiếp (`contractId`, `roomId`, `roomName`, `area`, `buildingId`, `buildingName`, `address`, `ward`, `province`, `rentPrice`, `startDate`, `endDate`). Hiện tại backend/mock server chưa chắc chắn đã deploy phiên bản Tenancy Service mới này, do đó client tạm thời giữ nguyên cấu trúc DTO hiện có để tránh lỗi kết nối môi trường test.
    - **Điều kiện dọn dẹp:** Khi Backend thông báo đã triển khai hoàn tất Tenancy Service PR #56 / PR #70 trên môi trường Staging/Production, tiến hành cập nhật `RoomApiService.kt`, tái cấu trúc `MyRoomHubResponseDto.kt` sang dạng list kèm phân trang, và cập nhật `ExploreViewModel` để nạp danh sách phòng đang thuê của tenant.

15. **[CHỜ DỌN DẸP] Chấm trạng thái trực tuyến (Online Presence) cho Hội thoại 1-1 trên Tab Tin nhắn**:
    - **Tính năng & Màn hình liên quan:** Chấm xanh trạng thái đang hoạt động (`viewOnlineDot`, 12dp góc dưới phải avatar) trên Tab Tin nhắn (`ChatFragment` / `item_conversation.xml` / `ConversationAdapter.kt`).
    - **Dữ liệu cần Backend/API bổ sung:** Trường `isOnline: Boolean` (hoặc `lastSeenAt: String?`) của người đối diện trong `ConversationListResponseDto` (`GET /conversations`), kèm sự kiện presence thời gian thực qua Socket.IO (`user:presence` hoặc `user:online` / `user:offline` trong `docs/community-socket-v1.md`).
    - **Trạng thái hiện tại trên UI:** Đang ẩn chấm xanh (`viewOnlineDot` đã đặt sẵn trong `item_conversation.xml` ở trạng thái `android:visibility="gone"`, không dùng dữ liệu giả).

16. **[CHỜ DỌN DẸP] Nhãn "Chủ trọ" trên Item Hội thoại và Chip lọc "Chủ trọ" trên Tab Tin nhắn**:
    - **Tính năng & Màn hình liên quan:** Chip lọc `"Chủ trọ"` (`chipFilterLandlord` trong `fragment_chat.xml`) và nhãn `"Chủ trọ"` (`tvLandlordBadge` cạnh tên người nhận trong `item_conversation.xml`) trên Tab Tin nhắn (`ChatFragment` / `ConversationAdapter.kt`).
    - **Dữ liệu cần Backend/API bổ sung:** Trường vai trò của người đối diện trong hội thoại 1-1 (ví dụ `otherUserRole: "landlord" | "tenant"` hoặc `isLandlord: Boolean`) trong `ConversationListResponseDto` của `GET /conversations`.
    - **Trạng thái hiện tại trên UI:** Đang ẩn cả chip lọc `"Chủ trọ"` (`chipFilterLandlord` để sẵn ở trạng thái `android:visibility="gone"`) lẫn nhãn `"Chủ trọ"` trên từng dòng hội thoại (`tvLandlordBadge` để sẵn ở trạng thái `android:visibility="gone"`).

17. **[CHỜ DỌN DẸP] Định danh `otherUserId` và `lastMessage.senderUserId` trong `ConversationListResponseDto` trên Tab Tin nhắn**:
    - **Tính năng & Màn hình liên quan:** Hash màu avatar chữ cái cố định theo `userId` cho hội thoại 1-1 (`RentifyAvatarView.kt`) và hiển thị tiền tố `"Bạn:"` cho tin nhắn cuối của chính mình (`ConversationAdapter.kt`) trên Tab Tin nhắn (`ChatFragment`).
    - **Dữ liệu cần Backend/API bổ sung:** Bổ sung `otherUserId: String?` vào `ConversationListResponseDto` và `senderUserId: String?` vào `ConversationLastMessageDto` trong phản hồi REST `GET /conversations` (hiện tại `senderUserId` mới chỉ có trong sự kiện socket `chat:message-received`, còn REST `lastMessage` chỉ trả `senderName`).
    - **Trạng thái hiện tại trên UI:** Khi `otherUserId` chưa có từ REST, hash màu avatar 1-1 đang fallback ổn định theo `conversation.id`; tiền tố `"Bạn:"` kết hợp kiểm tra `lastMessage.senderUserId == currentUserId` (khi nhận qua socket) và đối chiếu `senderName != displayName` đối với hội thoại 1-1.

18. **[CHỜ DỌN DẸP] Màn hình "Thông tin hội thoại" cho Chat Cá nhân (1-1)**:
    - **Tính năng & Màn hình liên quan:** Màn hình/Sheet "Thông tin hội thoại" cho cuộc trò chuyện 1-1, mở từ Header của `ChatDetailActivity` (`activity_chat_detail.xml` / `ChatDetailActivity.kt`).
    - **Dữ liệu & Thiết kế cần có:** Thiết kế chi tiết màn hình "Thông tin hội thoại" cho chat cá nhân (xem hồ sơ người đối diện, ảnh chung, tìm kiếm tin nhắn).
    - **Trạng thái hiện tại trên UI:** Khi ở chế độ Chat cá nhân (`isGroupChat == false`), vùng avatar và tên trên Header đang để không bấm được (`isClickable = false`, ẩn mũi tên chevron phải); sẽ bật lại thao tác bấm khi màn hình "Thông tin hội thoại" cá nhân được triển khai.

19. **[CHỜ DỌN DẸP] Thu hồi tin nhắn (Message Recall) trên Màn hình Chat**:
    - **Tính năng & Màn hình liên quan:** Hiển thị và thao tác thu hồi tin nhắn trong `ChatDetailActivity` (`MessageAdapter.kt`, `item_message_me.xml`, `item_message_other.xml`).
    - **Dữ liệu cần Backend/API bổ sung:** Endpoint thu hồi tin nhắn (`DELETE /messages/{id}` hoặc `POST /messages/{id}/recall`), trường `isRecalled: Boolean` / `recalledAt: String?` (hoặc `type = "recalled"`) trong `MessageListResponseDto`, và sự kiện Socket.IO `message:recalled`.
    - **Trạng thái hiện tại trên UI:** `MessageAdapter` và drawable `bg_message_recalled.xml` đã dựng sẵn giao diện bong bóng nền trắng, viền nét đứt 1dp `rentify_border`, chữ nghiêng `rentify_text_faint` `"Tin nhắn đã được thu hồi"`, đồng thời khối trích dẫn trả lời (`layoutReplyQuote`) đã hiển thị `"Tin nhắn đã được thu hồi"` ở dòng 2 khi `replyTo.isUnavailable == true`.

20. **[CHỜ DỌN DẸP] Danh sách thành viên rút gọn trong `ConversationListResponseDto` (`GET /conversations`) để ghép 2 Avatar Nhóm**:
    - **Tính năng & Màn hình liên quan:** Ghép 2 avatar thành viên chồng chéo (65% đường kính) trên `RentifyAvatarView` cho nhóm chưa có ảnh tại màn Danh sách Tin nhắn (`ChatFragment` / `ConversationAdapter.kt`).
    - **Dữ liệu cần Backend/API bổ sung:** Bổ sung mảng `memberPreviews` (hoặc 2 thành viên khác người dùng ưu tiên người gửi gần nhất gồm `userId`, `displayName`, `avatarUrl`) vào `ConversationListResponseDto` của `GET /conversations`.
    - **Trạng thái hiện tại trên UI:** Tại Header màn Chat chi tiết (`ChatDetailActivity`) và Sheet Thông tin nhóm (`GroupInfoBottomSheetDialog`), `RentifyAvatarView` đã ghép 2 avatar thành viên chồng chéo đầy đủ nhờ `members[]` từ `GET /conversations/{id}`; riêng tại màn Danh sách Tin nhắn (`ChatFragment`), do `GET /conversations` chưa trả `members[]`, nhóm chưa có ảnh hiện hiển thị theo mức ưu tiên 3 (chữ cái đầu của tên nhóm + màu theo hash `groupId` + huy hiệu nhóm).

21. **[CHỜ DỌN DẸP] Ngày đăng phòng (`createdAt` / `postedAt`) trên Màn hình Chi tiết phòng**:
    - **Tính năng & Màn hình liên quan:** Nhãn `"Đăng <x> ngày trước"` (`tvPostedAgo`) ở góc phải hàng trạng thái trên Màn hình Chi tiết phòng (`RoomDetailActivity` / `activity_room_detail.xml`).
    - **Dữ liệu cần Backend/API bổ sung:** Trường `createdAt` hoặc `postedAt` (ISO-8601 date-time) trong `RoomDetailResponseDto` của `GET /rooms/{roomId}`.
    - **Trạng thái hiện tại trên UI:** Đã khai báo `createdAt: String? = null` trong `RoomDetailResponseDto` và xử lý tính số ngày trong `RoomDetailActivity.bindPostedAgo()`; khi API chưa trả về trường này (`null`), `tvPostedAgo` tự động ẩn (`visibility = GONE`).

22. **[CHỜ DỌN DẸP] Đơn giá Điện và Nước (`đ/kWh`, `đ/m³`) trên Màn hình Chi tiết phòng**:
    - **Tính năng & Màn hình liên quan:** Mục `"Điện và nước"` (`tvElectricityPolicy`, `tvWaterPolicy`) trên Màn hình Chi tiết phòng (`RoomDetailActivity` / `activity_room_detail.xml`).
    - **Dữ liệu cần Backend/API bổ sung:** Bổ sung đơn giá cụ thể (`electricityUnitPrice`, `waterUnitPrice` hoặc trả chuỗi số tiền) bên cạnh loại cách tính (`"flat"` / `"tiered"` / `"meter"`) trong `effectiveRatePolicySummary` của `GET /rooms/{roomId}`.
    - **Trạng thái hiện tại trên UI:** Nếu `effectiveRatePolicySummary` trả về giá trị số dương thì hiển thị `"<đơn giá> đ/kWh"` và `"<đơn giá> đ/m³"`; khi API chỉ trả về cách tính như hiện tại thì hiển thị `"Giá cố định"` hoặc `"Theo công tơ"` / `"Theo khối tiêu thụ"`.

23. **[CHỜ DỌN DẸP] Ràng buộc `minimum: 1` cho `maxOccupancy` và `floorNumber` trong Schema `RoomDetailResponseDto`**:
    - **Tính năng & Màn hình liên quan:** Lưới thông số 2x2 (`tvSpecOccupancy`, `tvSpecFloor`, `tvSpecArea`) trên Màn hình Chi tiết phòng (`RoomDetailActivity`).
    - **Nguyên nhân & Lỗi dữ liệu phát hiện được:** Trong `rentify.json`, hai trường `maxOccupancy` và `floorNumber` của `RoomDetailResponseDto` có kiểu `integer` nhưng chưa khai báo `"minimum": 1`, dẫn đến việc APIDog Mock Server tự sinh số nguyên âm ngẫu nhiên (ví dụ `-42158392`).
    - **Trạng thái hiện tại trên UI:** Client đã kiểm tra bảo vệ: với `maxOccupancy`, nếu `null` thì hiển thị `"Không giới hạn"` (đúng quy ước trong spec), nếu `<= 0` thì hiển thị `"Chưa cập nhật"`; với `area` và `floorNumber`, nếu `null` hoặc `<= 0` thì hiển thị `"Chưa cập nhật"`. Cần bổ sung `"minimum": 1` trong spec `rentify.json` để mock server sinh dữ liệu hợp lệ.

24. **[CHỜ DỌN DẸP] `landlord` đang nhận null trong `RoomDetailResponseDto`**:
    - **Vị trí:** `mobile/core-network/.../dto/room/RoomDetailDtos.kt` (`landlord: RoomLandlordDto? = null`), `mobile/app-tenant/.../ui/room/detail/RoomDetailActivity.kt` (ẩn `tvLandlordSectionTitle`/`layoutLandlordCard` khi null), `RoomDetailViewModel.kt` (`contactLandlord`, `callLandlord`), `activity_room_detail.xml` (id `tvLandlordSectionTitle`, `layoutLandlordCard`).
    - **Nguyên nhân:** Server thật trả `landlord: null` cho cả 4 phòng, cả khi đã đăng nhập, trái spec (bắt buộc). Nếu giữ DTO không null thì Moshi báo lỗi cho cả phản hồi và không mở được chi tiết phòng.
    - **Điều kiện dọn dẹp:** Khi BE trả `landlord` đầy đủ cho phòng có chủ, đổi lại `landlord: RoomLandlordDto` (không null), bỏ nhánh null trong Activity và ViewModel, xóa hai id thừa nếu không còn cần.

25. **[CHỜ DỌN DẸP] Nhóm phòng theo `buildingId` ở phía app để hiển thị tòa nhà**:
    - **Vị trí:** `mobile/app-tenant/src/main/java/com/rentify/app/tenant/data/repository/TenantBuildingRepositoryImpl.kt` (nhóm và tóm tắt), `TenantRoomRepositoryImpl.getAllPublicRooms` (tải hết các trang), `BuildingWardResolver.kt`, `ui/explore/ExploreViewModel.kt` (`loadBuildings`), `ui/building/*` (gồm `BuildingListActivity` mở từ nút "Xem tất cả" của mục Tòa nhà nổi bật).
    - **Nguyên nhân:** Backend chưa có API phòng theo tòa và thông tin tóm tắt tòa (ảnh, giá thấp nhất, số phòng trống). Cách tạm tải toàn bộ phòng (tối đa 100 phòng/trang, giới hạn 20 trang) rồi nhóm ở app; sẽ chậm khi số phòng lớn.
    - **Điều kiện dọn dẹp:** Khi BE có `filter[buildingId]` (hoặc `GET /buildings/{id}/rooms`) và các trường tóm tắt tòa nhà, đổi `TenantBuildingRepositoryImpl` sang gọi thẳng API (phân trang), bỏ `getAllPublicRooms`, bỏ vòng tải toàn bộ phòng và lấy ảnh/giá/số phòng từ API.

26. **[CHỜ DỌN DẸP] Tự tra `ward` qua `buildingId` (`BuildingWardResolver`)**:
    - **Vị trí:** `mobile/app-tenant/src/main/java/com/rentify/app/tenant/data/repository/BuildingWardResolver.kt`, nơi gọi trong `TenantRoomRepositoryImpl` và `TenantBuildingRepositoryImpl`.
    - **Nguyên nhân:** `GET /rooms` và `GET /rooms/{id}` chưa trả `ward`/`buildingName`. Hạn chế đã biết: bộ nhớ đệm `static` không bao giờ làm mới (tòa nhà mới hoặc đổi phường chỉ hiện sau khi tắt hẳn app); gọi `/buildings` lỗi thì ward trống mà không báo; lúc đệm nguội phải gọi tới 20 trang trước khi có địa chỉ.
    - **Điều kiện dọn dẹp:** Khi BE trả `ward` và `buildingName` trong `GET /rooms` và `GET /rooms/{id}` (`BACKEND_REQUESTS` A1), ưu tiên dữ liệu từ BE và bỏ bước ghép ward; giữ phần cache tòa nhà nếu mục 25 còn dùng. Trong lúc chờ, nên thêm cơ chế làm mới đệm và báo lỗi rõ khi không tra được ward (chưa làm, cần kế hoạch riêng).

27. **[CHỜ DỌN DẸP] Gọi thêm `/me/invoices` cho từng hợp đồng để lấy hóa đơn hiện tại (tab Phòng)**:
    - **Vị trí:** `mobile/app-tenant/src/main/java/com/rentify/app/tenant/data/repository/MyRoomHubLoader.kt` (`latestInvoiceOf`).
    - **Nguyên nhân:** `GET /me/room-hub` không có hóa đơn hiện tại; mỗi hợp đồng tốn thêm một lần gọi và hóa đơn mới nhất được chọn theo `period` ở phía app.
    - **Điều kiện dọn dẹp:** Khi BE trả hóa đơn hiện tại trong room-hub (`BACKEND_REQUESTS` A4) thì bỏ lần gọi thêm; khi `GET /me/invoices` có đủ trạng thái thanh toán (A5) thì kiểm tra lại cách chọn hóa đơn.

28. **[CHỜ DỌN DẸP] Dữ liệu thiếu trong room-hub: chủ trọ, bạn cùng phòng, số sự cố, `contractCode`, `rentalType`**:
    - **Vị trí:** `MyRoomHubLoader.toHub` (giá trị mặc định rỗng/null), `MyRoomHubResponseDto.kt` (`landlord` nullable), `MyRoomFragment.bindActiveRoomData`, `MyRoomCardBinder`, `MyRoomViewModel.contactLandlord`, `fragment_my_room.xml` (id `tvPeopleTitle`).
    - **Nguyên nhân:** Spec và server không trả các trường này trong room-hub, nên UI ẩn hàng chủ trọ, mục "Mọi người", số sự cố và dùng giá trị rỗng cho mã hợp đồng, loại thuê.
    - **Điều kiện dọn dẹp:** Khi BE xác nhận nguồn dữ liệu (A4), map dữ liệu thật trong `toHub`, bỏ các nhánh ẩn UI, đổi `landlord` về không null nếu BE bảo đảm có.

29. **[CHỜ DỌN DẸP] Tab Phòng mới chỉ hiển thị chi tiết khi đúng một hợp đồng đang hiệu lực, chưa kiểm thử với dữ liệu thật**:
    - **Vị trí:** `MyRoomViewModel.loadRoomHub` (`hubs.singleOrNull()`, `toContractSummary()`), `ExploreViewModel.fetchMyRoomHub`.
    - **Nguyên nhân:** Tài khoản test chưa có hợp đồng nên mới xác minh được trạng thái "Chưa có phòng". Nhánh một phòng, nhiều phòng và hợp đồng đã kết thúc chưa được thử với dữ liệu server thật.
    - **Điều kiện dọn dẹp:** Khi có tài khoản tenant có hợp đồng đang hiệu lực (một và nhiều), kiểm thử cả ba nhánh trên máy thật và sửa lỗi nếu có.

30. **[CHỜ DỌN DẸP] Tra tên và ảnh người gửi/nhận lời mời ghép đôi từ danh sách hồ sơ (`RoommateProfileResolver`)**:
    - **Vị trí:** `mobile/app-tenant/.../data/repository/RoommateProfileResolver.kt`, `MatchRequestItem.kt`, `ui/matching/MatchingViewModel.loadRequests` và `MatchRequestAdapter`.
    - **Nguyên nhân:** `GET /match-requests` chỉ trả `requesterId`, `targetId` (không có `requester`, `target`). App tải các trang `GET /roommate-profiles` (100/trang, tối đa 10 trang, tải lại tối thiểu cách nhau 15 giây) để ghép `tenantId` thành tên và ảnh. Người ẩn hoặc xóa hồ sơ sẽ hiện "Người dùng"; lời mời không còn `message` và `matchScore`.
    - **Điều kiện dọn dẹp:** Khi BE trả `requester`/`target` rút gọn (hoặc cho tra user công khai theo ID), bỏ `RoommateProfileResolver` và lấy thông tin trực tiếp từ response (`BACKEND_REQUESTS` mục C1).
