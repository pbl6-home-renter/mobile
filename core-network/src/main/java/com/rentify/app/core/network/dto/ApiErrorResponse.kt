package com.rentify.app.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FieldErrorDetail(
    @field:Json(name = "field") val field: String,
    @field:Json(name = "message") val message: String,
    @field:Json(name = "rule") val rule: String? = null
)

@JsonClass(generateAdapter = true)
data class ApiErrorResponse(
    @field:Json(name = "code") val code: String,
    @field:Json(name = "message") val message: String,
    @field:Json(name = "errors") val errors: List<FieldErrorDetail>? = null
)
