package com.rentify.app.landlord.data.repository

import com.rentify.app.core.data.repository.BaseRepository
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.api.MeterApiService
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.meter.BulkMeterReadingCreateRequestDto
import com.rentify.app.core.network.dto.meter.BulkMeterReadingResponseDto
import com.rentify.app.core.network.dto.meter.MeterReadingCreateRequestDto
import com.rentify.app.core.network.dto.meter.MeterReadingEvidenceAttachRequestDto
import com.rentify.app.core.network.dto.meter.MeterReadingListResponseDto

class LandlordMeterRepositoryImpl(
    private val meterApiService: MeterApiService
) : BaseRepository(), LandlordMeterRepository {

    override suspend fun getMeterReadings(
        roomId: String?,
        buildingId: String?,
        period: String?,
        type: String?,
        pageNo: Int,
        pageSize: Int
    ): NetworkResult<ApiListResponse<MeterReadingListResponseDto>> {
        return safeApiCall {
            meterApiService.getMeterReadings(
                roomId = roomId,
                buildingId = buildingId,
                period = period,
                type = type,
                pageNo = pageNo,
                pageSize = pageSize
            )
        }
    }

    override suspend fun bulkCreateMeterReadings(
        request: BulkMeterReadingCreateRequestDto
    ): NetworkResult<ApiSingleResponse<BulkMeterReadingResponseDto>> {
        return safeApiCall {
            meterApiService.bulkCreateMeterReadings(request)
        }
    }

    override suspend fun createMeterReading(
        request: MeterReadingCreateRequestDto
    ): NetworkResult<ApiSingleResponse<MeterReadingListResponseDto>> {
        return safeApiCall {
            meterApiService.createMeterReading(request)
        }
    }

    override suspend fun attachEvidence(
        readingId: String,
        evidenceMediaIds: List<String>
    ): NetworkResult<ApiSingleResponse<MeterReadingListResponseDto>> {
        return safeApiCall {
            meterApiService.attachEvidence(
                readingId = readingId,
                request = MeterReadingEvidenceAttachRequestDto(evidenceMediaIds = evidenceMediaIds)
            )
        }
    }
}

