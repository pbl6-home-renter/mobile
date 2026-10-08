package com.rentify.app.core.network.adapter

import com.rentify.app.core.network.dto.ApiErrorResponse

sealed interface NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>
    data class ApiError(val code: Int, val error: ApiErrorResponse?) : NetworkResult<Nothing>
    data class NetworkError(val exception: Throwable) : NetworkResult<Nothing>
    data class UnknownError(val exception: Throwable?) : NetworkResult<Nothing>
}
