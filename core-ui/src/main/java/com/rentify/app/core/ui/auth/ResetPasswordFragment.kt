package com.rentify.app.core.ui.auth

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.ViewModelProvider
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.data.repository.auth.AuthRepositoryImpl
import com.rentify.app.core.network.api.AuthApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.core.ui.R
import com.rentify.app.core.ui.base.BaseFragment
import com.rentify.app.core.ui.databinding.FragmentResetPasswordBinding
import com.rentify.app.core.ui.state.UiState

class ResetPasswordFragment : BaseFragment<FragmentResetPasswordBinding>(FragmentResetPasswordBinding::inflate) {

    interface ResetPasswordListener {
        fun onResetPasswordSuccess()
        fun onNavigateToLogin()
    }

    private var listener: ResetPasswordListener? = null

    val targetToken: String
        get() = arguments?.getString(ARG_TOKEN).orEmpty()

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
        listener = context as? ResetPasswordListener
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun initView() {
        binding.etNewPassword.doOnTextChanged { _, _, _, _ ->
            binding.tilNewPassword.error = null
            hideInlineError()
            updateButtonState()
        }

        binding.etConfirmPassword.doOnTextChanged { _, _, _, _ ->
            binding.tilConfirmPassword.error = null
            hideInlineError()
            updateButtonState()
        }

        updateButtonState()

        binding.btnResetPassword.setOnClickListener {
            performResetPassword()
        }

        binding.tvLoginNow.setOnClickListener {
            listener?.onNavigateToLogin()
        }

        binding.llFooter.setOnClickListener {
            listener?.onNavigateToLogin()
        }
    }

    private fun isFormValid(): Boolean {
        val newPassword = binding.etNewPassword.text?.toString().orEmpty()
        val confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty()
        return newPassword.isNotEmpty() && confirmPassword.isNotEmpty()
    }

    private fun updateButtonState() {
        binding.btnResetPassword.isEnabled = isFormValid()
    }

    private fun performResetPassword() {
        val newPassword = binding.etNewPassword.text?.toString().orEmpty()
        val confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty()

        var hasError = false
        if (newPassword.isBlank()) {
            binding.tilNewPassword.error = getString(R.string.auth_reset_password_error_empty)
            hasError = true
        } else if (newPassword.length < 6) {
            binding.tilNewPassword.error = getString(R.string.auth_reset_password_error_length)
            hasError = true
        } else {
            binding.tilNewPassword.error = null
        }

        if (confirmPassword.isBlank()) {
            binding.tilConfirmPassword.error = getString(R.string.auth_reset_password_error_empty)
            hasError = true
        } else if (confirmPassword != newPassword) {
            binding.tilConfirmPassword.error = getString(R.string.auth_reset_password_error_mismatch)
            hasError = true
        } else {
            binding.tilConfirmPassword.error = null
        }

        if (hasError) return

        hideInlineError()
        viewModel.resetPassword(targetToken, newPassword)
    }

    override fun observeData() {
        viewModel.resetPasswordState.collectWhenStarted { state ->
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
                    viewModel.resetResetPasswordState()
                    listener?.onResetPasswordSuccess()
                }
                is UiState.Error -> {
                    showLoadingState(false)
                    showInlineError(state.message)
                }
            }
        }
    }

    private fun showLoadingState(isLoading: Boolean) {
        binding.btnResetPassword.isEnabled = !isLoading && isFormValid()
        binding.pbLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnResetPassword.text = if (isLoading) "" else getString(R.string.auth_reset_password_button)
    }

    private fun showInlineError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun hideInlineError() {
        binding.tvError.visibility = View.GONE
    }

    companion object {
        private const val ARG_TOKEN = "arg_token"

        fun newInstance(token: String): ResetPasswordFragment {
            return ResetPasswordFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TOKEN, token)
                }
            }
        }
    }
}
