package com.rentify.app.core.network.api

import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.building.BuildingCreateRequestDto
import com.rentify.app.core.network.dto.building.BuildingMyDetailResponseDto
import com.rentify.app.core.network.dto.building.BuildingMyListResponseDto
import com.rentify.app.core.network.dto.building.BuildingUpdateRequestDto
import com.rentify.app.core.network.dto.building.BuildingVietQrConfigRequestDto
import com.rentify.app.core.network.dto.building.BuildingVietQrConfigResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface BuildingApiService {

    @GET("me/buildings")
    suspend fun getMyBuildings(
        @Query("filter[search]") search: String? = null,
        @Query("filter[status]") status: String? = null,
        @Query("sort") sort: String? = "createdAt",
        @Query("order") order: String? = "desc",
        @Query("pageNo") pageNo: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("format") format: String = "json"
    ): Response<ApiListResponse<BuildingMyListResponseDto>>

    @POST("buildings")
    suspend fun createBuilding(
        @Body request: BuildingCreateRequestDto
    ): Response<ApiSingleResponse<BuildingMyDetailResponseDto>>

    @GET("me/buildings/{buildingId}")
    suspend fun getMyBuildingDetail(
        @Path("buildingId") buildingId: String
    ): Response<ApiSingleResponse<BuildingMyDetailResponseDto>>

    @PATCH("buildings/{buildingId}")
    suspend fun updateBuilding(
        @Path("buildingId") buildingId: String,
        @Body request: BuildingUpdateRequestDto
    ): Response<ApiSingleResponse<BuildingMyDetailResponseDto>>

    @DELETE("buildings/{buildingId}")
    suspend fun deleteBuilding(
        @Path("buildingId") buildingId: String
    ): Response<Unit>

    @PUT("buildings/{buildingId}/vietqr-config")
    suspend fun configureVietQr(
        @Path("buildingId") buildingId: String,
        @Body request: BuildingVietQrConfigRequestDto
    ): Response<ApiSingleResponse<BuildingVietQrConfigResponseDto>>
}

