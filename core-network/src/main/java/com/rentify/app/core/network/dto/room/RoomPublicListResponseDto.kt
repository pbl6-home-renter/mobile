package com.rentify.app.core.network.dto.room

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RoomPublicListResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "buildingId") val buildingId: String,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "roomType") val roomType: String? = null,
    @field:Json(name = "thumbnailUrl") val thumbnailUrl: String? = null,
    @field:Json(name = "area") val area: String,
    @field:Json(name = "rentPrice") val rentPrice: Long,
    @field:Json(name = "genderPolicy") val genderPolicy: String,
    @field:Json(name = "address") val address: String,
    @field:Json(name = "district") val district: String,
    @field:Json(name = "province") val province: String,
    @field:Json(name = "isFavorited") val isFavorited: Boolean = false
)
