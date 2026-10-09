package com.rentify.app.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PaginationMeta(
    @field:Json(name = "page") val page: Int,
    @field:Json(name = "limit") val limit: Int,
    @field:Json(name = "totalPages") val totalPages: Int,
    @field:Json(name = "totalItems") val totalItems: Int
)

@JsonClass(generateAdapter = true)
data class ApiListResponse<T>(
    @field:Json(name = "code") val code: String,
    @field:Json(name = "message") val message: String,
    @field:Json(name = "data") val data: List<T> = emptyList(),
    @field:Json(name = "meta") val meta: PaginationMeta? = null
)
