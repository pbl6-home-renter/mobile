package com.rentify.app.core.network.dto.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthLoginRequestDto(
    @field:Json(name = "identifier") val identifier: String,
    @field:Json(name = "password") val password: String,
    @field:Json(name = "rememberMe") val rememberMe: Boolean
)
