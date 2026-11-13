package com.rentify.app.tenant.ui.room.detail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.data.local.session.SessionManager
import com.rentify.app.core.network.api.ChatApiService
import com.rentify.app.core.network.api.RoomApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.tenant.data.repository.TenantChatRepositoryImpl
import com.rentify.app.tenant.data.repository.TenantRoomRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class RoomDetailViewModelFactory(
    private val context: Context,
    private val roomId: String,
    private val initialIsFavorited: Boolean = false
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoomDetailViewModel::class.java)) {
            val tokenDataStore = TokenDataStore(context.applicationContext)
            val tokenProvider: () -> String? = {
                runBlocking { tokenDataStore.accessToken.first() }
            }

            val roomApiService = NetworkClient.createService<RoomApiService>(tokenProvider = tokenProvider)
            val chatApiService = NetworkClient.createService<ChatApiService>(tokenProvider = tokenProvider)

            val roomRepository = TenantRoomRepositoryImpl(roomApiService)
            val chatRepository = TenantChatRepositoryImpl(chatApiService)
            val sessionManager = SessionManager(tokenDataStore)

            return RoomDetailViewModel(
                roomId = roomId,
                initialIsFavorited = initialIsFavorited,
                roomRepository = roomRepository,
                chatRepository = chatRepository,
                sessionManager = sessionManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

