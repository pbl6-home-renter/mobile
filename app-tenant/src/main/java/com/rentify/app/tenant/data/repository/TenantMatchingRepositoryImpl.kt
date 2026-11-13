package com.rentify.app.tenant.data.repository

import com.rentify.app.core.data.repository.BaseRepository
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.api.MatchingApiService
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.matching.MatchRequestAcceptResponseDto
import com.rentify.app.core.network.dto.matching.MatchRequestCreateRequestDto
import com.rentify.app.core.network.dto.matching.MatchRequestCreateResponseDto
import com.rentify.app.core.network.dto.matching.MatchRequestListResponseDto
import com.rentify.app.core.network.dto.matching.MatchRequestRejectResponseDto
import com.rentify.app.core.network.dto.matching.MatchRequestWithdrawResponseDto
import com.rentify.app.core.network.dto.matching.RoommateProfileDetailResponseDto
import com.rentify.app.core.network.dto.matching.RoommateProfileListResponseDto
import com.rentify.app.core.network.dto.matching.RoommateProfileMyResponseDto
import com.rentify.app.core.network.dto.matching.RoommateProfileSaveRequestDto
import com.rentify.app.core.network.dto.matching.RoommateProfileSaveResponseDto

class TenantMatchingRepositoryImpl(
    private val matchingApiService: MatchingApiService
) : BaseRepository(), TenantMatchingRepository {

    override suspend fun getRoommateProfiles(
        preferredArea: String?,
        pageNo: Int,
        pageSize: Int
    ): NetworkResult<ApiListResponse<RoommateProfileListResponseDto>> {
        return safeApiCall {
            matchingApiService.getRoommateProfiles(
                preferredArea = preferredArea,
                pageNo = pageNo,
                pageSize = pageSize
            )
        }
    }

    override suspend fun getRoommateProfileDetail(
        roommateProfileId: String
    ): NetworkResult<ApiSingleResponse<RoommateProfileDetailResponseDto>> {
        return safeApiCall {
            matchingApiService.getRoommateProfileDetail(roommateProfileId)
        }
    }

    override suspend fun getMyRoommateProfile(): NetworkResult<ApiSingleResponse<RoommateProfileMyResponseDto>> {
        return safeApiCall {
            matchingApiService.getMyRoommateProfile()
        }
    }

    override suspend fun saveMyRoommateProfile(
        request: RoommateProfileSaveRequestDto
    ): NetworkResult<ApiSingleResponse<RoommateProfileSaveResponseDto>> {
        return safeApiCall {
            matchingApiService.saveMyRoommateProfile(request)
        }
    }

    override suspend fun getMatchRequests(
        direction: String?,
        status: String?,
        pageNo: Int,
        pageSize: Int
    ): NetworkResult<ApiListResponse<MatchRequestListResponseDto>> {
        return safeApiCall {
            matchingApiService.getMatchRequests(
                direction = direction,
                status = status,
                pageNo = pageNo,
                pageSize = pageSize
            )
        }
    }

    override suspend fun createMatchRequest(
        targetId: String
    ): NetworkResult<ApiSingleResponse<MatchRequestCreateResponseDto>> {
        return safeApiCall {
            matchingApiService.createMatchRequest(
                MatchRequestCreateRequestDto(targetId = targetId)
            )
        }
    }

    override suspend fun acceptMatchRequest(
        matchRequestId: String
    ): NetworkResult<ApiSingleResponse<MatchRequestAcceptResponseDto>> {
        return safeApiCall {
            matchingApiService.acceptMatchRequest(matchRequestId)
        }
    }

    override suspend fun rejectMatchRequest(
        matchRequestId: String
    ): NetworkResult<ApiSingleResponse<MatchRequestRejectResponseDto>> {
        return safeApiCall {
            matchingApiService.rejectMatchRequest(matchRequestId)
        }
    }

    override suspend fun withdrawMatchRequest(
        matchRequestId: String
    ): NetworkResult<ApiSingleResponse<MatchRequestWithdrawResponseDto>> {
        return safeApiCall {
            matchingApiService.withdrawMatchRequest(matchRequestId)
        }
    }
}

