# Rentify Mobile

Hệ thống ứng dụng di động quản lý căn hộ cho thuê (Rentify), được xây dựng trên nền tảng **Android Native (Kotlin + XML View)**.

---

## 🛠 Tech Stack & Kiến trúc

- **Ngôn ngữ**: Kotlin
- **UI Framework**: XML View, ViewBinding, Material Design 3 (Material3)
- **Kiến trúc**: MVVM, Multi-Activity (chia theo luồng nghiệp vụ) + Navigation Component + Fragment
- **Bất đồng bộ & State**: Coroutines, Flow, StateFlow
- **Quản lý Dependency**: Gradle Version Catalog (`gradle/libs.versions.toml`)
- **SDK**: `minSdk = 26`, `compileSdk = 36`, `targetSdk = 36`

---

## 📁 Cấu trúc Module

```
mobile/
├── app-landlord/   # Module ứng dụng dành cho Chủ trọ (com.rentify.app.landlord)
├── app-tenant/     # Module ứng dụng dành cho Người thuê (com.rentify.app.tenant)
├── core-ui/        # Library module chứa giao diện & component dùng chung (com.rentify.app.core.ui)
└── gradle/         # Gradle configuration & Version Catalog
```

---

## 🎨 Module `core-ui`

Module `core-ui` đóng vai trò là Design System và cung cấp các lớp cơ sở (Base Classes) cũng như Utility Extensions cho các module app (`app-landlord`, `app-tenant`).

### 1. Base Components (`com.rentify.app.core.ui.base`)
- **`BaseActivity`**: Kế thừa `AppCompatActivity`, hỗ trợ ViewBinding, tự động bật Edge-to-Edge (`enableRentifyEdgeToEdge`), cung cấp `collectWhenStarted`, `showLoading` và `hideLoading`.
- **`BaseFragment`**: Kế thừa `Fragment`, hỗ trợ ViewBinding, lifecycle-aware coroutine collection (`collectWhenStarted`), tích hợp quản lý `LoadingDialog` và hàm `renderState`.
- **`BaseViewModel`**: Kế thừa `ViewModel`, hỗ trợ hàm `launchSafe` giúp bắt Exception an toàn và không nuốt `CancellationException`.
- **`BaseListAdapter`**: Kế thừa `ListAdapter`, xử lý click an toàn chống click liên tục (`setOnSingleClickListener`) và cung cấp helper `simpleDiff` để tạo `DiffUtil.ItemCallback` nhanh chóng.

### 2. State Management (`com.rentify.app.core.ui.state`)
- **`UiState<T>`**: Standard sealed interface quản lý trạng thái UI: `Idle`, `Loading`, `Success<T>`, `Error`.

### 3. Extension Helpers (`com.rentify.app.core.ui.extension`)
- **`InsetsExt`**: `Activity.enableRentifyEdgeToEdge()`, `View.applySystemBarsPadding()`, `View.applyImePadding()`.
- **`ViewExt`**: `visible()`, `gone()`, `invisible()`, `showIf()`, `setOnSingleClickListener()`, `toast()`, `hideKeyboard()`.
- **`FormatExt`**: `Long.toVnd()`, `Double.toVnd()`, `String.toDisplayDate()`, `String.toDisplayDateTime()`, `String.toDisplayMonth()`.

### 4. Components & Theme (`com.rentify.app.core.ui.component`, `res`)
- **`LoadingDialog`**: `DialogFragment` hiển thị loading trong suốt với Material 3 `CircularProgressIndicator`.
- **`Theme.Rentify`**: Theme chuẩn Material 3 Light với hệ màu custom (`rentify_primary`, `rentify_surface`, ...), status bar / navigation bar trong suốt và icon tối.
- **Common Layouts**: `view_empty_state.xml`, `view_error_state.xml`, `dialog_loading.xml`.

---

## 🚀 Hướng dẫn Biên dịch (Build)

Mở Terminal tại thư mục gốc `mobile/` và chạy lệnh:

```bash
# Biên dịch toàn bộ các module
./gradlew :core-ui:assembleDebug :app-landlord:assembleDebug :app-tenant:assembleDebug
```

---

## 📌 Nguyên tắc Phát triển

1. Module `core-ui` chỉ chứa tài nguyên UI dùng chung, không chứa mã nguồn gọi API (Retrofit), Hilt, DataStore hay Business Logic.
2. Các thư viện UI cơ bản (`appcompat`, `activity-ktx`, `fragment-ktx`, `lifecycle`, `material`, `recyclerview`) được xuất khẩu qua `api(...)` trong `core-ui`.
3. Toàn bộ giao diện tuân thủ chuẩn Edge-to-Edge và hệ thống bảng màu định sẵn (`@color/rentify_*`).
