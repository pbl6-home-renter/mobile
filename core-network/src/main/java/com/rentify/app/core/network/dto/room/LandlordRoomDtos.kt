package com.rentify.app.core.network.dto.room

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RoomMyListResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "buildingId") val buildingId: String,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "area") val area: String? = null,
    @field:Json(name = "rentPrice") val rentPrice: Long = 0L,
    @field:Json(name = "status") val status: String = "available",
    @field:Json(name = "floorNumber") val floorNumber: Int? = null,
    @field:Json(name = "currentTenantName") val currentTenantName: String? = null,
    @field:Json(name = "currentTenantPhone") val currentTenantPhone: String? = null,
    @field:Json(name = "activeIssuesCount") val activeIssuesCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class RoomCreateRequestDto(
    @field:Json(name = "buildingId") val buildingId: String,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "floorNumber") val floorNumber: Int,
    @field:Json(name = "area") val area: String,
    @field:Json(name = "rentPrice") val rentPrice: Long,
    @field:Json(name = "depositAmount") val depositAmount: Long,
    @field:Json(name = "maxOccupancy") val maxOccupancy: Int? = null,
    @field:Json(name = "genderPolicy") val genderPolicy: String = "any",
    @field:Json(name = "amenities") val amenities: RoomAmenitiesDto? = null,
    @field:Json(name = "photoMediaIds") val photoMediaIds: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class RoomUpdateRequestDto(
    @field:Json(name = "name") val name: String? = null,
    @field:Json(name = "floorNumber") val floorNumber: Int? = null,
    @field:Json(name = "description") val description: String? = null,
    @field:Json(name = "area") val area: Double? = null,
    @field:Json(name = "amenities") val amenities: RoomAmenitiesDto? = null,
    @field:Json(name = "maxOccupancy") val maxOccupancy: Int? = null,
    @field:Json(name = "genderPolicy") val genderPolicy: String? = null,
    @field:Json(name = "rentPrice") val rentPrice: Long? = null
)

@JsonClass(generateAdapter = true)
data class RoomMarkReadyResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "status") val status: String = "available"
)

