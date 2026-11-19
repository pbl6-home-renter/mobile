package com.rentify.app.tenant.data.repository

import com.rentify.app.core.data.repository.BaseRepository
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.api.BuildingApiService
import com.rentify.app.core.network.dto.building.BuildingPublicResponseDto
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class BuildingWardResolver(
    private val buildingApiService: BuildingApiService
) : BaseRepository() {

    suspend fun wardsOf(buildingIds: Collection<String>): Map<String, String> {
        return buildingsOf(buildingIds).mapValues { it.value.ward }
    }

    suspend fun buildingsOf(buildingIds: Collection<String>): Map<String, BuildingPublicResponseDto> {
        val ids = buildingIds.filter { it.isNotBlank() }.toSet()
        if (ids.isEmpty()) return emptyMap()

        return mutex.withLock {
            if (ids.any { !cache.containsKey(it) }) loadAllBuildings()
            ids.filter { !cache.containsKey(it) }.forEach { loadBuilding(it) }
            ids.mapNotNull { id -> cache[id]?.let { id to it } }.toMap()
        }
    }

    private suspend fun loadAllBuildings() {
        var pageNo = 1
        var hasNextPage: Boolean
        do {
            val result = safeApiCall { buildingApiService.getPublicBuildings(pageNo = pageNo) }
            if (result !is NetworkResult.Success) return
            result.data.data.forEach { cache[it.id] = it }
            hasNextPage = result.data.pagination?.hasNextPage == true
            pageNo++
        } while (hasNextPage && pageNo <= MAX_PAGES)
    }

    private suspend fun loadBuilding(buildingId: String) {
        val result = safeApiCall { buildingApiService.getPublicBuilding(buildingId) }
        if (result is NetworkResult.Success) {
            result.data.data?.let { cache[it.id] = it }
        }
    }

    private companion object {
        const val MAX_PAGES = 20
        val cache = ConcurrentHashMap<String, BuildingPublicResponseDto>()
        val mutex = Mutex()
    }
}
