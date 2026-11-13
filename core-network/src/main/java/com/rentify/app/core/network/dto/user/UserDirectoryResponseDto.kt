package com.rentify.app.core.network.dto.user

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserDirectoryResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "name") val name: String
)

