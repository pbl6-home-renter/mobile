package com.rentify.app.core.network.api

import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.notification.NotificationListResponseDto
import com.rentify.app.core.network.dto.notification.NotificationMarkAllReadResponseDto
import com.rentify.app.core.network.dto.notification.NotificationMarkReadResponseDto
import com.rentify.app.core.network.dto.notification.NotificationPreferenceDetailResponseDto
import com.rentify.app.core.network.dto.notification.NotificationPreferenceUpdateRequestDto
import com.rentify.app.core.network.dto.notification.NotificationPreferenceUpdateResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationApiService {

    @GET("notifications")
    suspend fun getNotifications(
        @Query("filter[unreadOnly]") unreadOnly: Boolean? = null,
        @Query("cursor") cursor: String? = null,
        @Query("pageSize") pageSize: Int = 20
    ): Response<ApiListResponse<NotificationListResponseDto>>

    @POST("notifications/{notificationId}/mark-read")
    suspend fun markNotificationRead(
        @Path("notificationId") notificationId: String
    ): Response<ApiSingleResponse<NotificationMarkReadResponseDto>>

    @POST("notifications/mark-all-read")
    suspend fun markAllNotificationsRead(): Response<ApiSingleResponse<NotificationMarkAllReadResponseDto>>

    @GET("me/notification-preference")
    suspend fun getNotificationPreferences(): Response<ApiListResponse<NotificationPreferenceDetailResponseDto>>

    @PUT("me/notification-preference")
    suspend fun updateNotificationPreferences(
        @Body request: NotificationPreferenceUpdateRequestDto
    ): Response<ApiSingleResponse<NotificationPreferenceUpdateResponseDto>>
}
