package com.rentify.app.core.ui.auth

import android.content.Context
import android.view.View
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.ViewModelProvider
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.data.repository.auth.AuthRepositoryImpl
import com.rentify.app.core.network.api.AuthApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.core.network.dto.auth.AuthLoginResponseDto
import com.rentify.app.core.ui.R
import com.rentify.app.core.ui.base.BaseFragment
import com.rentify.app.core.ui.databinding.FragmentLoginBinding
import com.rentify.app.core.ui.state.UiState

class LoginFragment : BaseFragment<FragmentLoginBinding>(FragmentLoginBinding::inflate) {

    interface LoginListener {
        fun onLoginSuccess(response: AuthLoginResponseDto)
        fun onNavigateToRegister()
        fun onNavigateToForgotPassword()
    }

    private var listener: LoginListener? = null

    private var customViewModel: LoginViewModel? = null
    private val viewModel: LoginViewModel by lazy {
        customViewModel ?: run {
            val authApiService = NetworkClient.createService<AuthApiService>()
            val tokenDataStore = TokenDataStore(requireContext().applicationContext)
            val authRepository = AuthRepositoryImpl(authApiService, tokenDataStore)
            val factory = LoginViewModelFactory(authRepository)
            ViewModelProvider(this, factory)[LoginViewModel::class.java]
        }
    }

    fun setViewModel(viewModel: LoginViewModel) {
        this.customViewModel = viewModel
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? LoginListener
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun initView() {
        binding.etIdentifier.doOnTextChanged { _, _, _, _ ->
            binding.tilIdentifier.error = null
            hideInlineError()
            updateButtonState()
        }

        binding.etPassword.doOnTextChanged { _, _, _, _ ->
            binding.tilPassword.error = null
            hideInlineError()
            updateButtonState()
        }

        updateButtonState()

        binding.btnLogin.setOnClickListener {
            performLogin()
        }

        binding.tvForgotPassword.setOnClickListener {
            listener?.onNavigateToForgotPassword()
        }

        binding.tvRegisterNow.setOnClickListener {
            listener?.onNavigateToRegister()
        }

        binding.llFooter.setOnClickListener {
            listener?.onNavigateToRegister()
        }
    }

    private fun isFormValid(): Boolean {
        val identifier = binding.etIdentifier.text?.toString().orEmpty().trim()
        val password = binding.etPassword.text?.toString().orEmpty()
        return identifier.isNotEmpty() && password.isNotEmpty()
    }

    private fun updateButtonState() {
        binding.btnLogin.isEnabled = isFormValid()
    }

    private fun performLogin() {
        val identifier = binding.etIdentifier.text?.toString().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()

        var hasError = false
        if (identifier.isBlank()) {
            binding.tilIdentifier.error = getString(R.string.auth_login_error_empty_identifier)
            hasError = true
        } else {
            binding.tilIdentifier.error = null
        }

        if (password.isBlank()) {
            binding.tilPassword.error = getString(R.string.auth_login_error_empty_password)
            hasError = true
        } else {
            binding.tilPassword.error = null
        }

        if (hasError) return

        hideInlineError()
        viewModel.login(identifier, password)
    }

    override fun observeData() {
        viewModel.loginState.collectWhenStarted { state ->
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
                    listener?.onLoginSuccess(state.data)
                    viewModel.resetState()
                }
                is UiState.Error -> {
                    showLoadingState(false)
                    showInlineError(state.message)
                }
            }
        }
    }

    private fun showLoadingState(isLoading: Boolean) {
        binding.btnLogin.isEnabled = !isLoading && isFormValid()
        binding.pbLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.text = if (isLoading) "" else getString(R.string.auth_login_button)
    }

    private fun showInlineError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun hideInlineError() {
        binding.tvError.visibility = View.GONE
    }

    companion object {
        fun newInstance(): LoginFragment {
            return LoginFragment()
        }
    }
}
