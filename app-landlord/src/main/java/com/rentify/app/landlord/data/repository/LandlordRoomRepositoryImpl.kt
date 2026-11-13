package com.rentify.app.landlord.data.repository

import com.rentify.app.core.data.repository.BaseRepository
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.api.RoomApiService
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.room.RoomCreateRequestDto
import com.rentify.app.core.network.dto.room.RoomDetailResponseDto
import com.rentify.app.core.network.dto.room.RoomMarkReadyResponseDto
import com.rentify.app.core.network.dto.room.RoomMyListResponseDto
import com.rentify.app.core.network.dto.room.RoomUpdateRequestDto

class LandlordRoomRepositoryImpl(
    private val roomApiService: RoomApiService
) : BaseRepository(), LandlordRoomRepository {

    override suspend fun getMyRooms(
        buildingId: String?,
        status: String?,
        pageNo: Int,
        pageSize: Int
    ): NetworkResult<ApiListResponse<RoomMyListResponseDto>> {
        return safeApiCall {
            roomApiService.getMyRooms(
                buildingId = buildingId,
                status = status,
                pageNo = pageNo,
                pageSize = pageSize
            )
        }
    }

    override suspend fun createRoom(
        request: RoomCreateRequestDto
    ): NetworkResult<ApiSingleResponse<RoomDetailResponseDto>> {
        return safeApiCall {
            roomApiService.createRoom(request)
        }
    }

    override suspend fun updateRoom(
        roomId: String,
        request: RoomUpdateRequestDto
    ): NetworkResult<ApiSingleResponse<RoomDetailResponseDto>> {
        return safeApiCall {
            roomApiService.updateRoom(roomId, request)
        }
    }

    override suspend fun deleteRoom(
        roomId: String
    ): NetworkResult<Unit> {
        return safeApiCall {
            roomApiService.deleteRoom(roomId)
        }
    }

    override suspend fun markRoomReady(
        roomId: String
    ): NetworkResult<ApiSingleResponse<RoomMarkReadyResponseDto>> {
        return safeApiCall {
            roomApiService.markRoomReady(roomId)
        }
    }
}

