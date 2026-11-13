package com.rentify.app.tenant.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.data.repository.auth.AuthRepositoryImpl
import com.rentify.app.core.data.repository.user.UserRepositoryImpl
import com.rentify.app.core.network.api.AuthApiService
import com.rentify.app.core.network.api.UserApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.tenant.data.local.TenantDevPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ProfileViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            val tokenDataStore = TokenDataStore(context.applicationContext)
            val tenantDevPreferences = TenantDevPreferences(context.applicationContext)

            val tokenProvider: () -> String? = {
                runBlocking { tokenDataStore.accessToken.first() }
            }

            val userApiService = NetworkClient.createService<UserApiService>(tokenProvider = tokenProvider)
            val authApiService = NetworkClient.createService<AuthApiService>(tokenProvider = tokenProvider)

            val userRepository = UserRepositoryImpl(userApiService)
            val authRepository = AuthRepositoryImpl(authApiService, tokenDataStore)

            return ProfileViewModel(
                userRepository = userRepository,
                authRepository = authRepository,
                tokenDataStore = tokenDataStore,
                tenantDevPreferences = tenantDevPreferences
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
