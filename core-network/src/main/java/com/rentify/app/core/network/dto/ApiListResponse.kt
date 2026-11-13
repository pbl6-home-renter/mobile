package com.rentify.app.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PaginationDto(
    @field:Json(name = "type") val type: String? = null,
    @field:Json(name = "pageNo") val pageNo: Int = 1,
    @field:Json(name = "pageSize") val pageSize: Int = 20,
    @field:Json(name = "totalItems") val totalItems: Int = 0,
    @field:Json(name = "totalPages") val totalPages: Int = 1,
    @field:Json(name = "hasNextPage") val hasNextPage: Boolean = false,
    @field:Json(name = "hasPreviousPage") val hasPreviousPage: Boolean = false,
    @field:Json(name = "nextCursor") val nextCursor: String? = null
)

@JsonClass(generateAdapter = true)
data class PaginationMeta(
    @field:Json(name = "page") val page: Int,
    @field:Json(name = "limit") val limit: Int,
    @field:Json(name = "totalPages") val totalPages: Int,
    @field:Json(name = "totalItems") val totalItems: Int
)

@JsonClass(generateAdapter = true)
data class ApiListResponse<T>(
    @field:Json(name = "statusCode") val statusCode: Int? = null,
    @field:Json(name = "code") val code: String,
    @field:Json(name = "message") val message: String? = null,
    @field:Json(name = "data") val data: List<T> = emptyList(),
    @field:Json(name = "pagination") val pagination: PaginationDto? = null,
    @field:Json(name = "meta") val meta: PaginationMeta? = null
)
