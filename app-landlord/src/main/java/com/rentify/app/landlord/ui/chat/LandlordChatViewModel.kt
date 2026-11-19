package com.rentify.app.landlord.ui.chat

import androidx.lifecycle.viewModelScope
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.chat.ConversationCreateRequestDto
import com.rentify.app.core.network.dto.chat.ConversationDetailResponseDto
import com.rentify.app.core.network.dto.chat.ConversationLastMessageDto
import com.rentify.app.core.network.dto.chat.ConversationListResponseDto
import com.rentify.app.core.network.dto.user.UserDirectoryResponseDto
import com.rentify.app.core.network.socket.CommunitySocketClient
import com.rentify.app.core.network.socket.CommunitySocketEvent
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.landlord.data.repository.LandlordChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface ConversationListUiState {
    object Loading : ConversationListUiState
    data class Success(val conversations: List<ConversationListResponseDto>) : ConversationListUiState
    data class Error(val message: String) : ConversationListUiState
}

sealed interface NewConversationActionState {
    object Idle : NewConversationActionState
    object Loading : NewConversationActionState
    data class Created(val detail: ConversationDetailResponseDto) : NewConversationActionState
    data class Error(val message: String) : NewConversationActionState
}

class LandlordChatViewModel(
    private val chatRepository: LandlordChatRepository,
    private val tokenDataStore: TokenDataStore
) : BaseViewModel() {

    private val _conversationListState = MutableStateFlow<ConversationListUiState>(ConversationListUiState.Loading)
    val conversationListState: StateFlow<ConversationListUiState> = _conversationListState.asStateFlow()

    private val _directoryUsersState = MutableStateFlow<List<UserDirectoryResponseDto>>(emptyList())
    val directoryUsersState: StateFlow<List<UserDirectoryResponseDto>> = _directoryUsersState.asStateFlow()

    private val _newConversationState = MutableStateFlow<NewConversationActionState>(NewConversationActionState.Idle)
    val newConversationState: StateFlow<NewConversationActionState> = _newConversationState.asStateFlow()

    private var lastKind: String? = null
    private var lastQuery: String? = null
    private var currentUserId: String? = null

    init {
        observeSocketEvents()
    }

    private fun observeSocketEvents() {
        viewModelScope.launch {
            CommunitySocketClient.events.collect { event ->
                when (event) {
                    is CommunitySocketEvent.SessionReady -> {
                        if (event.userId.isNotBlank()) {
                            currentUserId = event.userId
                        }
                        if (event.isReconnect) {
                            refreshSilently()
                        }
                    }
                    is CommunitySocketEvent.ChatMessageReceived -> {
                        val payload = event.payload
                        val currentState = _conversationListState.value
                        if (currentState is ConversationListUiState.Success) {
                            val list = currentState.conversations
                            val index = list.indexOfFirst { it.id == payload.conversationId }
                            if (index >= 0) {
                                val target = list[index]
                                val isFromMe = !currentUserId.isNullOrBlank() && payload.senderUserId == currentUserId
                                val nextUnread = if (isFromMe) target.unreadCount else target.unreadCount + 1
                                val updated = target.copy(
                                    displayName = payload.conversationName ?: target.displayName,
                                    unreadCount = nextUnread,
                                    lastMessage = ConversationLastMessageDto(
                                        id = payload.messageId,
                                        sequence = payload.sequence,
                                        senderName = payload.senderName,
                                        contentPreview = payload.contentPreview,
                                        type = payload.type,
                                        sentAt = payload.createdAt
                                    )
                                )
                                val newList = list.toMutableList().apply {
                                    removeAt(index)
                                    add(0, updated)
                                }
                                _conversationListState.value = ConversationListUiState.Success(newList)
                            } else {
                                refreshSilently()
                            }
                        }
                    }
                    is CommunitySocketEvent.ConversationReadUpdated -> {
                        val payload = event.payload
                        val currentState = _conversationListState.value
                        if (currentState is ConversationListUiState.Success) {
                            val updatedList = currentState.conversations.map { conv ->
                                if (conv.id == payload.conversationId) {
                                    conv.copy(unreadCount = payload.unreadCount)
                                } else conv
                            }
                            _conversationListState.value = ConversationListUiState.Success(updatedList)
                        }
                    }
                    is CommunitySocketEvent.ConversationAvailable -> {
                        refreshSilently()
                    }
                    is CommunitySocketEvent.ConversationAccessRevoked -> {
                        val currentState = _conversationListState.value
                        if (currentState is ConversationListUiState.Success) {
                            val filtered = currentState.conversations.filterNot { it.id == event.conversationId }
                            _conversationListState.value = ConversationListUiState.Success(filtered)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun refreshSilently() {
        viewModelScope.launch {
            val result = chatRepository.getConversations(kind = lastKind, q = lastQuery)
            if (result is NetworkResult.Success) {
                _conversationListState.value = ConversationListUiState.Success(result.data.data)
            }
        }
    }

    fun loadConversations(kind: String? = null, q: String? = null) {
        lastKind = kind
        lastQuery = q
        _conversationListState.value = ConversationListUiState.Loading
        viewModelScope.launch {
            currentUserId = tokenDataStore.userId.first()
            val token = tokenDataStore.accessToken.first()
            if (!token.isNullOrBlank()) {
                CommunitySocketClient.ensureConnected(token)
            }
            when (val result = chatRepository.getConversations(kind = kind, q = q)) {
                is NetworkResult.Success -> {
                    _conversationListState.value = ConversationListUiState.Success(result.data.data)
                }
                is NetworkResult.ApiError -> {
                    _conversationListState.value = ConversationListUiState.Error(
                        result.error?.message ?: "Không thể tải danh sách cuộc trò chuyện"
                    )
                }
                is NetworkResult.NetworkError -> {
                    _conversationListState.value = ConversationListUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _conversationListState.value = ConversationListUiState.Error("Lỗi không xác định")
                }
            }
        }
    }

    fun searchDirectoryUsers(q: String? = null) {
        viewModelScope.launch {
            val myId = currentUserId ?: tokenDataStore.userId.first()
            when (val result = chatRepository.getUsersDirectory(q = q)) {
                is NetworkResult.Success -> {
                    _directoryUsersState.value = result.data.data.filterNot { it.id == myId }
                }
                else -> {
                    _directoryUsersState.value = emptyList()
                }
            }
        }
    }

    fun startDirectConversation(recipientUserId: String) {
        _newConversationState.value = NewConversationActionState.Loading
        viewModelScope.launch {
            val req = ConversationCreateRequestDto(
                kind = "direct",
                recipientUserId = recipientUserId
            )
            when (val result = chatRepository.createConversation(req)) {
                is NetworkResult.Success -> {
                    val detail = result.data.data
                    if (detail != null) {
                        _newConversationState.value = NewConversationActionState.Created(detail)
                        refreshSilently()
                    } else {
                        _newConversationState.value = NewConversationActionState.Error("Không thể mở cuộc trò chuyện")
                    }
                }
                is NetworkResult.ApiError -> {
                    _newConversationState.value = NewConversationActionState.Error(
                        result.error?.message ?: "Tạo cuộc trò chuyện thất bại"
                    )
                }
                else -> {
                    _newConversationState.value = NewConversationActionState.Error("Lỗi kết nối khi mở cuộc trò chuyện")
                }
            }
        }
    }

    fun createGroupConversation(memberUserIds: List<String>, title: String?) {
        _newConversationState.value = NewConversationActionState.Loading
        viewModelScope.launch {
            val req = ConversationCreateRequestDto(
                kind = "group",
                memberUserIds = memberUserIds,
                title = title?.takeIf { it.isNotBlank() }
            )
            when (val result = chatRepository.createConversation(req)) {
                is NetworkResult.Success -> {
                    val detail = result.data.data
                    if (detail != null) {
                        _newConversationState.value = NewConversationActionState.Created(detail)
                        refreshSilently()
                    } else {
                        _newConversationState.value = NewConversationActionState.Error("Không thể tạo nhóm")
                    }
                }
                is NetworkResult.ApiError -> {
                    _newConversationState.value = NewConversationActionState.Error(
                        result.error?.message ?: "Tạo nhóm thất bại"
                    )
                }
                else -> {
                    _newConversationState.value = NewConversationActionState.Error("Lỗi kết nối khi tạo nhóm")
                }
            }
        }
    }

    fun resetNewConversationState() {
        _newConversationState.value = NewConversationActionState.Idle
    }
}

