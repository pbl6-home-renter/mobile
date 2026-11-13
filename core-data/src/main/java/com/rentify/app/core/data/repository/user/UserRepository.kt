package com.rentify.app.core.data.repository.user

import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.user.UserChangePasswordRequestDto
import com.rentify.app.core.network.dto.user.UserCurrentResponseDto
import com.rentify.app.core.network.dto.user.UserCurrentUpdateRequestDto
import com.rentify.app.core.network.dto.user.UserCurrentUpdateResponseDto

interface UserRepository {
    suspend fun getCurrentUser(): NetworkResult<ApiSingleResponse<UserCurrentResponseDto>>
    suspend fun updateCurrentUser(name: String, phone: String): NetworkResult<ApiSingleResponse<UserCurrentUpdateResponseDto>>
    suspend fun changePassword(request: UserChangePasswordRequestDto): NetworkResult<Unit>
}
