package com.rentify.app.tenant.data.repository

import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.contract.ContractDetailResponseDto
import com.rentify.app.core.network.dto.contract.ContractMyListResponseDto

interface TenantContractRepository {

    suspend fun getMyContracts(
        status: String? = null,
        roomId: String? = null,
        pageNo: Int = 1,
        pageSize: Int = 20
    ): NetworkResult<ApiListResponse<ContractMyListResponseDto>>

    suspend fun getContractDetail(
        contractId: String
    ): NetworkResult<ApiSingleResponse<ContractDetailResponseDto>>
}

