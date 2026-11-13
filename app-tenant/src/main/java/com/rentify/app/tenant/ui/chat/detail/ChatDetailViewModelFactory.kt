package com.rentify.app.tenant.ui.chat.detail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.network.api.ChatApiService
import com.rentify.app.core.network.api.MediaApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.tenant.data.repository.TenantChatRepositoryImpl
import com.rentify.app.tenant.data.repository.TenantMediaRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ChatDetailViewModelFactory(
    private val context: Context,
    private val conversationId: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatDetailViewModel::class.java)) {
            val tokenDataStore = TokenDataStore(context.applicationContext)
            val tokenProvider: () -> String? = {
                runBlocking { tokenDataStore.accessToken.first() }
            }

            val chatApiService = NetworkClient.createService<ChatApiService>(tokenProvider = tokenProvider)
            val chatRepository = TenantChatRepositoryImpl(chatApiService)

            val mediaApiService = NetworkClient.createService<MediaApiService>(tokenProvider = tokenProvider)
            val mediaRepository = TenantMediaRepositoryImpl(mediaApiService)

            return ChatDetailViewModel(
                conversationId = conversationId,
                chatRepository = chatRepository,
                mediaRepository = mediaRepository,
                tokenDataStore = tokenDataStore
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
