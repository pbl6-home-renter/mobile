package com.rentify.app.core.network.client

data class NetworkConfig(
    val baseUrl: String = DEFAULT_BASE_URL,
    val connectTimeoutSeconds: Long = 30L,
    val readTimeoutSeconds: Long = 30L
) {
    companion object {
        const val DEFAULT_BASE_URL = "https://mock.apidog.com/m1/1370283-1374978-1459060/"
    }
}
