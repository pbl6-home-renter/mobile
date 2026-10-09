package com.rentify.app.core.data.local.session

import com.rentify.app.core.data.local.datastore.TokenDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SessionManager(private val tokenDataStore: TokenDataStore) {

    val isLoggedIn: Flow<Boolean> = tokenDataStore.accessToken.map { token ->
        !token.isNullOrBlank()
    }

    suspend fun logout() {
        tokenDataStore.clearTokens()
    }
}
