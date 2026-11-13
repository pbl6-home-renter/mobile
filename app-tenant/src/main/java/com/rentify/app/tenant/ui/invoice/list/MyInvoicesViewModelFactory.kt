package com.rentify.app.tenant.ui.invoice.list

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.network.api.InvoiceApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.tenant.data.repository.TenantInvoiceRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MyInvoicesViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MyInvoicesViewModel::class.java)) {
            val tokenDataStore = TokenDataStore(context.applicationContext)
            val tokenProvider: () -> String? = {
                runBlocking { tokenDataStore.accessToken.first() }
            }

            val apiService = NetworkClient.createService<InvoiceApiService>(tokenProvider = tokenProvider)
            val repository = TenantInvoiceRepositoryImpl(apiService)

            return MyInvoicesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

