package com.rentify.app.tenant.ui.search

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.network.api.RoomApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.tenant.data.repository.TenantRoomRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class SearchResultViewModelFactory(
    private val context: Context,
    private val initialFilter: RoomFilter
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchResultViewModel::class.java)) {
            val tokenDataStore = TokenDataStore(context.applicationContext)
            val tokenProvider: () -> String? = {
                runBlocking { tokenDataStore.accessToken.first() }
            }

            val roomApiService = NetworkClient.createService<RoomApiService>(tokenProvider = tokenProvider)
            val roomRepository = TenantRoomRepositoryImpl(roomApiService)

            return SearchResultViewModel(
                roomRepository = roomRepository,
                tokenDataStore = tokenDataStore,
                initialFilter = initialFilter
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
