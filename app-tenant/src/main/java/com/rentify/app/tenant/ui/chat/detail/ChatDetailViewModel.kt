package com.rentify.app.tenant.ui.chat.detail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.chat.ConversationDetailResponseDto
import com.rentify.app.core.network.dto.chat.ConversationMemberDto
import com.rentify.app.core.network.dto.chat.MessageListResponseDto
import com.rentify.app.core.network.dto.chat.MessageReplyReferenceDto
import com.rentify.app.core.network.socket.CommunitySocketClient
import com.rentify.app.core.network.socket.CommunitySocketEvent
import com.rentify.app.core.ui.base.BaseViewModel
import com.rentify.app.tenant.data.repository.TenantChatRepository
import com.rentify.app.tenant.data.repository.TenantMediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ChatDetailViewModel(
    initialConversationId: String,
    private val recipientUserId: String? = null,
    private val chatRepository: TenantChatRepository,
    private val mediaRepository: TenantMediaRepository,
    private val tokenDataStore: TokenDataStore
) : BaseViewModel() {

    private val _activeConversationId = MutableStateFlow(initialConversationId)
    val activeConversationId: StateFlow<String> = _activeConversationId.asStateFlow()

    val conversationId: String
        get() = _activeConversationId.value

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    private val _conversationDetail = MutableStateFlow<ConversationDetailResponseDto?>(null)
    val conversationDetail: StateFlow<ConversationDetailResponseDto?> = _conversationDetail.asStateFlow()

    private val _members = MutableStateFlow<List<ConversationMemberDto>>(emptyList())
    val members: StateFlow<List<ConversationMemberDto>> = _members.asStateFlow()

    private val _messages = MutableStateFlow<List<MessageListResponseDto>>(emptyList())
    val messages: StateFlow<List<MessageListResponseDto>> = _messages.asStateFlow()

    private val _isLoadingInitial = MutableStateFlow(false)
    val isLoadingInitial: StateFlow<Boolean> = _isLoadingInitial.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _isOtherTyping = MutableStateFlow(false)
    val isOtherTyping: StateFlow<Boolean> = _isOtherTyping.asStateFlow()

    private val _replyTarget = MutableStateFlow<MessageListResponseDto?>(null)
    val replyTarget: StateFlow<MessageListResponseDto?> = _replyTarget.asStateFlow()

    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    private var nextCursor: String? = null
    private var hasNextPage: Boolean = false
    private var typingTimeoutJob: Job? = null

    init {
        observeSocketEvents()
        loadInitial()
    }

    private fun observeSocketEvents() {
        viewModelScope.launch {
            CommunitySocketClient.events.collect { event ->
                val convId = _activeConversationId.value
                if (convId.isBlank()) return@collect
                when (event) {
                    is CommunitySocketEvent.SessionReady -> {
                        if (event.userId.isNotBlank()) {
                            _currentUserId.value = event.userId
                        }
                        if (event.isReconnect) {
                            _isOtherTyping.value = false
                            syncMissedMessages()
                            refreshConversationDetail()
                        }
                    }
                    is CommunitySocketEvent.MessageCreated -> {
                        if (event.message.conversationId == convId) {
                            onSocketMessageCreated(event.message)
                        }
                    }
                    is CommunitySocketEvent.ConversationMemberRead -> {
                        val payload = event.payload
                        if (payload.conversationId == convId) {
                            onSocketMemberRead(payload.userId, payload.lastReadSequence)
                        }
                    }
                    is CommunitySocketEvent.ConversationTyping -> {
                        val payload = event.payload
                        if (payload.conversationId == convId && payload.userId != _currentUserId.value) {
                            handleRemoteTyping(payload.isTyping)
                        }
                    }
                    is CommunitySocketEvent.ConversationAccessRevoked -> {
                        if (event.conversationId == convId) {
                            _isOtherTyping.value = false
                            _errorEvents.emit("Quyền truy cập cuộc trò chuyện đã bị thu hồi")
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun onSocketMemberRead(userId: String, lastReadSequence: String) {
        val incomingSeq = lastReadSequence.toLongOrNull() ?: return
        val currentMembers = _members.value
        if (currentMembers.isEmpty()) return

        var changed = false
        val updated = currentMembers.map { m ->
            if (m.userId == userId) {
                val existingSeq = m.lastReadSequence.toLongOrNull() ?: 0L
                if (incomingSeq > existingSeq) {
                    changed = true
                    m.copy(lastReadSequence = lastReadSequence)
                } else {
                    m
                }
            } else {
                m
            }
        }
        if (changed) {
            _members.value = updated
        }
    }

    private fun onSocketMessageCreated(incoming: MessageListResponseDto) {
        val currentList = _messages.value

        val hasSameId = currentList.any { it.id == incoming.id }
        if (hasSameId) return

        val hasSameClientMsgId = !incoming.clientMessageId.isNullOrBlank() &&
            currentList.any { it.clientMessageId == incoming.clientMessageId }

        if (hasSameClientMsgId) {
            _messages.value = currentList.map { item ->
                if (item.clientMessageId == incoming.clientMessageId) {
                    incoming.copy(
                        content = if (incoming.type == "image" && incoming.content.isNullOrBlank()) item.content else incoming.content
                    )
                } else {
                    item
                }
            }
        } else {
            _messages.value = listOf(incoming) + currentList
        }

        if (incoming.senderUserId != _currentUserId.value) {
            _isOtherTyping.value = false
            typingTimeoutJob?.cancel()
        }

        val seqLong = incoming.sequence.toLongOrNull() ?: 0L
        if (seqLong > 0L) {
            markRead(incoming.sequence)
        }
    }

    private fun handleRemoteTyping(isTyping: Boolean) {
        typingTimeoutJob?.cancel()
        _isOtherTyping.value = isTyping
        if (isTyping) {
            typingTimeoutJob = viewModelScope.launch {
                delay(4000L)
                _isOtherTyping.value = false
            }
        }
    }

    fun onRoomActive() {
        viewModelScope.launch {
            val token = tokenDataStore.accessToken.first()
            if (!token.isNullOrBlank()) {
                CommunitySocketClient.ensureConnected(token)
                val convId = _activeConversationId.value
                if (convId.isNotBlank()) {
                    CommunitySocketClient.joinConversation(convId)
                }
            }
        }
    }

    fun onRoomInactive() {
        typingTimeoutJob?.cancel()
        _isOtherTyping.value = false
        val convId = _activeConversationId.value
        if (convId.isNotBlank()) {
            CommunitySocketClient.sendTyping(convId, false)
            CommunitySocketClient.leaveConversation(convId)
        }
    }

    fun onInputTypingChanged(hasText: Boolean) {
        val convId = _activeConversationId.value
        if (convId.isNotBlank()) {
            CommunitySocketClient.sendTyping(convId, hasText)
        }
    }

    fun setReplyTarget(message: MessageListResponseDto) {
        _replyTarget.value = message
    }

    fun clearReplyTarget() {
        _replyTarget.value = null
    }

    fun refreshConversationDetail() {
        val convId = _activeConversationId.value
        if (convId.isBlank()) return
        viewModelScope.launch {
            val detailResult = chatRepository.getConversationDetail(convId)
            if (detailResult is NetworkResult.Success) {
                val detail = detailResult.data.data
                _conversationDetail.value = detail
                if (detail != null) {
                    _members.value = detail.members
                }
            }
        }
    }

    fun loadInitial() {
        viewModelScope.launch {
            _currentUserId.value = tokenDataStore.userId.first()
            val token = tokenDataStore.accessToken.first()
            if (!token.isNullOrBlank()) {
                CommunitySocketClient.ensureConnected(token)
            }

            val convId = _activeConversationId.value
            if (convId.isBlank()) {
                // Chế độ chat 1-1 trống: chưa tạo hội thoại trên server cho đến khi gửi tin đầu tiên
                _isLoadingInitial.value = false
                _messages.value = emptyList()
                return@launch
            }

            _isLoadingInitial.value = true
            refreshConversationDetail()

            launchSafe {
                val result = chatRepository.getMessages(conversationId = convId, cursor = null, pageSize = 30)
                _isLoadingInitial.value = false

                when (result) {
                    is NetworkResult.Success -> {
                        val data = result.data.data
                        val pagination = result.data.pagination
                        nextCursor = pagination?.nextCursor
                        hasNextPage = pagination?.hasNextPage == true

                        _messages.value = data

                        val maxSeq = data.mapNotNull { it.sequence.toLongOrNull() }.maxOrNull()
                        if (maxSeq != null && maxSeq > 0) {
                            markRead(maxSeq.toString())
                        }
                    }
                    is NetworkResult.ApiError -> {
                        _errorEvents.emit(result.error?.message ?: "Lỗi khi tải tin nhắn")
                    }
                    is NetworkResult.NetworkError -> {
                        _errorEvents.emit("Không có kết nối mạng. Vui lòng kiểm tra lại.")
                    }
                    is NetworkResult.UnknownError -> {
                        _errorEvents.emit("Đã xảy ra lỗi không xác định.")
                    }
                }
            }
        }
    }

    private suspend fun syncMissedMessages() {
        val convId = _activeConversationId.value
        if (convId.isBlank()) return
        val result = chatRepository.getMessages(conversationId = convId, cursor = null, pageSize = 30)
        if (result is NetworkResult.Success) {
            val serverList = result.data.data
            val currentList = _messages.value
            val currentIds = currentList.map { it.id }.toSet()
            val currentClientIds = currentList.mapNotNull { it.clientMessageId }.toSet()

            val newMessages = serverList.filter { msg ->
                !currentIds.contains(msg.id) && (msg.clientMessageId == null || !currentClientIds.contains(msg.clientMessageId))
            }

            if (newMessages.isNotEmpty()) {
                _messages.value = newMessages + currentList
                val maxSeq = newMessages.mapNotNull { it.sequence.toLongOrNull() }.maxOrNull()
                if (maxSeq != null && maxSeq > 0) {
                    markRead(maxSeq.toString())
                }
            }
        }
    }

    fun loadMore() {
        val convId = _activeConversationId.value
        if (convId.isBlank() || _isLoadingMore.value || !hasNextPage || nextCursor.isNullOrBlank()) return

        _isLoadingMore.value = true
        launchSafe {
            val result = chatRepository.getMessages(conversationId = convId, cursor = nextCursor, pageSize = 30)
            _isLoadingMore.value = false

            when (result) {
                is NetworkResult.Success -> {
                    val olderMessages = result.data.data
                    val pagination = result.data.pagination
                    nextCursor = pagination?.nextCursor
                    hasNextPage = pagination?.hasNextPage == true

                    val existingIds = _messages.value.map { it.id }.toSet()
                    val deduplicated = olderMessages.filter { !existingIds.contains(it.id) }
                    _messages.value = _messages.value + deduplicated
                }
                else -> Unit
            }
        }
    }

    private suspend fun ensureConversationCreatedIfNeeded(): String? {
        val currentId = _activeConversationId.value
        if (currentId.isNotBlank()) return currentId

        val targetUserId = recipientUserId?.takeIf { it.isNotBlank() } ?: return null
        val createResult = chatRepository.getOrCreateDirectConversation(targetUserId)
        return when (createResult) {
            is NetworkResult.Success -> {
                val detail = createResult.data.data
                val resolvedId = detail?.id
                if (!resolvedId.isNullOrBlank()) {
                    _activeConversationId.value = resolvedId
                    _conversationDetail.value = detail
                    _members.value = detail.members
                    CommunitySocketClient.joinConversation(resolvedId)
                    resolvedId
                } else {
                    _errorEvents.emit("Không thể khởi tạo cuộc trò chuyện")
                    null
                }
            }
            is NetworkResult.ApiError -> {
                _errorEvents.emit(createResult.error?.message ?: "Không thể khởi tạo cuộc trò chuyện")
                null
            }
            is NetworkResult.NetworkError -> {
                _errorEvents.emit("Lỗi kết nối mạng khi tạo cuộc trò chuyện")
                null
            }
            is NetworkResult.UnknownError -> {
                _errorEvents.emit("Không thể khởi tạo cuộc trò chuyện")
                null
            }
        }
    }

    fun sendMessage(content: String) {
        val trimmed = content.trim()
        if (trimmed.isBlank() || _isSending.value) return

        val existingConvId = _activeConversationId.value
        if (existingConvId.isNotBlank()) {
            CommunitySocketClient.sendTyping(existingConvId, false)
        }

        val clientMessageId = UUID.randomUUID().toString()
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())

        val replyTargetSnapshot = _replyTarget.value
        val replyToId = replyTargetSnapshot?.id
        val optimisticReply = if (replyTargetSnapshot != null) {
            val previewText = when (replyTargetSnapshot.type.lowercase()) {
                "image" -> "[Hình ảnh]"
                else -> replyTargetSnapshot.content ?: ""
            }
            MessageReplyReferenceDto(
                id = replyTargetSnapshot.id,
                sequence = replyTargetSnapshot.sequence,
                senderName = replyTargetSnapshot.senderName,
                type = replyTargetSnapshot.type,
                contentPreview = previewText.take(117).let { if (previewText.length > 117) "$it..." else it }
            )
        } else null

        val optimisticMessage = MessageListResponseDto(
            id = clientMessageId,
            conversationId = existingConvId,
            senderUserId = _currentUserId.value,
            senderName = "Tôi",
            senderType = "user",
            sequence = ((_messages.value.mapNotNull { it.sequence.toLongOrNull() }.maxOrNull() ?: 0L) + 1L).toString(),
            clientMessageId = clientMessageId,
            type = "text",
            content = trimmed,
            replyTo = optimisticReply,
            createdAt = nowIso
        )

        _replyTarget.value = null
        _messages.value = listOf(optimisticMessage) + _messages.value
        _isSending.value = true

        viewModelScope.launch {
            val resolvedConvId = ensureConversationCreatedIfNeeded()
            if (resolvedConvId.isNullOrBlank()) {
                _isSending.value = false
                _messages.value = _messages.value.filter { it.clientMessageId != clientMessageId }
                return@launch
            }

            val result = chatRepository.sendMessage(
                conversationId = resolvedConvId,
                content = trimmed,
                type = "text",
                clientMessageId = clientMessageId,
                replyToMessageId = replyToId
            )
            _isSending.value = false

            when (result) {
                is NetworkResult.Success -> {
                    val serverMsg = result.data.data ?: return@launch
                    val updatedList = _messages.value.map { item ->
                        if (item.clientMessageId == clientMessageId) {
                            item.copy(
                                id = serverMsg.id,
                                conversationId = resolvedConvId,
                                sequence = serverMsg.sequence,
                                createdAt = serverMsg.createdAt
                            )
                        } else {
                            item
                        }
                    }
                    _messages.value = updatedList
                    markRead(serverMsg.sequence)
                }
                is NetworkResult.ApiError -> {
                    _errorEvents.emit(result.error?.message ?: "Gửi tin nhắn thất bại")
                }
                is NetworkResult.NetworkError -> {
                    _errorEvents.emit("Lỗi kết nối mạng khi gửi tin nhắn")
                }
                is NetworkResult.UnknownError -> {
                    _errorEvents.emit("Không thể gửi tin nhắn")
                }
            }
        }
    }

    fun sendImageMessage(uri: Uri, context: Context) {
        val clientMessageId = UUID.randomUUID().toString()
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())

        val replyTargetSnapshot = _replyTarget.value
        val replyToId = replyTargetSnapshot?.id
        val optimisticReply = if (replyTargetSnapshot != null) {
            val previewText = when (replyTargetSnapshot.type.lowercase()) {
                "image" -> "[Hình ảnh]"
                else -> replyTargetSnapshot.content ?: ""
            }
            MessageReplyReferenceDto(
                id = replyTargetSnapshot.id,
                sequence = replyTargetSnapshot.sequence,
                senderName = replyTargetSnapshot.senderName,
                type = replyTargetSnapshot.type,
                contentPreview = previewText.take(117).let { if (previewText.length > 117) "$it..." else it }
            )
        } else null

        val optimisticMessage = MessageListResponseDto(
            id = clientMessageId,
            conversationId = _activeConversationId.value,
            senderUserId = _currentUserId.value,
            senderName = "Tôi",
            senderType = "user",
            sequence = ((_messages.value.mapNotNull { it.sequence.toLongOrNull() }.maxOrNull() ?: 0L) + 1L).toString(),
            clientMessageId = clientMessageId,
            type = "image",
            content = uri.toString(),
            mediaId = null,
            replyTo = optimisticReply,
            createdAt = nowIso
        )

        _replyTarget.value = null
        _messages.value = listOf(optimisticMessage) + _messages.value

        viewModelScope.launch {
            val resolvedConvId = ensureConversationCreatedIfNeeded()
            if (resolvedConvId.isNullOrBlank()) {
                _messages.value = _messages.value.filter { it.clientMessageId != clientMessageId }
                return@launch
            }

            val bytes = withContext(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    if (bitmap == null) return@withContext null

                    val maxDim = 1400
                    val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                        val ratio = kotlin.math.min(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
                        Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
                    } else {
                        bitmap
                    }
                    val out = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
                    out.toByteArray()
                } catch (_: Exception) {
                    null
                }
            }

            if (bytes == null || bytes.isEmpty()) {
                _errorEvents.emit("Không thể đọc tệp ảnh đã chọn")
                _messages.value = _messages.value.filter { it.clientMessageId != clientMessageId }
                return@launch
            }

            val uploadResult = mediaRepository.uploadChatImage(resolvedConvId, bytes)
            when (uploadResult) {
                is NetworkResult.Success -> {
                    val mediaDto = uploadResult.data
                    val sendResult = chatRepository.sendMessage(
                        conversationId = resolvedConvId,
                        content = "",
                        type = "image",
                        clientMessageId = clientMessageId,
                        mediaId = mediaDto.id,
                        replyToMessageId = replyToId
                    )

                    when (sendResult) {
                        is NetworkResult.Success -> {
                            val serverMsg = sendResult.data.data
                            _messages.value = _messages.value.map { item ->
                                if (item.clientMessageId == clientMessageId) {
                                    item.copy(
                                        id = serverMsg?.id ?: item.id,
                                        conversationId = resolvedConvId,
                                        mediaId = mediaDto.id,
                                        sequence = serverMsg?.sequence ?: item.sequence,
                                        createdAt = serverMsg?.createdAt ?: item.createdAt
                                    )
                                } else {
                                    item
                                }
                            }
                            if (serverMsg?.sequence != null) {
                                markRead(serverMsg.sequence)
                            }
                        }
                        is NetworkResult.ApiError -> {
                            _errorEvents.emit(sendResult.error?.message ?: "Lỗi khi gửi tin nhắn ảnh")
                            _messages.value = _messages.value.filter { it.clientMessageId != clientMessageId }
                        }
                        else -> {
                            _errorEvents.emit("Không thể gửi tin nhắn ảnh")
                            _messages.value = _messages.value.filter { it.clientMessageId != clientMessageId }
                        }
                    }
                }
                is NetworkResult.ApiError -> {
                    _errorEvents.emit(uploadResult.error?.message ?: "Lỗi khi tải ảnh lên máy chủ")
                    _messages.value = _messages.value.filter { it.clientMessageId != clientMessageId }
                }
                is NetworkResult.NetworkError -> {
                    _errorEvents.emit("Lỗi kết nối khi tải ảnh")
                    _messages.value = _messages.value.filter { it.clientMessageId != clientMessageId }
                }
                is NetworkResult.UnknownError -> {
                    _errorEvents.emit("Tải ảnh thất bại")
                    _messages.value = _messages.value.filter { it.clientMessageId != clientMessageId }
                }
            }
        }
    }

    private fun markRead(sequence: String) {
        val convId = _activeConversationId.value
        if (convId.isBlank()) return
        viewModelScope.launch {
            chatRepository.markMessagesAsRead(convId, sequence)
        }
    }

    override fun onCleared() {
        super.onCleared()
        onRoomInactive()
    }

    companion object {
        /**
         * Hàm dùng chung để lấy tên nhóm đồng nhất giữa Header màn Chat và Sheet Thông tin nhóm.
         */
        fun resolveUnifiedGroupTitle(
            detail: ConversationDetailResponseDto?,
            fallbackTitle: String? = null
        ): String {
            return detail?.title?.takeIf { it.isNotBlank() }
                ?: detail?.displayName?.takeIf { it.isNotBlank() }
                ?: fallbackTitle?.takeIf { it.isNotBlank() }
                ?: "Nhóm trao đổi"
        }
    }

    suspend fun getMediaDownloadUrl(mediaId: String): String? {
        return (mediaRepository.getMediaDownloadUrl(mediaId) as? NetworkResult.Success)?.data
    }
}
