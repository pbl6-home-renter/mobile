package com.rentify.app.core.data.model

data class UserSession(
    val userId: String,
    val role: String,
    val accessToken: String,
    val isLandlordProfileCompleted: Boolean = false
)
