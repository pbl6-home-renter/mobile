package com.rentify.app.core.data.repository.user

import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.user.PayoutAccountDto
import com.rentify.app.core.network.dto.user.UserChangePasswordRequestDto
import com.rentify.app.core.network.dto.user.UserCurrentResponseDto
import com.rentify.app.core.network.dto.user.UserCurrentUpdateResponseDto

interface UserRepository {
    suspend fun getCurrentUser(): NetworkResult<ApiSingleResponse<UserCurrentResponseDto>>
    suspend fun updateCurrentUser(
        name: String,
        phone: String,
        payoutAccount: PayoutAccountDto? = null
    ): NetworkResult<ApiSingleResponse<UserCurrentUpdateResponseDto>>
    suspend fun changePassword(request: UserChangePasswordRequestDto): NetworkResult<Unit>
}
