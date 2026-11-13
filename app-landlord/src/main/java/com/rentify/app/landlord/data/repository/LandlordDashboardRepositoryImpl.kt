package com.rentify.app.landlord.data.repository

import com.rentify.app.core.data.repository.BaseRepository
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.api.DashboardApiService
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.dashboard.DashboardResponseDto

class LandlordDashboardRepositoryImpl(
    private val dashboardApiService: DashboardApiService
) : BaseRepository(), LandlordDashboardRepository {

    override suspend fun getDashboard(
        from: String?,
        to: String?
    ): NetworkResult<ApiSingleResponse<DashboardResponseDto>> {
        return safeApiCall {
            dashboardApiService.getDashboard(from = from, to = to)
        }
    }
}

