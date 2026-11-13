# Rentify – Project Log

## 1. Trạng thái hiện tại
<!-- Cập nhật đè, luôn phản ánh hiện tại -->
| Module | Trạng thái | Người phụ trách | Ghi chú |
|---|---|---|---|
| Core Network | Hoàn thành | AI & Dev | Đã dựng Retrofit, OkHttp, Moshi DTO wrappers, Auth DTOs (Email Verification), Room DTOs, User DTOs (avatarUrl), Chat DTOs v2 (Reply quotes, maxSequence, Search), Matching DTOs, Issue DTOs, Contract DTOs, Invoice DTOs (/me/invoices, String amounts & breakdown), Media DTOs (Download URL) & Chat Group DTOs |
| Core Data | Hoàn thành chuẩn hóa (Hướng B) | AI & Dev | Đảm bảo 100% dùng chung: TokenDataStore, AppDataStore (Onboarding), SessionManager, BaseRepository, AuthRepository, UserRepository |
| Core UI | Hoàn thành | AI & Dev | Hoàn thiện toàn bộ luồng Auth, base components, vector icons & dialogs dùng chung |
| App Tenant | Hoàn thành | AI & Dev | Hoàn thiện Explore, Profile, FavoriteRooms, MyRoom Hub, Ghép bạn ở cùng, Chi tiết xem trọ, Quản lý Sự cố, Quản lý Hợp đồng, Quản lý Hóa đơn (/me/invoices), và Nâng cấp Chat v2 (Gửi ảnh R2, Photo Viewer, Quản lý nhóm, Nhấn giữ để trả lời tin nhắn - Reply quote) |
| App Landlord | Hoàn thành Giai đoạn 1 | AI & Dev | Đã hoàn thành Nền tảng Điều hướng 5 Tab, Dashboard KPI, Quản lý Tòa nhà & Phòng trọ, Tích hợp Đơn vị Hành chính & Cấu hình VietQR |

## 2. Quyết định
<!-- Mới nhất ở trên. Mỗi mục 2–4 dòng -->
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
   - **Điều kiện dọn dẹp:** Khi Backend thật đưa vào hoạt động và trả link ảnh hợp lệ từ Cloud Storage / S3 / CDN. Xóa bỏ hàm `resolveImageUrl` và load trực tiếp URL từ DTO.

3. **[CHỜ DỌN DẸP] Cấu hình CookieJar cho Refresh Token & Logout**:
   - **Vị trí:** `mobile/core-network/src/main/java/com/rentify/app/core/network/client/NetworkClient.kt` và `interceptor/AuthInterceptor.kt`.
   - **Nguyên nhân:** APIDog Mock Server hiện cho phép test qua Header/Body. Tuy nhiên theo API Spec chuẩn (`rentify.json`), các endpoint `/auth/refresh` và `/auth/logout` kỳ vọng `refreshToken` được truyền qua Cookie `HttpOnly`.
   - **Điều kiện dọn dẹp:** Khi kết nối với Backend Production, cần cấu hình `OkHttpClient.Builder().cookieJar(...)` để tự động lưu và gửi Cookie giữa client và server.

4. **[CHỜ DỌN DẸP] Cấu hình Base URL động môi trường**:
   - **Vị trí:** `mobile/core-network/src/main/java/com/rentify/app/core/network/client/NetworkConfig.kt`.
   - **Nguyên nhân:** Hiện tại `DEFAULT_BASE_URL` đang gắn cứng đường dẫn mock của APIDog (`https://mock.apidog.com/m1/960538-944208-default/`).
   - **Điều kiện dọn dẹp:** Đưa cấu hình `BASE_URL` vào `local.properties` và xuất qua `BuildConfig` để dễ dàng đổi giữa Mock, Local BE (10.0.2.2), Staging và Production.

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

8. **[CHỜ DỌN DẸP] Tích hợp Socket.IO v4 cho Realtime Chat (thay thế Smart Polling)**:
   - **Vị trí:** `mobile/app-tenant/src/main/java/com/rentify/app/tenant/ui/chat/detail/ChatDetailViewModel.kt`, `ChatFragment.kt`, và bổ sung `CommunitySocketManager` trong `:core-network`.
   - **Nguyên nhân:** APIDog Cloud Mock hiện tại chỉ là HTTP REST mock, không hỗ trợ máy chủ Socket.IO (`/api/v1/community/socket.io`), nên client tạm thời dùng Smart Polling (chu kỳ 3.5s qua `GET /messages`) để mô phỏng luồng nhận tin nhắn tức thì khi test.
   - **Điều kiện dọn dẹp:** Khi Backend thật (Localhost hoặc Staging) đưa Community Service và Socket.IO Gateway vào hoạt động theo đặc tả `api-doc/docs/community-socket-v1.md`, tiến hành thêm thư viện `io.socket:socket.io-client`, kết nối Socket.IO (lắng nghe `message:created`, `chat:message-received`, emit `conversation:join` / `leave`) và chuyển Smart Polling thành cơ chế fallback khi rớt mạng.

9. **[CHỜ DỌN DẸP] Mock Avatar URL cho Đoạn Chat & Chi tiết Hội thoại**:
   - **Vị trí:** `mobile/app-tenant/src/main/java/com/rentify/app/tenant/ui/chat/util/ChatAvatarHelper.kt` (`MOCK_USER_AVATARS`, `MOCK_GROUP_AVATARS`), `ConversationAdapter.kt`, `ChatDetailActivity.kt`.
   - **Nguyên nhân:** Mock server APIDog hiện tại chưa có kho lưu trữ ảnh đại diện người dùng và nhóm chat thật. Client tạm thời cấu hình fallback sang bộ sưu tập ảnh chân dung/không gian Unsplash theo hash mã hội thoại để tối ưu trải nghiệm thị giác khi review/demo.
   - **Điều kiện dọn dẹp:** Khi Backend thật đưa vào hoạt động và trả về `avatarMediaId` hợp lệ qua `/media/{id}`, xóa bỏ danh sách URL mẫu Unsplash và chuyển sang tải 100% từ API.

10. **[CHỜ DỌN DẸP] Mock Chat Media Image Fallback cho Tin nhắn Hình ảnh (MessageAdapter)**:
    - **Vị trí:** `mobile/app-tenant/src/main/java/com/rentify/app/tenant/ui/chat/detail/MessageAdapter.kt` (`MOCK_CHAT_PHOTOS`, `resolveImageUrl`), `PhotoViewerDialogFragment.kt`.
    - **Nguyên nhân:** Mock server APIDog trả về `mediaId` giả lập ngẫu nhiên nhưng endpoint `/media/{id}` trên mock không có file nhị phân thật, dẫn đến việc Coil tải ảnh bị HTTP 404 và rơi vào icon placeholder ngọn núi xám. Client tạm thời cấu hình fallback sang bộ sưu tập 6 ảnh phòng trọ/nội thất Unsplash chất lượng cao theo mã hash của `mediaId` hoặc message `id`.
    - **Điều kiện dọn dẹp:** Khi Backend Production đưa dịch vụ R2 Storage vào hoạt động và phục vụ file nhị phân qua `/media/{id}`, xóa bỏ `MOCK_CHAT_PHOTOS` và chuyển `resolveImageUrl` về trả link `${NetworkConfig.DEFAULT_BASE_URL}media/${item.mediaId}` nguyên bản.

11. **[CHỜ DỌN DẸP] Tối ưu hóa hiệu năng & tốc độ phản hồi tab Phòng tôi (My Room Hub)**:
    - **Vị trí:** `mobile/app-tenant/src/main/java/com/rentify/app/tenant/ui/myroom/MyRoomViewModel.kt`, `MyRoomFragment.kt`.
    - **Nguyên nhân:** Đang gọi tuần tự 2 API (`getMyContracts` rồi mới đến `getMyRoomHub`), bị kích hoạt gọi trùng lặp giữa `init` và `onResume()`, và luôn reset về `MyRoomUiState.Loading` khiến người dùng phải chờ xoay vòng mỗi khi chuyển tab.
    - **Kế hoạch cập nhật tương lai:** Chuyển sang gọi song song 2 API bằng Coroutine `async`/`await`, áp dụng cơ chế Instant Display / Stale-While-Revalidate (giữ dữ liệu cũ hiển thị ngay lập tức, chỉ cập nhật ngầm), khử trùng lặp request giữa vòng đời Fragment và ViewModel, tối ưu fallback ảnh bìa phòng Unsplash.
