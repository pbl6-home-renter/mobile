package com.rentify.app.core.network.api

import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.chat.BotReminderSendRequestDto
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
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApiService {

    @GET("conversations")
    suspend fun getConversations(
        @Query("pageNo") pageNo: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("kind") kind: String? = null,
        @Query("hasContext") hasContext: Boolean? = null,
        @Query("q") q: String? = null,
        @Query("otherUserId") otherUserId: String? = null,
        @Query("contextType") contextType: String? = null,
        @Query("contextId") contextId: String? = null
    ): Response<ApiListResponse<ConversationListResponseDto>>

    @GET("conversations/unread-summary")
    suspend fun getConversationsUnreadSummary(): Response<ApiSingleResponse<ConversationUnreadSummaryResponseDto>>

    @POST("conversations")
    suspend fun createConversation(
        @Body request: ConversationCreateRequestDto
    ): Response<ApiSingleResponse<ConversationDetailResponseDto>>

    @GET("conversations/{conversationId}")
    suspend fun getConversationDetail(
        @Path("conversationId") conversationId: String
    ): Response<ApiSingleResponse<ConversationDetailResponseDto>>

    @PATCH("conversations/{conversationId}")
    suspend fun updateConversationMetadata(
        @Path("conversationId") conversationId: String,
        @Body request: ConversationMetadataUpdateRequestDto
    ): Response<ApiSingleResponse<ConversationMetadataUpdateResponseDto>>

    @GET("conversations/{conversationId}/members")
    suspend fun getConversationMembers(
        @Path("conversationId") conversationId: String
    ): Response<ApiSingleResponse<List<ConversationMemberDto>>>

    @POST("conversations/{conversationId}/members")
    suspend fun addConversationMembers(
        @Path("conversationId") conversationId: String,
        @Body request: ConversationMemberAddRequestDto
    ): Response<ApiSingleResponse<List<ConversationMemberDto>>>

    @DELETE("conversations/{conversationId}/members/{userId}")
    suspend fun removeConversationMember(
        @Path("conversationId") conversationId: String,
        @Path("userId") userId: String
    ): Response<ApiSingleResponse<Any?>>

    @PUT("conversations/{conversationId}/members/{userId}/role")
    suspend fun updateMemberRole(
        @Path("conversationId") conversationId: String,
        @Path("userId") userId: String,
        @Body request: ConversationMemberRoleUpdateRequestDto
    ): Response<ApiSingleResponse<Any?>>

    @DELETE("conversations/{conversationId}/members/me")
    suspend fun leaveConversation(
        @Path("conversationId") conversationId: String
    ): Response<ApiSingleResponse<Any?>>

    @POST("conversations/{conversationId}/bot-reminders")
    suspend fun sendBotReminder(
        @Path("conversationId") conversationId: String,
        @Body request: BotReminderSendRequestDto
    ): Response<ApiSingleResponse<Any?>>

    @GET("messages")
    suspend fun getMessages(
        @Query("conversationId") conversationId: String,
        @Query("cursor") cursor: String? = null,
        @Query("pageSize") pageSize: Int = 30,
        @Query("maxSequence") maxSequence: String? = null
    ): Response<ApiListResponse<MessageListResponseDto>>

    @GET("messages/search")
    suspend fun searchMessages(
        @Query("conversationId") conversationId: String,
        @Query("q") query: String,
        @Query("cursor") cursor: String? = null,
        @Query("pageSize") pageSize: Int = 30
    ): Response<ApiListResponse<MessageSearchResponseDto>>

    @POST("messages")
    suspend fun sendMessage(
        @Body request: MessageSendRequestDto
    ): Response<ApiSingleResponse<MessageSendResponseDto>>

    @POST("messages/mark-read")
    suspend fun markMessagesAsRead(
        @Body request: MessageMarkReadRequestDto
    ): Response<ApiSingleResponse<MessageMarkReadResponseDto>>
}

