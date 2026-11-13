package com.rentify.app.tenant.data.repository

import com.rentify.app.core.data.repository.BaseRepository
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.api.RoomApiService
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.room.FavoriteRoomListResponseDto
import com.rentify.app.core.network.dto.room.FavoriteRoomToggleRequestDto
import com.rentify.app.core.network.dto.room.FavoriteRoomToggleResponseDto
import com.rentify.app.core.network.dto.room.MyRoomHubResponseDto
import com.rentify.app.core.network.dto.room.RoomDetailResponseDto
import com.rentify.app.core.network.dto.room.RoomPublicListResponseDto

class TenantRoomRepositoryImpl(
    private val roomApiService: RoomApiService
) : BaseRepository(), TenantRoomRepository {

    override suspend fun getPublicRooms(
        province: String?,
        ward: String?,
        minPrice: Long?,
        maxPrice: Long?,
        genderPolicy: String?,
        sort: String?,
        order: String?,
        pageNo: Int,
        pageSize: Int
    ): NetworkResult<ApiListResponse<RoomPublicListResponseDto>> {
        return safeApiCall {
            roomApiService.getPublicRooms(
                province = province,
                ward = ward,
                minPrice = minPrice,
                maxPrice = maxPrice,
                genderPolicy = genderPolicy,
                sort = sort,
                order = order,
                pageNo = pageNo,
                pageSize = pageSize
            )
        }
    }

    override suspend fun getRoomDetail(roomId: String): NetworkResult<ApiSingleResponse<RoomDetailResponseDto>> {
        return safeApiCall { roomApiService.getRoomDetail(roomId) }
    }

    override suspend fun getMyRoomHub(): NetworkResult<ApiSingleResponse<MyRoomHubResponseDto>> {
        return safeApiCall { roomApiService.getMyRoomHub() }
    }

    override suspend fun getFavoriteRooms(
        pageNo: Int,
        pageSize: Int
    ): NetworkResult<ApiListResponse<FavoriteRoomListResponseDto>> {
        return safeApiCall {
            roomApiService.getFavoriteRooms(
                pageNo = pageNo,
                pageSize = pageSize
            )
        }
    }

    override suspend fun addFavoriteRoom(roomId: String): NetworkResult<ApiSingleResponse<FavoriteRoomToggleResponseDto>> {
        return safeApiCall {
            roomApiService.addFavoriteRoom(FavoriteRoomToggleRequestDto(roomId = roomId))
        }
    }

    override suspend fun removeFavoriteRoom(roomId: String): NetworkResult<Unit> {
        return safeApiCall { roomApiService.removeFavoriteRoom(roomId) }
    }
}

