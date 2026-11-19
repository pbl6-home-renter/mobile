package com.rentify.app.tenant.data.repository

import com.rentify.app.core.network.dto.room.RoomPublicListResponseDto

data class BuildingSummary(
    val id: String,
    val name: String,
    val address: String,
    val ward: String,
    val province: String,
    val minRentPrice: Long,
    val coverUrl: String?,
    val rooms: List<RoomPublicListResponseDto>
) {
    val roomCount: Int
        get() = rooms.size
}
