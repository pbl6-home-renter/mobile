package com.rentify.app.core.network.dto.auth

import com.rentify.app.core.network.dto.user.UserCurrentResponseDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthLoginResponseDto(
    @field:Json(name = "accessToken") val accessToken: String,
    @field:Json(name = "refreshToken") val refreshToken: String,
    @field:Json(name = "profile") val profile: UserCurrentResponseDto
)
