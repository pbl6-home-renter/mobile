package com.rentify.app.core.data.repository.auth

import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.data.repository.BaseRepository
import com.rentify.app.core.network.adapter.NetworkResult
import kotlinx.coroutines.flow.first
import com.rentify.app.core.network.dto.auth.AuthLogoutRequestDto
import com.rentify.app.core.network.dto.auth.AuthRefreshRequestDto
import com.rentify.app.core.network.api.AuthApiService
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.auth.AuthLoginRequestDto
import com.rentify.app.core.network.dto.auth.AuthLoginResponseDto
import com.rentify.app.core.network.dto.auth.AuthRefreshResponseDto
import com.rentify.app.core.network.dto.auth.AuthRegisterRequestDto
import com.rentify.app.core.network.dto.auth.AuthRegisterResponseDto
import com.rentify.app.core.network.dto.auth.AuthRequestEmailVerificationRequestDto
import com.rentify.app.core.network.dto.auth.AuthRequestEmailVerificationResponseDto
import com.rentify.app.core.network.dto.auth.AuthRequestPasswordResetRequestDto
import com.rentify.app.core.network.dto.auth.AuthRequestPasswordResetResponseDto
import com.rentify.app.core.network.dto.auth.AuthResetPasswordRequestDto
import com.rentify.app.core.network.dto.auth.AuthVerifyEmailRequestDto

class AuthRepositoryImpl(
    private val authApiService: AuthApiService,
    private val tokenDataStore: TokenDataStore
) : BaseRepository(), AuthRepository {

    override suspend fun register(request: AuthRegisterRequestDto): NetworkResult<ApiSingleResponse<AuthRegisterResponseDto>> {
        return safeApiCall { authApiService.register(request) }
    }

    override suspend fun login(request: AuthLoginRequestDto): NetworkResult<ApiSingleResponse<AuthLoginResponseDto>> {
        val result = safeApiCall { authApiService.login(request) }
        if (result is NetworkResult.Success) {
            result.data.data?.let { loginData ->
                tokenDataStore.saveTokens(
                    accessToken = loginData.accessToken,
                    refreshToken = loginData.refreshToken,
                    userId = loginData.profile.id,
                    userRole = loginData.profile.role
                )
            }
        }
        return result
    }

    override suspend fun refreshToken(): NetworkResult<ApiSingleResponse<AuthRefreshResponseDto>> {
        val storedRefreshToken = tokenDataStore.refreshToken.first()
        if (storedRefreshToken.isNullOrBlank()) return NetworkResult.ApiError(401, null)

        val result = safeApiCall { authApiService.refreshToken(AuthRefreshRequestDto(storedRefreshToken)) }
        if (result is NetworkResult.Success) {
            result.data.data?.let { refreshData ->
                tokenDataStore.saveTokens(
                    accessToken = refreshData.accessToken,
                    refreshToken = refreshData.refreshToken
                )
            }
        } else if (result is NetworkResult.ApiError && result.code == 401) {
            tokenDataStore.clearTokens()
        }
        return result
    }

    override suspend fun logout(): NetworkResult<Unit> {
        val storedRefreshToken = tokenDataStore.refreshToken.first()
        val result = if (storedRefreshToken.isNullOrBlank()) {
            NetworkResult.Success(Unit)
        } else {
            safeApiCall { authApiService.logout(AuthLogoutRequestDto(storedRefreshToken)) }
        }
        tokenDataStore.clearTokens()
        return result
    }

    override suspend fun requestPasswordReset(request: AuthRequestPasswordResetRequestDto): NetworkResult<ApiSingleResponse<AuthRequestPasswordResetResponseDto>> {
        return safeApiCall { authApiService.requestPasswordReset(request) }
    }

    override suspend fun resetPassword(request: AuthResetPasswordRequestDto): NetworkResult<Unit> {
        return safeApiCall { authApiService.resetPassword(request) }
    }

    override suspend fun requestEmailVerification(email: String): NetworkResult<ApiSingleResponse<AuthRequestEmailVerificationResponseDto>> {
        return safeApiCall {
            authApiService.requestEmailVerification(AuthRequestEmailVerificationRequestDto(email))
        }
    }

    override suspend fun verifyEmail(token: String): NetworkResult<Unit> {
        return safeApiCall {
            authApiService.verifyEmail(AuthVerifyEmailRequestDto(token))
        }
    }
}
