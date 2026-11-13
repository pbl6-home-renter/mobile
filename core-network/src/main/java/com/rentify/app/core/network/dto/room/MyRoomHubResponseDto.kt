package com.rentify.app.core.network.dto.room

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MyRoomHubResponseDto(
    @field:Json(name = "room") val room: RoomHubRoomDto,
    @field:Json(name = "landlord") val landlord: RoomHubLandlordDto,
    @field:Json(name = "contract") val contract: RoomHubContractDto,
    @field:Json(name = "currentInvoice") val currentInvoice: RoomHubInvoiceDto? = null,
    @field:Json(name = "roommates") val roommates: List<RoomHubRoommateDto> = emptyList(),
    @field:Json(name = "activeIssuesCount") val activeIssuesCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class RoomHubRoomDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "roomType") val roomType: String? = null,
    @field:Json(name = "area") val area: String? = null,
    @field:Json(name = "rentPrice") val rentPrice: Long,
    @field:Json(name = "buildingId") val buildingId: String,
    @field:Json(name = "buildingName") val buildingName: String,
    @field:Json(name = "address") val address: String,
    @field:Json(name = "photoUrls") val photoUrls: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class RoomHubLandlordDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "phone") val phone: String,
    @field:Json(name = "avatarUrl") val avatarUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class RoomHubContractDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "contractCode") val contractCode: String,
    @field:Json(name = "rentalType") val rentalType: String,
    @field:Json(name = "startDate") val startDate: String,
    @field:Json(name = "endDate") val endDate: String,
    @field:Json(name = "status") val status: String,
    @field:Json(name = "signedContractUrl") val signedContractUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class RoomHubInvoiceDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "code") val code: String,
    @field:Json(name = "period") val period: String,
    @field:Json(name = "totalAmount") val totalAmount: Long,
    @field:Json(name = "remainingAmount") val remainingAmount: Long,
    @field:Json(name = "dueDate") val dueDate: String,
    @field:Json(name = "invoiceStatus") val invoiceStatus: String,
    @field:Json(name = "vietQrPaymentUrl") val vietQrPaymentUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class RoomHubRoommateDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "phone") val phone: String? = null,
    @field:Json(name = "avatarUrl") val avatarUrl: String? = null,
    @field:Json(name = "isRepresentative") val isRepresentative: Boolean = false
)
