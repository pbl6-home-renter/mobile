package com.rentify.app.core.ui.auth

import com.rentify.app.core.data.repository.auth.AuthRepository
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.auth.AuthLoginRequestDto
import com.rentify.app.core.network.dto.auth.AuthLoginResponseDto
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.core.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LoginViewModel(
    private val authRepository: AuthRepository
) : BaseViewModel() {

    private val _loginState = MutableStateFlow<UiState<AuthLoginResponseDto>>(UiState.Idle)
    val loginState: StateFlow<UiState<AuthLoginResponseDto>> = _loginState.asStateFlow()

    fun login(identifier: String, password: String, rememberMe: Boolean = true) {
        if (identifier.isBlank() || password.isBlank()) return

        _loginState.value = UiState.Loading
        launchSafe(
            onError = { throwable ->
                _loginState.value = UiState.Error(throwable.message ?: "Đã có lỗi xảy ra")
            }
        ) {
            val request = AuthLoginRequestDto(
                identifier = identifier.trim(),
                password = password,
                rememberMe = rememberMe
            )
            when (val result = authRepository.login(request)) {
                is NetworkResult.Success -> {
                    val loginData = result.data.data
                    if (loginData != null) {
                        _loginState.value = UiState.Success(loginData)
                    } else {
                        _loginState.value = UiState.Error("Đã có lỗi xảy ra khi xử lý dữ liệu")
                    }
                }
                is NetworkResult.ApiError -> {
                    val errorCode = result.error?.code ?: result.code.toString()
                    val errorMessage = when (errorCode) {
                        "INVALID_CREDENTIALS" -> "Email hoặc mật khẩu không chính xác"
                        "ACCOUNT_LOCKED" -> "Tài khoản của bạn đã bị khóa"
                        else -> result.error?.message ?: "Đăng nhập thất bại (Mã $errorCode)"
                    }
                    _loginState.value = UiState.Error(errorMessage)
                }
                is NetworkResult.NetworkError -> {
                    _loginState.value = UiState.Error("Không có kết nối mạng, vui lòng kiểm tra lại")
                }
                is NetworkResult.UnknownError -> {
                    _loginState.value = UiState.Error(result.exception?.message ?: "Đã có lỗi không xác định xảy ra")
                }
            }
        }
    }

    fun resetState() {
        _loginState.value = UiState.Idle
    }
}
