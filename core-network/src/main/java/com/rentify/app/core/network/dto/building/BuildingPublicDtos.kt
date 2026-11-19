package com.rentify.app.core.network.dto.building

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BuildingPublicResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "name") val name: String = "",
    @field:Json(name = "address") val address: String = "",
    @field:Json(name = "ward") val ward: String = "",
    @field:Json(name = "province") val province: String = ""
)
