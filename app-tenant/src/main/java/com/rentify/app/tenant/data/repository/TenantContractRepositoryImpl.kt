package com.rentify.app.tenant.data.repository

import com.rentify.app.core.data.repository.BaseRepository
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.api.ContractApiService
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.contract.ContractDetailResponseDto
import com.rentify.app.core.network.dto.contract.ContractMyListResponseDto

class TenantContractRepositoryImpl(
    private val contractApiService: ContractApiService
) : BaseRepository(), TenantContractRepository {

    override suspend fun getMyContracts(
        status: String?,
        roomId: String?,
        pageNo: Int,
        pageSize: Int
    ): NetworkResult<ApiListResponse<ContractMyListResponseDto>> {
        return safeApiCall {
            contractApiService.getMyContracts(
                status = status,
                roomId = roomId,
                pageNo = pageNo,
                pageSize = pageSize
            )
        }
    }

    override suspend fun getContractDetail(
        contractId: String
    ): NetworkResult<ApiSingleResponse<ContractDetailResponseDto>> {
        return safeApiCall {
            contractApiService.getContractDetail(contractId)
        }
    }
}

