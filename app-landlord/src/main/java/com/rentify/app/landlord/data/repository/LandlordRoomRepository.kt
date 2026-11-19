package com.rentify.app.landlord.data.repository

import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.room.RoomCreateRequestDto
import com.rentify.app.core.network.dto.room.RoomCreateResponseDto
import com.rentify.app.core.network.dto.room.RoomDetailResponseDto
import com.rentify.app.core.network.dto.room.RoomMarkReadyResponseDto
import com.rentify.app.core.network.dto.room.RoomMyListResponseDto
import com.rentify.app.core.network.dto.room.RoomUpdateRequestDto
import com.rentify.app.core.network.dto.room.RoomUpdateResponseDto

interface LandlordRoomRepository {
    suspend fun getMyRooms(
        buildingId: String? = null,
        status: String? = null,
        pageNo: Int = 1,
        pageSize: Int = 50
    ): NetworkResult<ApiListResponse<RoomMyListResponseDto>>

    suspend fun getRoomDetail(
        roomId: String
    ): NetworkResult<ApiSingleResponse<RoomDetailResponseDto>>

    suspend fun createRoom(
        request: RoomCreateRequestDto
    ): NetworkResult<ApiSingleResponse<RoomCreateResponseDto>>

    suspend fun updateRoom(
        roomId: String,
        request: RoomUpdateRequestDto
    ): NetworkResult<ApiSingleResponse<RoomUpdateResponseDto>>

    suspend fun deleteRoom(
        roomId: String
    ): NetworkResult<Unit>

    suspend fun markRoomReady(
        roomId: String
    ): NetworkResult<ApiSingleResponse<RoomMarkReadyResponseDto>>
}

