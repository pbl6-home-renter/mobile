package com.rentify.app.tenant.ui.issue.list

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.network.api.IssueApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.tenant.data.repository.TenantIssueRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MyIssuesViewModelFactory(
    private val context: Context,
    private val roomId: String? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MyIssuesViewModel::class.java)) {
            val tokenDataStore = TokenDataStore(context.applicationContext)
            val tokenProvider: () -> String? = {
                runBlocking { tokenDataStore.accessToken.first() }
            }

            val issueApiService = NetworkClient.createService<IssueApiService>(tokenProvider = tokenProvider)
            val issueRepository = TenantIssueRepositoryImpl(issueApiService)

            return MyIssuesViewModel(
                initialRoomId = roomId,
                issueRepository = issueRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

