package com.rentify.app.landlord.data.repository

import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.building.BuildingCreateRequestDto
import com.rentify.app.core.network.dto.building.BuildingMyDetailResponseDto
import com.rentify.app.core.network.dto.building.BuildingMyListResponseDto
import com.rentify.app.core.network.dto.building.BuildingUpdateRequestDto
import com.rentify.app.core.network.dto.building.BuildingVietQrConfigRequestDto
import com.rentify.app.core.network.dto.building.BuildingVietQrConfigResponseDto

interface LandlordBuildingRepository {
    suspend fun getMyBuildings(
        search: String? = null,
        status: String? = null,
        pageNo: Int = 1,
        pageSize: Int = 50
    ): NetworkResult<ApiListResponse<BuildingMyListResponseDto>>

    suspend fun createBuilding(
        request: BuildingCreateRequestDto
    ): NetworkResult<ApiSingleResponse<BuildingMyDetailResponseDto>>

    suspend fun getMyBuildingDetail(
        buildingId: String
    ): NetworkResult<ApiSingleResponse<BuildingMyDetailResponseDto>>

    suspend fun updateBuilding(
        buildingId: String,
        request: BuildingUpdateRequestDto
    ): NetworkResult<ApiSingleResponse<BuildingMyDetailResponseDto>>

    suspend fun deleteBuilding(
        buildingId: String
    ): NetworkResult<Unit>

    suspend fun configureVietQr(
        buildingId: String,
        request: BuildingVietQrConfigRequestDto
    ): NetworkResult<ApiSingleResponse<BuildingVietQrConfigResponseDto>>
}

