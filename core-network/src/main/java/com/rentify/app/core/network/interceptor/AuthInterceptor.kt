package com.rentify.app.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val apiHost: String,
    private val tokenProvider: () -> String?
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        if (originalRequest.url.host != apiHost) {
            return chain.proceed(originalRequest)
        }

        val token = tokenProvider()
        if (token.isNullOrBlank()) {
            return chain.proceed(originalRequest)
        }

        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(authenticatedRequest)
    }
}
