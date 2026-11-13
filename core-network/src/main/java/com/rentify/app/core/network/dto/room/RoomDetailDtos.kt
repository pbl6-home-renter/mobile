package com.rentify.app.core.network.dto.room

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RoomDetailResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "buildingId") val buildingId: String,
    @field:Json(name = "buildingName") val buildingName: String = "",
    @field:Json(name = "name") val name: String = "",
    @field:Json(name = "description") val description: String? = null,
    @field:Json(name = "area") val area: String? = null,
    @field:Json(name = "floorNumber") val floorNumber: Int? = null,
    @field:Json(name = "rentPrice") val rentPrice: Long = 0L,
    @field:Json(name = "depositAmount") val depositAmount: Long = 0L,
    @field:Json(name = "status") val status: String = "available",
    @field:Json(name = "maxOccupancy") val maxOccupancy: Int? = null,
    @field:Json(name = "genderPolicy") val genderPolicy: String? = null,
    @field:Json(name = "amenities") val amenities: RoomAmenitiesDto? = null,
    @field:Json(name = "photoUrls") val photoUrls: List<String> = emptyList(),
    @field:Json(name = "landlord") val landlord: RoomLandlordDto,
    @field:Json(name = "currentContract") val currentContract: RoomContractSummaryDto? = null,
    @field:Json(name = "currentMembers") val currentMembers: List<RoomMemberDto> = emptyList(),
    @field:Json(name = "latestMeterReadings") val latestMeterReadings: RoomMeterReadingsDto? = null,
    @field:Json(name = "effectiveRatePolicySummary") val effectiveRatePolicySummary: RoomRatePolicySummaryDto? = null
)

@JsonClass(generateAdapter = true)
data class RoomAmenitiesDto(
    @field:Json(name = "aircon") val aircon: Boolean? = false,
    @field:Json(name = "fridge") val fridge: Boolean? = false,
    @field:Json(name = "wcInside") val wcInside: Boolean? = false,
    @field:Json(name = "waterHeater") val waterHeater: Boolean? = false,
    @field:Json(name = "mezzanine") val mezzanine: Boolean? = false,
    @field:Json(name = "table") val table: Boolean? = false,
    @field:Json(name = "chair") val chair: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class RoomLandlordDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "name") val name: String = "",
    @field:Json(name = "phone") val phone: String = ""
)

@JsonClass(generateAdapter = true)
data class RoomContractSummaryDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "contractCode") val contractCode: String? = null,
    @field:Json(name = "startDate") val startDate: String? = null,
    @field:Json(name = "endDate") val endDate: String? = null,
    @field:Json(name = "status") val status: String? = null
)

@JsonClass(generateAdapter = true)
data class RoomMemberDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "name") val name: String = "",
    @field:Json(name = "phone") val phone: String? = null,
    @field:Json(name = "avatarUrl") val avatarUrl: String? = null,
    @field:Json(name = "isRepresentative") val isRepresentative: Boolean = false
)

@JsonClass(generateAdapter = true)
data class RoomMeterReadingsDto(
    @field:Json(name = "electricity") val electricity: String? = null,
    @field:Json(name = "water") val water: String? = null,
    @field:Json(name = "recordedAt") val recordedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class RoomRatePolicySummaryDto(
    @field:Json(name = "electricity") val electricity: String? = null,
    @field:Json(name = "water") val water: String? = null
)

