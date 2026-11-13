package com.rentify.app.landlord.ui.chat

import androidx.lifecycle.viewModelScope
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.chat.MessageListResponseDto
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.landlord.data.repository.LandlordChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface MessageListUiState {
    object Loading : MessageListUiState
    data class Success(val messages: List<MessageListResponseDto>) : MessageListUiState
    data class Error(val message: String) : MessageListUiState
}

sealed interface SendMessageUiState {
    object Idle : SendMessageUiState
    object Sending : SendMessageUiState
    object Sent : SendMessageUiState
    data class Error(val message: String) : SendMessageUiState
}

sealed interface BotReminderUiState {
    object Idle : BotReminderUiState
    object Loading : BotReminderUiState
    data class Success(val message: String) : BotReminderUiState
    data class Error(val message: String) : BotReminderUiState
}

class LandlordChatDetailViewModel(
    private val chatRepository: LandlordChatRepository
) : BaseViewModel() {

    private val _messagesState = MutableStateFlow<MessageListUiState>(MessageListUiState.Loading)
    val messagesState: StateFlow<MessageListUiState> = _messagesState.asStateFlow()

    private val _sendState = MutableStateFlow<SendMessageUiState>(SendMessageUiState.Idle)
    val sendState: StateFlow<SendMessageUiState> = _sendState.asStateFlow()

    private val _botReminderState = MutableStateFlow<BotReminderUiState>(BotReminderUiState.Idle)
    val botReminderState: StateFlow<BotReminderUiState> = _botReminderState.asStateFlow()

    fun loadMessages(conversationId: String) {
        _messagesState.value = MessageListUiState.Loading
        viewModelScope.launch {
            when (val result = chatRepository.getMessages(conversationId = conversationId, pageSize = 50)) {
                is NetworkResult.Success -> {
                    _messagesState.value = MessageListUiState.Success(result.data.data)
                }
                is NetworkResult.ApiError -> {
                    _messagesState.value = MessageListUiState.Error(
                        result.error?.message ?: "Không thể tải danh sách tin nhắn"
                    )
                }
                is NetworkResult.NetworkError -> {
                    _messagesState.value = MessageListUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _messagesState.value = MessageListUiState.Error("Lỗi không xác định")
                }
            }
        }
    }

    fun sendMessage(conversationId: String, content: String) {
        if (content.isBlank()) return
        _sendState.value = SendMessageUiState.Sending
        viewModelScope.launch {
            val clientMsgId = UUID.randomUUID().toString()
            when (val result = chatRepository.sendMessage(
                conversationId = conversationId,
                content = content,
                clientMessageId = clientMsgId
            )) {
                is NetworkResult.Success -> {
                    _sendState.value = SendMessageUiState.Sent
                    loadMessages(conversationId)
                }
                is NetworkResult.ApiError -> {
                    _sendState.value = SendMessageUiState.Error(
                        result.error?.message ?: "Lỗi gửi tin nhắn"
                    )
                }
                is NetworkResult.NetworkError -> {
                    _sendState.value = SendMessageUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _sendState.value = SendMessageUiState.Error("Lỗi không xác định")
                }
            }
        }
    }

    fun sendBotReminder(conversationId: String, templateType: String, resourceId: String) {
        _botReminderState.value = BotReminderUiState.Loading
        viewModelScope.launch {
            val clientMsgId = UUID.randomUUID().toString()
            when (val result = chatRepository.sendBotReminder(
                conversationId = conversationId,
                templateType = templateType,
                resourceId = resourceId,
                clientMessageId = clientMsgId
            )) {
                is NetworkResult.Success -> {
                    _botReminderState.value = BotReminderUiState.Success("Đã gửi lời nhắc tự động thành công")
                    loadMessages(conversationId)
                }
                is NetworkResult.ApiError -> {
                    _botReminderState.value = BotReminderUiState.Error(
                        result.error?.message ?: "Lỗi gửi lời nhắc bot"
                    )
                }
                is NetworkResult.NetworkError -> {
                    _botReminderState.value = BotReminderUiState.Error("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    _botReminderState.value = BotReminderUiState.Error("Lỗi không xác định")
                }
            }
        }
    }

    fun markRead(conversationId: String, lastSequence: String) {
        viewModelScope.launch {
            chatRepository.markMessagesAsRead(conversationId, lastSequence)
        }
    }

    fun resetSendState() {
        _sendState.value = SendMessageUiState.Idle
    }

    fun resetBotReminderState() {
        _botReminderState.value = BotReminderUiState.Idle
    }
}

