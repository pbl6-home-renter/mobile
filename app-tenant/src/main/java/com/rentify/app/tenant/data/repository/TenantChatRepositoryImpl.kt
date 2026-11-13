package com.rentify.app.tenant.data.repository

import com.rentify.app.core.data.repository.BaseRepository
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.api.ChatApiService
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.chat.ConversationCreateRequestDto
import com.rentify.app.core.network.dto.chat.ConversationDetailResponseDto
import com.rentify.app.core.network.dto.chat.ConversationListResponseDto
import com.rentify.app.core.network.dto.chat.ConversationMemberAddRequestDto
import com.rentify.app.core.network.dto.chat.ConversationMemberDto
import com.rentify.app.core.network.dto.chat.ConversationMemberRoleUpdateRequestDto
import com.rentify.app.core.network.dto.chat.ConversationMetadataUpdateRequestDto
import com.rentify.app.core.network.dto.chat.ConversationMetadataUpdateResponseDto
import com.rentify.app.core.network.dto.chat.ConversationUnreadSummaryResponseDto
import com.rentify.app.core.network.dto.chat.MessageListResponseDto
import com.rentify.app.core.network.dto.chat.MessageMarkReadRequestDto
import com.rentify.app.core.network.dto.chat.MessageMarkReadResponseDto
import com.rentify.app.core.network.dto.chat.MessageSearchResponseDto
import com.rentify.app.core.network.dto.chat.MessageSendRequestDto
import com.rentify.app.core.network.dto.chat.MessageSendResponseDto

class TenantChatRepositoryImpl(
    private val chatApiService: ChatApiService
) : BaseRepository(), TenantChatRepository {

    override suspend fun getConversations(
        pageNo: Int,
        pageSize: Int,
        kind: String?,
        hasContext: Boolean?,
        q: String?,
        otherUserId: String?,
        contextType: String?,
        contextId: String?
    ): NetworkResult<ApiListResponse<ConversationListResponseDto>> {
        return safeApiCall {
            chatApiService.getConversations(
                pageNo = pageNo,
                pageSize = pageSize,
                kind = kind,
                hasContext = hasContext,
                q = q,
                otherUserId = otherUserId,
                contextType = contextType,
                contextId = contextId
            )
        }
    }

    override suspend fun getConversationsUnreadSummary(): NetworkResult<ApiSingleResponse<ConversationUnreadSummaryResponseDto>> {
        return safeApiCall {
            chatApiService.getConversationsUnreadSummary()
        }
    }

    override suspend fun getOrCreateDirectConversation(
        recipientUserId: String
    ): NetworkResult<ApiSingleResponse<ConversationDetailResponseDto>> {
        return safeApiCall {
            chatApiService.createConversation(
                ConversationCreateRequestDto(
                    kind = "direct",
                    recipientUserId = recipientUserId
                )
            )
        }
    }

    override suspend fun createGroupConversation(
        title: String?,
        memberUserIds: List<String>
    ): NetworkResult<ApiSingleResponse<ConversationDetailResponseDto>> {
        return safeApiCall {
            chatApiService.createConversation(
                ConversationCreateRequestDto(
                    kind = "group",
                    title = title,
                    memberUserIds = memberUserIds
                )
            )
        }
    }

    override suspend fun getConversationDetail(
        conversationId: String
    ): NetworkResult<ApiSingleResponse<ConversationDetailResponseDto>> {
        return safeApiCall {
            chatApiService.getConversationDetail(conversationId)
        }
    }

    override suspend fun updateConversationMetadata(
        conversationId: String,
        title: String?,
        avatarMediaId: String?
    ): NetworkResult<ApiSingleResponse<ConversationMetadataUpdateResponseDto>> {
        return safeApiCall {
            chatApiService.updateConversationMetadata(
                conversationId = conversationId,
                request = ConversationMetadataUpdateRequestDto(
                    title = title,
                    avatarMediaId = avatarMediaId
                )
            )
        }
    }

    override suspend fun getConversationMembers(
        conversationId: String
    ): NetworkResult<ApiSingleResponse<List<ConversationMemberDto>>> {
        return safeApiCall {
            chatApiService.getConversationMembers(conversationId)
        }
    }

    override suspend fun addConversationMembers(
        conversationId: String,
        userIds: List<String>
    ): NetworkResult<ApiSingleResponse<List<ConversationMemberDto>>> {
        return safeApiCall {
            chatApiService.addConversationMembers(
                conversationId = conversationId,
                request = ConversationMemberAddRequestDto(userIds = userIds)
            )
        }
    }

    override suspend fun removeConversationMember(
        conversationId: String,
        userId: String
    ): NetworkResult<ApiSingleResponse<Any?>> {
        return safeApiCall {
            chatApiService.removeConversationMember(conversationId, userId)
        }
    }

    override suspend fun updateMemberRole(
        conversationId: String,
        userId: String,
        role: String
    ): NetworkResult<ApiSingleResponse<Any?>> {
        return safeApiCall {
            chatApiService.updateMemberRole(
                conversationId = conversationId,
                userId = userId,
                request = ConversationMemberRoleUpdateRequestDto(role = role)
            )
        }
    }

    override suspend fun leaveConversation(
        conversationId: String
    ): NetworkResult<ApiSingleResponse<Any?>> {
        return safeApiCall {
            chatApiService.leaveConversation(conversationId)
        }
    }

    override suspend fun getMessages(
        conversationId: String,
        cursor: String?,
        pageSize: Int,
        maxSequence: String?
    ): NetworkResult<ApiListResponse<MessageListResponseDto>> {
        return safeApiCall {
            chatApiService.getMessages(
                conversationId = conversationId,
                cursor = cursor,
                pageSize = pageSize,
                maxSequence = maxSequence
            )
        }
    }

    override suspend fun searchMessages(
        conversationId: String,
        query: String,
        cursor: String?,
        pageSize: Int
    ): NetworkResult<ApiListResponse<MessageSearchResponseDto>> {
        return safeApiCall {
            chatApiService.searchMessages(
                conversationId = conversationId,
                query = query,
                cursor = cursor,
                pageSize = pageSize
            )
        }
    }

    override suspend fun sendMessage(
        conversationId: String,
        content: String,
        type: String,
        clientMessageId: String,
        mediaId: String?,
        replyToMessageId: String?
    ): NetworkResult<ApiSingleResponse<MessageSendResponseDto>> {
        return safeApiCall {
            chatApiService.sendMessage(
                MessageSendRequestDto(
                    conversationId = conversationId,
                    clientMessageId = clientMessageId,
                    type = type,
                    content = content,
                    mediaId = mediaId,
                    replyToMessageId = replyToMessageId
                )
            )
        }
    }

    override suspend fun markMessagesAsRead(
        conversationId: String,
        lastReadSequence: String
    ): NetworkResult<ApiSingleResponse<MessageMarkReadResponseDto>> {
        return safeApiCall {
            chatApiService.markMessagesAsRead(
                MessageMarkReadRequestDto(
                    conversationId = conversationId,
                    lastReadSequence = lastReadSequence
                )
            )
        }
    }
}
