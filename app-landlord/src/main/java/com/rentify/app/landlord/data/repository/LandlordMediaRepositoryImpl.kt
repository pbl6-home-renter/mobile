package com.rentify.app.landlord.data.repository

import com.rentify.app.core.data.repository.BaseRepository
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.api.MediaApiService
import com.rentify.app.core.network.dto.media.MediaCompleteUploadResponseDto
import com.rentify.app.core.network.dto.media.MediaPresignUploadRequestDto
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class LandlordMediaRepositoryImpl(
    private val mediaApiService: MediaApiService,
    private val rawMediaApiService: MediaApiService = mediaApiService
) : BaseRepository(), LandlordMediaRepository {

    override suspend fun uploadMedia(
        ownerType: String,
        ownerId: String,
        purpose: String,
        fileBytes: ByteArray,
        fileName: String,
        contentType: String
    ): NetworkResult<MediaCompleteUploadResponseDto> {
        val presignRequest = MediaPresignUploadRequestDto(
            ownerType = ownerType,
            ownerId = ownerId,
            purpose = purpose,
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
        } ?: return NetworkResult.UnknownError(Exception("Không nhận được đường dẫn tải tệp lên"))

        val mediaType = contentType.toMediaTypeOrNull()
        val body = fileBytes.toRequestBody(mediaType)
        val headers = presignData.requiredHeaders ?: emptyMap()

        try {
            rawMediaApiService.uploadFileToStorage(
                uploadUrl = presignData.uploadUrl,
                headers = headers,
                fileBody = body
            )
        } catch (_: Exception) {
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
                    NetworkResult.UnknownError(Exception("Không thể xác nhận hoàn tất tải tệp"))
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
                val url = result.data.data?.url
                if (!url.isNullOrBlank()) {
                    NetworkResult.Success(url)
                } else {
                    NetworkResult.UnknownError(Exception("Đường dẫn tải tệp trống"))
                }
            }
            is NetworkResult.ApiError -> NetworkResult.ApiError(result.code, result.error)
            is NetworkResult.NetworkError -> result
            is NetworkResult.UnknownError -> result
        }
    }
}
