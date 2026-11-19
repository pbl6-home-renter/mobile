package com.rentify.app.core.network.dto.notification

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NotificationListResponseDto(
    @field:Json(name = "id") val id: String = "",
    @field:Json(name = "recipientUserId") val recipientUserId: String = "",
    @field:Json(name = "sourceService") val sourceService: String = "",
    @field:Json(name = "eventId") val eventId: String = "",
    @field:Json(name = "type") val type: String = "system",
    @field:Json(name = "title") val title: String = "",
    @field:Json(name = "body") val body: String = "",
    @field:Json(name = "deepLink") val deepLink: String? = null,
    @field:Json(name = "data") val data: Map<String, Any>? = null,
    @field:Json(name = "createdAt") val createdAt: String = "",
    @field:Json(name = "readAt") val readAt: String? = null
)

@JsonClass(generateAdapter = true)
data class NotificationMarkReadResponseDto(
    @field:Json(name = "id") val id: String = "",
    @field:Json(name = "readAt") val readAt: String? = null
)

@JsonClass(generateAdapter = true)
data class NotificationMarkAllReadResponseDto(
    @field:Json(name = "markedCount") val markedCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class NotificationPreferenceDetailResponseDto(
    @field:Json(name = "type") val type: String = "",
    @field:Json(name = "pushEnabled") val pushEnabled: Boolean = true
)

@JsonClass(generateAdapter = true)
data class NotificationPreferenceItemRequestDto(
    @field:Json(name = "type") val type: String,
    @field:Json(name = "pushEnabled") val pushEnabled: Boolean
)

@JsonClass(generateAdapter = true)
data class NotificationPreferenceUpdateRequestDto(
    @field:Json(name = "preferences") val preferences: List<NotificationPreferenceItemRequestDto>
)

@JsonClass(generateAdapter = true)
data class NotificationPreferenceUpdateResponseDto(
    @field:Json(name = "updatedAt") val updatedAt: String = ""
)
