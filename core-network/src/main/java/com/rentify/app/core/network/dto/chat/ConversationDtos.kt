package com.rentify.app.core.network.dto.chat

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ConversationListResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "kind") val kind: String,
    @field:Json(name = "contextType") val contextType: String? = null,
    @field:Json(name = "contextId") val contextId: String? = null,
    @field:Json(name = "title") val title: String? = null,
    @field:Json(name = "displayName") val displayName: String? = null,
    @field:Json(name = "avatarMediaId") val avatarMediaId: String? = null,
    @field:Json(name = "unreadCount") val unreadCount: Int = 0,
    @field:Json(name = "lastMessage") val lastMessage: ConversationLastMessageDto? = null
)

@JsonClass(generateAdapter = true)
data class ConversationLastMessageDto(
    @field:Json(name = "id") val id: String? = null,
    @field:Json(name = "sequence") val sequence: String? = null,
    @field:Json(name = "senderName") val senderName: String? = null,
    @field:Json(name = "contentPreview") val contentPreview: String? = null,
    @field:Json(name = "type") val type: String? = null,
    @field:Json(name = "sentAt") val sentAt: String? = null,
    @field:Json(name = "payload") val payload: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class ConversationDetailResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "kind") val kind: String,
    @field:Json(name = "contextType") val contextType: String? = null,
    @field:Json(name = "contextId") val contextId: String? = null,
    @field:Json(name = "title") val title: String? = null,
    @field:Json(name = "displayName") val displayName: String? = null,
    @field:Json(name = "avatarMediaId") val avatarMediaId: String? = null,
    @field:Json(name = "unreadCount") val unreadCount: Int = 0,
    @field:Json(name = "members") val members: List<ConversationMemberDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ConversationMemberDto(
    @field:Json(name = "conversationId") val conversationId: String? = null,
    @field:Json(name = "userId") val userId: String? = null,
    @field:Json(name = "displayName") val displayName: String = "",
    @field:Json(name = "participantType") val participantType: String = "user",
    @field:Json(name = "role") val role: String = "member",
    @field:Json(name = "status") val status: String = "active",
    @field:Json(name = "joinedAt") val joinedAt: String? = null,
    @field:Json(name = "leftAt") val leftAt: String? = null,
    @field:Json(name = "leftSequence") val leftSequence: String? = null,
    @field:Json(name = "lastReadSequence") val lastReadSequence: String = "0"
)

@JsonClass(generateAdapter = true)
data class ConversationCreateRequestDto(
    @field:Json(name = "kind") val kind: String,
    @field:Json(name = "recipientUserId") val recipientUserId: String? = null,
    @field:Json(name = "memberUserIds") val memberUserIds: List<String>? = null,
    @field:Json(name = "title") val title: String? = null
)

@JsonClass(generateAdapter = true)
data class ConversationMetadataUpdateRequestDto(
    @field:Json(name = "title") val title: String? = null,
    @field:Json(name = "avatarMediaId") val avatarMediaId: String? = null
)

@JsonClass(generateAdapter = true)
data class ConversationMetadataUpdateResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "kind") val kind: String,
    @field:Json(name = "contextType") val contextType: String? = null,
    @field:Json(name = "contextId") val contextId: String? = null,
    @field:Json(name = "title") val title: String? = null,
    @field:Json(name = "avatarMediaId") val avatarMediaId: String? = null
)

@JsonClass(generateAdapter = true)
data class ConversationUnreadSummaryResponseDto(
    @field:Json(name = "totalUnreadCount") val totalUnreadCount: Int = 0
)

