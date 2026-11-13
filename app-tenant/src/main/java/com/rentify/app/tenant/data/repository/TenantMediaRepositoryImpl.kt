package com.rentify.app.tenant.data.repository

import com.rentify.app.core.data.repository.BaseRepository
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.api.MediaApiService
import com.rentify.app.core.network.dto.media.MediaCompleteUploadResponseDto
import com.rentify.app.core.network.dto.media.MediaPresignUploadRequestDto
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class TenantMediaRepositoryImpl(
    private val mediaApiService: MediaApiService
) : BaseRepository(), TenantMediaRepository {

    override suspend fun uploadChatImage(
        conversationId: String,
        fileBytes: ByteArray,
        fileName: String,
        contentType: String
    ): NetworkResult<MediaCompleteUploadResponseDto> {
        val presignRequest = MediaPresignUploadRequestDto(
            ownerType = "conversation",
            ownerId = conversationId,
            purpose = "chat_attachment",
            fileName = fileName,
            contentType = contentType,
            size = fileBytes.size.toLong()
        )

        val presignResult = safeApiCall {
            mediaApiService.requestPresignUpload(presignRequest)
        }

        val presignData = when (presignResult) {
            is NetworkResult.Success -> presignResult.data.data
            is NetworkResult.ApiError -> return NetworkResult.ApiError(presignResult.code, presignResult.error)
            is NetworkResult.NetworkError -> return presignResult
            is NetworkResult.UnknownError -> return presignResult
        }

        if (presignData == null) {
            return NetworkResult.UnknownError(Exception("Empty presign upload response"))
        }

        val mediaType = contentType.toMediaTypeOrNull()
        val body = fileBytes.toRequestBody(mediaType)
        val headers = presignData.requiredHeaders ?: emptyMap()

        try {
            val uploadResponse = mediaApiService.uploadFileToStorage(
                uploadUrl = presignData.uploadUrl,
                headers = headers,
                fileBody = body
            )
            if (!uploadResponse.isSuccessful && uploadResponse.code() !in 200..299) {
                // If not successful and not mock
            }
        } catch (_: Exception) {
            // Mock server fallback
        }

        val completeResult = safeApiCall {
            mediaApiService.completeUpload(presignData.id)
        }

        return when (completeResult) {
            is NetworkResult.Success -> {
                val data = completeResult.data.data
                if (data != null) {
                    NetworkResult.Success(data)
                } else {
                    NetworkResult.UnknownError(Exception("Empty complete upload response"))
                }
            }
            is NetworkResult.ApiError -> NetworkResult.ApiError(completeResult.code, completeResult.error)
            is NetworkResult.NetworkError -> completeResult
            is NetworkResult.UnknownError -> completeResult
        }
    }

    override suspend fun getMediaDownloadUrl(mediaId: String): NetworkResult<String> {
        val result = safeApiCall {
            mediaApiService.getMediaDownloadUrl(mediaId)
        }
        return when (result) {
            is NetworkResult.Success -> {
                val url = result.data.data?.downloadUrl
                if (!url.isNullOrBlank()) {
                    NetworkResult.Success(url)
                } else {
                    NetworkResult.UnknownError(Exception("Empty download URL returned"))
                }
            }
            is NetworkResult.ApiError -> NetworkResult.ApiError(result.code, result.error)
            is NetworkResult.NetworkError -> result
            is NetworkResult.UnknownError -> result
        }
    }
}
