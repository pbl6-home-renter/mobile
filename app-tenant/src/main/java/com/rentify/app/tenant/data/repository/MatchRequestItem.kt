package com.rentify.app.tenant.data.repository

import com.rentify.app.core.network.dto.matching.MatchRequestListResponseDto

data class MatchRequestCounterpart(
    val id: String,
    val name: String,
    val avatarUrl: String?
)

data class MatchRequestItem(
    val request: MatchRequestListResponseDto,
    val counterpart: MatchRequestCounterpart
)
