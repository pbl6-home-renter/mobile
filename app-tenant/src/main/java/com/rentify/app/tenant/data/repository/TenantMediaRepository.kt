package com.rentify.app.tenant.data.repository

import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.media.MediaCompleteUploadResponseDto

interface TenantMediaRepository {
    suspend fun uploadChatImage(
        conversationId: String,
        fileBytes: ByteArray,
        fileName: String = "image_${System.currentTimeMillis()}.jpg",
        contentType: String = "image/jpeg"
    ): NetworkResult<MediaCompleteUploadResponseDto>

    suspend fun getMediaDownloadUrl(mediaId: String): NetworkResult<String>
}

