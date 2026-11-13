package com.rentify.app.landlord.data.repository

import com.rentify.app.core.data.repository.BaseRepository
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.api.AdministrativeUnitsApiService
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.administrative.AdministrativeProvinceDto
import com.rentify.app.core.network.dto.administrative.AdministrativeWardDto

class AdministrativeUnitsRepositoryImpl(
    private val administrativeUnitsApiService: AdministrativeUnitsApiService
) : BaseRepository(), AdministrativeUnitsRepository {

    override suspend fun getProvinces(): NetworkResult<ApiListResponse<AdministrativeProvinceDto>> {
        return safeApiCall {
            administrativeUnitsApiService.getProvinces()
        }
    }

    override suspend fun getWards(provinceCode: String): NetworkResult<ApiListResponse<AdministrativeWardDto>> {
        return safeApiCall {
            administrativeUnitsApiService.getWards(provinceCode)
        }
    }
}

