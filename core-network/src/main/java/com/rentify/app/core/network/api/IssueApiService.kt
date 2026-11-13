package com.rentify.app.core.network.api

import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.issue.IssueReportCancelResponseDto
import com.rentify.app.core.network.dto.issue.IssueReportCreateRequestDto
import com.rentify.app.core.network.dto.issue.IssueReportCreateResponseDto
import com.rentify.app.core.network.dto.issue.IssueReportDetailResponseDto
import com.rentify.app.core.network.dto.issue.IssueReportListResponseDto
import com.rentify.app.core.network.dto.issue.IssueReportMyListResponseDto
import com.rentify.app.core.network.dto.issue.IssueReportUpdateRequestDto
import com.rentify.app.core.network.dto.issue.IssueReportUpdateResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface IssueApiService {

    @GET("me/issues")
    suspend fun getMyIssues(
        @Query("filter[status]") status: String? = null,
        @Query("filter[roomId]") roomId: String? = null,
        @Query("sort") sort: String? = "createdAt",
        @Query("order") order: String? = "desc",
        @Query("pageNo") pageNo: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("format") format: String = "json"
    ): Response<ApiListResponse<IssueReportMyListResponseDto>>

    @GET("issues")
    suspend fun getLandlordIssues(
        @Query("filter[status]") status: String? = null,
        @Query("filter[roomId]") roomId: String? = null,
        @Query("filter[buildingId]") buildingId: String? = null,
        @Query("sort") sort: String? = "createdAt",
        @Query("order") order: String? = "desc",
        @Query("pageNo") pageNo: Int = 1,
        @Query("pageSize") pageSize: Int = 50
    ): Response<ApiListResponse<IssueReportListResponseDto>>

    @POST("issues")
    suspend fun createIssue(
        @Body request: IssueReportCreateRequestDto
    ): Response<ApiSingleResponse<IssueReportCreateResponseDto>>

    @GET("issues/{issueId}")
    suspend fun getIssueDetail(
        @Path("issueId") issueId: String
    ): Response<ApiSingleResponse<IssueReportDetailResponseDto>>

    @PATCH("issues/{issueId}")
    suspend fun updateIssue(
        @Path("issueId") issueId: String,
        @Body request: IssueReportUpdateRequestDto
    ): Response<ApiSingleResponse<IssueReportUpdateResponseDto>>

    @POST("issues/{issueId}/cancel")
    suspend fun cancelIssue(
        @Path("issueId") issueId: String
    ): Response<ApiSingleResponse<IssueReportCancelResponseDto>>
}

