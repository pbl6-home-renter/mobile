package com.rentify.app.landlord.data.repository

import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.notification.NotificationListResponseDto
import com.rentify.app.core.network.dto.notification.NotificationMarkAllReadResponseDto
import com.rentify.app.core.network.dto.notification.NotificationMarkReadResponseDto
import com.rentify.app.core.network.dto.notification.NotificationPreferenceDetailResponseDto
import com.rentify.app.core.network.dto.notification.NotificationPreferenceUpdateRequestDto
import com.rentify.app.core.network.dto.notification.NotificationPreferenceUpdateResponseDto

interface LandlordNotificationRepository {
    suspend fun getNotifications(
        unreadOnly: Boolean? = null,
        cursor: String? = null,
        pageSize: Int = 50
    ): NetworkResult<ApiListResponse<NotificationListResponseDto>>

    suspend fun markNotificationRead(
        notificationId: String
    ): NetworkResult<ApiSingleResponse<NotificationMarkReadResponseDto>>

    suspend fun markAllNotificationsRead(): NetworkResult<ApiSingleResponse<NotificationMarkAllReadResponseDto>>

    suspend fun getNotificationPreferences(): NetworkResult<ApiListResponse<NotificationPreferenceDetailResponseDto>>

    suspend fun updateNotificationPreferences(
        request: NotificationPreferenceUpdateRequestDto
    ): NetworkResult<ApiSingleResponse<NotificationPreferenceUpdateResponseDto>>
}
