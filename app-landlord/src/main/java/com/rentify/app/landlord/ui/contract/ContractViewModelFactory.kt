package com.rentify.app.landlord.ui.contract

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.network.api.BuildingApiService
import com.rentify.app.core.network.api.ContractApiService
import com.rentify.app.core.network.api.MediaApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.landlord.data.repository.LandlordBuildingRepositoryImpl
import com.rentify.app.landlord.data.repository.LandlordContractRepositoryImpl
import com.rentify.app.landlord.data.repository.LandlordMediaRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ContractViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ContractViewModel::class.java)) {
            val tokenDataStore = TokenDataStore(context.applicationContext)
            val tokenProvider = { runBlocking { tokenDataStore.accessToken.first() } }
            val contractApiService = NetworkClient.createService<ContractApiService>(
                tokenProvider = tokenProvider
            )
            val buildingApiService = NetworkClient.createService<BuildingApiService>(
                tokenProvider = tokenProvider
            )
            val mediaApiService = NetworkClient.createService<MediaApiService>(
                tokenProvider = tokenProvider
            )
            val repository = LandlordContractRepositoryImpl(contractApiService)
            val buildingRepository = LandlordBuildingRepositoryImpl(buildingApiService)
            val mediaRepository = LandlordMediaRepositoryImpl(mediaApiService)
            return ContractViewModel(repository, buildingRepository, mediaRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
