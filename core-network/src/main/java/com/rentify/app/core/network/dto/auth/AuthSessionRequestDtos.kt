package com.rentify.app.core.network.dto.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthRefreshRequestDto(
    @field:Json(name = "refreshToken") val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class AuthLogoutRequestDto(
    @field:Json(name = "refreshToken") val refreshToken: String
)
