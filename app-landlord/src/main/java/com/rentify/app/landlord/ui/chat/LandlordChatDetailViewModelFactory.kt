package com.rentify.app.landlord.ui.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.network.api.ChatApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.landlord.data.repository.LandlordChatRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class LandlordChatDetailViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LandlordChatDetailViewModel::class.java)) {
            val tokenDataStore = TokenDataStore(context.applicationContext)
            val chatApiService = NetworkClient.createService<ChatApiService>(
                tokenProvider = { runBlocking { tokenDataStore.accessToken.first() } }
            )
            val repository = LandlordChatRepositoryImpl(chatApiService)
            return LandlordChatDetailViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

