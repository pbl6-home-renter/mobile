package com.rentify.app.landlord.data.repository

import com.rentify.app.core.data.repository.BaseRepository
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.api.ChatApiService
import com.rentify.app.core.network.api.UserApiService
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.chat.BotReminderSendRequestDto
import com.rentify.app.core.network.dto.chat.ConversationCreateRequestDto
import com.rentify.app.core.network.dto.chat.ConversationDetailResponseDto
import com.rentify.app.core.network.dto.chat.ConversationListResponseDto
import com.rentify.app.core.network.dto.chat.ConversationMemberAddRequestDto
import com.rentify.app.core.network.dto.chat.ConversationMemberDto
import com.rentify.app.core.network.dto.chat.ConversationMetadataUpdateRequestDto
import com.rentify.app.core.network.dto.chat.ConversationMetadataUpdateResponseDto
import com.rentify.app.core.network.dto.chat.MessageListResponseDto
import com.rentify.app.core.network.dto.chat.MessageMarkReadRequestDto
import com.rentify.app.core.network.dto.chat.MessageMarkReadResponseDto
import com.rentify.app.core.network.dto.chat.MessageSendRequestDto
import com.rentify.app.core.network.dto.chat.MessageSendResponseDto
import com.rentify.app.core.network.dto.user.UserDirectoryResponseDto

class LandlordChatRepositoryImpl(
    private val chatApiService: ChatApiService,
    private val userApiService: UserApiService? = null
) : BaseRepository(), LandlordChatRepository {

    override suspend fun getConversations(
        pageNo: Int,
        pageSize: Int,
        kind: String?,
        q: String?
    ): NetworkResult<ApiListResponse<ConversationListResponseDto>> {
        return safeApiCall {
            chatApiService.getConversations(
                pageNo = pageNo,
                pageSize = pageSize,
                kind = kind,
                q = q
            )
        }
    }

    override suspend fun createConversation(
        request: ConversationCreateRequestDto
    ): NetworkResult<ApiSingleResponse<ConversationDetailResponseDto>> {
        return safeApiCall {
            chatApiService.createConversation(request)
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

    override suspend fun leaveConversation(
        conversationId: String
    ): NetworkResult<ApiSingleResponse<Any?>> {
        return safeApiCall {
            chatApiService.leaveConversation(conversationId)
        }
    }

    override suspend fun getUsersDirectory(
        q: String?,
        pageNo: Int,
        pageSize: Int
    ): NetworkResult<ApiListResponse<UserDirectoryResponseDto>> {
        val service = userApiService ?: return NetworkResult.UnknownError(Exception("UserApiService not available"))
        return safeApiCall {
            service.getUsersDirectory(q = q, pageNo = pageNo, pageSize = pageSize)
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
                    content = content,
                    type = type,
                    clientMessageId = clientMessageId,
                    mediaId = mediaId,
                    replyToMessageId = replyToMessageId
                )
            )
        }
    }

    override suspend fun sendBotReminder(
        conversationId: String,
        templateType: String,
        resourceId: String,
        clientMessageId: String
    ): NetworkResult<ApiSingleResponse<Any?>> {
        return safeApiCall {
            chatApiService.sendBotReminder(
                conversationId = conversationId,
                request = BotReminderSendRequestDto(
                    templateType = templateType,
                    resourceId = resourceId,
                    clientMessageId = clientMessageId
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

