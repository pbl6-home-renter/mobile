package com.rentify.app.landlord.data.repository

import com.rentify.app.core.data.repository.BaseRepository
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.api.BuildingApiService
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.building.BuildingCreateRequestDto
import com.rentify.app.core.network.dto.building.BuildingMyDetailResponseDto
import com.rentify.app.core.network.dto.building.BuildingMyListResponseDto
import com.rentify.app.core.network.dto.building.BuildingUpdateRequestDto
import com.rentify.app.core.network.dto.building.BuildingVietQrConfigRequestDto
import com.rentify.app.core.network.dto.building.BuildingVietQrConfigResponseDto

class LandlordBuildingRepositoryImpl(
    private val buildingApiService: BuildingApiService
) : BaseRepository(), LandlordBuildingRepository {

    override suspend fun getMyBuildings(
        search: String?,
        status: String?,
        pageNo: Int,
        pageSize: Int
    ): NetworkResult<ApiListResponse<BuildingMyListResponseDto>> {
        return safeApiCall {
            buildingApiService.getMyBuildings(
                search = search,
                status = status,
                pageNo = pageNo,
                pageSize = pageSize
            )
        }
    }

    override suspend fun createBuilding(
        request: BuildingCreateRequestDto
    ): NetworkResult<ApiSingleResponse<BuildingMyDetailResponseDto>> {
        return safeApiCall {
            buildingApiService.createBuilding(request)
        }
    }

    override suspend fun getMyBuildingDetail(
        buildingId: String
    ): NetworkResult<ApiSingleResponse<BuildingMyDetailResponseDto>> {
        return safeApiCall {
            buildingApiService.getMyBuildingDetail(buildingId)
        }
    }

    override suspend fun updateBuilding(
        buildingId: String,
        request: BuildingUpdateRequestDto
    ): NetworkResult<ApiSingleResponse<BuildingMyDetailResponseDto>> {
        return safeApiCall {
            buildingApiService.updateBuilding(buildingId, request)
        }
    }

    override suspend fun deleteBuilding(
        buildingId: String
    ): NetworkResult<Unit> {
        return safeApiCall {
            buildingApiService.deleteBuilding(buildingId)
        }
    }

    override suspend fun configureVietQr(
        buildingId: String,
        request: BuildingVietQrConfigRequestDto
    ): NetworkResult<ApiSingleResponse<BuildingVietQrConfigResponseDto>> {
        return safeApiCall {
            buildingApiService.configureVietQr(buildingId, request)
        }
    }
}

