package com.rentify.app.core.data.repository

import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.core.network.dto.ApiErrorResponse
import retrofit2.Response

abstract class BaseRepository {

    private val errorAdapter by lazy {
        NetworkClient.createMoshi().adapter(ApiErrorResponse::class.java)
    }

    protected suspend fun <T> safeApiCall(
        apiCall: suspend () -> Response<T>
    ): NetworkResult<T> {
        return try {
            val response = apiCall()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    NetworkResult.Success(body)
                } else if (response.code() == 204) {
                    @Suppress("UNCHECKED_CAST")
                    NetworkResult.Success(Unit as T)
                } else {
                    NetworkResult.ApiError(response.code(), null)
                }
            } else {
                val errorBodyString = response.errorBody()?.string()
                val apiError = parseErrorBody(errorBodyString)
                NetworkResult.ApiError(response.code(), apiError)
            }
        } catch (e: java.io.IOException) {
            NetworkResult.NetworkError(e)
        } catch (e: Exception) {
            NetworkResult.UnknownError(e)
        }
    }

    private fun parseErrorBody(errorBody: String?): ApiErrorResponse? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            errorAdapter.fromJson(errorBody)
        } catch (e: Exception) {
            null
        }
    }
}
