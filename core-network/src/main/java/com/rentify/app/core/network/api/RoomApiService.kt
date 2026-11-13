package com.rentify.app.core.network.api

import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.room.FavoriteRoomListResponseDto
import com.rentify.app.core.network.dto.room.FavoriteRoomToggleRequestDto
import com.rentify.app.core.network.dto.room.FavoriteRoomToggleResponseDto
import com.rentify.app.core.network.dto.room.MyRoomHubResponseDto
import com.rentify.app.core.network.dto.room.RoomCreateRequestDto
import com.rentify.app.core.network.dto.room.RoomDetailResponseDto
import com.rentify.app.core.network.dto.room.RoomMarkReadyResponseDto
import com.rentify.app.core.network.dto.room.RoomMyListResponseDto
import com.rentify.app.core.network.dto.room.RoomPublicListResponseDto
import com.rentify.app.core.network.dto.room.RoomUpdateRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface RoomApiService {

    @GET("rooms")
    suspend fun getPublicRooms(
        @Query("filter[province]") province: String? = null,
        @Query("filter[ward]") ward: String? = null,
        @Query("filter[minPrice]") minPrice: Long? = null,
        @Query("filter[maxPrice]") maxPrice: Long? = null,
        @Query("filter[genderPolicy]") genderPolicy: String? = null,
        @Query("sort") sort: String? = "rentPrice",
        @Query("order") order: String? = "asc",
        @Query("pageNo") pageNo: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("format") format: String = "json"
    ): Response<ApiListResponse<RoomPublicListResponseDto>>

    @GET("rooms/{roomId}")
    suspend fun getRoomDetail(
        @Path("roomId") roomId: String
    ): Response<ApiSingleResponse<RoomDetailResponseDto>>

    @GET("me/room-hub")
    suspend fun getMyRoomHub(): Response<ApiSingleResponse<MyRoomHubResponseDto>>

    @GET("me/favorite-rooms")
    suspend fun getFavoriteRooms(
        @Query("pageNo") pageNo: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("format") format: String = "json"
    ): Response<ApiListResponse<FavoriteRoomListResponseDto>>

    @POST("me/favorite-rooms")
    suspend fun addFavoriteRoom(
        @Body request: FavoriteRoomToggleRequestDto
    ): Response<ApiSingleResponse<FavoriteRoomToggleResponseDto>>

    @DELETE("me/favorite-rooms/{roomId}")
    suspend fun removeFavoriteRoom(
        @Path("roomId") roomId: String
    ): Response<Unit>

    @GET("me/rooms")
    suspend fun getMyRooms(
        @Query("filter[buildingId]") buildingId: String? = null,
        @Query("filter[status]") status: String? = null,
        @Query("sort") sort: String? = "name",
        @Query("order") order: String? = "asc",
        @Query("pageNo") pageNo: Int = 1,
        @Query("pageSize") pageSize: Int = 50,
        @Query("format") format: String = "json"
    ): Response<ApiListResponse<RoomMyListResponseDto>>

    @POST("rooms")
    suspend fun createRoom(
        @Body request: RoomCreateRequestDto
    ): Response<ApiSingleResponse<RoomDetailResponseDto>>

    @PATCH("rooms/{roomId}")
    suspend fun updateRoom(
        @Path("roomId") roomId: String,
        @Body request: RoomUpdateRequestDto
    ): Response<ApiSingleResponse<RoomDetailResponseDto>>

    @DELETE("rooms/{roomId}")
    suspend fun deleteRoom(
        @Path("roomId") roomId: String
    ): Response<Unit>

    @POST("rooms/{roomId}/mark-ready")
    suspend fun markRoomReady(
        @Path("roomId") roomId: String
    ): Response<ApiSingleResponse<RoomMarkReadyResponseDto>>
}
