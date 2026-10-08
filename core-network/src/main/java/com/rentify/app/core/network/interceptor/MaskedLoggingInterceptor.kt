package com.rentify.app.core.network.interceptor

import android.util.Log
import com.rentify.app.core.network.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor

class MaskedLoggingInterceptor : Interceptor {

    private val delegate = HttpLoggingInterceptor { message ->
        val sanitizedMessage = maskSensitiveInformation(message)
        Log.d(TAG, sanitizedMessage)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!BuildConfig.DEBUG) {
            return chain.proceed(chain.request())
        }
        return delegate.intercept(chain)
    }

    private fun maskSensitiveInformation(message: String): String {
        if (message.startsWith("Authorization:", ignoreCase = true)) {
            return "Authorization: Bearer ***MASKED***"
        }
        return message
    }

    companion object {
        private const val TAG = "NetworkLog"
    }
}
