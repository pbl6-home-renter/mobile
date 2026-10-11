package com.rentify.app.core.network.dto.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthRequestPasswordResetRequestDto(
    @field:Json(name = "email") val email: String
)
