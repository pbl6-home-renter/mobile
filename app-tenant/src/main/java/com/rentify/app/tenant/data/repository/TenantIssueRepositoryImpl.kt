package com.rentify.app.tenant.data.repository

import com.rentify.app.core.data.repository.BaseRepository
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.api.IssueApiService
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.issue.IssueReportCancelResponseDto
import com.rentify.app.core.network.dto.issue.IssueReportCreateRequestDto
import com.rentify.app.core.network.dto.issue.IssueReportCreateResponseDto
import com.rentify.app.core.network.dto.issue.IssueReportDetailResponseDto
import com.rentify.app.core.network.dto.issue.IssueReportMyListResponseDto

class TenantIssueRepositoryImpl(
    private val issueApiService: IssueApiService
) : BaseRepository(), TenantIssueRepository {

    override suspend fun getMyIssues(
        status: String?,
        roomId: String?,
        pageNo: Int,
        pageSize: Int
    ): NetworkResult<ApiListResponse<IssueReportMyListResponseDto>> {
        return safeApiCall {
            issueApiService.getMyIssues(
                status = status,
                roomId = roomId,
                pageNo = pageNo,
                pageSize = pageSize
            )
        }
    }

    override suspend fun createIssue(
        request: IssueReportCreateRequestDto
    ): NetworkResult<ApiSingleResponse<IssueReportCreateResponseDto>> {
        return safeApiCall {
            issueApiService.createIssue(request)
        }
    }

    override suspend fun getIssueDetail(
        issueId: String
    ): NetworkResult<ApiSingleResponse<IssueReportDetailResponseDto>> {
        return safeApiCall {
            issueApiService.getIssueDetail(issueId)
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

