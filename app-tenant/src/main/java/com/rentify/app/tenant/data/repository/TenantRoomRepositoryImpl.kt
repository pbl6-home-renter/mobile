package com.rentify.app.tenant.data.repository

import com.rentify.app.core.data.repository.BaseRepository
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.api.RoomApiService
import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.room.FavoriteRoomListResponseDto
import com.rentify.app.core.network.dto.room.FavoriteRoomToggleRequestDto
import com.rentify.app.core.network.dto.room.FavoriteRoomToggleResponseDto
import com.rentify.app.core.network.dto.room.MyRoomHubItemDto
import com.rentify.app.core.network.dto.room.RoomDetailResponseDto
import com.rentify.app.core.network.dto.room.RoomPublicListResponseDto

class TenantRoomRepositoryImpl(
    private val roomApiService: RoomApiService,
    private val buildingWardResolver: BuildingWardResolver
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
        }.withWards()
    }

    override suspend fun getAllPublicRooms(
        province: String?,
        ward: String?
    ): NetworkResult<List<RoomPublicListResponseDto>> {
        val rooms = mutableListOf<RoomPublicListResponseDto>()
        var pageNo = 1
        while (pageNo <= MAX_ROOM_PAGES) {
            when (val result = safeApiCall {
                roomApiService.getPublicRooms(
                    province = province,
                    ward = ward,
                    pageNo = pageNo,
                    pageSize = ROOM_PAGE_SIZE
                )
            }) {
                is NetworkResult.Success -> {
                    rooms += result.data.data
                    if (result.data.pagination?.hasNextPage != true) break
                    pageNo++
                }
                is NetworkResult.ApiError -> return result
                is NetworkResult.NetworkError -> return result
                is NetworkResult.UnknownError -> return result
            }
        }
        val wards = buildingWardResolver.wardsOf(rooms.map { it.buildingId })
        return NetworkResult.Success(
            rooms.map { room ->
                if (room.ward.isBlank()) room.copy(ward = wards[room.buildingId].orEmpty()) else room
            }
        )
    }

    override suspend fun getRoomDetail(roomId: String): NetworkResult<ApiSingleResponse<RoomDetailResponseDto>> {
        return safeApiCall { roomApiService.getRoomDetail(roomId) }.withDetailWard()
    }

    override suspend fun getMyRoomHub(): NetworkResult<ApiListResponse<MyRoomHubItemDto>> {
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

    private suspend fun NetworkResult<ApiListResponse<RoomPublicListResponseDto>>.withWards():
        NetworkResult<ApiListResponse<RoomPublicListResponseDto>> {
        if (this !is NetworkResult.Success) return this
        val wards = buildingWardResolver.wardsOf(data.data.map { it.buildingId })
        val rooms = data.data.map { room ->
            if (room.ward.isBlank()) room.copy(ward = wards[room.buildingId].orEmpty()) else room
        }
        return NetworkResult.Success(data.copy(data = rooms))
    }

    private suspend fun NetworkResult<ApiSingleResponse<RoomDetailResponseDto>>.withDetailWard():
        NetworkResult<ApiSingleResponse<RoomDetailResponseDto>> {
        if (this !is NetworkResult.Success) return this
        val room = data.data ?: return this
        if (room.ward.isNotBlank()) return this
        val ward = buildingWardResolver.wardsOf(listOf(room.buildingId))[room.buildingId].orEmpty()
        return NetworkResult.Success(data.copy(data = room.copy(ward = ward)))
    }

    private companion object {
        const val ROOM_PAGE_SIZE = 100
        const val MAX_ROOM_PAGES = 20
    }
}
