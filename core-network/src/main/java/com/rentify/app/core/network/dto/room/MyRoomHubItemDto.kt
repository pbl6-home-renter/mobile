package com.rentify.app.core.network.dto.room

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MyRoomHubItemDto(
    @field:Json(name = "contractId") val contractId: String,
    @field:Json(name = "roomId") val roomId: String,
    @field:Json(name = "roomName") val roomName: String = "",
    @field:Json(name = "area") val area: Double? = null,
    @field:Json(name = "buildingId") val buildingId: String = "",
    @field:Json(name = "buildingName") val buildingName: String = "",
    @field:Json(name = "address") val address: String = "",
    @field:Json(name = "ward") val ward: String = "",
    @field:Json(name = "province") val province: String = "",
    @field:Json(name = "rentPrice") val rentPrice: Double = 0.0,
    @field:Json(name = "startDate") val startDate: String = "",
    @field:Json(name = "endDate") val endDate: String = ""
)
