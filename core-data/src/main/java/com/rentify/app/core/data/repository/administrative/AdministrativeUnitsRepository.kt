package com.rentify.app.core.data.repository.administrative

import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.administrative.AdministrativeProvinceDto
import com.rentify.app.core.network.dto.administrative.AdministrativeWardDto

interface AdministrativeUnitsRepository {
    suspend fun getProvinces(forceRefresh: Boolean = false): NetworkResult<ApiListResponse<AdministrativeProvinceDto>>
    suspend fun getWards(provinceCode: String, forceRefresh: Boolean = false): NetworkResult<ApiListResponse<AdministrativeWardDto>>
}

