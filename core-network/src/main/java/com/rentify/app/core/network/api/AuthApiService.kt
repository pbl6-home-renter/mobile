package com.rentify.app.core.network.api

import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.auth.AuthLoginRequestDto
import com.rentify.app.core.network.dto.auth.AuthLoginResponseDto
import com.rentify.app.core.network.dto.auth.AuthRefreshResponseDto
import com.rentify.app.core.network.dto.auth.AuthRegisterRequestDto
import com.rentify.app.core.network.dto.auth.AuthRegisterResponseDto
import com.rentify.app.core.network.dto.auth.AuthRequestPasswordResetRequestDto
import com.rentify.app.core.network.dto.auth.AuthRequestPasswordResetResponseDto
import com.rentify.app.core.network.dto.auth.AuthResetPasswordRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {

    @POST("auth/register")
    suspend fun register(
        @Body request: AuthRegisterRequestDto
    ): Response<ApiSingleResponse<AuthRegisterResponseDto>>

    @POST("auth/login")
    suspend fun login(
        @Body request: AuthLoginRequestDto
    ): Response<ApiSingleResponse<AuthLoginResponseDto>>

    @POST("auth/refresh")
    suspend fun refreshToken(): Response<ApiSingleResponse<AuthRefreshResponseDto>>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    @POST("auth/password-reset-requests")
    suspend fun requestPasswordReset(
        @Body request: AuthRequestPasswordResetRequestDto
    ): Response<ApiSingleResponse<AuthRequestPasswordResetResponseDto>>

    @POST("auth/password-resets")
    suspend fun resetPassword(
        @Body request: AuthResetPasswordRequestDto
    ): Response<Unit>
}
