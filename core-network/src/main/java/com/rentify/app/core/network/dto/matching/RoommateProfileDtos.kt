package com.rentify.app.core.network.dto.matching

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LifestyleDto(
    @field:Json(name = "cleanAndTidy") val cleanAndTidy: Boolean? = null,
    @field:Json(name = "earlyBird") val earlyBird: Boolean? = null,
    @field:Json(name = "nightOwl") val nightOwl: Boolean? = null,
    @field:Json(name = "petFriendly") val petFriendly: Boolean? = null,
    @field:Json(name = "smoking") val smoking: Boolean? = null,
    @field:Json(name = "cooking") val cooking: Boolean? = null,
    @field:Json(name = "guestsAllowed") val guestsAllowed: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class RoommateProfileListResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "tenantId") val tenantId: String,
    @field:Json(name = "tenantName") val tenantName: String,
    @field:Json(name = "avatarUrl") val avatarUrl: String? = null,
    @field:Json(name = "gender") val gender: String,
    @field:Json(name = "birthYear") val birthYear: Int? = null,
    @field:Json(name = "occupation") val occupation: String? = null,
    @field:Json(name = "budgetMin") val budgetMin: Long? = null,
    @field:Json(name = "budgetMax") val budgetMax: Long? = null,
    @field:Json(name = "preferredDistrict") val preferredDistrict: String? = null,
    @field:Json(name = "preferredArea") val preferredArea: String? = null,
    @field:Json(name = "lifestyleTags") val lifestyleTags: List<String> = emptyList(),
    @field:Json(name = "matchScore") val matchScore: Int? = null,
    @field:Json(name = "bio") val bio: String? = null,
    @field:Json(name = "isVisible") val isVisible: Boolean
)

@JsonClass(generateAdapter = true)
data class RoommateProfileDetailResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "tenantId") val tenantId: String,
    @field:Json(name = "tenantName") val tenantName: String,
    @field:Json(name = "avatarUrl") val avatarUrl: String? = null,
    @field:Json(name = "gender") val gender: String,
    @field:Json(name = "birthYear") val birthYear: Int? = null,
    @field:Json(name = "occupation") val occupation: String? = null,
    @field:Json(name = "preferredProvince") val preferredProvince: String? = null,
    @field:Json(name = "preferredDistrict") val preferredDistrict: String? = null,
    @field:Json(name = "preferredArea") val preferredArea: String? = null,
    @field:Json(name = "budgetMin") val budgetMin: Long? = null,
    @field:Json(name = "budgetMax") val budgetMax: Long? = null,
    @field:Json(name = "bio") val bio: String? = null,
    @field:Json(name = "lifestyleDetails") val lifestyleDetails: LifestyleDto? = null,
    @field:Json(name = "aiMatchScore") val aiMatchScore: Int? = null,
    @field:Json(name = "aiAdvice") val aiAdvice: String? = null,
    @field:Json(name = "isVisible") val isVisible: Boolean
)

@JsonClass(generateAdapter = true)
data class RoommateProfileMyResponseDto(
    @field:Json(name = "tenantId") val tenantId: String,
    @field:Json(name = "gender") val gender: String? = null,
    @field:Json(name = "birthYear") val birthYear: Int? = null,
    @field:Json(name = "occupation") val occupation: String? = null,
    @field:Json(name = "preferredProvince") val preferredProvince: String? = null,
    @field:Json(name = "preferredDistrict") val preferredDistrict: String? = null,
    @field:Json(name = "preferredArea") val preferredArea: String? = null,
    @field:Json(name = "budgetMin") val budgetMin: Long? = null,
    @field:Json(name = "budgetMax") val budgetMax: Long? = null,
    @field:Json(name = "moveInOn") val moveInOn: String? = null,
    @field:Json(name = "bio") val bio: String? = null,
    @field:Json(name = "lifestyle") val lifestyle: LifestyleDto? = null,
    @field:Json(name = "personality") val personality: Map<String, Any>? = null,
    @field:Json(name = "isVisible") val isVisible: Boolean
)

@JsonClass(generateAdapter = true)
data class RoommateProfileSaveRequestDto(
    @field:Json(name = "gender") val gender: String? = null,
    @field:Json(name = "birthYear") val birthYear: Int? = null,
    @field:Json(name = "occupation") val occupation: String? = null,
    @field:Json(name = "preferredProvince") val preferredProvince: String? = null,
    @field:Json(name = "preferredDistrict") val preferredDistrict: String? = null,
    @field:Json(name = "preferredArea") val preferredArea: String? = null,
    @field:Json(name = "budgetMin") val budgetMin: Long? = null,
    @field:Json(name = "budgetMax") val budgetMax: Long? = null,
    @field:Json(name = "moveInOn") val moveInOn: String? = null,
    @field:Json(name = "bio") val bio: String? = null,
    @field:Json(name = "lifestyle") val lifestyle: LifestyleDto? = null,
    @field:Json(name = "personality") val personality: Map<String, Any>? = null,
    @field:Json(name = "isVisible") val isVisible: Boolean
)

@JsonClass(generateAdapter = true)
data class RoommateProfileSaveResponseDto(
    @field:Json(name = "tenantId") val tenantId: String,
    @field:Json(name = "isVisible") val isVisible: Boolean
)

