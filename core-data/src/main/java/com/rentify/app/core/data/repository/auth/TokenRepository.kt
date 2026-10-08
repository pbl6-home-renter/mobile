package com.rentify.app.core.data.repository.auth

import com.rentify.app.core.data.local.datastore.TokenDataStore
import kotlinx.coroutines.flow.Flow

interface TokenRepository {
    val accessToken: Flow<String?>
    val refreshToken: Flow<String?>
    val userId: Flow<String?>
    val userRole: Flow<String?>
    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String? = null,
        userId: String? = null,
        userRole: String? = null
    )
    suspend fun clearTokens()
}

class TokenRepositoryImpl(
    private val tokenDataStore: TokenDataStore
) : TokenRepository {

    override val accessToken: Flow<String?> = tokenDataStore.accessToken
    override val refreshToken: Flow<String?> = tokenDataStore.refreshToken
    override val userId: Flow<String?> = tokenDataStore.userId
    override val userRole: Flow<String?> = tokenDataStore.userRole

    override suspend fun saveTokens(
        accessToken: String,
        refreshToken: String?,
        userId: String?,
        userRole: String?
    ) {
        tokenDataStore.saveTokens(accessToken, refreshToken, userId, userRole)
    }

    override suspend fun clearTokens() {
        tokenDataStore.clearTokens()
    }
}
