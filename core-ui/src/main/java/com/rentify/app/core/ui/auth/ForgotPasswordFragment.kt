package com.rentify.app.core.ui.auth

import android.content.Context
import android.util.Patterns
import android.view.View
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.ViewModelProvider
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.data.repository.auth.AuthRepositoryImpl
import com.rentify.app.core.network.api.AuthApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.core.ui.R
import com.rentify.app.core.ui.base.BaseFragment
import com.rentify.app.core.ui.databinding.FragmentForgotPasswordBinding
import com.rentify.app.core.ui.state.UiState

class ForgotPasswordFragment : BaseFragment<FragmentForgotPasswordBinding>(FragmentForgotPasswordBinding::inflate) {

    interface ForgotPasswordListener {
        fun onRequestResetSuccess(email: String)
        fun onNavigateToLogin()
    }

    private var listener: ForgotPasswordListener? = null

    private var customViewModel: ForgotPasswordViewModel? = null
    private val viewModel: ForgotPasswordViewModel by lazy {
        customViewModel ?: run {
            val authApiService = NetworkClient.createService<AuthApiService>()
            val tokenDataStore = TokenDataStore(requireContext().applicationContext)
            val authRepository = AuthRepositoryImpl(authApiService, tokenDataStore)
            val factory = ForgotPasswordViewModelFactory(authRepository)
            ViewModelProvider(this, factory)[ForgotPasswordViewModel::class.java]
        }
    }

    fun setViewModel(viewModel: ForgotPasswordViewModel) {
        this.customViewModel = viewModel
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? ForgotPasswordListener
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun initView() {
        binding.etIdentifier.doOnTextChanged { _, _, _, _ ->
            binding.tilIdentifier.error = null
            hideInlineError()
        }

        binding.btnSendRequest.setOnClickListener {
            performSendRequest()
        }

        binding.tvLoginNow.setOnClickListener {
            listener?.onNavigateToLogin()
        }

        binding.llFooter.setOnClickListener {
            listener?.onNavigateToLogin()
        }
    }

    private fun performSendRequest() {
        val email = binding.etIdentifier.text?.toString().orEmpty().trim()

        if (email.isBlank()) {
            binding.tilIdentifier.error = getString(R.string.auth_forgot_password_error_empty)
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilIdentifier.error = getString(R.string.auth_forgot_password_error_invalid_email)
            return
        }

        binding.tilIdentifier.error = null
        hideInlineError()
        viewModel.requestPasswordReset(email)
    }

    override fun observeData() {
        viewModel.requestResetState.collectWhenStarted { state ->
            when (state) {
                is UiState.Idle -> {
                    showLoadingState(false)
                    hideInlineError()
                }
                is UiState.Loading -> {
                    showLoadingState(true)
                    hideInlineError()
                }
                is UiState.Success -> {
                    showLoadingState(false)
                    val email = binding.etIdentifier.text?.toString().orEmpty().trim()
                    listener?.onRequestResetSuccess(email)
                    viewModel.resetRequestState()
                }
                is UiState.Error -> {
                    showLoadingState(false)
                    showInlineError(state.message)
                }
            }
        }
    }

    private fun showLoadingState(isLoading: Boolean) {
        binding.btnSendRequest.isEnabled = !isLoading
        binding.pbLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSendRequest.text = if (isLoading) "" else getString(R.string.auth_forgot_password_button)
    }

    private fun showInlineError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun hideInlineError() {
        binding.tvError.visibility = View.GONE
    }

    companion object {
        fun newInstance(): ForgotPasswordFragment {
            return ForgotPasswordFragment()
        }
    }
}
