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
data class ContractListResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "contractCode") val contractCode: String = "",
    @field:Json(name = "roomId") val roomId: String,
    @field:Json(name = "roomName") val roomName: String = "",
    @field:Json(name = "tenantName") val tenantName: String? = null,
    @field:Json(name = "tenantPhone") val tenantPhone: String? = null,
    @field:Json(name = "status") val status: String = "active",
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

@JsonClass(generateAdapter = true)
data class ContractCreateRequestDto(
    @field:Json(name = "roomId") val roomId: String,
    @field:Json(name = "tenantId") val tenantId: String? = null,
    @field:Json(name = "tenantName") val tenantName: String? = null,
    @field:Json(name = "tenantPhone") val tenantPhone: String? = null,
    @field:Json(name = "depositAmount") val depositAmount: Double,
    @field:Json(name = "rentPrice") val rentPrice: Double,
    @field:Json(name = "startDate") val startDate: String,
    @field:Json(name = "endDate") val endDate: String,
    @field:Json(name = "vehicleCount") val vehicleCount: Int? = null
)

@JsonClass(generateAdapter = true)
data class ContractUpdateRequestDto(
    @field:Json(name = "rentPrice") val rentPrice: Double? = null,
    @field:Json(name = "depositAmount") val depositAmount: Double? = null,
    @field:Json(name = "startDate") val startDate: String? = null,
    @field:Json(name = "endDate") val endDate: String? = null,
    @field:Json(name = "vehicleCount") val vehicleCount: Int? = null,
    @field:Json(name = "description") val description: String? = null
)

@JsonClass(generateAdapter = true)
data class ContractActivateRequestDto(
    @field:Json(name = "signedContractMediaId") val signedContractMediaId: String
)

@JsonClass(generateAdapter = true)
data class ContractCompleteCheckoutRequestDto(
    @field:Json(name = "completedOn") val completedOn: String
)

@JsonClass(generateAdapter = true)
data class DepositSettlementDeductionDto(
    @field:Json(name = "category") val category: String,
    @field:Json(name = "amount") val amount: Double,
    @field:Json(name = "reason") val reason: String
)

@JsonClass(generateAdapter = true)
data class DepositSettlementPayoutAccountDto(
    @field:Json(name = "bankCode") val bankCode: String,
    @field:Json(name = "accountNumber") val accountNumber: String,
    @field:Json(name = "accountHolderName") val accountHolderName: String
)

@JsonClass(generateAdapter = true)
data class DepositSettlementRequestDto(
    @field:Json(name = "contractId") val contractId: String,
    @field:Json(name = "originalDeposit") val originalDeposit: Double,
    @field:Json(name = "deductions") val deductions: List<DepositSettlementDeductionDto> = emptyList(),
    @field:Json(name = "refundAmount") val refundAmount: Double,
    @field:Json(name = "paymentMethod") val paymentMethod: String = "CASH",
    @field:Json(name = "refundPayoutAccount") val refundPayoutAccount: DepositSettlementPayoutAccountDto? = null,
    @field:Json(name = "note") val note: String? = null
)

@JsonClass(generateAdapter = true)
data class ContractMemberListResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "contractId") val contractId: String,
    @field:Json(name = "tenantId") val tenantId: String? = null,
    @field:Json(name = "tenantName") val tenantName: String? = null,
    @field:Json(name = "tenantPhone") val tenantPhone: String? = null,
    @field:Json(name = "status") val status: String = "active",
    @field:Json(name = "joinedAt") val joinedAt: String = "",
    @field:Json(name = "leftAt") val leftAt: String? = null
)

@JsonClass(generateAdapter = true)
data class ContractMemberAddRequestDto(
    @field:Json(name = "contractId") val contractId: String,
    @field:Json(name = "tenantId") val tenantId: String? = null,
    @field:Json(name = "tenantName") val tenantName: String,
    @field:Json(name = "tenantPhone") val tenantPhone: String? = null,
    @field:Json(name = "joinedAt") val joinedAt: String
)

@JsonClass(generateAdapter = true)
data class ContractMemberLeaveRequestDto(
    @field:Json(name = "contractId") val contractId: String,
    @field:Json(name = "leftOn") val leftOn: String
)

@JsonClass(generateAdapter = true)
data class ContractMemberLeaveResponseDto(
    @field:Json(name = "id") val id: String = "",
    @field:Json(name = "leftAt") val leftAt: String = ""
)
