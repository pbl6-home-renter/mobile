package com.rentify.app.landlord.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.network.api.BuildingApiService
import com.rentify.app.core.network.api.ContractApiService
import com.rentify.app.core.network.api.InvoiceApiService
import com.rentify.app.core.network.api.IssueApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.landlord.data.repository.LandlordBuildingRepositoryImpl
import com.rentify.app.landlord.data.repository.LandlordContractRepositoryImpl
import com.rentify.app.landlord.data.repository.LandlordDashboardRepositoryImpl
import com.rentify.app.landlord.data.repository.LandlordInvoiceRepositoryImpl
import com.rentify.app.landlord.data.repository.LandlordIssueRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class DashboardViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            val tokenDataStore = TokenDataStore(context.applicationContext)
            val tokenProvider: () -> String? = { runBlocking { tokenDataStore.accessToken.first() } }
            val buildingRepository = LandlordBuildingRepositoryImpl(
                NetworkClient.createService<BuildingApiService>(tokenProvider = tokenProvider)
            )
            val contractRepository = LandlordContractRepositoryImpl(
                NetworkClient.createService<ContractApiService>(tokenProvider = tokenProvider)
            )
            val issueRepository = LandlordIssueRepositoryImpl(
                NetworkClient.createService<IssueApiService>(tokenProvider = tokenProvider)
            )
            val invoiceRepository = LandlordInvoiceRepositoryImpl(
                NetworkClient.createService<InvoiceApiService>(tokenProvider = tokenProvider)
            )
            val repository = LandlordDashboardRepositoryImpl(
                buildingRepository = buildingRepository,
                contractRepository = contractRepository,
                issueRepository = issueRepository,
                invoiceRepository = invoiceRepository
            )
            return DashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
