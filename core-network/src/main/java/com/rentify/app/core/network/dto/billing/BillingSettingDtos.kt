package com.rentify.app.core.network.dto.billing

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BillingSettingResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "landlordId") val landlordId: String = "",
    @field:Json(name = "scope") val scope: String = "building",
    @field:Json(name = "buildingId") val buildingId: String = "",
    @field:Json(name = "roomId") val roomId: String? = null,
    @field:Json(name = "paymentDueDay") val paymentDueDay: Int = 5,
    @field:Json(name = "remindDay") val remindDay: Int? = null,
    @field:Json(name = "botEnabled") val botEnabled: Boolean = true,
    @field:Json(name = "isActive") val isActive: Boolean = true,
    @field:Json(name = "createdAt") val createdAt: String = "",
    @field:Json(name = "updatedAt") val updatedAt: String = ""
)

@JsonClass(generateAdapter = true)
data class BillingSettingCreateRequestDto(
    @field:Json(name = "scope") val scope: String,
    @field:Json(name = "buildingId") val buildingId: String? = null,
    @field:Json(name = "roomId") val roomId: String? = null,
    @field:Json(name = "paymentDueDay") val paymentDueDay: Int = 5,
    @field:Json(name = "remindDay") val remindDay: Int? = null,
    @field:Json(name = "botEnabled") val botEnabled: Boolean = true
)

@JsonClass(generateAdapter = true)
data class BillingSettingUpdateRequestDto(
    @field:Json(name = "paymentDueDay") val paymentDueDay: Int? = null,
    @field:Json(name = "remindDay") val remindDay: Int? = null,
    @field:Json(name = "botEnabled") val botEnabled: Boolean? = null,
    @field:Json(name = "isActive") val isActive: Boolean? = null
)
