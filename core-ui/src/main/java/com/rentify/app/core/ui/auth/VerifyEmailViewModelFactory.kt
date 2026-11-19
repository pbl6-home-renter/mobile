package com.rentify.app.core.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rentify.app.core.data.repository.auth.AuthRepository

class VerifyEmailViewModelFactory(
    private val email: String,
    private val startCooldown: Boolean,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VerifyEmailViewModel::class.java)) {
            return VerifyEmailViewModel(email, startCooldown, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
