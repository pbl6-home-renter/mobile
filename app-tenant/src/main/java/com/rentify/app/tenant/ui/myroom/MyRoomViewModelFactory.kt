package com.rentify.app.tenant.ui.myroom

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.network.api.ChatApiService
import com.rentify.app.core.network.api.ContractApiService
import com.rentify.app.core.network.api.BuildingApiService
import com.rentify.app.core.network.api.RoomApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.tenant.data.local.TenantDevPreferences
import com.rentify.app.tenant.data.repository.TenantChatRepositoryImpl
import com.rentify.app.tenant.data.repository.TenantContractRepositoryImpl
import com.rentify.app.tenant.data.repository.BuildingWardResolver
import com.rentify.app.tenant.data.repository.MyRoomHubLoader
import com.rentify.app.tenant.data.repository.TenantRoomRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MyRoomViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MyRoomViewModel::class.java)) {
            val tokenDataStore = TokenDataStore(context.applicationContext)
            val tenantDevPreferences = TenantDevPreferences(context.applicationContext)

            val tokenProvider: () -> String? = {
                runBlocking { tokenDataStore.accessToken.first() }
            }

            val roomApiService = NetworkClient.createService<RoomApiService>(tokenProvider = tokenProvider)
            val chatApiService = NetworkClient.createService<ChatApiService>(tokenProvider = tokenProvider)
            val contractApiService = NetworkClient.createService<ContractApiService>(tokenProvider = tokenProvider)

            val roomRepository = TenantRoomRepositoryImpl(roomApiService, BuildingWardResolver(NetworkClient.createService<BuildingApiService>()))
            val hubLoader = MyRoomHubLoader.create(tokenDataStore, tokenProvider, roomRepository)
            val chatRepository = TenantChatRepositoryImpl(chatApiService)
            val contractRepository = TenantContractRepositoryImpl(contractApiService)

            return MyRoomViewModel(
                hubLoader = hubLoader,
                chatRepository = chatRepository,
                contractRepository = contractRepository,
                tokenDataStore = tokenDataStore,
                tenantDevPreferences = tenantDevPreferences
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
