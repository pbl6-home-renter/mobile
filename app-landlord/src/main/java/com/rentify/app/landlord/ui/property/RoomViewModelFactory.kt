package com.rentify.app.landlord.ui.property

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.network.api.BillingSettingApiService
import com.rentify.app.core.network.api.BuildingApiService
import com.rentify.app.core.network.api.MediaApiService
import com.rentify.app.core.network.api.RatePolicyApiService
import com.rentify.app.core.network.api.RoomApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.landlord.data.repository.LandlordBuildingRepositoryImpl
import com.rentify.app.landlord.data.repository.LandlordMediaRepositoryImpl
import com.rentify.app.landlord.data.repository.LandlordPolicySettingRepositoryImpl
import com.rentify.app.landlord.data.repository.LandlordRoomRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class RoomViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoomViewModel::class.java)) {
            val tokenDataStore = TokenDataStore(context.applicationContext)
            val tokenProvider = { runBlocking { tokenDataStore.accessToken.first() } }
            val roomApiService = NetworkClient.createService<RoomApiService>(
                tokenProvider = tokenProvider
            )
            val buildingApiService = NetworkClient.createService<BuildingApiService>(
                tokenProvider = tokenProvider
            )
            val mediaApiService = NetworkClient.createService<MediaApiService>(
                tokenProvider = tokenProvider
            )
            val ratePolicyApiService = NetworkClient.createService<RatePolicyApiService>(
                tokenProvider = tokenProvider
            )
            val billingSettingApiService = NetworkClient.createService<BillingSettingApiService>(
                tokenProvider = tokenProvider
            )
            val roomRepository = LandlordRoomRepositoryImpl(roomApiService)
            val buildingRepository = LandlordBuildingRepositoryImpl(buildingApiService)
            val mediaRepository = LandlordMediaRepositoryImpl(mediaApiService)
            val policySettingRepository = LandlordPolicySettingRepositoryImpl(
                ratePolicyApiService = ratePolicyApiService,
                billingSettingApiService = billingSettingApiService
            )

            return RoomViewModel(
                roomRepository = roomRepository,
                buildingRepository = buildingRepository,
                mediaRepository = mediaRepository,
                policySettingRepository = policySettingRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

