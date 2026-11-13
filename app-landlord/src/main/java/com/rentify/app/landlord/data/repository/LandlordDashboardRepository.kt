package com.rentify.app.landlord.data.repository

import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.dashboard.DashboardResponseDto

interface LandlordDashboardRepository {
    suspend fun getDashboard(from: String? = null, to: String? = null): NetworkResult<ApiSingleResponse<DashboardResponseDto>>
}

