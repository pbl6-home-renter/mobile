# Rentify – Project Log

## 1. Trạng thái hiện tại
<!-- Cập nhật đè, luôn phản ánh hiện tại -->
| Module | Trạng thái | Người phụ trách | Ghi chú |
|---|---|---|---|
| Core Network | Hoàn thành | AI & Dev | Đã dựng Retrofit, OkHttp, Moshi DTO wrappers, Auth DTOs & AuthApiService |
| Core Data | Hoàn thành | AI & Dev | Đã dựng Jetpack DataStore, SessionManager, BaseRepository & AuthRepository |
| Core UI | Hoàn thành | AI & Dev | Đã hoàn thiện toàn bộ luồng Auth (Login, Register, Forgot Password, Verify OTP, Reset Password) |
| App Tenant | Hoàn thành Auth Flow | AI & Dev | Điều hướng trọn vẹn Onboarding -> Auth Flow |
| App Landlord | Hoàn thành Auth Flow | AI & Dev | Điều hướng trọn vẹn Onboarding -> Auth Flow |

## 2. Quyết định
<!-- Mới nhất ở trên. Mỗi mục 2–4 dòng -->
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
- [MỞ] 2026-10-11 – API `/auth/refresh` và `/auth/logout` kỳ vọng Cookie `refreshToken` (HttpOnly); cần tích hợp CookieJar khi chạy thực tế với server production.
- [ĐÃ XỬ LÝ] 2026-10-11 – Chuẩn hóa `role` trong `RegisterViewModel` sang lowercase để tránh lỗi 400 Bad Request từ backend validation.
- [ĐÃ XỬ LÝ] 2026-10-10 – Tùy chỉnh `app:cursorColor` trên `TextInputLayout` để ghi đè màu tint con trỏ mặc định của Material Components.
- [ĐÃ XỬ LÝ] 2026-10-10 – Xử lý phản hồi `204 No Content` trong `BaseRepository` (cho endpoint `/auth/logout` và `/auth/password-resets`) bằng cách trả về `NetworkResult.Success(Unit)`.
- [ĐÃ XỬ LÝ] 2026-10-08 – Thêm `consumer-rules.pro` và `proguard-rules.pro` cho `:core-data` và `:core-network` tránh lỗi merge ProGuard AAR.

## 4. Nhật ký thay đổi lớn
<!-- Chỉ thay đổi đáng kể, kèm link PR -->
- 2026-10-11 – Hoàn thiện trọn vẹn UI/UX và logic 6 màn hình thuộc luồng xác thực Auth (Login, Register, Forgot Password, Verify OTP, Reset Password).
- 2026-10-10 – Hoàn thiện UI Auth (`LoginFragment`, `RegisterFragment`, `LoginViewModel`, `RegisterViewModel` & layouts).
- 2026-10-10 – Xây dựng Auth Data Layer (Auth DTOs, AuthApiService, AuthRepository & AuthRepositoryImpl).
- 2026-10-08 – Khởi tạo module `:core-data` (Jetpack DataStore Preferences, SessionManager, BaseRepository).
- 2026-10-08 – Dựng tầng network layer (Retrofit, Moshi, OkHttp Interceptors).
