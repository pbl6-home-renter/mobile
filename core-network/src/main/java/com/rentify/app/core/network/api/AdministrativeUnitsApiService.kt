package com.rentify.app.core.network.api

import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.administrative.AdministrativeProvinceDto
import com.rentify.app.core.network.dto.administrative.AdministrativeWardDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface AdministrativeUnitsApiService {

    @GET("administrative-units/provinces")
    suspend fun getProvinces(): Response<ApiListResponse<AdministrativeProvinceDto>>

    @GET("administrative-units/provinces/{provinceCode}/wards")
    suspend fun getWards(
        @Path("provinceCode") provinceCode: String
    ): Response<ApiListResponse<AdministrativeWardDto>>
}

