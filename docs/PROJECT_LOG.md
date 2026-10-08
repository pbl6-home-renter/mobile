# Rentify – Project Log

## 1. Trạng thái hiện tại
<!-- Cập nhật đè, luôn phản ánh hiện tại -->
| Module | Trạng thái | Người phụ trách | Ghi chú |
|---|---|---|---|
| Core Network | Hoàn thành | AI & Dev | Đã dựng Retrofit, OkHttp, Moshi DTO wrappers & MaskedLoggingInterceptor |

## 2. Quyết định
<!-- Mới nhất ở trên. Mỗi mục 2–4 dòng -->
### 2026-10-08 – Xây dựng Module core-network với MaskedLoggingInterceptor
- **Lý do:** Cần tầng Network Layer dùng chung cho cả `app-landlord` và `app-tenant` để kết nối APIDog Mock Server.
- **Ảnh hưởng:** Đảm bảo che header `Authorization` khi log debug theo quy tắc D.11.

## 3. Vấn đề & lưu ý
<!-- [MỞ] / [ĐÃ XỬ LÝ] -->
- [ĐÃ XỬ LÝ] 2026-10-08 – Thêm `consumer-rules.pro` và `proguard-rules.pro` cho `:core-network` tránh lỗi merge ProGuard AAR.

## 4. Nhật ký thay đổi lớn
<!-- Chỉ thay đổi đáng kể, kèm link PR -->
- 2026-10-08 – Dựng tầng network layer (Retrofit, Moshi, OkHttp Interceptors).
