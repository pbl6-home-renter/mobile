package com.rentify.app.core.ui.auth

import androidx.lifecycle.viewModelScope
import com.rentify.app.core.data.repository.auth.AuthRepository
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.core.ui.state.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VerifyEmailViewModel(
    private val email: String,
    startCooldown: Boolean,
    private val authRepository: AuthRepository
) : BaseViewModel() {

    private val _resendState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val resendState: StateFlow<UiState<Unit>> = _resendState.asStateFlow()

    private val _cooldownSeconds = MutableStateFlow(0)
    val cooldownSeconds: StateFlow<Int> = _cooldownSeconds.asStateFlow()

    init {
        if (startCooldown) startCooldown()
    }

    fun resend() {
        if (_cooldownSeconds.value > 0 || _resendState.value is UiState.Loading) return

        _resendState.value = UiState.Loading
        launchSafe(
            onError = { throwable ->
                _resendState.value = UiState.Error(throwable.message ?: "Đã có lỗi xảy ra")
            }
        ) {
            when (val result = authRepository.requestEmailVerification(email)) {
                is NetworkResult.Success -> {
                    _resendState.value = UiState.Success(Unit)
                    startCooldown()
                }
                is NetworkResult.ApiError -> {
                    val code = result.error?.code ?: result.code.toString()
                    val message = when (code) {
                        "EMAIL_REQUIRED", "EMAIL_NOT_EMPTY" -> "Vui lòng nhập email"
                        "EMAIL_INVALID_LENGTH", "EMAIL_MUST_BE_STRING", "EMAIL_INVALID_FORMAT" -> "Email không đúng định dạng"
                        else -> result.error?.message ?: "Gửi lại email thất bại (Mã $code)"
                    }
                    _resendState.value = UiState.Error(message)
                }
                is NetworkResult.NetworkError -> {
                    _resendState.value = UiState.Error("Không có kết nối mạng, vui lòng kiểm tra lại")
                }
                is NetworkResult.UnknownError -> {
                    _resendState.value = UiState.Error(result.exception?.message ?: "Đã có lỗi không xác định xảy ra")
                }
            }
        }
    }

    fun resetResendState() {
        _resendState.value = UiState.Idle
    }

    private fun startCooldown() {
        viewModelScope.launch {
            _cooldownSeconds.value = COOLDOWN_SECONDS
            while (_cooldownSeconds.value > 0) {
                delay(1000)
                _cooldownSeconds.value -= 1
            }
        }
    }

    private companion object {
        const val COOLDOWN_SECONDS = 60
    }
}
