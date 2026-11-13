package com.rentify.app.landlord.ui.billing

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.network.api.InvoiceApiService
import com.rentify.app.core.network.api.MeterApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.landlord.data.repository.LandlordInvoiceRepositoryImpl
import com.rentify.app.landlord.data.repository.LandlordMeterRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BillingViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BillingViewModel::class.java)) {
            val tokenDataStore = TokenDataStore(context.applicationContext)
            val tokenProvider = { runBlocking { tokenDataStore.accessToken.first() } }

            val meterApiService = NetworkClient.createService<MeterApiService>(tokenProvider = tokenProvider)
            val invoiceApiService = NetworkClient.createService<InvoiceApiService>(tokenProvider = tokenProvider)

            val meterRepository = LandlordMeterRepositoryImpl(meterApiService)
            val invoiceRepository = LandlordInvoiceRepositoryImpl(invoiceApiService)

            return BillingViewModel(meterRepository, invoiceRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

