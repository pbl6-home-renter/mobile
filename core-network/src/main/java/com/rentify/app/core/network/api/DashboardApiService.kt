package com.rentify.app.core.network.api

import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.dashboard.DashboardResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface DashboardApiService {

    @GET("dashboard")
    suspend fun getDashboard(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): Response<ApiSingleResponse<DashboardResponseDto>>
}

