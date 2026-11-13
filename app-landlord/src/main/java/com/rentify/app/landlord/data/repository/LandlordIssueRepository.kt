package com.rentify.app.landlord.data.repository

import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.issue.IssueReportCancelResponseDto
import com.rentify.app.core.network.dto.issue.IssueReportDetailResponseDto
import com.rentify.app.core.network.dto.issue.IssueReportListResponseDto
import com.rentify.app.core.network.dto.issue.IssueReportUpdateRequestDto
import com.rentify.app.core.network.dto.issue.IssueReportUpdateResponseDto

interface LandlordIssueRepository {
    suspend fun getIssues(
        status: String? = null,
        roomId: String? = null,
        buildingId: String? = null,
        sort: String? = "createdAt",
        order: String? = "desc",
        pageNo: Int = 1,
        pageSize: Int = 50
    ): NetworkResult<ApiListResponse<IssueReportListResponseDto>>

    suspend fun getIssueDetail(
        issueId: String
    ): NetworkResult<ApiSingleResponse<IssueReportDetailResponseDto>>

    suspend fun updateIssue(
        issueId: String,
        request: IssueReportUpdateRequestDto
    ): NetworkResult<ApiSingleResponse<IssueReportUpdateResponseDto>>

    suspend fun cancelIssue(
        issueId: String
    ): NetworkResult<ApiSingleResponse<IssueReportCancelResponseDto>>
}

