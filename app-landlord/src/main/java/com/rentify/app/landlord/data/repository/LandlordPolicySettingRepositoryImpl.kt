package com.rentify.app.landlord.data.repository

import com.rentify.app.core.data.repository.BaseRepository
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.api.BillingSettingApiService
import com.rentify.app.core.network.api.RatePolicyApiService
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.billing.BillingSettingCreateRequestDto
import com.rentify.app.core.network.dto.billing.BillingSettingResponseDto
import com.rentify.app.core.network.dto.billing.BillingSettingUpdateRequestDto
import com.rentify.app.core.network.dto.billing.BuildingRatePoliciesBatchCreateRequestDto
import com.rentify.app.core.network.dto.billing.RateItemDto
import com.rentify.app.core.network.dto.billing.RatePolicyCreateRequestDto
import com.rentify.app.core.network.dto.billing.RatePolicyResponseDto
import com.rentify.app.core.network.dto.billing.RatePolicyUpdateRequestDto

class LandlordPolicySettingRepositoryImpl(
    private val ratePolicyApiService: RatePolicyApiService,
    private val billingSettingApiService: BillingSettingApiService
) : BaseRepository(), LandlordPolicySettingRepository {

    override suspend fun getRatePolicies(
        scope: String?,
        buildingId: String?,
        roomId: String?
    ): NetworkResult<ApiListResponse<RatePolicyResponseDto>> {
        return safeApiCall {
            ratePolicyApiService.getRatePolicies(
                scope = scope,
                buildingId = buildingId,
                roomId = roomId
            )
        }
    }

    override suspend fun createRatePolicy(
        request: RatePolicyCreateRequestDto
    ): NetworkResult<ApiSingleResponse<RatePolicyResponseDto>> {
        return safeApiCall {
            ratePolicyApiService.createRatePolicy(request)
        }
    }

    override suspend fun updateRatePolicy(
        ratePolicyId: String,
        request: RatePolicyUpdateRequestDto
    ): NetworkResult<ApiSingleResponse<RatePolicyResponseDto>> {
        return safeApiCall {
            ratePolicyApiService.updateRatePolicy(ratePolicyId, request)
        }
    }

    override suspend fun deactivateRatePolicy(
        ratePolicyId: String
    ): NetworkResult<ApiSingleResponse<RatePolicyResponseDto>> {
        return safeApiCall {
            ratePolicyApiService.deactivateRatePolicy(ratePolicyId)
        }
    }

    override suspend fun saveBuildingRatePoliciesBatch(
        buildingId: String,
        rates: List<RateItemDto>
    ): NetworkResult<ApiSingleResponse<RatePolicyResponseDto>> {
        return safeApiCall {
            ratePolicyApiService.saveBuildingRatePoliciesBatch(
                buildingId = buildingId,
                request = BuildingRatePoliciesBatchCreateRequestDto(rates = rates)
            )
        }
    }

    override suspend fun getBillingSettings(
        scope: String?,
        buildingId: String?,
        roomId: String?
    ): NetworkResult<ApiListResponse<BillingSettingResponseDto>> {
        return safeApiCall {
            billingSettingApiService.getBillingSettings(
                scope = scope,
                buildingId = buildingId,
                roomId = roomId
            )
        }
    }

    override suspend fun createBillingSetting(
        request: BillingSettingCreateRequestDto
    ): NetworkResult<ApiSingleResponse<BillingSettingResponseDto>> {
        return safeApiCall {
            billingSettingApiService.createBillingSetting(request)
        }
    }

    override suspend fun updateBillingSetting(
        billingSettingId: String,
        request: BillingSettingUpdateRequestDto
    ): NetworkResult<ApiSingleResponse<BillingSettingResponseDto>> {
        return safeApiCall {
            billingSettingApiService.updateBillingSetting(billingSettingId, request)
        }
    }
}
