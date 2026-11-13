package com.rentify.app.tenant.ui.chat

import androidx.lifecycle.viewModelScope
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.chat.ConversationListResponseDto
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.core.ui.state.UiState
import com.rentify.app.tenant.data.repository.TenantChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: TenantChatRepository,
    private val tokenDataStore: TokenDataStore
) : BaseViewModel() {

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _conversationsState = MutableStateFlow<UiState<List<ConversationListResponseDto>>>(UiState.Loading)
    val conversationsState: StateFlow<UiState<List<ConversationListResponseDto>>> = _conversationsState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun checkAuthAndLoad(isRefresh: Boolean = false) {
        viewModelScope.launch {
            val token = tokenDataStore.accessToken.first()
            val loggedIn = !token.isNullOrBlank()
            _isLoggedIn.value = loggedIn

            if (!loggedIn) {
                _conversationsState.value = UiState.Idle
                _isRefreshing.value = false
                return@launch
            }

            if (isRefresh) {
                _isRefreshing.value = true
            } else if (_conversationsState.value !is UiState.Success) {
                _conversationsState.value = UiState.Loading
            }

            launchSafe {
                val result = chatRepository.getConversations(pageNo = 1, pageSize = 50)
                _isRefreshing.value = false

                when (result) {
                    is NetworkResult.Success -> {
                        val items = result.data.data
                        _conversationsState.value = UiState.Success(items)
                    }
                    is NetworkResult.ApiError -> {
                        _conversationsState.value = UiState.Error(
                            result.error?.message ?: "Không thể tải danh sách cuộc trò chuyện"
                        )
                    }
                    is NetworkResult.NetworkError -> {
                        _conversationsState.value = UiState.Error("Không có kết nối mạng. Vui lòng thử lại.")
                    }
                    is NetworkResult.UnknownError -> {
                        _conversationsState.value = UiState.Error("Đã xảy ra lỗi không xác định. Vui lòng thử lại.")
                    }
                }
            }
        }
    }
}

