package com.rentify.app.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiSingleResponse<T>(
    @field:Json(name = "code") val code: String,
    @field:Json(name = "message") val message: String,
    @field:Json(name = "data") val data: T? = null
)
