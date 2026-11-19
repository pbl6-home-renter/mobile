package com.rentify.app.tenant.ui.building

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.network.api.BuildingApiService
import com.rentify.app.core.network.api.RoomApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.tenant.data.repository.BuildingWardResolver
import com.rentify.app.tenant.data.repository.TenantBuildingRepositoryImpl
import com.rentify.app.tenant.data.repository.TenantRoomRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BuildingDetailViewModelFactory(
    private val context: Context,
    private val buildingId: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BuildingDetailViewModel::class.java)) {
            val tokenDataStore = TokenDataStore(context.applicationContext)
            val tokenProvider: () -> String? = {
                runBlocking { tokenDataStore.accessToken.first() }
            }

            val roomApiService = NetworkClient.createService<RoomApiService>(tokenProvider = tokenProvider)
            val buildingWardResolver = BuildingWardResolver(NetworkClient.createService<BuildingApiService>())
            val roomRepository = TenantRoomRepositoryImpl(roomApiService, buildingWardResolver)
            val buildingRepository = TenantBuildingRepositoryImpl(roomRepository, buildingWardResolver)

            return BuildingDetailViewModel(buildingId, buildingRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
