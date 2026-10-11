package com.rentify.app.core.network.dto.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthRefreshResponseDto(
    @field:Json(name = "accessToken") val accessToken: String,
    @field:Json(name = "refreshToken") val refreshToken: String
)
