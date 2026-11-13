package com.rentify.app.core.network.api

import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.contract.ContractDetailResponseDto
import com.rentify.app.core.network.dto.contract.ContractMyListResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ContractApiService {

    @GET("me/contracts")
    suspend fun getMyContracts(
        @Query("filter[status]") status: String? = null,
        @Query("filter[roomId]") roomId: String? = null,
        @Query("pageNo") pageNo: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("format") format: String = "json"
    ): Response<ApiListResponse<ContractMyListResponseDto>>

    @GET("contracts/{contractId}")
    suspend fun getContractDetail(
        @Path("contractId") contractId: String
    ): Response<ApiSingleResponse<ContractDetailResponseDto>>
}
