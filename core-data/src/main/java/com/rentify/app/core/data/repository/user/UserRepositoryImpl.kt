package com.rentify.app.core.data.repository.user

import com.rentify.app.core.data.repository.BaseRepository
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.api.UserApiService
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.user.UserChangePasswordRequestDto
import com.rentify.app.core.network.dto.user.UserCurrentResponseDto
import com.rentify.app.core.network.dto.user.UserCurrentUpdateRequestDto
import com.rentify.app.core.network.dto.user.UserCurrentUpdateResponseDto

class UserRepositoryImpl(
    private val userApiService: UserApiService
) : BaseRepository(), UserRepository {

    override suspend fun getCurrentUser(): NetworkResult<ApiSingleResponse<UserCurrentResponseDto>> {
        return safeApiCall { userApiService.getCurrentUser() }
    }

    override suspend fun updateCurrentUser(name: String, phone: String): NetworkResult<ApiSingleResponse<UserCurrentUpdateResponseDto>> {
        return safeApiCall {
            userApiService.updateCurrentUser(
                UserCurrentUpdateRequestDto(name = name, phone = phone, payoutAccount = null)
            )
        }
    }

    override suspend fun changePassword(request: UserChangePasswordRequestDto): NetworkResult<Unit> {
        return safeApiCall { userApiService.changePassword(request) }
    }
}
