package com.rentify.app.core.network.dto.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthRequestPasswordResetResponseDto(
    @field:Json(name = "accepted") val accepted: Boolean
)
