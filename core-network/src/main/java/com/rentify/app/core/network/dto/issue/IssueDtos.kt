package com.rentify.app.core.network.dto.issue

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class IssueReportMyListResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "roomId") val roomId: String,
    @field:Json(name = "roomName") val roomName: String = "",
    @field:Json(name = "title") val title: String = "",
    @field:Json(name = "category") val category: String = "OTHER",
    @field:Json(name = "urgency") val urgency: String = "MEDIUM",
    @field:Json(name = "status") val status: String = "open",
    @field:Json(name = "description") val description: String? = null,
    @field:Json(name = "createdAt") val createdAt: String = ""
)

@JsonClass(generateAdapter = true)
data class IssueReportCreateRequestDto(
    @field:Json(name = "roomId") val roomId: String,
    @field:Json(name = "title") val title: String,
    @field:Json(name = "category") val category: String,
    @field:Json(name = "urgency") val urgency: String,
    @field:Json(name = "description") val description: String? = null,
    @field:Json(name = "issuePhotoMediaIds") val issuePhotoMediaIds: List<String> = emptyList(),
    @field:Json(name = "taggedUserId") val taggedUserId: String? = null,
    @field:Json(name = "taggedRoomId") val taggedRoomId: String? = null
)

@JsonClass(generateAdapter = true)
data class IssueReportCreateResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "status") val status: String = "open",
    @field:Json(name = "title") val title: String = ""
)

@JsonClass(generateAdapter = true)
data class IssueReportDetailResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "roomId") val roomId: String,
    @field:Json(name = "roomName") val roomName: String = "",
    @field:Json(name = "buildingId") val buildingId: String? = null,
    @field:Json(name = "buildingName") val buildingName: String = "",
    @field:Json(name = "reporterId") val reporterId: String? = null,
    @field:Json(name = "reporterName") val reporterName: String = "",
    @field:Json(name = "reporterPhone") val reporterPhone: String = "",
    @field:Json(name = "title") val title: String = "",
    @field:Json(name = "category") val category: String = "OTHER",
    @field:Json(name = "urgency") val urgency: String = "MEDIUM",
    @field:Json(name = "description") val description: String? = null,
    @field:Json(name = "status") val status: String = "open",
    @field:Json(name = "issuePhotoMediaIds") val issuePhotoMediaIds: List<String> = emptyList(),
    @field:Json(name = "assignedTo") val assignedTo: String? = null,
    @field:Json(name = "repairCost") val repairCost: Long? = null,
    @field:Json(name = "resolutionNote") val resolutionNote: String? = null,
    @field:Json(name = "resolutionMediaUrls") val resolutionMediaUrls: List<String> = emptyList(),
    @field:Json(name = "createdAt") val createdAt: String = "",
    @field:Json(name = "closedAt") val closedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class IssueReportCancelResponseDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "status") val status: String = "cancelled"
)

