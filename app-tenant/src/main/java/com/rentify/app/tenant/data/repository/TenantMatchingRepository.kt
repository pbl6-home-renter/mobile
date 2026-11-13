package com.rentify.app.tenant.data.repository

import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.matching.MatchRequestAcceptResponseDto
import com.rentify.app.core.network.dto.matching.MatchRequestCreateResponseDto
import com.rentify.app.core.network.dto.matching.MatchRequestListResponseDto
import com.rentify.app.core.network.dto.matching.MatchRequestRejectResponseDto
import com.rentify.app.core.network.dto.matching.MatchRequestWithdrawResponseDto
import com.rentify.app.core.network.dto.matching.RoommateProfileDetailResponseDto
import com.rentify.app.core.network.dto.matching.RoommateProfileListResponseDto
import com.rentify.app.core.network.dto.matching.RoommateProfileMyResponseDto
import com.rentify.app.core.network.dto.matching.RoommateProfileSaveRequestDto
import com.rentify.app.core.network.dto.matching.RoommateProfileSaveResponseDto

interface TenantMatchingRepository {

    suspend fun getRoommateProfiles(
        preferredArea: String? = null,
        pageNo: Int = 1,
        pageSize: Int = 20
    ): NetworkResult<ApiListResponse<RoommateProfileListResponseDto>>

    suspend fun getRoommateProfileDetail(
        roommateProfileId: String
    ): NetworkResult<ApiSingleResponse<RoommateProfileDetailResponseDto>>

    suspend fun getMyRoommateProfile(): NetworkResult<ApiSingleResponse<RoommateProfileMyResponseDto>>

    suspend fun saveMyRoommateProfile(
        request: RoommateProfileSaveRequestDto
    ): NetworkResult<ApiSingleResponse<RoommateProfileSaveResponseDto>>

    suspend fun getMatchRequests(
        direction: String? = null,
        status: String? = null,
        pageNo: Int = 1,
        pageSize: Int = 20
    ): NetworkResult<ApiListResponse<MatchRequestListResponseDto>>

    suspend fun createMatchRequest(
        targetId: String
    ): NetworkResult<ApiSingleResponse<MatchRequestCreateResponseDto>>

    suspend fun acceptMatchRequest(
        matchRequestId: String
    ): NetworkResult<ApiSingleResponse<MatchRequestAcceptResponseDto>>

    suspend fun rejectMatchRequest(
        matchRequestId: String
    ): NetworkResult<ApiSingleResponse<MatchRequestRejectResponseDto>>

    suspend fun withdrawMatchRequest(
        matchRequestId: String
    ): NetworkResult<ApiSingleResponse<MatchRequestWithdrawResponseDto>>
}

