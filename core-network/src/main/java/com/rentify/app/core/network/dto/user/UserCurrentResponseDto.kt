package com.rentify.app.core.network.dto.user

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserCurrentResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "email") val email: String,
    @field:Json(name = "phone") val phone: String,
    @field:Json(name = "role") val role: String,
    @field:Json(name = "status") val status: String,
    @field:Json(name = "payoutAccount") val payoutAccount: PayoutAccountDto? = null
)
