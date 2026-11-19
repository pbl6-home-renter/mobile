package com.rentify.app.landlord.ui.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.network.api.ChatApiService
import com.rentify.app.core.network.api.ContractApiService
import com.rentify.app.core.network.api.InvoiceApiService
import com.rentify.app.core.network.api.MediaApiService
import com.rentify.app.core.network.api.UserApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.landlord.data.repository.LandlordChatRepositoryImpl
import com.rentify.app.landlord.data.repository.LandlordContractRepositoryImpl
import com.rentify.app.landlord.data.repository.LandlordInvoiceRepositoryImpl
import com.rentify.app.landlord.data.repository.LandlordMediaRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class LandlordChatDetailViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LandlordChatDetailViewModel::class.java)) {
            val tokenDataStore = TokenDataStore(context.applicationContext)
            val tokenProvider = { runBlocking { tokenDataStore.accessToken.first() } }
            val chatApiService = NetworkClient.createService<ChatApiService>(
                tokenProvider = tokenProvider
            )
            val userApiService = NetworkClient.createService<UserApiService>(
                tokenProvider = tokenProvider
            )
            val mediaApiService = NetworkClient.createService<MediaApiService>(
                tokenProvider = tokenProvider
            )
            val invoiceApiService = NetworkClient.createService<InvoiceApiService>(
                tokenProvider = tokenProvider
            )
            val contractApiService = NetworkClient.createService<ContractApiService>(
                tokenProvider = tokenProvider
            )
            val repository = LandlordChatRepositoryImpl(chatApiService, userApiService)
            val mediaRepository = LandlordMediaRepositoryImpl(mediaApiService)
            val invoiceRepository = LandlordInvoiceRepositoryImpl(invoiceApiService)
            val contractRepository = LandlordContractRepositoryImpl(contractApiService)
            return LandlordChatDetailViewModel(
                chatRepository = repository,
                tokenDataStore = tokenDataStore,
                mediaRepository = mediaRepository,
                invoiceRepository = invoiceRepository,
                contractRepository = contractRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
