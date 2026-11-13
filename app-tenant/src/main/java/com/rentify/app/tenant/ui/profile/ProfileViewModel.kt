package com.rentify.app.tenant.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.data.repository.auth.AuthRepository
import com.rentify.app.core.data.repository.user.UserRepository
import com.rentify.app.tenant.data.local.TenantDevPreferences
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.user.UserChangePasswordRequestDto
import com.rentify.app.core.network.dto.user.UserCurrentResponseDto
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoggedIn: Boolean = false,
    val profile: UserCurrentResponseDto? = null,
    val role: String = "TENANT",
    val isUpdating: Boolean = false,
    val devTweakHasRoom: Boolean? = null
)

sealed interface ProfileEvent {
    data class ShowToast(val message: String) : ProfileEvent
    data class ShowError(val message: String) : ProfileEvent
    object ProfileUpdated : ProfileEvent
    object PasswordChanged : ProfileEvent
    object LoggedOut : ProfileEvent
}

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val tokenDataStore: TokenDataStore,
    private val tenantDevPreferences: TenantDevPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ProfileEvent>()
    val events: SharedFlow<ProfileEvent> = _events.asSharedFlow()

    init {
        observeAuthState()
        observeDevTweaks()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            combine(tokenDataStore.accessToken, tokenDataStore.userRole) { token, role ->
                Pair(token, role)
            }.collect { (token, role) ->
                val loggedIn = !token.isNullOrBlank()
                _uiState.update {
                    it.copy(
                        isLoggedIn = loggedIn,
                        role = role ?: "TENANT"
                    )
                }
                if (loggedIn) {
                    loadUserProfile()
                } else {
                    _uiState.update { it.copy(profile = null) }
                }
            }
        }
    }

    private fun observeDevTweaks() {
        viewModelScope.launch {
            tenantDevPreferences.devTweakHasRoom.collect { hasRoom ->
                _uiState.update { it.copy(devTweakHasRoom = hasRoom) }
            }
        }
    }

    fun setDevTweakHasRoom(hasRoom: Boolean?) {
        viewModelScope.launch {
            tenantDevPreferences.setDevTweakHasRoom(hasRoom)
        }
    }

    fun loadUserProfile(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = true) }
            } else {
                _uiState.update { it.copy(isLoading = true) }
            }

            when (val result = userRepository.getCurrentUser()) {
                is NetworkResult.Success -> {
                    val profileData = result.data.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            profile = profileData
                        )
                    }
                }
                is NetworkResult.ApiError -> {
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
                    if (result.code == 401) {
                        tokenDataStore.clearTokens()
                        _events.emit(ProfileEvent.ShowToast("Phiên đăng nhập đã hết hạn"))
                    } else {
                        _events.emit(ProfileEvent.ShowError(result.error?.message ?: "Không thể tải thông tin cá nhân"))
                    }
                }
                is NetworkResult.NetworkError -> {
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
                    _events.emit(ProfileEvent.ShowError("Lỗi kết nối mạng, vui lòng kiểm tra lại"))
                }
                is NetworkResult.UnknownError -> {
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
                    _events.emit(ProfileEvent.ShowError(result.exception?.message ?: "Đã có lỗi xảy ra"))
                }
            }
        }
    }

    fun updateProfile(name: String, phone: String) {
        val trimmedName = name.trim()
        val trimmedPhone = phone.trim()

        if (trimmedName.isBlank()) {
            viewModelScope.launch {
                _events.emit(ProfileEvent.ShowError("Họ tên không được để trống"))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true) }

            when (val result = userRepository.updateCurrentUser(trimmedName, trimmedPhone)) {
                is NetworkResult.Success -> {
                    val updated = result.data.data
                    _uiState.update { current ->
                        current.copy(
                            isUpdating = false,
                            profile = current.profile?.copy(
                                name = updated?.name ?: trimmedName,
                                phone = updated?.phone ?: trimmedPhone
                            )
                        )
                    }
                    _events.emit(ProfileEvent.ProfileUpdated)
                    _events.emit(ProfileEvent.ShowToast("Cập nhật thông tin thành công"))
                }
                is NetworkResult.ApiError -> {
                    _uiState.update { it.copy(isUpdating = false) }
                    _events.emit(ProfileEvent.ShowError(result.error?.message ?: "Cập nhật thất bại"))
                }
                is NetworkResult.NetworkError -> {
                    _uiState.update { it.copy(isUpdating = false) }
                    _events.emit(ProfileEvent.ShowError("Lỗi kết nối mạng"))
                }
                is NetworkResult.UnknownError -> {
                    _uiState.update { it.copy(isUpdating = false) }
                    _events.emit(ProfileEvent.ShowError(result.exception?.message ?: "Có lỗi xảy ra"))
                }
            }
        }
    }

    fun changePassword(currentPass: String, newPass: String, confirmPass: String) {
        if (currentPass.isBlank()) {
            viewModelScope.launch { _events.emit(ProfileEvent.ShowError("Vui lòng nhập mật khẩu hiện tại")) }
            return
        }
        if (newPass.length < 6) {
            viewModelScope.launch { _events.emit(ProfileEvent.ShowError("Mật khẩu mới phải có ít nhất 6 ký tự")) }
            return
        }
        if (newPass != confirmPass) {
            viewModelScope.launch { _events.emit(ProfileEvent.ShowError("Xác nhận mật khẩu mới không khớp")) }
            return
        }
        if (newPass == currentPass) {
            viewModelScope.launch { _events.emit(ProfileEvent.ShowError("Mật khẩu mới không được trùng mật khẩu cũ")) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdating = true) }

            val request = UserChangePasswordRequestDto(
                currentPassword = currentPass,
                newPassword = newPass,
                confirmPassword = confirmPass
            )

            when (val result = userRepository.changePassword(request)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isUpdating = false) }
                    _events.emit(ProfileEvent.PasswordChanged)
                    _events.emit(ProfileEvent.ShowToast("Đổi mật khẩu thành công"))
                }
                is NetworkResult.ApiError -> {
                    _uiState.update { it.copy(isUpdating = false) }
                    _events.emit(ProfileEvent.ShowError(result.error?.message ?: "Đổi mật khẩu thất bại"))
                }
                is NetworkResult.NetworkError -> {
                    _uiState.update { it.copy(isUpdating = false) }
                    _events.emit(ProfileEvent.ShowError("Lỗi kết nối mạng"))
                }
                is NetworkResult.UnknownError -> {
                    _uiState.update { it.copy(isUpdating = false) }
                    _events.emit(ProfileEvent.ShowError(result.exception?.message ?: "Có lỗi xảy ra"))
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            authRepository.logout()
            tokenDataStore.clearTokens()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isLoggedIn = false,
                    profile = null
                )
            }
            _events.emit(ProfileEvent.LoggedOut)
        }
    }
}
