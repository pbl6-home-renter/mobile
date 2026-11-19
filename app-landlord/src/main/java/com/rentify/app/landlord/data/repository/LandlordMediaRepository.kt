package com.rentify.app.landlord.data.repository

import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.media.MediaCompleteUploadResponseDto

interface LandlordMediaRepository {
    suspend fun uploadMedia(
        ownerType: String,
        ownerId: String,
        purpose: String,
        fileBytes: ByteArray,
        fileName: String = "file_${System.currentTimeMillis()}.jpg",
        contentType: String = "image/jpeg"
    ): NetworkResult<MediaCompleteUploadResponseDto>

    suspend fun getMediaDownloadUrl(mediaId: String): NetworkResult<String>
}
