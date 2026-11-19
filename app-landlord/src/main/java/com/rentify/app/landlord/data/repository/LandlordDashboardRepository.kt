package com.rentify.app.landlord.data.repository

import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.invoice.InvoiceListResponseDto
import com.rentify.app.core.network.dto.issue.IssueReportListResponseDto

data class LandlordDashboardMetrics(
    val totalBuildings: Int,
    val pendingApprovalBuildings: Int,
    val totalRooms: Int,
    val occupancyRate: String,
    val activeContracts: Int,
    val recentIssues: List<IssueReportListResponseDto> = emptyList(),
    val recentInvoices: List<InvoiceListResponseDto> = emptyList()
)

interface LandlordDashboardRepository {
    suspend fun getDashboard(): NetworkResult<LandlordDashboardMetrics>
}
