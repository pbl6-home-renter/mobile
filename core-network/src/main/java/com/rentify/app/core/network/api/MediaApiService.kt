package com.rentify.app.core.network.api

import com.rentify.app.core.network.dto.ApiSingleResponse
import com.rentify.app.core.network.dto.media.MediaCompleteUploadResponseDto
import com.rentify.app.core.network.dto.media.MediaDownloadUrlResponseDto
import com.rentify.app.core.network.dto.media.MediaPresignUploadRequestDto
import com.rentify.app.core.network.dto.media.MediaPresignUploadResponseDto
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Url

interface MediaApiService {

    @POST("media/presign-upload")
    suspend fun requestPresignUpload(
        @Body request: MediaPresignUploadRequestDto
    ): Response<ApiSingleResponse<MediaPresignUploadResponseDto>>

    @POST("media/{mediaId}/complete-upload")
    suspend fun completeUpload(
        @Path("mediaId") mediaId: String
    ): Response<ApiSingleResponse<MediaCompleteUploadResponseDto>>

    @GET("media/{mediaId}/download-url")
    suspend fun getMediaDownloadUrl(
        @Path("mediaId") mediaId: String
    ): Response<ApiSingleResponse<MediaDownloadUrlResponseDto>>

    @PUT
    suspend fun uploadFileToStorage(
        @Url uploadUrl: String,
        @HeaderMap headers: Map<String, String>,
        @Body fileBody: RequestBody
    ): Response<Unit>
}

