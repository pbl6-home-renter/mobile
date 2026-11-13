package com.rentify.app.tenant.ui.contract.list

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.network.api.ContractApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.tenant.data.repository.TenantContractRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MyContractsViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MyContractsViewModel::class.java)) {
            val tokenDataStore = TokenDataStore(context.applicationContext)
            val tokenProvider: () -> String? = {
                runBlocking { tokenDataStore.accessToken.first() }
            }

            val contractApiService = NetworkClient.createService<ContractApiService>(tokenProvider = tokenProvider)
            val contractRepository = TenantContractRepositoryImpl(contractApiService)

            return MyContractsViewModel(contractRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

