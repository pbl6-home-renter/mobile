package com.rentify.app.landlord.ui.chat

import androidx.lifecycle.viewModelScope
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.chat.ConversationDetailResponseDto
import com.rentify.app.core.network.dto.chat.MessageListResponseDto
import com.rentify.app.core.network.dto.contract.ContractListResponseDto
import com.rentify.app.core.network.dto.invoice.InvoiceListResponseDto
import com.rentify.app.core.network.socket.CommunitySocketClient
import com.rentify.app.core.network.socket.CommunitySocketEvent
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.landlord.data.repository.LandlordChatRepository
import com.rentify.app.landlord.data.repository.LandlordContractRepository
import com.rentify.app.landlord.data.repository.LandlordInvoiceRepository
import com.rentify.app.landlord.data.repository.LandlordMediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    private val chatRepository: LandlordChatRepository,
    private val tokenDataStore: TokenDataStore,
    private val mediaRepository: LandlordMediaRepository? = null,
    private val invoiceRepository: LandlordInvoiceRepository? = null,
    private val contractRepository: LandlordContractRepository? = null
) : BaseViewModel() {

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    private val _conversationDetailState = MutableStateFlow<ConversationDetailResponseDto?>(null)
    val conversationDetailState: StateFlow<ConversationDetailResponseDto?> = _conversationDetailState.asStateFlow()

    private val _messagesState = MutableStateFlow<MessageListUiState>(MessageListUiState.Loading)
    val messagesState: StateFlow<MessageListUiState> = _messagesState.asStateFlow()

    private val _sendState = MutableStateFlow<SendMessageUiState>(SendMessageUiState.Idle)
    val sendState: StateFlow<SendMessageUiState> = _sendState.asStateFlow()

    private val _botReminderState = MutableStateFlow<BotReminderUiState>(BotReminderUiState.Idle)
    val botReminderState: StateFlow<BotReminderUiState> = _botReminderState.asStateFlow()

    private val _reminderInvoicesState = MutableStateFlow<List<InvoiceListResponseDto>>(emptyList())
    val reminderInvoicesState: StateFlow<List<InvoiceListResponseDto>> = _reminderInvoicesState.asStateFlow()

    private val _reminderContractsState = MutableStateFlow<List<ContractListResponseDto>>(emptyList())
    val reminderContractsState: StateFlow<List<ContractListResponseDto>> = _reminderContractsState.asStateFlow()

    private var activeConversationId: String? = null
    private var hasHandledInitialSession = false

    init {
        viewModelScope.launch {
            _currentUserId.value = tokenDataStore.userId.first()
        }
        observeSocketEvents()
    }

    private fun observeSocketEvents() {
        viewModelScope.launch {
            CommunitySocketClient.events.collect { event ->
                val convId = activeConversationId ?: return@collect
                when (event) {
                    is CommunitySocketEvent.MessageCreated -> {
                        if (event.message.conversationId != convId) return@collect
                        appendIncomingMessage(event.message)
                        markRead(convId, event.message.sequence)
                    }
                    is CommunitySocketEvent.SessionReady -> {
                        if (event.userId.isNotBlank()) {
                            _currentUserId.value = event.userId
                        }
                        if (event.isReconnect && hasHandledInitialSession) {
                            syncMessagesSilently(convId)
                        } else {
                            hasHandledInitialSession = true
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun appendIncomingMessage(message: MessageListResponseDto) {
        val currentState = _messagesState.value
        if (currentState is MessageListUiState.Success) {
            val currentList = currentState.messages
            if (currentList.any { it.id == message.id }) return
            val updated = (currentList + message).sortedBy { it.sequence.toLongOrNull() ?: 0L }
            _messagesState.value = MessageListUiState.Success(updated)
        }
    }

    private fun syncMessagesSilently(conversationId: String) {
        viewModelScope.launch {
            val result = chatRepository.getMessages(conversationId = conversationId, pageSize = 50)
            if (result is NetworkResult.Success) {
                val sorted = result.data.data.sortedBy { it.sequence.toLongOrNull() ?: 0L }
                _messagesState.value = MessageListUiState.Success(sorted)
            }
        }
    }

    fun onRoomActive(conversationId: String) {
        activeConversationId = conversationId
        viewModelScope.launch {
            if (_currentUserId.value.isNullOrBlank()) {
                _currentUserId.value = tokenDataStore.userId.first()
            }
            val token = tokenDataStore.accessToken.first()
            if (!token.isNullOrBlank()) {
                CommunitySocketClient.ensureConnected(token)
                CommunitySocketClient.joinConversation(conversationId)
            }
        }
    }

    fun onRoomInactive(conversationId: String) {
        CommunitySocketClient.leaveConversation(conversationId)
    }

    fun loadConversationDetail(conversationId: String) {
        viewModelScope.launch {
            when (val result = chatRepository.getConversationDetail(conversationId)) {
                is NetworkResult.Success -> {
                    _conversationDetailState.value = result.data.data
                }
                else -> {}
            }
        }
    }

    fun loadReminderResources() {
        viewModelScope.launch {
            invoiceRepository?.let { repo ->
                val res = repo.getInvoices(pageSize = 50)
                if (res is NetworkResult.Success) {
                    _reminderInvoicesState.value = res.data.data
                }
            }
            contractRepository?.let { repo ->
                val res = repo.getContracts(pageSize = 50)
                if (res is NetworkResult.Success) {
                    _reminderContractsState.value = res.data.data
                }
            }
        }
    }

    fun loadMessages(conversationId: String) {
        activeConversationId = conversationId
        _messagesState.value = MessageListUiState.Loading
        viewModelScope.launch {
            if (_currentUserId.value.isNullOrBlank()) {
                _currentUserId.value = tokenDataStore.userId.first()
            }
            loadConversationDetail(conversationId)
            when (val result = chatRepository.getMessages(conversationId = conversationId, pageSize = 50)) {
                is NetworkResult.Success -> {
                    val sorted = result.data.data.sortedBy { it.sequence.toLongOrNull() ?: 0L }
                    _messagesState.value = MessageListUiState.Success(sorted)
                    sorted.lastOrNull()?.let { markRead(conversationId, it.sequence) }
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

    fun sendMessage(conversationId: String, content: String, replyToMessageId: String? = null) {
        if (content.isBlank()) return
        _sendState.value = SendMessageUiState.Sending
        viewModelScope.launch {
            val clientMsgId = UUID.randomUUID().toString()
            when (val result = chatRepository.sendMessage(
                conversationId = conversationId,
                content = content,
                type = "text",
                clientMessageId = clientMsgId,
                replyToMessageId = replyToMessageId
            )) {
                is NetworkResult.Success -> {
                    _sendState.value = SendMessageUiState.Sent
                    syncMessagesSilently(conversationId)
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

    fun sendImageMessage(
        conversationId: String,
        fileBytes: ByteArray,
        fileName: String,
        contentType: String,
        replyToMessageId: String? = null
    ) {
        val repo = mediaRepository ?: return
        _sendState.value = SendMessageUiState.Sending
        viewModelScope.launch {
            val uploadRes = repo.uploadMedia(
                ownerType = "conversation",
                ownerId = conversationId,
                purpose = "chat_attachment",
                fileBytes = fileBytes,
                fileName = fileName,
                contentType = contentType
            )
            when (uploadRes) {
                is NetworkResult.Success -> {
                    val clientMsgId = UUID.randomUUID().toString()
                    when (val msgRes = chatRepository.sendMessage(
                        conversationId = conversationId,
                        content = "",
                        type = "image",
                        clientMessageId = clientMsgId,
                        mediaId = uploadRes.data.id,
                        replyToMessageId = replyToMessageId
                    )) {
                        is NetworkResult.Success -> {
                            _sendState.value = SendMessageUiState.Sent
                            syncMessagesSilently(conversationId)
                        }
                        is NetworkResult.ApiError -> {
                            _sendState.value = SendMessageUiState.Error(
                                msgRes.error?.message ?: "Gửi ảnh thất bại"
                            )
                        }
                        else -> {
                            _sendState.value = SendMessageUiState.Error("Lỗi kết nối khi gửi tin nhắn ảnh")
                        }
                    }
                }
                is NetworkResult.ApiError -> {
                    _sendState.value = SendMessageUiState.Error(
                        uploadRes.error?.message ?: "Tải ảnh đính kèm thất bại"
                    )
                }
                else -> {
                    _sendState.value = SendMessageUiState.Error("Lỗi kết nối mạng khi tải ảnh")
                }
            }
        }
    }

    fun updateGroupTitle(conversationId: String, newTitle: String) {
        viewModelScope.launch {
            when (val res = chatRepository.updateConversationMetadata(conversationId, title = newTitle)) {
                is NetworkResult.Success -> {
                    loadConversationDetail(conversationId)
                    _botReminderState.value = BotReminderUiState.Success("Đã cập nhật tên nhóm thành công!")
                }
                is NetworkResult.ApiError -> {
                    _botReminderState.value = BotReminderUiState.Error(res.error?.message ?: "Đổi tên nhóm thất bại")
                }
                else -> {
                    _botReminderState.value = BotReminderUiState.Error("Lỗi kết nối khi đổi tên nhóm")
                }
            }
        }
    }

    fun removeGroupMember(conversationId: String, userId: String) {
        viewModelScope.launch {
            when (val res = chatRepository.removeConversationMember(conversationId, userId)) {
                is NetworkResult.Success -> {
                    loadConversationDetail(conversationId)
                    _botReminderState.value = BotReminderUiState.Success("Đã xóa thành viên khỏi nhóm")
                }
                is NetworkResult.ApiError -> {
                    _botReminderState.value = BotReminderUiState.Error(res.error?.message ?: "Không thể xóa thành viên")
                }
                else -> {
                    _botReminderState.value = BotReminderUiState.Error("Lỗi kết nối khi xóa thành viên")
                }
            }
        }
    }

    fun leaveGroupConversation(conversationId: String, onLeft: () -> Unit) {
        viewModelScope.launch {
            when (val res = chatRepository.leaveConversation(conversationId)) {
                is NetworkResult.Success -> {
                    onLeft()
                }
                is NetworkResult.ApiError -> {
                    _botReminderState.value = BotReminderUiState.Error(res.error?.message ?: "Không thể rời nhóm")
                }
                else -> {
                    _botReminderState.value = BotReminderUiState.Error("Lỗi kết nối khi rời nhóm")
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
                    syncMessagesSilently(conversationId)
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
