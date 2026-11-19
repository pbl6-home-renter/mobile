package com.rentify.app.landlord.data.repository

import com.rentify.app.core.network.adapter.NetworkResult

class LandlordDashboardRepositoryImpl(
    private val buildingRepository: LandlordBuildingRepository,
    private val contractRepository: LandlordContractRepository,
    private val issueRepository: LandlordIssueRepository,
    private val invoiceRepository: LandlordInvoiceRepository
) : LandlordDashboardRepository {

    override suspend fun getDashboard(): NetworkResult<LandlordDashboardMetrics> {
        val buildingsResult = buildingRepository.getMyBuildings(pageSize = BUILDING_PAGE_SIZE)
        val buildings = when (buildingsResult) {
            is NetworkResult.Success -> buildingsResult.data.data
            is NetworkResult.ApiError -> return buildingsResult
            is NetworkResult.NetworkError -> return buildingsResult
            is NetworkResult.UnknownError -> return buildingsResult
        }

        val contractsResult = contractRepository.getContracts(status = ACTIVE_STATUS, pageSize = 1)
        val activeContracts = when (contractsResult) {
            is NetworkResult.Success -> contractsResult.data.pagination?.totalItems ?: contractsResult.data.data.size
            is NetworkResult.ApiError -> return contractsResult
            is NetworkResult.NetworkError -> return contractsResult
            is NetworkResult.UnknownError -> return contractsResult
        }

        val recentIssues = when (val issuesResult = issueRepository.getIssues(pageSize = RECENT_PAGE_SIZE)) {
            is NetworkResult.Success -> issuesResult.data.data
                .filter { it.status.equals("open", ignoreCase = true) || it.status.equals("in_progress", ignoreCase = true) }
                .take(3)
            else -> emptyList()
        }

        val recentInvoices = when (val invoicesResult = invoiceRepository.getInvoices(pageSize = 3)) {
            is NetworkResult.Success -> invoicesResult.data.data.take(3)
            else -> emptyList()
        }

        val totalRooms = buildings.sumOf { it.totalRooms }
        val occupiedRooms = buildings.sumOf { it.occupiedRooms }
        val occupancyPercent = if (totalRooms > 0) Math.round(occupiedRooms * 100f / totalRooms) else 0

        return NetworkResult.Success(
            LandlordDashboardMetrics(
                totalBuildings = buildings.size,
                pendingApprovalBuildings = buildings.count { it.status.equals(PENDING_STATUS, ignoreCase = true) },
                totalRooms = totalRooms,
                occupancyRate = "$occupancyPercent%",
                activeContracts = activeContracts,
                recentIssues = recentIssues,
                recentInvoices = recentInvoices
            )
        )
    }

    private companion object {
        const val BUILDING_PAGE_SIZE = 100
        const val RECENT_PAGE_SIZE = 10
        const val ACTIVE_STATUS = "active"
        const val PENDING_STATUS = "pending"
    }
}
