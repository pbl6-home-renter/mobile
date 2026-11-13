package com.rentify.app.landlord.data.repository

import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.contract.ContractCreateRequestDto
import com.rentify.app.core.network.dto.contract.ContractDetailResponseDto
import com.rentify.app.core.network.dto.contract.ContractListResponseDto
import com.rentify.app.core.network.dto.contract.ContractMemberAddRequestDto
import com.rentify.app.core.network.dto.contract.ContractMemberLeaveRequestDto
import com.rentify.app.core.network.dto.contract.ContractMemberLeaveResponseDto
import com.rentify.app.core.network.dto.contract.ContractMemberListResponseDto
import com.rentify.app.core.network.dto.contract.ContractUpdateRequestDto
import com.rentify.app.core.network.dto.contract.DepositSettlementRequestDto

interface LandlordContractRepository {
    suspend fun getContracts(
        status: String? = null,
        roomId: String? = null,
        buildingId: String? = null,
        tenantId: String? = null,
        pageNo: Int = 1,
        pageSize: Int = 50
    ): NetworkResult<ApiListResponse<ContractListResponseDto>>

    suspend fun createContract(
        request: ContractCreateRequestDto
    ): NetworkResult<ApiSingleResponse<ContractDetailResponseDto>>

    suspend fun getContractDetail(
        contractId: String
    ): NetworkResult<ApiSingleResponse<ContractDetailResponseDto>>

    suspend fun updateContract(
        contractId: String,
        request: ContractUpdateRequestDto
    ): NetworkResult<ApiSingleResponse<ContractDetailResponseDto>>

    suspend fun activateContract(
        contractId: String,
        signedContractMediaId: String
    ): NetworkResult<ApiSingleResponse<ContractDetailResponseDto>>

    suspend fun completeCheckout(
        contractId: String,
        completedOn: String
    ): NetworkResult<ApiSingleResponse<ContractDetailResponseDto>>

    suspend fun depositSettlement(
        contractId: String,
        request: DepositSettlementRequestDto
    ): NetworkResult<ApiSingleResponse<Any>>

    suspend fun getContractMembers(
        contractId: String
    ): NetworkResult<ApiListResponse<ContractMemberListResponseDto>>

    suspend fun addContractMember(
        request: ContractMemberAddRequestDto
    ): NetworkResult<ApiSingleResponse<ContractMemberListResponseDto>>

    suspend fun leaveContract(
        tenantId: String,
        request: ContractMemberLeaveRequestDto
    ): NetworkResult<ApiSingleResponse<ContractMemberLeaveResponseDto>>
}

