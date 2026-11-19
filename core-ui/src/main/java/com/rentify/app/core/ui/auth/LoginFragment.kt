package com.rentify.app.core.ui.auth

import android.content.Context
import android.graphics.Color
import android.util.Patterns
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec
import com.google.android.material.progressindicator.IndeterminateDrawable
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
        fun onNavigateToVerifyEmail(email: String)
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

    private val loadingDrawable: IndeterminateDrawable<CircularProgressIndicatorSpec> by lazy {
        val ctx = requireContext()
        val density = ctx.resources.displayMetrics.density
        val spec = CircularProgressIndicatorSpec(ctx, null).apply {
            indicatorSize = (20f * density + 0.5f).toInt()
            trackThickness = (2.2f * density + 0.5f).toInt()
            indicatorColors = intArrayOf(ContextCompat.getColor(ctx, R.color.rentify_on_primary))
            trackColor = Color.TRANSPARENT
        }
        IndeterminateDrawable.createCircularDrawable(ctx, spec)
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
        binding.tilIdentifier.editText.doAfterTextChanged {
            clearFieldErrors()
            updateButtonState()
        }

        binding.tilPassword.editText.doAfterTextChanged {
            clearFieldErrors()
            updateButtonState()
        }

        binding.tilPassword.editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (binding.btnLogin.isEnabled && binding.btnLogin.isClickable) {
                    performLogin()
                }
                true
            } else {
                false
            }
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
        val identifier = binding.tilIdentifier.text.trim()
        val password = binding.tilPassword.text
        return identifier.isNotEmpty() && password.isNotEmpty()
    }

    private fun updateButtonState() {
        val enabled = isFormValid()
        binding.btnLogin.isEnabled = enabled
        binding.btnLogin.elevation = if (enabled) 6f * resources.displayMetrics.density else 0f
    }

    private fun performLogin() {
        val identifier = binding.tilIdentifier.text
        val password = binding.tilPassword.text

        var hasError = false
        if (identifier.isBlank()) {
            binding.tilIdentifier.setError(getString(R.string.auth_login_error_empty_identifier))
            hasError = true
        } else {
            binding.tilIdentifier.setError(null)
        }

        if (password.isBlank()) {
            binding.tilPassword.setError(getString(R.string.auth_login_error_empty_password))
            hasError = true
        } else {
            binding.tilPassword.setError(null)
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
        val density = resources.displayMetrics.density
        if (isLoading) {
            binding.btnLogin.isEnabled = true
            binding.btnLogin.elevation = 6f * density
            binding.btnLogin.isClickable = false
            binding.btnLogin.text = ""
            binding.btnLogin.iconPadding = 0
            binding.btnLogin.icon = loadingDrawable
            loadingDrawable.setVisible(true, true)
        } else {
            loadingDrawable.setVisible(false, false)
            binding.btnLogin.icon = null
            binding.btnLogin.iconPadding = (8f * density + 0.5f).toInt()
            binding.btnLogin.text = getString(R.string.auth_login_button)
            binding.btnLogin.isClickable = true
            updateButtonState()
        }
    }

    private fun showInlineError(message: String) {
        binding.tilIdentifier.setErrorHighlight()
        binding.tilPassword.setError(message)
        binding.tvError.visibility = View.GONE

        val identifier = binding.tilIdentifier.text.trim()
        if (viewModel.needsEmailVerification.value && Patterns.EMAIL_ADDRESS.matcher(identifier).matches()) {
            binding.tvError.text = getString(R.string.auth_login_resend_verification)
            binding.tvError.setTextColor(ContextCompat.getColor(requireContext(), R.color.rentify_primary))
            binding.tvError.setOnClickListener { listener?.onNavigateToVerifyEmail(identifier) }
            binding.tvError.visibility = View.VISIBLE
        }
    }

    private fun clearFieldErrors() {
        binding.tilIdentifier.setError(null)
        binding.tilPassword.setError(null)
        hideInlineError()
    }

    private fun hideInlineError() {
        binding.tvError.setOnClickListener(null)
        binding.tvError.setTextColor(ContextCompat.getColor(requireContext(), R.color.rentify_error))
        binding.tvError.visibility = View.GONE
    }

    companion object {
        fun newInstance(): LoginFragment {
            return LoginFragment()
        }
    }
}
