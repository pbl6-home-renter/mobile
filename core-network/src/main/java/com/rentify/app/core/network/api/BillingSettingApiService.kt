package com.rentify.app.core.network.api

import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.billing.BillingSettingCreateRequestDto
import com.rentify.app.core.network.dto.billing.BillingSettingResponseDto
import com.rentify.app.core.network.dto.billing.BillingSettingUpdateRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface BillingSettingApiService {

    @GET("billing-settings")
    suspend fun getBillingSettings(
        @Query("filter[scope]") scope: String? = null,
        @Query("filter[buildingId]") buildingId: String? = null,
        @Query("filter[roomId]") roomId: String? = null,
        @Query("pageNo") pageNo: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<ApiListResponse<BillingSettingResponseDto>>

    @POST("billing-settings")
    suspend fun createBillingSetting(
        @Body request: BillingSettingCreateRequestDto
    ): Response<ApiSingleResponse<BillingSettingResponseDto>>

    @PATCH("billing-settings/{billingSettingId}")
    suspend fun updateBillingSetting(
        @Path("billingSettingId") billingSettingId: String,
        @Body request: BillingSettingUpdateRequestDto
    ): Response<ApiSingleResponse<BillingSettingResponseDto>>
}
