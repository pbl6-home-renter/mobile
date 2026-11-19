package com.rentify.app.core.network.api

import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.billing.BuildingRatePoliciesBatchCreateRequestDto
import com.rentify.app.core.network.dto.billing.RatePolicyCreateRequestDto
import com.rentify.app.core.network.dto.billing.RatePolicyResponseDto
import com.rentify.app.core.network.dto.billing.RatePolicyUpdateRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface RatePolicyApiService {

    @GET("rate-policies")
    suspend fun getRatePolicies(
        @Query("filter[scope]") scope: String? = null,
        @Query("filter[buildingId]") buildingId: String? = null,
        @Query("filter[roomId]") roomId: String? = null,
        @Query("filter[type]") type: String? = null,
        @Query("sort") sort: String? = "createdAt",
        @Query("order") order: String? = "desc",
        @Query("pageNo") pageNo: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<ApiListResponse<RatePolicyResponseDto>>

    @POST("rate-policies")
    suspend fun createRatePolicy(
        @Body request: RatePolicyCreateRequestDto
    ): Response<ApiSingleResponse<RatePolicyResponseDto>>

    @PATCH("rate-policies/{ratePolicyId}")
    suspend fun updateRatePolicy(
        @Path("ratePolicyId") ratePolicyId: String,
        @Body request: RatePolicyUpdateRequestDto
    ): Response<ApiSingleResponse<RatePolicyResponseDto>>

    @POST("rate-policies/{ratePolicyId}/deactivate-policy")
    suspend fun deactivateRatePolicy(
        @Path("ratePolicyId") ratePolicyId: String
    ): Response<ApiSingleResponse<RatePolicyResponseDto>>

    @POST("buildings/{buildingId}/rate-policies/batch")
    suspend fun saveBuildingRatePoliciesBatch(
        @Path("buildingId") buildingId: String,
        @Body request: BuildingRatePoliciesBatchCreateRequestDto
    ): Response<ApiSingleResponse<RatePolicyResponseDto>>
}
