package com.rentify.app.core.network.dto.contract

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ContractMyListResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "contractCode") val contractCode: String = "",
    @field:Json(name = "roomId") val roomId: String,
    @field:Json(name = "roomName") val roomName: String = "",
    @field:Json(name = "buildingName") val buildingName: String = "",
    @field:Json(name = "status") val status: String = "active",
    @field:Json(name = "rentalType") val rentalType: String? = null,
    @field:Json(name = "rentPrice") val rentPrice: Double = 0.0,
    @field:Json(name = "depositAmount") val depositAmount: Double = 0.0,
    @field:Json(name = "startDate") val startDate: String = "",
    @field:Json(name = "endDate") val endDate: String = ""
)

@JsonClass(generateAdapter = true)
data class ContractDetailResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "roomId") val roomId: String,
    @field:Json(name = "status") val status: String = "active",
    @field:Json(name = "tenantId") val tenantId: String? = null,
    @field:Json(name = "tenantName") val tenantName: String? = null,
    @field:Json(name = "tenantPhone") val tenantPhone: String? = null,
    @field:Json(name = "rentPrice") val rentPrice: Double = 0.0,
    @field:Json(name = "depositAmount") val depositAmount: Double = 0.0,
    @field:Json(name = "startDate") val startDate: String = "",
    @field:Json(name = "endDate") val endDate: String = "",
    @field:Json(name = "vehicleCount") val vehicleCount: Int? = null,
    @field:Json(name = "description") val description: String? = null
)

