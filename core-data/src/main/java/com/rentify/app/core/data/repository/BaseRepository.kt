package com.rentify.app.core.data.repository

import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.ApiErrorResponse
import com.squareup.moshi.Moshi
import retrofit2.Response

abstract class BaseRepository {

    protected suspend fun <T> safeApiCall(
        apiCall: suspend () -> Response<T>
    ): NetworkResult<T> {
        return try {
            val response = apiCall()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    NetworkResult.Success(body)
                } else {
                    NetworkResult.ApiError(response.code(), null)
                }
            } else {
                val errorBodyString = response.errorBody()?.string()
                val apiError = parseErrorBody(errorBodyString)
                NetworkResult.ApiError(response.code(), apiError)
            }
        } catch (e: Exception) {
            NetworkResult.NetworkError(e)
        }
    }

    private fun parseErrorBody(errorBody: String?): ApiErrorResponse? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            val moshi = Moshi.Builder().build()
            val adapter = moshi.adapter(ApiErrorResponse::class.java)
            adapter.fromJson(errorBody)
        } catch (e: Exception) {
            null
        }
    }
}
