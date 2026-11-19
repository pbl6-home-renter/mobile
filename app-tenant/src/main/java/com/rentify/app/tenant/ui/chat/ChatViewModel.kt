package com.rentify.app.tenant.ui.chat

import androidx.lifecycle.viewModelScope
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.chat.ConversationLastMessageDto
import com.rentify.app.core.network.dto.chat.ConversationListResponseDto
import com.rentify.app.core.network.socket.CommunitySocketClient
import com.rentify.app.core.network.socket.CommunitySocketEvent
import com.rentify.app.core.network.socket.SocketMessageReceivedDto
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

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    init {
        observeSocketEvents()
    }

    private fun observeSocketEvents() {
        viewModelScope.launch {
            CommunitySocketClient.events.collect { event ->
                if (!_isLoggedIn.value) return@collect
                when (event) {
                    is CommunitySocketEvent.SessionReady -> {
                        if (event.userId.isNotBlank()) {
                            _currentUserId.value = event.userId
                        }
                        if (event.isReconnect) {
                            checkAuthAndLoad(isRefresh = true)
                        }
                    }
                    is CommunitySocketEvent.ChatMessageReceived -> {
                        handleRealtimeMessageReceived(event.payload)
                    }
                    is CommunitySocketEvent.ConversationReadUpdated -> {
                        val currentSuccess = _conversationsState.value as? UiState.Success ?: return@collect
                        val updated = currentSuccess.data.map { conv ->
                            if (conv.id == event.payload.conversationId) {
                                conv.copy(unreadCount = event.payload.unreadCount)
                            } else {
                                conv
                            }
                        }
                        _conversationsState.value = UiState.Success(updated)
                    }
                    is CommunitySocketEvent.ConversationAvailable,
                    is CommunitySocketEvent.ConversationAccessRevoked -> {
                        checkAuthAndLoad(isRefresh = true)
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun handleRealtimeMessageReceived(payload: SocketMessageReceivedDto) {
        val currentSuccess = _conversationsState.value as? UiState.Success
        val currentList = currentSuccess?.data
        if (currentList == null) {
            checkAuthAndLoad(isRefresh = true)
            return
        }

        val existingIndex = currentList.indexOfFirst { it.id == payload.conversationId }
        if (existingIndex == -1) {
            // Hội thoại direct mới có tin nhắn đầu tiên -> refetch GET /conversations theo đặc tả
            checkAuthAndLoad(isRefresh = true)
            return
        }

        val existing = currentList[existingIndex]
        val myUserId = _currentUserId.value
        val isFromMe = !myUserId.isNullOrBlank() && payload.senderUserId == myUserId
        val nextUnread = if (isFromMe) existing.unreadCount else existing.unreadCount + 1

        val updatedConv = existing.copy(
            displayName = payload.conversationName ?: existing.displayName,
            avatarUrl = if (existing.kind == "direct" && !payload.senderAvatarUrl.isNullOrBlank() && !isFromMe) {
                payload.senderAvatarUrl
            } else {
                existing.avatarUrl
            },
            unreadCount = nextUnread,
            lastMessage = ConversationLastMessageDto(
                id = payload.messageId,
                sequence = payload.sequence,
                senderUserId = payload.senderUserId,
                senderName = payload.senderName,
                contentPreview = payload.contentPreview,
                type = payload.type,
                sentAt = payload.createdAt
            )
        )

        val reordered = buildList {
            add(updatedConv)
            currentList.forEachIndexed { idx, item ->
                if (idx != existingIndex) add(item)
            }
        }
        _conversationsState.value = UiState.Success(reordered)
    }

    fun checkAuthAndLoad(isRefresh: Boolean = false) {
        viewModelScope.launch {
            val token = tokenDataStore.accessToken.first()
            _currentUserId.value = tokenDataStore.userId.first()
            val loggedIn = !token.isNullOrBlank()
            _isLoggedIn.value = loggedIn

            if (!loggedIn) {
                CommunitySocketClient.disconnect()
                _conversationsState.value = UiState.Idle
                _isRefreshing.value = false
                return@launch
            }

            CommunitySocketClient.ensureConnected(token)

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
