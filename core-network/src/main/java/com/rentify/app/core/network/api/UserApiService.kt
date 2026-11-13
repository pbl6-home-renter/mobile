package com.rentify.app.core.network.api

import com.rentify.app.core.network.dto.ApiListResponse
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.user.UserChangePasswordRequestDto
import com.rentify.app.core.network.dto.user.UserCurrentResponseDto
import com.rentify.app.core.network.dto.user.UserCurrentUpdateRequestDto
import com.rentify.app.core.network.dto.user.UserCurrentUpdateResponseDto
import com.rentify.app.core.network.dto.user.UserDirectoryResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Query

interface UserApiService {

    @GET("me")
    suspend fun getCurrentUser(): Response<ApiSingleResponse<UserCurrentResponseDto>>

    @PATCH("me")
    suspend fun updateCurrentUser(
        @Body request: UserCurrentUpdateRequestDto
    ): Response<ApiSingleResponse<UserCurrentUpdateResponseDto>>

    @PUT("me/password")
    suspend fun changePassword(
        @Body request: UserChangePasswordRequestDto
    ): Response<Unit>

    @GET("users/directory")
    suspend fun getUsersDirectory(
        @Query("q") q: String? = null,
        @Query("pageNo") pageNo: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<ApiListResponse<UserDirectoryResponseDto>>
}
