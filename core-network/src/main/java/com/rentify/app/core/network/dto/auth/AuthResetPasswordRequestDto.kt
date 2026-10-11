package com.rentify.app.core.network.dto.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthResetPasswordRequestDto(
    @field:Json(name = "token") val token: String,
    @field:Json(name = "newPassword") val newPassword: String
)
