package com.rentify.app.landlord.data.repository

import com.rentify.app.core.data.repository.BaseRepository
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.api.NotificationApiService
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.notification.NotificationListResponseDto
import com.rentify.app.core.network.dto.notification.NotificationMarkAllReadResponseDto
import com.rentify.app.core.network.dto.notification.NotificationMarkReadResponseDto
import com.rentify.app.core.network.dto.notification.NotificationPreferenceDetailResponseDto
import com.rentify.app.core.network.dto.notification.NotificationPreferenceUpdateRequestDto
import com.rentify.app.core.network.dto.notification.NotificationPreferenceUpdateResponseDto

class LandlordNotificationRepositoryImpl(
    private val notificationApiService: NotificationApiService
) : BaseRepository(), LandlordNotificationRepository {

    override suspend fun getNotifications(
        unreadOnly: Boolean?,
        cursor: String?,
        pageSize: Int
    ): NetworkResult<ApiListResponse<NotificationListResponseDto>> {
        return safeApiCall {
            notificationApiService.getNotifications(
                unreadOnly = unreadOnly,
                cursor = cursor,
                pageSize = pageSize
            )
        }
    }

    override suspend fun markNotificationRead(
        notificationId: String
    ): NetworkResult<ApiSingleResponse<NotificationMarkReadResponseDto>> {
        return safeApiCall {
            notificationApiService.markNotificationRead(notificationId)
        }
    }

    override suspend fun markAllNotificationsRead(): NetworkResult<ApiSingleResponse<NotificationMarkAllReadResponseDto>> {
        return safeApiCall {
            notificationApiService.markAllNotificationsRead()
        }
    }

    override suspend fun getNotificationPreferences(): NetworkResult<ApiListResponse<NotificationPreferenceDetailResponseDto>> {
        return safeApiCall {
            notificationApiService.getNotificationPreferences()
        }
    }

    override suspend fun updateNotificationPreferences(
        request: NotificationPreferenceUpdateRequestDto
    ): NetworkResult<ApiSingleResponse<NotificationPreferenceUpdateResponseDto>> {
        return safeApiCall {
            notificationApiService.updateNotificationPreferences(request)
        }
    }
}
