package com.rentify.app.tenant.data.repository

import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.issue.IssueReportCancelResponseDto
import com.rentify.app.core.network.dto.issue.IssueReportCreateRequestDto
import com.rentify.app.core.network.dto.issue.IssueReportCreateResponseDto
import com.rentify.app.core.network.dto.issue.IssueReportDetailResponseDto
import com.rentify.app.core.network.dto.issue.IssueReportMyListResponseDto

interface TenantIssueRepository {

    suspend fun getMyIssues(
        status: String? = null,
        roomId: String? = null,
        pageNo: Int = 1,
        pageSize: Int = 20
    ): NetworkResult<ApiListResponse<IssueReportMyListResponseDto>>

    suspend fun createIssue(
        request: IssueReportCreateRequestDto
    ): NetworkResult<ApiSingleResponse<IssueReportCreateResponseDto>>

    suspend fun getIssueDetail(
        issueId: String
    ): NetworkResult<ApiSingleResponse<IssueReportDetailResponseDto>>

    suspend fun cancelIssue(
        issueId: String
    ): NetworkResult<ApiSingleResponse<IssueReportCancelResponseDto>>
}

