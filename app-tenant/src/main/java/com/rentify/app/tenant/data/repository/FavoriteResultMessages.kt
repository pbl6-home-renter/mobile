package com.rentify.app.tenant.data.repository

import com.rentify.app.core.network.adapter.NetworkResult

object FavoriteResultMessages {

    private const val HTTP_UNAUTHORIZED = 401
    private const val HTTP_FORBIDDEN = 403
    private const val HTTP_NOT_FOUND = 404
    private const val HTTP_CONFLICT = 409

    fun isAlreadyFavorited(result: NetworkResult<*>): Boolean {
        return result is NetworkResult.ApiError && result.code == HTTP_CONFLICT
    }

    fun failureMessage(result: NetworkResult<*>, adding: Boolean): String {
        val action = if (adding) "lưu phòng" else "bỏ lưu phòng"
        return when (result) {
            is NetworkResult.ApiError -> when (result.code) {
                HTTP_UNAUTHORIZED -> "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại"
                HTTP_FORBIDDEN -> "Tài khoản không có quyền $action"
                HTTP_NOT_FOUND -> "Chức năng yêu thích chưa khả dụng, vui lòng thử lại sau"
                else -> "Không thể $action (Mã ${result.error?.code ?: result.code})"
            }
            is NetworkResult.NetworkError -> "Không có kết nối mạng, vui lòng kiểm tra lại"
            else -> "Không thể $action, vui lòng thử lại"
        }
    }
}
