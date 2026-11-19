package com.rentify.app.tenant.data.repository

import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.dto.matching.RoommateProfileListResponseDto

class RoommateProfileResolver(
    private val matchingRepository: TenantMatchingRepository
) {

    private var profilesByTenantId: Map<String, RoommateProfileListResponseDto> = emptyMap()
    private var lastLoadedAt = 0L

    suspend fun resolve(tenantIds: Collection<String>): Map<String, RoommateProfileListResponseDto> {
        val ids = tenantIds.toSet()
        val now = System.currentTimeMillis()
        if (ids.any { it !in profilesByTenantId } && now - lastLoadedAt > MIN_RELOAD_INTERVAL_MS) {
            loadAll()
            lastLoadedAt = now
        }
        return ids.mapNotNull { id -> profilesByTenantId[id]?.let { id to it } }.toMap()
    }

    private suspend fun loadAll() {
        val loaded = mutableMapOf<String, RoommateProfileListResponseDto>()
        var pageNo = 1
        do {
            val result = matchingRepository.getRoommateProfiles(pageNo = pageNo, pageSize = PAGE_SIZE)
            if (result !is NetworkResult.Success) return
            result.data.data?.forEach { loaded[it.tenantId] = it }
            val hasNextPage = result.data.pagination?.hasNextPage == true
            pageNo++
        } while (hasNextPage && pageNo <= MAX_PAGES)
        profilesByTenantId = loaded
    }

    private companion object {
        const val PAGE_SIZE = 100
        const val MAX_PAGES = 10
        const val MIN_RELOAD_INTERVAL_MS = 15_000L
    }
}
