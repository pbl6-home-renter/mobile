package com.rentify.app.core.network.api

import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.meter.BulkMeterReadingCreateRequestDto
import com.rentify.app.core.network.dto.meter.BulkMeterReadingResponseDto
import com.rentify.app.core.network.dto.meter.MeterReadingCreateRequestDto
import com.rentify.app.core.network.dto.meter.MeterReadingEvidenceAttachRequestDto
import com.rentify.app.core.network.dto.meter.MeterReadingListResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MeterApiService {

    @GET("meter-readings")
    suspend fun getMeterReadings(
        @Query("filter[roomId]") roomId: String? = null,
        @Query("filter[buildingId]") buildingId: String? = null,
        @Query("filter[period]") period: String? = null,
        @Query("filter[type]") type: String? = null,
        @Query("pageNo") pageNo: Int = 1,
        @Query("pageSize") pageSize: Int = 50
    ): Response<ApiListResponse<MeterReadingListResponseDto>>

    @POST("meter-readings")
    suspend fun createMeterReading(
        @Body request: MeterReadingCreateRequestDto
    ): Response<ApiSingleResponse<MeterReadingListResponseDto>>

    @POST("meter-readings/bulk")
    suspend fun bulkCreateMeterReadings(
        @Body request: BulkMeterReadingCreateRequestDto
    ): Response<ApiSingleResponse<BulkMeterReadingResponseDto>>

    @PATCH("meter-readings/{readingId}/evidence")
    suspend fun attachEvidence(
        @Path("readingId") readingId: String,
        @Body request: MeterReadingEvidenceAttachRequestDto
    ): Response<ApiSingleResponse<MeterReadingListResponseDto>>
}

