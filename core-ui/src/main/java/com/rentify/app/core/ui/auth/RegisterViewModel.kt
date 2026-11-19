package com.rentify.app.core.ui.auth

import com.rentify.app.core.data.repository.auth.AuthRepository
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.auth.AuthRegisterRequestDto
import com.rentify.app.core.network.dto.auth.AuthRegisterResponseDto
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.core.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RegisterViewModel(
    private val authRepository: AuthRepository
) : BaseViewModel() {

    private val _registerState = MutableStateFlow<UiState<AuthRegisterResponseDto>>(UiState.Idle)
    val registerState: StateFlow<UiState<AuthRegisterResponseDto>> = _registerState.asStateFlow()

    fun register(name: String, email: String, phone: String, password: String, role: String = "TENANT") {
        if (name.isBlank() || email.isBlank() || phone.isBlank() || password.isBlank()) return

        _registerState.value = UiState.Loading
        launchSafe(
            onError = { throwable ->
                _registerState.value = UiState.Error(throwable.message ?: "Đã có lỗi xảy ra")
            }
        ) {
            val request = AuthRegisterRequestDto(
                name = name.trim(),
                email = email.trim(),
                phone = phone.trim(),
                password = password,
                role = role.trim().lowercase()
            )
            when (val result = authRepository.register(request)) {
                is NetworkResult.Success -> {
                    val data = result.data.data
                    if (data != null) {
                        _registerState.value = UiState.Success(data)
                    } else {
                        _registerState.value = UiState.Error("Đã có lỗi xảy ra khi xử lý dữ liệu")
                    }
                }
                is NetworkResult.ApiError -> {
                    val errorCode = result.error?.code ?: result.code.toString()
                    val errorMessage = registerErrorMessage(errorCode)
                        ?: result.error?.message
                        ?: "Đăng ký thất bại (Mã $errorCode)"
                    _registerState.value = UiState.Error(errorMessage)
                }
                is NetworkResult.NetworkError -> {
                    _registerState.value = UiState.Error("Không có kết nối mạng, vui lòng kiểm tra lại")
                }
                is NetworkResult.UnknownError -> {
                    _registerState.value = UiState.Error(result.exception?.message ?: "Đã có lỗi không xác định xảy ra")
                }
            }
        }
    }

    fun resetState() {
        _registerState.value = UiState.Idle
    }

    private fun registerErrorMessage(code: String): String? = when (code) {
        "DUPLICATE_EMAIL" -> "Email này đã được sử dụng"
        "DUPLICATE_PHONE" -> "Số điện thoại này đã được sử dụng"
        "PASSWORD_REQUIRED", "PASSWORD_NOT_EMPTY" -> "Vui lòng nhập mật khẩu"
        "PASSWORD_MUST_BE_STRING" -> "Mật khẩu không hợp lệ"
        "PASSWORD_INVALID_LENGTH" -> "Mật khẩu phải có từ 8 đến 255 ký tự"
        "PASSWORD_INVALID_FORMAT" -> "Mật khẩu cần có chữ thường, chữ hoa và chữ số"
        "EMAIL_REQUIRED", "EMAIL_NOT_EMPTY" -> "Vui lòng nhập email"
        "EMAIL_INVALID_LENGTH" -> "Email phải có từ 5 đến 254 ký tự"
        "EMAIL_MUST_BE_STRING", "EMAIL_INVALID_FORMAT" -> "Email không đúng định dạng"
        "PHONE_REQUIRED", "PHONE_NOT_EMPTY" -> "Vui lòng nhập số điện thoại"
        "PHONE_MUST_BE_STRING", "PHONE_INVALID_FORMAT" -> "Số điện thoại không hợp lệ (ví dụ: 0901234567)"
        "NAME_REQUIRED", "NAME_NOT_EMPTY" -> "Vui lòng nhập họ và tên"
        "NAME_MUST_BE_STRING" -> "Họ và tên không hợp lệ"
        "NAME_INVALID_LENGTH" -> "Họ và tên tối đa 254 ký tự"
        "ROLE_REQUIRED", "ROLE_INVALID" -> "Loại tài khoản không hợp lệ"
        "AUTH_REGISTER_ROLE_NOT_ALLOWED" -> "Không thể đăng ký loại tài khoản này"
        "INVALID_JSON", "INVALID_REQUEST_BODY", "BAD_REQUEST", "UNEXPECTED_FIELD", "VALIDATION_FAILED" ->
            "Thông tin đăng ký chưa hợp lệ, vui lòng kiểm tra lại"
        else -> null
    }
}
