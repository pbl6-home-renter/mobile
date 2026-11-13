package com.rentify.app.tenant.data.repository

import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.room.FavoriteRoomListResponseDto
import com.rentify.app.core.network.dto.room.FavoriteRoomToggleResponseDto
import com.rentify.app.core.network.dto.room.MyRoomHubResponseDto
import com.rentify.app.core.network.dto.room.RoomDetailResponseDto
import com.rentify.app.core.network.dto.room.RoomPublicListResponseDto

interface TenantRoomRepository {
    suspend fun getPublicRooms(
        province: String? = null,
        ward: String? = null,
        minPrice: Long? = null,
        maxPrice: Long? = null,
        genderPolicy: String? = null,
        sort: String? = "rentPrice",
        order: String? = "asc",
        pageNo: Int = 1,
        pageSize: Int = 20
    ): NetworkResult<ApiListResponse<RoomPublicListResponseDto>>

    suspend fun getRoomDetail(roomId: String): NetworkResult<ApiSingleResponse<RoomDetailResponseDto>>

    suspend fun getMyRoomHub(): NetworkResult<ApiSingleResponse<MyRoomHubResponseDto>>

    suspend fun getFavoriteRooms(
        pageNo: Int = 1,
        pageSize: Int = 20
    ): NetworkResult<ApiListResponse<FavoriteRoomListResponseDto>>

    suspend fun addFavoriteRoom(roomId: String): NetworkResult<ApiSingleResponse<FavoriteRoomToggleResponseDto>>

    suspend fun removeFavoriteRoom(roomId: String): NetworkResult<Unit>
}

