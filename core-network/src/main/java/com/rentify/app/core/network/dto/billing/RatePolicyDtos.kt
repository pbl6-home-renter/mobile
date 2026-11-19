package com.rentify.app.core.network.dto.billing

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RateTierStepDto(
    @field:Json(name = "from") val from: Int,
    @field:Json(name = "to") val to: Int? = null,
    @field:Json(name = "price") val price: String
)

@JsonClass(generateAdapter = true)
data class RateItemDto(
    @field:Json(name = "type") val type: String,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "rateKind") val rateKind: String,
    @field:Json(name = "unitPrice") val unitPrice: String? = null,
    @field:Json(name = "steps") val steps: List<RateTierStepDto>? = null,
    @field:Json(name = "billingUnit") val billingUnit: String
)

@JsonClass(generateAdapter = true)
data class RatePolicyResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "landlordId") val landlordId: String = "",
    @field:Json(name = "scope") val scope: String = "building",
    @field:Json(name = "buildingId") val buildingId: String? = null,
    @field:Json(name = "roomId") val roomId: String? = null,
    @field:Json(name = "rates") val rates: List<RateItemDto> = emptyList(),
    @field:Json(name = "isActive") val isActive: Boolean = true,
    @field:Json(name = "createdAt") val createdAt: String = "",
    @field:Json(name = "updatedAt") val updatedAt: String = ""
)

@JsonClass(generateAdapter = true)
data class RatePolicyCreateRequestDto(
    @field:Json(name = "scope") val scope: String,
    @field:Json(name = "buildingId") val buildingId: String? = null,
    @field:Json(name = "roomId") val roomId: String? = null,
    @field:Json(name = "rates") val rates: List<RateItemDto>
)

@JsonClass(generateAdapter = true)
data class RatePolicyUpdateRequestDto(
    @field:Json(name = "rates") val rates: List<RateItemDto>? = null,
    @field:Json(name = "isActive") val isActive: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class BuildingRatePoliciesBatchCreateRequestDto(
    @field:Json(name = "rates") val rates: List<RateItemDto>
)
