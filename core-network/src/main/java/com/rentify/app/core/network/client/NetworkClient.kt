package com.rentify.app.core.network.client

import com.rentify.app.core.network.interceptor.AuthInterceptor
import com.rentify.app.core.network.interceptor.MaskedLoggingInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {

    fun createMoshi(): Moshi {
        return Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    fun createOkHttpClient(
        tokenProvider: (() -> String?)? = null,
        networkConfig: NetworkConfig = NetworkConfig()
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(networkConfig.connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(networkConfig.readTimeoutSeconds, TimeUnit.SECONDS)

        if (tokenProvider != null) {
            builder.addInterceptor(AuthInterceptor(tokenProvider))
        }

        builder.addInterceptor(MaskedLoggingInterceptor())

        return builder.build()
    }

    fun createRetrofit(
        networkConfig: NetworkConfig = NetworkConfig(),
        okHttpClient: OkHttpClient = createOkHttpClient(),
        moshi: Moshi = createMoshi()
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(networkConfig.baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    inline fun <reified T> createService(
        networkConfig: NetworkConfig = NetworkConfig(),
        noinline tokenProvider: (() -> String?)? = null
    ): T {
        val okHttpClient = createOkHttpClient(tokenProvider, networkConfig)
        val retrofit = createRetrofit(networkConfig, okHttpClient)
        return retrofit.create(T::class.java)
    }
}
