package com.rentify.app.landlord.data.repository

import com.rentify.app.core.data.repository.BaseRepository
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.api.ContractApiService
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.contract.ContractActivateRequestDto
import com.rentify.app.core.network.dto.contract.ContractCompleteCheckoutRequestDto
import com.rentify.app.core.network.dto.contract.ContractCreateRequestDto
import com.rentify.app.core.network.dto.contract.ContractDetailResponseDto
import com.rentify.app.core.network.dto.contract.ContractListResponseDto
import com.rentify.app.core.network.dto.contract.ContractMemberAddRequestDto
import com.rentify.app.core.network.dto.contract.ContractMemberLeaveRequestDto
import com.rentify.app.core.network.dto.contract.ContractMemberLeaveResponseDto
import com.rentify.app.core.network.dto.contract.ContractMemberListResponseDto
import com.rentify.app.core.network.dto.contract.ContractUpdateRequestDto
import com.rentify.app.core.network.dto.contract.DepositSettlementRequestDto

class LandlordContractRepositoryImpl(
    private val contractApiService: ContractApiService
) : BaseRepository(), LandlordContractRepository {

    override suspend fun getContracts(
        status: String?,
        roomId: String?,
        buildingId: String?,
        tenantId: String?,
        pageNo: Int,
        pageSize: Int
    ): NetworkResult<ApiListResponse<ContractListResponseDto>> {
        return safeApiCall {
            contractApiService.getContracts(
                status = status,
                roomId = roomId,
                buildingId = buildingId,
                tenantId = tenantId,
                pageNo = pageNo,
                pageSize = pageSize
            )
        }
    }

    override suspend fun createContract(
        request: ContractCreateRequestDto
    ): NetworkResult<ApiSingleResponse<ContractDetailResponseDto>> {
        return safeApiCall {
            contractApiService.createContract(request)
        }
    }

    override suspend fun getContractDetail(
        contractId: String
    ): NetworkResult<ApiSingleResponse<ContractDetailResponseDto>> {
        return safeApiCall {
            contractApiService.getContractDetail(contractId)
        }
    }

    override suspend fun updateContract(
        contractId: String,
        request: ContractUpdateRequestDto
    ): NetworkResult<ApiSingleResponse<ContractDetailResponseDto>> {
        return safeApiCall {
            contractApiService.updateContract(contractId, request)
        }
    }

    override suspend fun activateContract(
        contractId: String,
        signedContractMediaId: String
    ): NetworkResult<ApiSingleResponse<ContractDetailResponseDto>> {
        return safeApiCall {
            contractApiService.activateContract(
                contractId,
                ContractActivateRequestDto(signedContractMediaId = signedContractMediaId)
            )
        }
    }

    override suspend fun completeCheckout(
        contractId: String,
        completedOn: String
    ): NetworkResult<ApiSingleResponse<ContractDetailResponseDto>> {
        return safeApiCall {
            contractApiService.completeCheckout(
                contractId,
                ContractCompleteCheckoutRequestDto(completedOn = completedOn)
            )
        }
    }

    override suspend fun depositSettlement(
        contractId: String,
        request: DepositSettlementRequestDto
    ): NetworkResult<ApiSingleResponse<Any>> {
        return safeApiCall {
            contractApiService.depositSettlement(contractId, request)
        }
    }

    override suspend fun getContractMembers(
        contractId: String
    ): NetworkResult<ApiListResponse<ContractMemberListResponseDto>> {
        return safeApiCall {
            contractApiService.getContractMembers(contractId = contractId)
        }
    }

    override suspend fun addContractMember(
        request: ContractMemberAddRequestDto
    ): NetworkResult<ApiSingleResponse<ContractMemberListResponseDto>> {
        return safeApiCall {
            contractApiService.addContractMember(request)
        }
    }

    override suspend fun leaveContract(
        tenantId: String,
        request: ContractMemberLeaveRequestDto
    ): NetworkResult<ApiSingleResponse<ContractMemberLeaveResponseDto>> {
        return safeApiCall {
            contractApiService.leaveContract(tenantId, request)
        }
    }
}

