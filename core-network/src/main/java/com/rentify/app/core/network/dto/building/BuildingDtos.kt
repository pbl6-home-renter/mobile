package com.rentify.app.core.network.dto.building

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BuildingMyListResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "address") val address: String,
    @field:Json(name = "status") val status: String,
    @field:Json(name = "occupiedRooms") val occupiedRooms: Int = 0,
    @field:Json(name = "occupancyRate") val occupancyRate: String = "0%",
    @field:Json(name = "rejectionReason") val rejectionReason: String? = null,
    @field:Json(name = "createdAt") val createdAt: String = ""
)

@JsonClass(generateAdapter = true)
data class BuildingCreateRequestDto(
    @field:Json(name = "name") val name: String,
    @field:Json(name = "province") val province: String,
    @field:Json(name = "ward") val ward: String,
    @field:Json(name = "address") val address: String
)

@JsonClass(generateAdapter = true)
data class BuildingUpdateRequestDto(
    @field:Json(name = "name") val name: String? = null,
    @field:Json(name = "address") val address: String? = null
)

@JsonClass(generateAdapter = true)
data class BuildingVietQrConfigRequestDto(
    @field:Json(name = "bankCode") val bankCode: String,
    @field:Json(name = "bankName") val bankName: String,
    @field:Json(name = "accountNumber") val accountNumber: String,
    @field:Json(name = "accountHolderName") val accountHolderName: String,
    @field:Json(name = "branch") val branch: String? = null,
    @field:Json(name = "isDefault") val isDefault: Boolean? = true
)

@JsonClass(generateAdapter = true)
data class BuildingVietQrConfigResponseDto(
    @field:Json(name = "id") val id: String = "",
    @field:Json(name = "buildingId") val buildingId: String = "",
    @field:Json(name = "bankCode") val bankCode: String = "",
    @field:Json(name = "bankName") val bankName: String = "",
    @field:Json(name = "accountNumber") val accountNumber: String = "",
    @field:Json(name = "accountHolderName") val accountHolderName: String = "",
    @field:Json(name = "branch") val branch: String? = null,
    @field:Json(name = "qrSampleUrl") val qrSampleUrl: String? = null,
    @field:Json(name = "isDefault") val isDefault: Boolean = true,
    @field:Json(name = "createdAt") val createdAt: String? = null,
    @field:Json(name = "updatedAt") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class BuildingRatePolicyItemDto(
    @field:Json(name = "id") val id: String = "",
    @field:Json(name = "name") val name: String = "",
    @field:Json(name = "category") val category: String = "",
    @field:Json(name = "rateKind") val rateKind: String = "",
    @field:Json(name = "unit") val unit: String = "",
    @field:Json(name = "unitPrice") val unitPrice: Int? = null
)

@JsonClass(generateAdapter = true)
data class BuildingMyDetailResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "province") val province: String = "",
    @field:Json(name = "ward") val ward: String = "",
    @field:Json(name = "address") val address: String = "",
    @field:Json(name = "totalFloors") val totalFloors: Int? = null,
    @field:Json(name = "occupiedRooms") val occupiedRooms: Int = 0,
    @field:Json(name = "occupancyRate") val occupancyRate: String = "0%",
    @field:Json(name = "status") val status: String = "pending",
    @field:Json(name = "reviewNote") val reviewNote: String? = null,
    @field:Json(name = "ownershipCertificateUrls") val ownershipCertificateUrls: List<String> = emptyList(),
    @field:Json(name = "vietQrConfigured") val vietQrConfigured: Boolean = false,
    @field:Json(name = "ratePolicies") val ratePolicies: List<BuildingRatePolicyItemDto> = emptyList(),
    @field:Json(name = "createdAt") val createdAt: String = ""
)
