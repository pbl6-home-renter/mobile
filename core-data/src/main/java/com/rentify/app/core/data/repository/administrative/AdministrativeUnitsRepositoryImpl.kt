package com.rentify.app.core.data.repository.administrative

import com.rentify.app.core.data.repository.BaseRepository
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.api.AdministrativeUnitsApiService
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.administrative.AdministrativeProvinceDto
import com.rentify.app.core.network.dto.administrative.AdministrativeWardDto
import java.util.concurrent.ConcurrentHashMap

class AdministrativeUnitsRepositoryImpl(
    private val apiService: AdministrativeUnitsApiService
) : BaseRepository(), AdministrativeUnitsRepository {

    companion object {
        @Volatile
        private var cachedProvinces: ApiListResponse<AdministrativeProvinceDto>? = null
        private val cachedWards = ConcurrentHashMap<String, ApiListResponse<AdministrativeWardDto>>()
    }

    override suspend fun getProvinces(forceRefresh: Boolean): NetworkResult<ApiListResponse<AdministrativeProvinceDto>> {
        if (!forceRefresh) {
            cachedProvinces?.let { return NetworkResult.Success(it) }
        }
        val result = safeApiCall { apiService.getProvinces() }
        if (result is NetworkResult.Success) {
            cachedProvinces = result.data
        }
        return result
    }

    override suspend fun getWards(
        provinceCode: String,
        forceRefresh: Boolean
    ): NetworkResult<ApiListResponse<AdministrativeWardDto>> {
        if (!forceRefresh) {
            cachedWards[provinceCode]?.let { return NetworkResult.Success(it) }
        }
        val result = safeApiCall { apiService.getWards(provinceCode) }
        if (result is NetworkResult.Success) {
            cachedWards[provinceCode] = result.data
        }
        return result
    }
}
