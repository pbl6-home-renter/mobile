package com.rentify.app.core.network.dto.chat

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MessageReplyReferenceDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "sequence") val sequence: String? = null,
    @field:Json(name = "senderName") val senderName: String? = null,
    @field:Json(name = "type") val type: String? = null,
    @field:Json(name = "contentPreview") val contentPreview: String? = null,
    @field:Json(name = "isUnavailable") val isUnavailable: Boolean = false
)

@JsonClass(generateAdapter = true)
data class MessageListResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "conversationId") val conversationId: String,
    @field:Json(name = "senderUserId") val senderUserId: String? = null,
    @field:Json(name = "senderName") val senderName: String = "",
    @field:Json(name = "senderType") val senderType: String = "user",
    @field:Json(name = "sequence") val sequence: String = "0",
    @field:Json(name = "clientMessageId") val clientMessageId: String? = null,
    @field:Json(name = "type") val type: String = "text",
    @field:Json(name = "content") val content: String? = null,
    @field:Json(name = "mediaId") val mediaId: String? = null,
    @field:Json(name = "replyTo") val replyTo: MessageReplyReferenceDto? = null,
    @field:Json(name = "createdAt") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class MessageSendRequestDto(
    @field:Json(name = "conversationId") val conversationId: String,
    @field:Json(name = "clientMessageId") val clientMessageId: String,
    @field:Json(name = "type") val type: String = "text",
    @field:Json(name = "content") val content: String? = null,
    @field:Json(name = "mediaId") val mediaId: String? = null,
    @field:Json(name = "replyToMessageId") val replyToMessageId: String? = null
)

@JsonClass(generateAdapter = true)
data class MessageSendResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "conversationId") val conversationId: String,
    @field:Json(name = "senderName") val senderName: String = "",
    @field:Json(name = "type") val type: String = "text",
    @field:Json(name = "content") val content: String? = null,
    @field:Json(name = "mediaId") val mediaId: String? = null,
    @field:Json(name = "sequence") val sequence: String = "0",
    @field:Json(name = "clientMessageId") val clientMessageId: String? = null,
    @field:Json(name = "replyTo") val replyTo: MessageReplyReferenceDto? = null,
    @field:Json(name = "createdAt") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class MessageSearchResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "sequence") val sequence: String,
    @field:Json(name = "senderName") val senderName: String = "",
    @field:Json(name = "type") val type: String = "text",
    @field:Json(name = "contentPreview") val contentPreview: String? = null,
    @field:Json(name = "createdAt") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class MessageMarkReadRequestDto(
    @field:Json(name = "conversationId") val conversationId: String,
    @field:Json(name = "lastReadSequence") val lastReadSequence: String
)

@JsonClass(generateAdapter = true)
data class MessageMarkReadResponseDto(
    @field:Json(name = "conversationId") val conversationId: String,
    @field:Json(name = "unreadCount") val unreadCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class BotReminderSendRequestDto(
    @field:Json(name = "templateType") val templateType: String,
    @field:Json(name = "resourceId") val resourceId: String,
    @field:Json(name = "clientMessageId") val clientMessageId: String
)

@JsonClass(generateAdapter = true)
data class ConversationMemberAddRequestDto(
    @field:Json(name = "userIds") val userIds: List<String>
)

@JsonClass(generateAdapter = true)
data class ConversationMemberRoleUpdateRequestDto(
    @field:Json(name = "role") val role: String
)

