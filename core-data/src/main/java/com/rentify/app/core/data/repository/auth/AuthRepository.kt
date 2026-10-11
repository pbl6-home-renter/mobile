package com.rentify.app.core.data.repository.auth

import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.auth.AuthLoginRequestDto
import com.rentify.app.core.network.dto.auth.AuthLoginResponseDto
import com.rentify.app.core.network.dto.auth.AuthRefreshResponseDto
import com.rentify.app.core.network.dto.auth.AuthRegisterRequestDto
import com.rentify.app.core.network.dto.auth.AuthRegisterResponseDto
import com.rentify.app.core.network.dto.auth.AuthRequestPasswordResetRequestDto
import com.rentify.app.core.network.dto.auth.AuthRequestPasswordResetResponseDto
import com.rentify.app.core.network.dto.auth.AuthResetPasswordRequestDto

interface AuthRepository {
    suspend fun register(request: AuthRegisterRequestDto): NetworkResult<ApiSingleResponse<AuthRegisterResponseDto>>
    suspend fun login(request: AuthLoginRequestDto): NetworkResult<ApiSingleResponse<AuthLoginResponseDto>>
    suspend fun refreshToken(): NetworkResult<ApiSingleResponse<AuthRefreshResponseDto>>
    suspend fun logout(): NetworkResult<Unit>
    suspend fun requestPasswordReset(request: AuthRequestPasswordResetRequestDto): NetworkResult<ApiSingleResponse<AuthRequestPasswordResetResponseDto>>
    suspend fun resetPassword(request: AuthResetPasswordRequestDto): NetworkResult<Unit>
}
