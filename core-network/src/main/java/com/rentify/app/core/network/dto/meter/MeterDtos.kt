package com.rentify.app.core.network.dto.meter

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MeterReadingListResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "roomId") val roomId: String,
    @field:Json(name = "buildingId") val buildingId: String,
    @field:Json(name = "type") val type: String,
    @field:Json(name = "period") val period: String,
    @field:Json(name = "previousReading") val previousReading: String = "0",
    @field:Json(name = "currentReading") val currentReading: String = "0",
    @field:Json(name = "consumption") val consumption: String = "0",
    @field:Json(name = "isBaseline") val isBaseline: Boolean = false,
    @field:Json(name = "evidenceMediaIds") val evidenceMediaIds: List<String> = emptyList(),
    @field:Json(name = "evidenceMediaUrls") val evidenceMediaUrls: List<String> = emptyList(),
    @field:Json(name = "createdAt") val createdAt: String = ""
) {
    val previousReadingDouble: Double get() = previousReading.toDoubleOrNull() ?: 0.0
    val currentReadingDouble: Double get() = currentReading.toDoubleOrNull() ?: 0.0
    val consumptionDouble: Double get() = consumption.toDoubleOrNull() ?: 0.0
}

@JsonClass(generateAdapter = true)
data class MeterReadingCreateRequestDto(
    @field:Json(name = "roomId") val roomId: String,
    @field:Json(name = "type") val type: String,
    @field:Json(name = "period") val period: String,
    @field:Json(name = "currentReading") val currentReading: String,
    @field:Json(name = "previousReading") val previousReading: String? = null,
    @field:Json(name = "isBaseline") val isBaseline: Boolean = false
)

@JsonClass(generateAdapter = true)
data class BulkMeterReadingItemDto(
    @field:Json(name = "roomId") val roomId: String,
    @field:Json(name = "type") val type: String,
    @field:Json(name = "currentReading") val currentReading: String
)

@JsonClass(generateAdapter = true)
data class BulkMeterReadingCreateRequestDto(
    @field:Json(name = "buildingId") val buildingId: String,
    @field:Json(name = "recordedMonth") val recordedMonth: String,
    @field:Json(name = "readings") val readings: List<BulkMeterReadingItemDto>
)

@JsonClass(generateAdapter = true)
data class BulkMeterReadingResultDto(
    @field:Json(name = "roomId") val roomId: String,
    @field:Json(name = "roomName") val roomName: String = "",
    @field:Json(name = "electricityReadingId") val electricityReadingId: String? = null,
    @field:Json(name = "electricityUsage") val electricityUsage: String? = null,
    @field:Json(name = "waterReadingId") val waterReadingId: String? = null,
    @field:Json(name = "waterUsage") val waterUsage: String? = null,
    @field:Json(name = "status") val status: String = "SUCCESS",
    @field:Json(name = "note") val note: String? = null
)

@JsonClass(generateAdapter = true)
data class BulkMeterReadingResponseDto(
    @field:Json(name = "buildingId") val buildingId: String,
    @field:Json(name = "buildingName") val buildingName: String = "",
    @field:Json(name = "recordedMonth") val recordedMonth: String = "",
    @field:Json(name = "totalRoomsRecorded") val totalRoomsRecorded: Int = 0,
    @field:Json(name = "totalElectricityUsage") val totalElectricityUsage: String = "0",
    @field:Json(name = "totalWaterUsage") val totalWaterUsage: String = "0",
    @field:Json(name = "results") val results: List<BulkMeterReadingResultDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class MeterReadingEvidenceAttachRequestDto(
    @field:Json(name = "evidenceMediaIds") val evidenceMediaIds: List<String>
)
