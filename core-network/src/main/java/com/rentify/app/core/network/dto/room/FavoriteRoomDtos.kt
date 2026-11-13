package com.rentify.app.core.network.dto.room

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FavoriteRoomToggleRequestDto(
    @field:Json(name = "roomId") val roomId: String
)

@JsonClass(generateAdapter = true)
data class FavoriteRoomToggleResponseDto(
    @field:Json(name = "roomId") val roomId: String,
    @field:Json(name = "roomName") val roomName: String
)

@JsonClass(generateAdapter = true)
data class FavoriteRoomListResponseDto(
    @field:Json(name = "roomId") val roomId: String,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "rentPrice") val rentPrice: Long
)
