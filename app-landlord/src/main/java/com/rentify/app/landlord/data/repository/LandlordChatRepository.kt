package com.rentify.app.landlord.data.repository

import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.chat.ConversationDetailResponseDto
import com.rentify.app.core.network.dto.chat.ConversationListResponseDto
import com.rentify.app.core.network.dto.chat.MessageListResponseDto
import com.rentify.app.core.network.dto.chat.MessageMarkReadResponseDto
import com.rentify.app.core.network.dto.chat.MessageSendResponseDto

interface LandlordChatRepository {
    suspend fun getConversations(
        pageNo: Int = 1,
        pageSize: Int = 50,
        kind: String? = null,
        q: String? = null
    ): NetworkResult<ApiListResponse<ConversationListResponseDto>>

    suspend fun getConversationDetail(
        conversationId: String
    ): NetworkResult<ApiSingleResponse<ConversationDetailResponseDto>>

    suspend fun getMessages(
        conversationId: String,
        cursor: String? = null,
        pageSize: Int = 30,
        maxSequence: String? = null
    ): NetworkResult<ApiListResponse<MessageListResponseDto>>

    suspend fun sendMessage(
        conversationId: String,
        content: String,
        type: String = "text",
        clientMessageId: String,
        mediaId: String? = null,
        replyToMessageId: String? = null
    ): NetworkResult<ApiSingleResponse<MessageSendResponseDto>>

    suspend fun sendBotReminder(
        conversationId: String,
        templateType: String,
        resourceId: String,
        clientMessageId: String
    ): NetworkResult<ApiSingleResponse<Any?>>

    suspend fun markMessagesAsRead(
        conversationId: String,
        lastReadSequence: String
    ): NetworkResult<ApiSingleResponse<MessageMarkReadResponseDto>>
}

