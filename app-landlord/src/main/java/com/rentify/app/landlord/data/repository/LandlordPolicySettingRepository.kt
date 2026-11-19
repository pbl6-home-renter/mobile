package com.rentify.app.landlord.data.repository

import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.billing.BillingSettingCreateRequestDto
import com.rentify.app.core.network.dto.billing.BillingSettingResponseDto
import com.rentify.app.core.network.dto.billing.BillingSettingUpdateRequestDto
import com.rentify.app.core.network.dto.billing.RateItemDto
import com.rentify.app.core.network.dto.billing.RatePolicyCreateRequestDto
import com.rentify.app.core.network.dto.billing.RatePolicyResponseDto
import com.rentify.app.core.network.dto.billing.RatePolicyUpdateRequestDto

interface LandlordPolicySettingRepository {
    suspend fun getRatePolicies(
        scope: String? = null,
        buildingId: String? = null,
        roomId: String? = null
    ): NetworkResult<ApiListResponse<RatePolicyResponseDto>>

    suspend fun createRatePolicy(
        request: RatePolicyCreateRequestDto
    ): NetworkResult<ApiSingleResponse<RatePolicyResponseDto>>

    suspend fun updateRatePolicy(
        ratePolicyId: String,
        request: RatePolicyUpdateRequestDto
    ): NetworkResult<ApiSingleResponse<RatePolicyResponseDto>>

    suspend fun deactivateRatePolicy(
        ratePolicyId: String
    ): NetworkResult<ApiSingleResponse<RatePolicyResponseDto>>

    suspend fun saveBuildingRatePoliciesBatch(
        buildingId: String,
        rates: List<RateItemDto>
    ): NetworkResult<ApiSingleResponse<RatePolicyResponseDto>>

    suspend fun getBillingSettings(
        scope: String? = null,
        buildingId: String? = null,
        roomId: String? = null
    ): NetworkResult<ApiListResponse<BillingSettingResponseDto>>

    suspend fun createBillingSetting(
        request: BillingSettingCreateRequestDto
    ): NetworkResult<ApiSingleResponse<BillingSettingResponseDto>>

    suspend fun updateBillingSetting(
        billingSettingId: String,
        request: BillingSettingUpdateRequestDto
    ): NetworkResult<ApiSingleResponse<BillingSettingResponseDto>>
}
