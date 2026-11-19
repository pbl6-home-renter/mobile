package com.rentify.app.core.data.local.datastore

import android.content.Context
import com.rentify.app.core.network.api.AuthApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.core.network.dto.auth.AuthRefreshRequestDto
import com.rentify.app.core.network.interceptor.TokenRefresher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.IOException

class DataStoreTokenRefresher(context: Context) : TokenRefresher {

    private val tokenDataStore = TokenDataStore(context.applicationContext)
    private val authApiService by lazy { NetworkClient.createService<AuthApiService>() }
    private val lock = Any()

    override fun refreshAccessToken(failedAccessToken: String): String? = synchronized(lock) {
        runBlocking {
            val currentAccessToken = tokenDataStore.accessToken.first()
            if (!currentAccessToken.isNullOrBlank() && currentAccessToken != failedAccessToken) {
                return@runBlocking currentAccessToken
            }

            val refreshToken = tokenDataStore.refreshToken.first()
            if (refreshToken.isNullOrBlank()) return@runBlocking null

            val response = try {
                authApiService.refreshToken(AuthRefreshRequestDto(refreshToken))
            } catch (_: IOException) {
                return@runBlocking null
            }

            if (response.isSuccessful) {
                val tokens = response.body()?.data ?: return@runBlocking null
                tokenDataStore.saveTokens(tokens.accessToken, tokens.refreshToken)
                tokens.accessToken
            } else {
                if (response.code() == 401 || response.code() == 422) tokenDataStore.clearTokens()
                null
            }
        }
    }
}
