package com.rentify.app.core.network.dto.user

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserChangePasswordRequestDto(
    @field:Json(name = "currentPassword") val currentPassword: String,
    @field:Json(name = "newPassword") val newPassword: String,
    @field:Json(name = "confirmPassword") val confirmPassword: String
)
