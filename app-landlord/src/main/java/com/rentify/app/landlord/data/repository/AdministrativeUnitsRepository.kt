package com.rentify.app.landlord.data.repository

import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.administrative.AdministrativeProvinceDto
import com.rentify.app.core.network.dto.administrative.AdministrativeWardDto

interface AdministrativeUnitsRepository {
    suspend fun getProvinces(): NetworkResult<ApiListResponse<AdministrativeProvinceDto>>
    suspend fun getWards(provinceCode: String): NetworkResult<ApiListResponse<AdministrativeWardDto>>
}

