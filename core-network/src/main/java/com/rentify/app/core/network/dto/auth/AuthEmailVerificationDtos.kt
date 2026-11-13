package com.rentify.app.core.network.dto.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthRequestEmailVerificationRequestDto(
    @field:Json(name = "email") val email: String
)

@JsonClass(generateAdapter = true)
data class AuthRequestEmailVerificationResponseDto(
    @field:Json(name = "accepted") val accepted: Boolean = true
)

@JsonClass(generateAdapter = true)
data class AuthVerifyEmailRequestDto(
    @field:Json(name = "token") val token: String
)

