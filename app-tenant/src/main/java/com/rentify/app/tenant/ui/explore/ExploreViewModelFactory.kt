package com.rentify.app.tenant.ui.explore

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.data.repository.user.UserRepositoryImpl
import com.rentify.app.core.network.api.RoomApiService
import com.rentify.app.core.network.api.UserApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.tenant.data.local.TenantDevPreferences
import com.rentify.app.tenant.data.repository.TenantRoomRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ExploreViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExploreViewModel::class.java)) {
            val tokenDataStore = TokenDataStore(context.applicationContext)
            val tenantDevPreferences = TenantDevPreferences(context.applicationContext)

            val tokenProvider: () -> String? = {
                runBlocking { tokenDataStore.accessToken.first() }
            }

            val roomApiService = NetworkClient.createService<RoomApiService>(tokenProvider = tokenProvider)
            val userApiService = NetworkClient.createService<UserApiService>(tokenProvider = tokenProvider)

            val roomRepository = TenantRoomRepositoryImpl(roomApiService)
            val userRepository = UserRepositoryImpl(userApiService)

            return ExploreViewModel(
                roomRepository = roomRepository,
                userRepository = userRepository,
                tokenDataStore = tokenDataStore,
                tenantDevPreferences = tenantDevPreferences
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
