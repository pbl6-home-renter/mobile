package com.rentify.app.core.network.dto.matching

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MatchRequestUserDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "avatarUrl") val avatarUrl: String? = null,
    @field:Json(name = "phone") val phone: String? = null,
    @field:Json(name = "gender") val gender: String
)

@JsonClass(generateAdapter = true)
data class MatchRequestListResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "requesterId") val requesterId: String,
    @field:Json(name = "requester") val requester: MatchRequestUserDto,
    @field:Json(name = "targetId") val targetId: String,
    @field:Json(name = "target") val target: MatchRequestUserDto,
    @field:Json(name = "matchScore") val matchScore: Int? = null,
    @field:Json(name = "message") val message: String? = null,
    @field:Json(name = "status") val status: String,
    @field:Json(name = "createdAt") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class MatchRequestCreateRequestDto(
    @field:Json(name = "targetId") val targetId: String
)

@JsonClass(generateAdapter = true)
data class MatchRequestCreateResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "status") val status: String,
    @field:Json(name = "respondedAt") val respondedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class MatchRequestAcceptResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "status") val status: String,
    @field:Json(name = "conversationId") val conversationId: String
)

@JsonClass(generateAdapter = true)
data class MatchRequestRejectResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "status") val status: String
)

@JsonClass(generateAdapter = true)
data class MatchRequestWithdrawResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "status") val status: String,
    @field:Json(name = "respondedAt") val respondedAt: String? = null
)

