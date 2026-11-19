package com.rentify.app.core.network.dto.administrative

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AdministrativeProvinceDto(
    @field:Json(name = "code") val code: String,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "divisionType") val divisionType: String = ""
)

@JsonClass(generateAdapter = true)
data class AdministrativeWardDto(
    @field:Json(name = "code") val code: String,
    @field:Json(name = "provinceCode") val provinceCode: String,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "divisionType") val divisionType: String = ""
)

