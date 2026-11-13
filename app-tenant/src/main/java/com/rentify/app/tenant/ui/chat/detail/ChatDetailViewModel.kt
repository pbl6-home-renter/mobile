package com.rentify.app.tenant.ui.chat.detail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.chat.MessageListResponseDto
import com.rentify.app.core.network.dto.chat.MessageReplyReferenceDto
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ChatDetailViewModel(
    val conversationId: String,
    private val chatRepository: TenantChatRepository,
    private val mediaRepository: TenantMediaRepository,
    private val tokenDataStore: TokenDataStore
) : BaseViewModel() {

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    private val _messages = MutableStateFlow<List<MessageListResponseDto>>(emptyList())
    val messages: StateFlow<List<MessageListResponseDto>> = _messages.asStateFlow()

    private val _isLoadingInitial = MutableStateFlow(false)
    val isLoadingInitial: StateFlow<Boolean> = _isLoadingInitial.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _replyTarget = MutableStateFlow<MessageListResponseDto?>(null)
    val replyTarget: StateFlow<MessageListResponseDto?> = _replyTarget.asStateFlow()

    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    private var nextCursor: String? = null
    private var hasNextPage: Boolean = false
    private var pollingJob: Job? = null

    init {
        loadInitial()
    }

    fun setReplyTarget(message: MessageListResponseDto) {
        _replyTarget.value = message
    }

    fun clearReplyTarget() {
        _replyTarget.value = null
    }

    fun loadInitial() {
        viewModelScope.launch {
            _currentUserId.value = tokenDataStore.userId.first()
            _isLoadingInitial.value = true

            launchSafe {
                val result = chatRepository.getMessages(conversationId = conversationId, cursor = null, pageSize = 30)
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

    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(3500)
                fetchNewMessages()
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun fetchNewMessages() {
        val result = chatRepository.getMessages(conversationId = conversationId, cursor = null, pageSize = 15)
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
        if (_isLoadingMore.value || !hasNextPage || nextCursor.isNullOrBlank()) return

        _isLoadingMore.value = true
        launchSafe {
            val result = chatRepository.getMessages(conversationId = conversationId, cursor = nextCursor, pageSize = 30)
            _isLoadingMore.value = false

            when (result) {
                is NetworkResult.Success -> {
                    val olderMessages = result.data.data
                    val pagination = result.data.pagination
                    nextCursor = pagination?.nextCursor
                    hasNextPage = pagination?.hasNextPage == true

                    _messages.value = _messages.value + olderMessages
                }
                else -> Unit
            }
        }
    }

    fun sendMessage(content: String) {
        val trimmed = content.trim()
        if (trimmed.isBlank() || _isSending.value) return

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
            conversationId = conversationId,
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
            val result = chatRepository.sendMessage(
                conversationId = conversationId,
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
            conversationId = conversationId,
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
                } catch (e: Exception) {
                    null
                }
            }

            if (bytes == null || bytes.isEmpty()) {
                _errorEvents.emit("Không thể đọc tệp ảnh đã chọn")
                _messages.value = _messages.value.filter { it.clientMessageId != clientMessageId }
                return@launch
            }

            val uploadResult = mediaRepository.uploadChatImage(conversationId, bytes)
            when (uploadResult) {
                is NetworkResult.Success -> {
                    val mediaDto = uploadResult.data
                    val sendResult = chatRepository.sendMessage(
                        conversationId = conversationId,
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
        viewModelScope.launch {
            chatRepository.markMessagesAsRead(conversationId, sequence)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
