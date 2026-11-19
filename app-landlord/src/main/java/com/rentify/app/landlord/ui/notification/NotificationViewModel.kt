package com.rentify.app.landlord.ui.notification

import androidx.lifecycle.viewModelScope
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.notification.NotificationListResponseDto
import com.rentify.app.core.network.dto.notification.NotificationPreferenceDetailResponseDto
import com.rentify.app.core.network.dto.notification.NotificationPreferenceItemRequestDto
import com.rentify.app.core.network.dto.notification.NotificationPreferenceUpdateRequestDto
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.landlord.data.repository.LandlordNotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface NotificationListUiState {
    object Loading : NotificationListUiState
    data class Success(val items: List<NotificationListResponseDto>) : NotificationListUiState
    data class Error(val message: String) : NotificationListUiState
}

sealed interface NotificationActionState {
    object Idle : NotificationActionState
    object Loading : NotificationActionState
    data class Success(val message: String) : NotificationActionState
    data class Error(val message: String) : NotificationActionState
}

class NotificationViewModel(
    private val repository: LandlordNotificationRepository
) : BaseViewModel() {

    private val _listState = MutableStateFlow<NotificationListUiState>(NotificationListUiState.Loading)
    val listState: StateFlow<NotificationListUiState> = _listState.asStateFlow()

    private val _preferences = MutableStateFlow<List<NotificationPreferenceDetailResponseDto>>(emptyList())
    val preferences: StateFlow<List<NotificationPreferenceDetailResponseDto>> = _preferences.asStateFlow()

    private val _actionState = MutableStateFlow<NotificationActionState>(NotificationActionState.Idle)
    val actionState: StateFlow<NotificationActionState> = _actionState.asStateFlow()

    private var currentUnreadOnly: Boolean? = null

    fun loadNotifications(unreadOnly: Boolean? = currentUnreadOnly) {
        currentUnreadOnly = unreadOnly
        _listState.value = NotificationListUiState.Loading
        viewModelScope.launch {
            when (val res = repository.getNotifications(unreadOnly = unreadOnly, pageSize = 50)) {
                is NetworkResult.Success -> {
                    _listState.value = NotificationListUiState.Success(res.data.data)
                }
                is NetworkResult.ApiError -> {
                    _listState.value = NotificationListUiState.Error(
                        res.error?.message ?: "Không thể tải danh sách thông báo"
                    )
                }
                else -> {
                    _listState.value = NotificationListUiState.Error("Lỗi kết nối khi tải thông báo")
                }
            }
        }
    }

    fun markRead(notificationId: String) {
        viewModelScope.launch {
            when (val res = repository.markNotificationRead(notificationId)) {
                is NetworkResult.Success -> {
                    val readAt = res.data.data?.readAt ?: "read"
                    val current = (_listState.value as? NotificationListUiState.Success)?.items ?: return@launch
                    val updated = if (currentUnreadOnly == true) {
                        current.filterNot { it.id == notificationId }
                    } else {
                        current.map { item ->
                            if (item.id == notificationId) item.copy(readAt = readAt) else item
                        }
                    }
                    _listState.value = NotificationListUiState.Success(updated)
                }
                else -> {}
            }
        }
    }

    fun markAllRead() {
        _actionState.value = NotificationActionState.Loading
        viewModelScope.launch {
            when (val res = repository.markAllNotificationsRead()) {
                is NetworkResult.Success -> {
                    val count = res.data.data?.markedCount ?: 0
                    _actionState.value = NotificationActionState.Success("Đã đánh dấu đọc $count thông báo")
                    loadNotifications(currentUnreadOnly)
                }
                is NetworkResult.ApiError -> {
                    _actionState.value = NotificationActionState.Error(
                        res.error?.message ?: "Không thể đánh dấu đã đọc"
                    )
                }
                else -> {
                    _actionState.value = NotificationActionState.Error("Lỗi kết nối mạng")
                }
            }
        }
    }

    fun loadPreferences() {
        viewModelScope.launch {
            when (val res = repository.getNotificationPreferences()) {
                is NetworkResult.Success -> {
                    _preferences.value = res.data.data
                }
                else -> {}
            }
        }
    }

    fun updatePreferences(chatEnabled: Boolean, invoiceEnabled: Boolean, issueEnabled: Boolean) {
        _actionState.value = NotificationActionState.Loading
        viewModelScope.launch {
            val req = NotificationPreferenceUpdateRequestDto(
                preferences = listOf(
                    NotificationPreferenceItemRequestDto(type = "chat", pushEnabled = chatEnabled),
                    NotificationPreferenceItemRequestDto(type = "invoice", pushEnabled = invoiceEnabled),
                    NotificationPreferenceItemRequestDto(type = "issue", pushEnabled = issueEnabled)
                )
            )
            when (val res = repository.updateNotificationPreferences(req)) {
                is NetworkResult.Success -> {
                    _actionState.value = NotificationActionState.Success("Đã lưu cài đặt thông báo")
                    loadPreferences()
                }
                is NetworkResult.ApiError -> {
                    _actionState.value = NotificationActionState.Error(
                        res.error?.message ?: "Không thể lưu cài đặt thông báo"
                    )
                }
                else -> {
                    _actionState.value = NotificationActionState.Error("Lỗi kết nối khi lưu cài đặt")
                }
            }
        }
    }

    fun resetActionState() {
        _actionState.value = NotificationActionState.Idle
    }
}
