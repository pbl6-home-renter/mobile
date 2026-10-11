package com.rentify.app.core.network.dto.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthRegisterRequestDto(
    @field:Json(name = "email") val email: String,
    @field:Json(name = "phone") val phone: String,
    @field:Json(name = "password") val password: String,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "role") val role: String
)
