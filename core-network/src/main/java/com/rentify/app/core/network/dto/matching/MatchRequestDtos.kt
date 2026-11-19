package com.rentify.app.core.network.dto.matching

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MatchRequestListResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "requesterId") val requesterId: String,
    @field:Json(name = "targetId") val targetId: String,
    @field:Json(name = "status") val status: String,
    @field:Json(name = "createdAt") val createdAt: String,
    @field:Json(name = "respondedAt") val respondedAt: String? = null
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
    @field:Json(name = "requesterId") val requesterId: String,
    @field:Json(name = "targetId") val targetId: String,
    @field:Json(name = "status") val status: String,
    @field:Json(name = "createdAt") val createdAt: String,
    @field:Json(name = "respondedAt") val respondedAt: String? = null
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

