# Rentify Mobile System

**Rentify** là hệ thống giải pháp di động toàn diện quản lý căn hộ cho thuê, được phát triển trên nền tảng **Android Native (Kotlin)**. Hệ thống bao gồm 2 ứng dụng độc lập dành cho **Chủ trọ (Landlord)** và **Người thuê (Tenant)**, kết hợp cùng hệ thống module thư viện dùng chung (`core-ui`, `core-network`, `core-data`).

---

## 🎯 Tổng quan Ứng dụng

Hệ thống Rentify chia làm 2 ứng dụng chính:

1. **`app-landlord` (Rentify Landlord)**:
   - Package: `com.rentify.app.landlord`
   - Quản lý danh sách toà nhà, dãy trọ, phòng trọ và trạng thái phòng.
   - Quản lý hợp đồng cho thuê, thông tin khách thuê và tiền cọc.
   - Lập và theo dõi hoá đơn hàng tháng (tiền nhà, điện, nước, dịch vụ).
   - Tiếp nhận và xử lý sự cố / yêu cầu sửa chữa từ người thuê.

2. **`app-tenant` (Rentify Tenant)**:
   - Package: `com.rentify.app.tenant`
   - Tìm kiếm phòng trọ, xem thông tin phòng và gửi yêu cầu thuê.
   - Theo dõi chi tiết hợp đồng thuê phòng hiện tại.
   - Tra cứu và thanh toán hoá đơn hàng tháng.
   - Gửi báo cáo sự cố / yêu cầu hỗ trợ trực tiếp tới chủ trọ.

---

## 🏗 Kiến trúc Kỹ thuật (Technical Architecture)

- **Ngôn ngữ**: Kotlin (minSdk 26, compileSdk 36, targetSdk 36)
- **UI Framework**: XML View (KHÔNG sử dụng Jetpack Compose), ViewBinding, Material Design 3 (`Theme.Rentify`)
- **Kiến trúc tổng thể**: Multi-Activity (chia theo luồng nghiệp vụ như Auth, Main, Detail) kết hợp Single-Activity per flow + Navigation Component + Fragment
- **Mô hình thiết kế**: MVVM (Model - View - ViewModel)
- **Bất đồng bộ & Luồng dữ liệu**: Coroutines, Flow, StateFlow
- **Giao diện chuẩn Edge-to-Edge**: Tự động tinh chỉnh Status bar và Navigation bar trong suốt với biểu tượng tối trên nền ứng dụng sáng
- **Quản lý Thư viện**: Gradle Version Catalog (`gradle/libs.versions.toml`)

---

## 📦 Sơ đồ Cấu trúc Module

```
mobile/
├── app-landlord/           # Application Module: Ứng dụng dành cho Chủ trọ
├── app-tenant/             # Application Module: Ứng dụng dành cho Người thuê
├── core-ui/                # Android Library: Thư viện giao diện & UI components dùng chung
├── core-network/           # (Định hướng) Client API, Retrofit, Interceptors, DTOs
├── core-data/              # (Định hướng) Repositories, Local DB (Room), DataStore
├── gradle/                 # Configuration & Version Catalog (libs.versions.toml)
├── build.gradle.kts        # Root build configuration
└── settings.gradle.kts     # Project settings & module inclusion
```

---

## 🎨 Chi tiết Module `core-ui` (`com.rentify.app.core.ui`)

Module `core-ui` đóng vai trò là Design System và cung cấp bộ khung lớp cơ sở (Base Classes) cho các ứng dụng:

### 1. Base Classes (`com.rentify.app.core.ui.base`)
- **`BaseActivity<VB : ViewBinding>`**:
  - Kế thừa `AppCompatActivity`, tự động kích hoạt `enableRentifyEdgeToEdge()`.
  - Khởi tạo ViewBinding, điều phối chu kỳ `initView()` và `observeData()`.
  - Hỗ trợ `collectWhenStarted` an toàn theo Lifecycle State.
  - Tích hợp `showLoading()` / `hideLoading()` an toàn qua `LoadingDialog`.
- **`BaseFragment<VB : ViewBinding>`**:
  - Kế thừa `Fragment`, quản lý ViewBinding lifecycle an toàn.
  - Cung cấp hàm `renderState(state, onError, onSuccess)` tự động đóng/mở loading và render dữ liệu.
- **`BaseViewModel`**:
  - Kế thừa `ViewModel`, tích hợp `launchSafe` giúp tự động catch Exception và re-throw `CancellationException`.
- **`BaseListAdapter<T, VB>`**:
  - Kế thừa `ListAdapter`, tích hợp anti-spam click (`setOnSingleClickListener`) và helper `simpleDiff` tạo `DiffUtil.ItemCallback` nhanh chóng.

### 2. State Management (`com.rentify.app.core.ui.state`)
- **`UiState<T>`**: Sealed interface đại diện cho trạng thái màn hình:
  - `UiState.Idle`: Trạng thái ban đầu.
  - `UiState.Loading`: Trạng thái đang tải dữ liệu.
  - `UiState.Success<T>`: Tải thành công, chứa payload `data: T`.
  - `UiState.Error`: Tải thất bại, chứa thông điệp lỗi `message: String`.

### 3. Utility Extensions (`com.rentify.app.core.ui.extension`)
- **`InsetsExt.kt`**:
  - `Activity.enableRentifyEdgeToEdge()`: Thiết lập thanh hệ thống trong suốt với icon màu tối.
  - `View.applySystemBarsPadding()`: Cộng padding theo thanh hệ thống, bảo lưu padding gốc của View.
  - `View.applyImePadding()`: Cộng padding bottom tương thích khi mở bàn phím mềm (IME).
- **`ViewExt.kt`**:
  - `View.visible()`, `View.gone()`, `View.invisible()`, `View.showIf(condition)`.
  - `View.setOnSingleClickListener(intervalMs)`: Chống bấm liên tục (spam click).
  - `Fragment.toast()`, `Activity.toast()`, `Fragment.hideKeyboard()`, `Activity.hideKeyboard()`.
- **`FormatExt.kt`**:
  - `Long.toVnd()`, `Double.toVnd()`: Định dạng tiền tệ dạng `"1.500.000 đ"`.
  - `String.toDisplayDate()`: Chuẩn hoá chuỗi ISO sang `"03/10/2026"`.
  - `String.toDisplayDateTime()`: Chuẩn hoá chuỗi ISO sang `"03/10/2026 10:15"`.
  - `String.toDisplayMonth()`: Chuẩn hoá chuỗi ISO sang `"10/2026"`.

### 4. Components & Res System
- **`LoadingDialog`**: `DialogFragment` nền trong suốt chứa `CircularProgressIndicator` Material 3 bo góc.
- **`Theme.Rentify`**: Map hệ màu chuẩn (`rentify_primary`, `rentify_surface`, ...), định hình style mặc định cho `MaterialButton`, `TextInputLayout`, `MaterialCardView` và các kiểu chữ `TextAppearance.Rentify.*`.
- **Common Layouts**: `view_empty_state.xml` (hiển thị trạng thái trống), `view_error_state.xml` (hiển thị trạng thái lỗi + nút thử lại), `dialog_loading.xml`.

---

## ⚙️ Quy trình Phát triển & Git Workflow

- **`main`**: Nhánh chính chứa mã nguồn đã kiểm thử và sẵn sàng phát hành (Protected branch, yêu cầu tạo Pull Request để gộp).
- **`develop`**: Nhánh tích hợp tính năng trong quá trình phát triển.

---

## 🛠 Hướng dẫn Cài đặt & Biên dịch (Setup & Build)

### 1. Yêu cầu môi trường
- **JDK**: Java 17 trở lên
- **Android Studio**: Jellyfish (2023.3.1) hoặc Ladybug trở lên
- **Android SDK**: Compile SDK 36, Min SDK 26

### 2. Các lệnh biên dịch bằng Gradle

Mở terminal tại thư mục gốc `mobile/`:

```bash
# Biên dịch module core-ui
./gradlew :core-ui:assembleDebug

# Biên dịch ứng dụng Chủ trọ (Landlord)
./gradlew :app-landlord:assembleDebug

# Biên dịch ứng dụng Người thuê (Tenant)
./gradlew :app-tenant:assembleDebug

# Biên dịch toàn bộ các module trong hệ thống
./gradlew :core-ui:assembleDebug :app-landlord:assembleDebug :app-tenant:assembleDebug
```
