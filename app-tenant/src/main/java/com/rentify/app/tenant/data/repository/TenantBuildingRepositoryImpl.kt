package com.rentify.app.tenant.data.repository

import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.building.BuildingPublicResponseDto
import com.rentify.app.core.network.dto.room.RoomPublicListResponseDto

class TenantBuildingRepositoryImpl(
    private val roomRepository: TenantRoomRepository,
    private val buildingWardResolver: BuildingWardResolver
) : TenantBuildingRepository {

    override suspend fun getBuildings(
        province: String?,
        ward: String?
    ): NetworkResult<List<BuildingSummary>> {
        val result = roomRepository.getAllPublicRooms(province = province, ward = ward)
        if (result !is NetworkResult.Success) return result.asFailure()

        val rooms = result.data
        val buildings = buildingWardResolver.buildingsOf(rooms.map { it.buildingId })
        val summaries = rooms
            .groupBy { it.buildingId }
            .map { (buildingId, buildingRooms) -> summarize(buildingId, buildings[buildingId], buildingRooms) }
            .sortedBy { it.minRentPrice }
        return NetworkResult.Success(summaries)
    }

    override suspend fun getBuilding(buildingId: String): NetworkResult<BuildingSummary> {
        val result = roomRepository.getAllPublicRooms()
        if (result !is NetworkResult.Success) return result.asFailure()

        val rooms = result.data.filter { it.buildingId == buildingId }
        val building = buildingWardResolver.buildingsOf(listOf(buildingId))[buildingId]
        if (building == null && rooms.isEmpty()) return NetworkResult.ApiError(NOT_FOUND, null)
        return NetworkResult.Success(summarize(buildingId, building, rooms))
    }

    private fun summarize(
        buildingId: String,
        building: BuildingPublicResponseDto?,
        rooms: List<RoomPublicListResponseDto>
    ): BuildingSummary {
        val sortedRooms = rooms.sortedBy { it.rentPrice }
        val firstRoom = sortedRooms.firstOrNull()
        return BuildingSummary(
            id = buildingId,
            name = building?.name?.takeIf { it.isNotBlank() } ?: "Tòa nhà",
            address = building?.address?.takeIf { it.isNotBlank() } ?: firstRoom?.address.orEmpty(),
            ward = building?.ward?.takeIf { it.isNotBlank() } ?: firstRoom?.ward.orEmpty(),
            province = building?.province?.takeIf { it.isNotBlank() } ?: firstRoom?.province.orEmpty(),
            minRentPrice = firstRoom?.rentPrice ?: 0L,
            coverUrl = sortedRooms.firstNotNullOfOrNull { it.coverUrl },
            rooms = sortedRooms
        )
    }

    private fun NetworkResult<*>.asFailure(): NetworkResult<Nothing> = when (this) {
        is NetworkResult.ApiError -> this
        is NetworkResult.NetworkError -> this
        is NetworkResult.UnknownError -> this
        is NetworkResult.Success -> NetworkResult.UnknownError(null)
    }

    private companion object {
        const val NOT_FOUND = 404
    }
}
