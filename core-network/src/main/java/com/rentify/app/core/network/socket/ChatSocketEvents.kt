package com.rentify.app.core.network.socket

import com.rentify.app.core.network.dto.chat.MessageListResponseDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SocketSessionReadyDto(
    @field:Json(name = "userId") val userId: String
)

@JsonClass(generateAdapter = true)
data class SocketJoinLeaveAckDto(
    @field:Json(name = "ok") val ok: Boolean = false,
    @field:Json(name = "code") val code: String? = null,
    @field:Json(name = "conversationId") val conversationId: String? = null
)

@JsonClass(generateAdapter = true)
data class SocketConversationIdDto(
    @field:Json(name = "conversationId") val conversationId: String
)

@JsonClass(generateAdapter = true)
data class SocketReadUpdatedDto(
    @field:Json(name = "conversationId") val conversationId: String,
    @field:Json(name = "unreadCount") val unreadCount: Int = 0,
    @field:Json(name = "lastReadSequence") val lastReadSequence: String = "0"
)

@JsonClass(generateAdapter = true)
data class SocketMemberReadDto(
    @field:Json(name = "conversationId") val conversationId: String,
    @field:Json(name = "userId") val userId: String,
    @field:Json(name = "lastReadSequence") val lastReadSequence: String = "0"
)

@JsonClass(generateAdapter = true)
data class SocketTypingDto(
    @field:Json(name = "conversationId") val conversationId: String,
    @field:Json(name = "userId") val userId: String? = null,
    @field:Json(name = "isTyping") val isTyping: Boolean = false
)

@JsonClass(generateAdapter = true)
data class SocketMessageReceivedDto(
    @field:Json(name = "messageId") val messageId: String,
    @field:Json(name = "conversationId") val conversationId: String,
    @field:Json(name = "sequence") val sequence: String = "0",
    @field:Json(name = "senderUserId") val senderUserId: String? = null,
    @field:Json(name = "senderName") val senderName: String = "",
    @field:Json(name = "senderAvatarUrl") val senderAvatarUrl: String? = null,
    @field:Json(name = "conversationName") val conversationName: String? = null,
    @field:Json(name = "type") val type: String = "text",
    @field:Json(name = "contentPreview") val contentPreview: String = "",
    @field:Json(name = "createdAt") val createdAt: String? = null
)

sealed interface CommunitySocketEvent {
    data class SessionReady(val userId: String, val isReconnect: Boolean) : CommunitySocketEvent
    data class MessageCreated(val message: MessageListResponseDto) : CommunitySocketEvent
    data class ChatMessageReceived(val payload: SocketMessageReceivedDto) : CommunitySocketEvent
    data class ConversationReadUpdated(val payload: SocketReadUpdatedDto) : CommunitySocketEvent
    data class ConversationMemberRead(val payload: SocketMemberReadDto) : CommunitySocketEvent
    data class ConversationTyping(val payload: SocketTypingDto) : CommunitySocketEvent
    data class ConversationAccessRevoked(val conversationId: String) : CommunitySocketEvent
    data class ConversationAvailable(val conversationId: String) : CommunitySocketEvent
}
