package com.rentify.app.landlord.ui.notification

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.network.api.NotificationApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.landlord.data.repository.LandlordNotificationRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class NotificationViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
            val tokenDataStore = TokenDataStore(context.applicationContext)
            val tokenProvider: () -> String? = { runBlocking { tokenDataStore.accessToken.first() } }
            val api = NetworkClient.createService<NotificationApiService>(tokenProvider = tokenProvider)
            val repository = LandlordNotificationRepositoryImpl(api)
            return NotificationViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
