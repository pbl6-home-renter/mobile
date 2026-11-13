package com.rentify.app.core.network.api

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
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface MatchingApiService {

    @GET("roommate-profiles")
    suspend fun getRoommateProfiles(
        @Query("filter[preferredArea]") preferredArea: String? = null,
        @Query("pageNo") pageNo: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("format") format: String = "json"
    ): Response<ApiListResponse<RoommateProfileListResponseDto>>

    @GET("roommate-profiles/{roommateProfileId}")
    suspend fun getRoommateProfileDetail(
        @Path("roommateProfileId") roommateProfileId: String
    ): Response<ApiSingleResponse<RoommateProfileDetailResponseDto>>

    @GET("me/roommate-profile")
    suspend fun getMyRoommateProfile(): Response<ApiSingleResponse<RoommateProfileMyResponseDto>>

    @PUT("me/roommate-profile")
    suspend fun saveMyRoommateProfile(
        @Body request: RoommateProfileSaveRequestDto
    ): Response<ApiSingleResponse<RoommateProfileSaveResponseDto>>

    @GET("match-requests")
    suspend fun getMatchRequests(
        @Query("filter[direction]") direction: String? = null,
        @Query("filter[status]") status: String? = null,
        @Query("pageNo") pageNo: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("format") format: String = "json"
    ): Response<ApiListResponse<MatchRequestListResponseDto>>

    @POST("match-requests")
    suspend fun createMatchRequest(
        @Body request: MatchRequestCreateRequestDto
    ): Response<ApiSingleResponse<MatchRequestCreateResponseDto>>

    @POST("match-requests/{matchRequestId}/accept-request")
    suspend fun acceptMatchRequest(
        @Path("matchRequestId") matchRequestId: String
    ): Response<ApiSingleResponse<MatchRequestAcceptResponseDto>>

    @POST("match-requests/{matchRequestId}/reject-request")
    suspend fun rejectMatchRequest(
        @Path("matchRequestId") matchRequestId: String
    ): Response<ApiSingleResponse<MatchRequestRejectResponseDto>>

    @POST("match-requests/{matchRequestId}/withdraw-request")
    suspend fun withdrawMatchRequest(
        @Path("matchRequestId") matchRequestId: String
    ): Response<ApiSingleResponse<MatchRequestWithdrawResponseDto>>
}

