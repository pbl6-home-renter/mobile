package com.rentify.app.core.network.interceptor

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

interface TokenRefresher {
    fun refreshAccessToken(failedAccessToken: String): String?
}

class TokenAuthenticator(
    private val refresherProvider: () -> TokenRefresher?
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val failedToken = response.request.header(AUTHORIZATION)
            ?.removePrefix(BEARER_PREFIX)
            ?.trim()
        if (failedToken.isNullOrBlank()) return null
        if (responseCount(response) >= MAX_ATTEMPTS) return null

        val newToken = refresherProvider()?.refreshAccessToken(failedToken) ?: return null
        return response.request.newBuilder()
            .header(AUTHORIZATION, BEARER_PREFIX + newToken)
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        const val AUTHORIZATION = "Authorization"
        const val BEARER_PREFIX = "Bearer "
        const val MAX_ATTEMPTS = 2
    }
}
