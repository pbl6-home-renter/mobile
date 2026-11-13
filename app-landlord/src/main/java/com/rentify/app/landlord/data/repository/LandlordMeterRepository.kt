package com.rentify.app.landlord.data.repository

import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.meter.BulkMeterReadingCreateRequestDto
import com.rentify.app.core.network.dto.meter.BulkMeterReadingResponseDto
import com.rentify.app.core.network.dto.meter.MeterReadingCreateRequestDto
import com.rentify.app.core.network.dto.meter.MeterReadingListResponseDto

interface LandlordMeterRepository {
    suspend fun getMeterReadings(
        roomId: String? = null,
        buildingId: String? = null,
        period: String? = null,
        type: String? = null,
        pageNo: Int = 1,
        pageSize: Int = 50
    ): NetworkResult<ApiListResponse<MeterReadingListResponseDto>>

    suspend fun bulkCreateMeterReadings(
        request: BulkMeterReadingCreateRequestDto
    ): NetworkResult<ApiSingleResponse<BulkMeterReadingResponseDto>>

    suspend fun createMeterReading(
        request: MeterReadingCreateRequestDto
    ): NetworkResult<ApiSingleResponse<MeterReadingListResponseDto>>

    suspend fun attachEvidence(
        readingId: String,
        evidenceMediaIds: List<String>
    ): NetworkResult<ApiSingleResponse<MeterReadingListResponseDto>>
}

