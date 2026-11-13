package com.rentify.app.core.network.dto.media

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MediaPresignUploadRequestDto(
    @field:Json(name = "ownerType") val ownerType: String,
    @field:Json(name = "ownerId") val ownerId: String,
    @field:Json(name = "purpose") val purpose: String,
    @field:Json(name = "fileName") val fileName: String,
    @field:Json(name = "contentType") val contentType: String,
    @field:Json(name = "size") val size: Long,
    @field:Json(name = "captureSource") val captureSource: String? = null
)

@JsonClass(generateAdapter = true)
data class MediaPresignUploadResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "ownerType") val ownerType: String = "",
    @field:Json(name = "ownerId") val ownerId: String = "",
    @field:Json(name = "purpose") val purpose: String = "",
    @field:Json(name = "uploadUrl") val uploadUrl: String = "",
    @field:Json(name = "requiredHeaders") val requiredHeaders: Map<String, String>? = null,
    @field:Json(name = "expiresIn") val expiresIn: Int = 0
)

@JsonClass(generateAdapter = true)
data class MediaCompleteUploadResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "ownerType") val ownerType: String = "",
    @field:Json(name = "ownerId") val ownerId: String = "",
    @field:Json(name = "purpose") val purpose: String = "",
    @field:Json(name = "fileType") val fileType: String = "",
    @field:Json(name = "url") val url: String = ""
)

@JsonClass(generateAdapter = true)
data class MediaDownloadUrlResponseDto(
    @field:Json(name = "downloadUrl") val downloadUrl: String,
    @field:Json(name = "expiresInSeconds") val expiresInSeconds: Int = 300
)

