package com.rentify.app.tenant.data.repository

import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.chat.ConversationDetailResponseDto
import com.rentify.app.core.network.dto.chat.ConversationListResponseDto
import com.rentify.app.core.network.dto.chat.ConversationMemberDto
import com.rentify.app.core.network.dto.chat.ConversationMetadataUpdateResponseDto
import com.rentify.app.core.network.dto.chat.ConversationUnreadSummaryResponseDto
import com.rentify.app.core.network.dto.chat.MessageListResponseDto
import com.rentify.app.core.network.dto.chat.MessageMarkReadResponseDto
import com.rentify.app.core.network.dto.chat.MessageSearchResponseDto
import com.rentify.app.core.network.dto.chat.MessageSendResponseDto

interface TenantChatRepository {

    suspend fun getConversations(
        pageNo: Int = 1,
        pageSize: Int = 20,
        kind: String? = null,
        hasContext: Boolean? = null,
        q: String? = null,
        otherUserId: String? = null,
        contextType: String? = null,
        contextId: String? = null
    ): NetworkResult<ApiListResponse<ConversationListResponseDto>>

    suspend fun getConversationsUnreadSummary(): NetworkResult<ApiSingleResponse<ConversationUnreadSummaryResponseDto>>

    suspend fun getOrCreateDirectConversation(
        recipientUserId: String
    ): NetworkResult<ApiSingleResponse<ConversationDetailResponseDto>>

    suspend fun createGroupConversation(
        title: String?,
        memberUserIds: List<String>
    ): NetworkResult<ApiSingleResponse<ConversationDetailResponseDto>>

    suspend fun getConversationDetail(
        conversationId: String
    ): NetworkResult<ApiSingleResponse<ConversationDetailResponseDto>>

    suspend fun updateConversationMetadata(
        conversationId: String,
        title: String? = null,
        avatarMediaId: String? = null
    ): NetworkResult<ApiSingleResponse<ConversationMetadataUpdateResponseDto>>

    suspend fun getConversationMembers(
        conversationId: String
    ): NetworkResult<ApiSingleResponse<List<ConversationMemberDto>>>

    suspend fun addConversationMembers(
        conversationId: String,
        userIds: List<String>
    ): NetworkResult<ApiSingleResponse<List<ConversationMemberDto>>>

    suspend fun removeConversationMember(
        conversationId: String,
        userId: String
    ): NetworkResult<ApiSingleResponse<Any?>>

    suspend fun updateMemberRole(
        conversationId: String,
        userId: String,
        role: String
    ): NetworkResult<ApiSingleResponse<Any?>>

    suspend fun leaveConversation(
        conversationId: String
    ): NetworkResult<ApiSingleResponse<Any?>>

    suspend fun getMessages(
        conversationId: String,
        cursor: String? = null,
        pageSize: Int = 30,
        maxSequence: String? = null
    ): NetworkResult<ApiListResponse<MessageListResponseDto>>

    suspend fun searchMessages(
        conversationId: String,
        query: String,
        cursor: String? = null,
        pageSize: Int = 30
    ): NetworkResult<ApiListResponse<MessageSearchResponseDto>>

    suspend fun sendMessage(
        conversationId: String,
        content: String,
        type: String = "text",
        clientMessageId: String,
        mediaId: String? = null,
        replyToMessageId: String? = null
    ): NetworkResult<ApiSingleResponse<MessageSendResponseDto>>

    suspend fun markMessagesAsRead(
        conversationId: String,
        lastReadSequence: String
    ): NetworkResult<ApiSingleResponse<MessageMarkReadResponseDto>>
}
