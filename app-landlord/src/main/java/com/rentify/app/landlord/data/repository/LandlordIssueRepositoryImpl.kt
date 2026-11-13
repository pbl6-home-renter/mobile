package com.rentify.app.landlord.data.repository

import com.rentify.app.core.data.repository.BaseRepository
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.api.IssueApiService
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.issue.IssueReportCancelResponseDto
import com.rentify.app.core.network.dto.issue.IssueReportDetailResponseDto
import com.rentify.app.core.network.dto.issue.IssueReportListResponseDto
import com.rentify.app.core.network.dto.issue.IssueReportUpdateRequestDto
import com.rentify.app.core.network.dto.issue.IssueReportUpdateResponseDto

class LandlordIssueRepositoryImpl(
    private val issueApiService: IssueApiService
) : BaseRepository(), LandlordIssueRepository {

    override suspend fun getIssues(
        status: String?,
        roomId: String?,
        buildingId: String?,
        sort: String?,
        order: String?,
        pageNo: Int,
        pageSize: Int
    ): NetworkResult<ApiListResponse<IssueReportListResponseDto>> {
        return safeApiCall {
            issueApiService.getLandlordIssues(
                status = status,
                roomId = roomId,
                buildingId = buildingId,
                sort = sort,
                order = order,
                pageNo = pageNo,
                pageSize = pageSize
            )
        }
    }

    override suspend fun getIssueDetail(
        issueId: String
    ): NetworkResult<ApiSingleResponse<IssueReportDetailResponseDto>> {
        return safeApiCall {
            issueApiService.getIssueDetail(issueId)
        }
    }

    override suspend fun updateIssue(
        issueId: String,
        request: IssueReportUpdateRequestDto
    ): NetworkResult<ApiSingleResponse<IssueReportUpdateResponseDto>> {
        return safeApiCall {
            issueApiService.updateIssue(issueId, request)
        }
    }

    override suspend fun cancelIssue(
        issueId: String
    ): NetworkResult<ApiSingleResponse<IssueReportCancelResponseDto>> {
        return safeApiCall {
            issueApiService.cancelIssue(issueId)
        }
    }
}

