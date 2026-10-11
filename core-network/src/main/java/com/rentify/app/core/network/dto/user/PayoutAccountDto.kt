package com.rentify.app.core.network.dto.user

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PayoutAccountDto(
    @field:Json(name = "bankCode") val bankCode: String,
    @field:Json(name = "accountNumber") val accountNumber: String,
    @field:Json(name = "accountName") val accountName: String
)
