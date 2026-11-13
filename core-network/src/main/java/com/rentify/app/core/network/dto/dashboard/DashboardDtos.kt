package com.rentify.app.core.network.dto.dashboard

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DashboardResponseDto(
    @field:Json(name = "totalUsers") val totalUsers: Int = 0,
    @field:Json(name = "totalLandlords") val totalLandlords: Int = 0,
    @field:Json(name = "totalTenants") val totalTenants: Int = 0,
    @field:Json(name = "totalBuildings") val totalBuildings: Int = 0,
    @field:Json(name = "pendingApprovalBuildings") val pendingApprovalBuildings: Int = 0,
    @field:Json(name = "totalRooms") val totalRooms: Int = 0,
    @field:Json(name = "occupancyRate") val occupancyRate: String = "0%",
    @field:Json(name = "systemTransactionVolume") val systemTransactionVolume: Long = 0L
)

