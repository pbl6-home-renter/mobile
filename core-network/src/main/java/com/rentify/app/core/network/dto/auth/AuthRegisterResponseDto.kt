package com.rentify.app.core.network.dto.auth

import com.rentify.app.core.network.dto.user.UserCurrentResponseDto
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthRegisterResponseDto(
    @field:Json(name = "id") private val _id: String? = null,
    @field:Json(name = "email") private val _email: String? = null,
    @field:Json(name = "phone") private val _phone: String? = null,
    @field:Json(name = "name") private val _name: String? = null,
    @field:Json(name = "role") private val _role: String? = null,
    @field:Json(name = "status") private val _status: String? = null,
    @field:Json(name = "verificationRequired") val verificationRequired: Boolean = false,
    @field:Json(name = "user") val user: UserCurrentResponseDto? = null
) {
    val id: String get() = _id ?: user?.id ?: ""
    val email: String get() = _email ?: user?.email ?: ""
    val phone: String get() = _phone ?: user?.phone ?: ""
    val name: String get() = _name ?: user?.name ?: ""
    val role: String get() = _role ?: user?.role ?: "tenant"
    val status: String get() = _status ?: user?.status ?: "active"
}
