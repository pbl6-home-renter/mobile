package com.rentify.app.landlord.ui.operations

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.network.api.IssueApiService
import com.rentify.app.core.network.api.RoomApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.landlord.data.repository.LandlordIssueRepositoryImpl
import com.rentify.app.landlord.data.repository.LandlordRoomRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class IssueViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IssueViewModel::class.java)) {
            val tokenDataStore = TokenDataStore(context.applicationContext)
            val tokenProvider = { runBlocking { tokenDataStore.accessToken.first() } }
            val issueApiService = NetworkClient.createService<IssueApiService>(
                tokenProvider = tokenProvider
            )
            val roomApiService = NetworkClient.createService<RoomApiService>(
                tokenProvider = tokenProvider
            )
            val repository = LandlordIssueRepositoryImpl(issueApiService)
            val roomRepository = LandlordRoomRepositoryImpl(roomApiService)
            return IssueViewModel(repository, roomRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

