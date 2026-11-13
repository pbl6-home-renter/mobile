package com.rentify.app.landlord.ui.property

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.network.api.AdministrativeUnitsApiService
import com.rentify.app.core.network.api.BuildingApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.landlord.data.repository.AdministrativeUnitsRepositoryImpl
import com.rentify.app.landlord.data.repository.LandlordBuildingRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BuildingViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BuildingViewModel::class.java)) {
            val tokenDataStore = TokenDataStore(context.applicationContext)
            val buildingApiService = NetworkClient.createService<BuildingApiService>(
                tokenProvider = { runBlocking { tokenDataStore.accessToken.first() } }
            )
            val administrativeUnitsApiService = NetworkClient.createService<AdministrativeUnitsApiService>(
                tokenProvider = { runBlocking { tokenDataStore.accessToken.first() } }
            )
            val buildingRepository = LandlordBuildingRepositoryImpl(buildingApiService)
            val administrativeUnitsRepository = AdministrativeUnitsRepositoryImpl(administrativeUnitsApiService)

            return BuildingViewModel(buildingRepository, administrativeUnitsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
