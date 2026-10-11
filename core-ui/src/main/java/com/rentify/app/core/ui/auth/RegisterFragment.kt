package com.rentify.app.core.ui.auth

import android.content.Context
import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.core.text.HtmlCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.ViewModelProvider
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.data.repository.auth.AuthRepositoryImpl
import com.rentify.app.core.network.api.AuthApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.core.network.dto.auth.AuthRegisterResponseDto
import com.rentify.app.core.ui.R
import com.rentify.app.core.ui.base.BaseFragment
import com.rentify.app.core.ui.databinding.FragmentRegisterBinding
import com.rentify.app.core.ui.state.UiState

class RegisterFragment : BaseFragment<FragmentRegisterBinding>(FragmentRegisterBinding::inflate) {

    interface RegisterListener {
        fun onRegisterSuccess(response: AuthRegisterResponseDto)
        fun onNavigateToLogin()
    }

    private var listener: RegisterListener? = null

    val defaultRole: String
        get() = arguments?.getString(ARG_ROLE) ?: "TENANT"

    private var customViewModel: RegisterViewModel? = null
    private val viewModel: RegisterViewModel by lazy {
        customViewModel ?: run {
            val authApiService = NetworkClient.createService<AuthApiService>()
            val tokenDataStore = TokenDataStore(requireContext().applicationContext)
            val authRepository = AuthRepositoryImpl(authApiService, tokenDataStore)
            val factory = RegisterViewModelFactory(authRepository)
            ViewModelProvider(this, factory)[RegisterViewModel::class.java]
        }
    }

    fun setViewModel(viewModel: RegisterViewModel) {
        this.customViewModel = viewModel
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? RegisterListener
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun initView() {
        if (defaultRole == "LANDLORD") {
            binding.tvTitle.text = getString(R.string.auth_register_landlord_title)
            binding.tvSubtitle.text = getString(R.string.auth_register_landlord_subtitle)
        } else {
            binding.tvTitle.text = getString(R.string.auth_register_tenant_title)
            binding.tvSubtitle.text = getString(R.string.auth_register_tenant_subtitle)
        }

        val termsHtml = "Tôi đồng ý với <font color='#2563EB'><b>Điều khoản dịch vụ</b></font> và <font color='#2563EB'><b>Chính sách bảo mật</b></font>"
        binding.tvTerms.text = HtmlCompat.fromHtml(termsHtml, HtmlCompat.FROM_HTML_MODE_LEGACY)

        binding.tvTerms.setOnClickListener {
            binding.cbTerms.isChecked = !binding.cbTerms.isChecked
        }

        binding.cbTerms.setOnCheckedChangeListener { _, _ ->
            hideInlineError()
            updateButtonState()
        }

        binding.etName.doOnTextChanged { _, _, _, _ ->
            binding.tilName.error = null
            hideInlineError()
            updateButtonState()
        }

        binding.etEmail.doOnTextChanged { _, _, _, _ ->
            binding.tilEmail.error = null
            hideInlineError()
            updateButtonState()
        }

        binding.etPhone.doOnTextChanged { _, _, _, _ ->
            binding.tilPhone.error = null
            hideInlineError()
            updateButtonState()
        }

        binding.etPassword.doOnTextChanged { _, _, _, _ ->
            binding.tilPassword.error = null
            hideInlineError()
            updateButtonState()
        }

        binding.etConfirmPassword.doOnTextChanged { _, _, _, _ ->
            binding.tilConfirmPassword.error = null
            hideInlineError()
            updateButtonState()
        }

        updateButtonState()

        binding.btnRegister.setOnClickListener {
            performRegister()
        }

        binding.tvLoginNow.setOnClickListener {
            listener?.onNavigateToLogin()
        }

        binding.llFooter.setOnClickListener {
            listener?.onNavigateToLogin()
        }
    }

    private fun isFormValid(): Boolean {
        val name = binding.etName.text?.toString().orEmpty().trim()
        val email = binding.etEmail.text?.toString().orEmpty().trim()
        val phone = binding.etPhone.text?.toString().orEmpty().trim()
        val password = binding.etPassword.text?.toString().orEmpty()
        val confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty()
        return name.isNotEmpty() &&
            email.isNotEmpty() &&
            phone.isNotEmpty() &&
            password.isNotEmpty() &&
            confirmPassword.isNotEmpty() &&
            binding.cbTerms.isChecked
    }

    private fun updateButtonState() {
        binding.btnRegister.isEnabled = isFormValid()
    }

    private fun performRegister() {
        val name = binding.etName.text?.toString().orEmpty().trim()
        val email = binding.etEmail.text?.toString().orEmpty().trim()
        val phone = binding.etPhone.text?.toString().orEmpty().trim()
        val password = binding.etPassword.text?.toString().orEmpty()
        val confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty()

        var hasError = false

        if (name.isBlank()) {
            binding.tilName.error = getString(R.string.auth_register_error_empty_name)
            hasError = true
        } else {
            binding.tilName.error = null
        }

        if (email.isBlank()) {
            binding.tilEmail.error = getString(R.string.auth_register_error_empty_email)
            hasError = true
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.auth_register_error_invalid_email)
            hasError = true
        } else {
            binding.tilEmail.error = null
        }

        if (phone.isBlank()) {
            binding.tilPhone.error = getString(R.string.auth_register_error_empty_phone)
            hasError = true
        } else if (phone.length < 10) {
            binding.tilPhone.error = getString(R.string.auth_register_error_invalid_phone)
            hasError = true
        } else {
            binding.tilPhone.error = null
        }

        if (password.isBlank()) {
            binding.tilPassword.error = getString(R.string.auth_register_error_empty_password)
            hasError = true
        } else if (password.length < 6) {
            binding.tilPassword.error = getString(R.string.auth_register_error_password_length)
            hasError = true
        } else {
            binding.tilPassword.error = null
        }

        if (confirmPassword != password) {
            binding.tilConfirmPassword.error = getString(R.string.auth_register_error_password_mismatch)
            hasError = true
        } else {
            binding.tilConfirmPassword.error = null
        }

        if (!binding.cbTerms.isChecked) {
            showInlineError(getString(R.string.auth_register_error_terms))
            hasError = true
        }

        if (hasError) return

        hideInlineError()
        viewModel.register(name, email, phone, password, defaultRole)
    }

    override fun observeData() {
        viewModel.registerState.collectWhenStarted { state ->
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
                    listener?.onRegisterSuccess(state.data)
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
        binding.btnRegister.isEnabled = !isLoading && isFormValid()
        binding.pbLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnRegister.text = if (isLoading) "" else getString(R.string.auth_register_button)
    }

    private fun showInlineError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun hideInlineError() {
        binding.tvError.visibility = View.GONE
    }

    companion object {
        private const val ARG_ROLE = "arg_role"

        fun newInstance(role: String = "TENANT"): RegisterFragment {
            return RegisterFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ROLE, role)
                }
            }
        }
    }
}
