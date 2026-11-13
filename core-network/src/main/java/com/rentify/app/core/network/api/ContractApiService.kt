package com.rentify.app.core.network.api

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
import com.rentify.app.core.network.dto.contract.ContractMyListResponseDto
import com.rentify.app.core.network.dto.contract.ContractUpdateRequestDto
import com.rentify.app.core.network.dto.contract.DepositSettlementRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
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

    @GET("contracts")
    suspend fun getContracts(
        @Query("filter[status]") status: String? = null,
        @Query("filter[roomId]") roomId: String? = null,
        @Query("filter[buildingId]") buildingId: String? = null,
        @Query("filter[tenantId]") tenantId: String? = null,
        @Query("sort") sort: String? = "createdAt",
        @Query("order") order: String? = "desc",
        @Query("pageNo") pageNo: Int = 1,
        @Query("pageSize") pageSize: Int = 50,
        @Query("format") format: String = "json"
    ): Response<ApiListResponse<ContractListResponseDto>>

    @POST("contracts")
    suspend fun createContract(
        @Body request: ContractCreateRequestDto
    ): Response<ApiSingleResponse<ContractDetailResponseDto>>

    @GET("contracts/{contractId}")
    suspend fun getContractDetail(
        @Path("contractId") contractId: String
    ): Response<ApiSingleResponse<ContractDetailResponseDto>>

    @PATCH("contracts/{contractId}")
    suspend fun updateContract(
        @Path("contractId") contractId: String,
        @Body request: ContractUpdateRequestDto
    ): Response<ApiSingleResponse<ContractDetailResponseDto>>

    @POST("contracts/{contractId}/activate-contract")
    suspend fun activateContract(
        @Path("contractId") contractId: String,
        @Body request: ContractActivateRequestDto
    ): Response<ApiSingleResponse<ContractDetailResponseDto>>

    @POST("contracts/{contractId}/complete-checkout")
    suspend fun completeCheckout(
        @Path("contractId") contractId: String,
        @Body request: ContractCompleteCheckoutRequestDto
    ): Response<ApiSingleResponse<ContractDetailResponseDto>>

    @POST("contracts/{contractId}/deposit-settlement")
    suspend fun depositSettlement(
        @Path("contractId") contractId: String,
        @Body request: DepositSettlementRequestDto
    ): Response<ApiSingleResponse<Any>>

    @GET("contract-members")
    suspend fun getContractMembers(
        @Query("filter[contractId]") contractId: String? = null,
        @Query("filter[status]") status: String? = null,
        @Query("pageNo") pageNo: Int = 1,
        @Query("pageSize") pageSize: Int = 50,
        @Query("format") format: String = "json"
    ): Response<ApiListResponse<ContractMemberListResponseDto>>

    @POST("contract-members")
    suspend fun addContractMember(
        @Body request: ContractMemberAddRequestDto
    ): Response<ApiSingleResponse<ContractMemberListResponseDto>>

    @POST("contract-members/{tenantId}/leave-contract")
    suspend fun leaveContract(
        @Path("tenantId") tenantId: String,
        @Body request: ContractMemberLeaveRequestDto
    ): Response<ApiSingleResponse<ContractMemberLeaveResponseDto>>
}
