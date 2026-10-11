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
                    val errorMessage = when (errorCode) {
                        "EMAIL_ALREADY_EXISTS" -> "Email này đã được sử dụng"
                        "PHONE_ALREADY_EXISTS" -> "Số điện thoại này đã được sử dụng"
                        else -> result.error?.message ?: "Đăng ký thất bại (Mã $errorCode)"
                    }
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
}
