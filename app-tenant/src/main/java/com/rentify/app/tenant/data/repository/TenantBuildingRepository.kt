package com.rentify.app.tenant.data.repository

import com.rentify.app.core.network.adapter.NetworkResult

interface TenantBuildingRepository {
    suspend fun getBuildings(
        province: String? = null,
        ward: String? = null
    ): NetworkResult<List<BuildingSummary>>

    suspend fun getBuilding(buildingId: String): NetworkResult<BuildingSummary>
}
