package com.rentify.app.core.network.client

data class NetworkConfig(
    val baseUrl: String = DEFAULT_BASE_URL,
    val connectTimeoutSeconds: Long = 30L,
    val readTimeoutSeconds: Long = 30L
) {
    companion object {
        const val DEFAULT_BASE_URL = "http://3.26.114.197/api/v1/"
    }
}
