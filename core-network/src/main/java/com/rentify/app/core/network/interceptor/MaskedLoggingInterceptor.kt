package com.rentify.app.core.network.interceptor

import android.util.Log
import com.rentify.app.core.network.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor

class MaskedLoggingInterceptor(
    private val isDebug: Boolean = true
) : Interceptor {

    private val delegate = HttpLoggingInterceptor { message ->
        val sanitizedMessage = maskSensitiveInformation(message)
        Log.d(TAG, sanitizedMessage)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!isDebug) {
            return chain.proceed(chain.request())
        }
        return delegate.intercept(chain)
    }

    private fun maskSensitiveInformation(message: String): String {
        if (message.startsWith("Authorization:", ignoreCase = true)) {
            return "Authorization: Bearer ***MASKED***"
        }
        if (message.startsWith("Cookie:", ignoreCase = true) || message.startsWith("Set-Cookie:", ignoreCase = true)) {
            return message.substringBefore(":") + ": ***MASKED***"
        }
        return SENSITIVE_JSON_VALUE.replace(message) { match ->
            "\"${match.groupValues[1]}\":\"$MASK\""
        }
    }

    companion object {
        private const val TAG = "NetworkLog"
        private const val MASK = "***MASKED***"
        private val SENSITIVE_JSON_VALUE = Regex(
            "\"(password|currentPassword|newPassword|confirmPassword|accessToken|refreshToken|token|" +
                "accountNumber|tenantCccd|cccd)\"" +
                """\s*:\s*""" +
                "\"" +
                """(?:\\.|[^"\\])*""" +
                "\"",
            RegexOption.IGNORE_CASE
        )
    }
}
