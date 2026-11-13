package com.rentify.app.landlord.ui.property

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.network.api.BuildingApiService
import com.rentify.app.core.network.api.RoomApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.landlord.data.repository.LandlordBuildingRepositoryImpl
import com.rentify.app.landlord.data.repository.LandlordRoomRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class RoomViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoomViewModel::class.java)) {
            val tokenDataStore = TokenDataStore(context.applicationContext)
            val roomApiService = NetworkClient.createService<RoomApiService>(
                tokenProvider = { runBlocking { tokenDataStore.accessToken.first() } }
            )
            val buildingApiService = NetworkClient.createService<BuildingApiService>(
                tokenProvider = { runBlocking { tokenDataStore.accessToken.first() } }
            )
            val roomRepository = LandlordRoomRepositoryImpl(roomApiService)
            val buildingRepository = LandlordBuildingRepositoryImpl(buildingApiService)

            return RoomViewModel(roomRepository, buildingRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

