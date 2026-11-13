package com.rentify.app.tenant.ui.matching.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.network.api.MatchingApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.tenant.data.repository.TenantMatchingRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MyRoommateProfileViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MyRoommateProfileViewModel::class.java)) {
            val tokenDataStore = TokenDataStore(context.applicationContext)
            val tokenProvider: () -> String? = {
                runBlocking { tokenDataStore.accessToken.first() }
            }

            val matchingApiService = NetworkClient.createService<MatchingApiService>(tokenProvider = tokenProvider)
            val matchingRepository = TenantMatchingRepositoryImpl(matchingApiService)

            return MyRoommateProfileViewModel(
                matchingRepository = matchingRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

