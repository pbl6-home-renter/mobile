package com.rentify.app.core.ui.auth

import com.rentify.app.core.data.repository.auth.AuthRepository
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.auth.AuthRequestPasswordResetRequestDto
import com.rentify.app.core.network.dto.auth.AuthRequestPasswordResetResponseDto
import com.rentify.app.core.network.dto.auth.AuthResetPasswordRequestDto
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.core.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ForgotPasswordViewModel(
    private val authRepository: AuthRepository
) : BaseViewModel() {

    private val _requestResetState = MutableStateFlow<UiState<AuthRequestPasswordResetResponseDto>>(UiState.Idle)
    val requestResetState: StateFlow<UiState<AuthRequestPasswordResetResponseDto>> = _requestResetState.asStateFlow()

    private val _resetPasswordState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val resetPasswordState: StateFlow<UiState<Unit>> = _resetPasswordState.asStateFlow()

    fun requestPasswordReset(email: String) {
        if (email.isBlank()) return

        _requestResetState.value = UiState.Loading
        launchSafe(
            onError = { throwable ->
                _requestResetState.value = UiState.Error(throwable.message ?: "Đã có lỗi xảy ra")
            }
        ) {
            val request = AuthRequestPasswordResetRequestDto(email = email.trim())
            when (val result = authRepository.requestPasswordReset(request)) {
                is NetworkResult.Success -> {
                    val data = result.data.data
                    if (data != null) {
                        _requestResetState.value = UiState.Success(data)
                    } else {
                        // In case response body is empty or 204
                        _requestResetState.value = UiState.Success(
                            AuthRequestPasswordResetResponseDto(accepted = true)
                        )
                    }
                }
                is NetworkResult.ApiError -> {
                    val errorCode = result.error?.code ?: result.code.toString()
                    val errorMessage = when (errorCode) {
                        "INVALID_EMAIL" -> "Email không đúng định dạng"
                        "EMAIL_REQUIRED" -> "Vui lòng nhập Email"
                        else -> result.error?.message ?: "Gửi yêu cầu thất bại (Mã $errorCode)"
                    }
                    _requestResetState.value = UiState.Error(errorMessage)
                }
                is NetworkResult.NetworkError -> {
                    _requestResetState.value = UiState.Error("Không có kết nối mạng, vui lòng kiểm tra lại")
                }
                is NetworkResult.UnknownError -> {
                    _requestResetState.value = UiState.Error(result.exception?.message ?: "Đã có lỗi không xác định xảy ra")
                }
            }
        }
    }

    fun resetPassword(token: String, newPassword: String) {
        if (token.isBlank() || newPassword.isBlank()) return

        _resetPasswordState.value = UiState.Loading
        launchSafe(
            onError = { throwable ->
                _resetPasswordState.value = UiState.Error(throwable.message ?: "Đã có lỗi xảy ra")
            }
        ) {
            val request = AuthResetPasswordRequestDto(token = token.trim(), newPassword = newPassword)
            when (val result = authRepository.resetPassword(request)) {
                is NetworkResult.Success -> {
                    _resetPasswordState.value = UiState.Success(Unit)
                }
                is NetworkResult.ApiError -> {
                    val errorCode = result.error?.code ?: result.code.toString()
                    val errorMessage = when (errorCode) {
                        "TOKEN_EXPIRED" -> "Mã xác thực đã hết hạn, vui lòng xin lại mã mới"
                        "TOKEN_USED" -> "Mã xác thực này đã được sử dụng"
                        "INVALID_TOKEN" -> "Mã xác thực không chính xác"
                        else -> result.error?.message ?: "Đặt lại mật khẩu thất bại (Mã $errorCode)"
                    }
                    _resetPasswordState.value = UiState.Error(errorMessage)
                }
                is NetworkResult.NetworkError -> {
                    _resetPasswordState.value = UiState.Error("Không có kết nối mạng, vui lòng kiểm tra lại")
                }
                is NetworkResult.UnknownError -> {
                    _resetPasswordState.value = UiState.Error(result.exception?.message ?: "Đã có lỗi không xác định xảy ra")
                }
            }
        }
    }

    fun resetRequestState() {
        _requestResetState.value = UiState.Idle
    }

    fun resetResetPasswordState() {
        _resetPasswordState.value = UiState.Idle
    }
}
