package com.rentify.app.core.ui.auth

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.data.repository.auth.AuthRepositoryImpl
import com.rentify.app.core.network.api.AuthApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.core.ui.R
import com.rentify.app.core.ui.base.BaseFragment
import com.rentify.app.core.ui.databinding.FragmentVerifyEmailBinding
import com.rentify.app.core.ui.extension.toast
import com.rentify.app.core.ui.state.UiState

class VerifyEmailFragment : BaseFragment<FragmentVerifyEmailBinding>(FragmentVerifyEmailBinding::inflate) {

    interface VerifyEmailListener {
        fun onVerifyEmailLoginRequested()
    }

    private var listener: VerifyEmailListener? = null

    private val email: String
        get() = arguments?.getString(ARG_EMAIL).orEmpty()

    private val viewModel: VerifyEmailViewModel by lazy {
        val authApiService = NetworkClient.createService<AuthApiService>()
        val tokenDataStore = TokenDataStore(requireContext().applicationContext)
        val authRepository = AuthRepositoryImpl(authApiService, tokenDataStore)
        val startCooldown = arguments?.getBoolean(ARG_JUST_SENT) ?: false
        ViewModelProvider(this, VerifyEmailViewModelFactory(email, startCooldown, authRepository))[VerifyEmailViewModel::class.java]
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? VerifyEmailListener
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun initView() {
        binding.tvSubtitle.text = getString(R.string.auth_verify_email_subtitle, email)

        binding.btnOpenMail.setOnClickListener { openMailApp() }
        binding.btnResend.setOnClickListener { viewModel.resend() }
        binding.llFooter.setOnClickListener { listener?.onVerifyEmailLoginRequested() }
    }

    override fun observeData() {
        viewModel.cooldownSeconds.collectWhenStarted { seconds ->
            binding.btnResend.isEnabled = seconds == 0
            binding.btnResend.text = if (seconds > 0) {
                getString(R.string.auth_verify_email_resend_countdown, seconds)
            } else {
                getString(R.string.auth_verify_email_resend)
            }
        }

        viewModel.resendState.collectWhenStarted { state ->
            when (state) {
                is UiState.Success -> {
                    binding.tvError.visibility = View.GONE
                    toast(getString(R.string.auth_verify_email_resent))
                    viewModel.resetResendState()
                }
                is UiState.Error -> {
                    binding.tvError.text = state.message
                    binding.tvError.visibility = View.VISIBLE
                }
                is UiState.Loading -> binding.tvError.visibility = View.GONE
                is UiState.Idle -> Unit
            }
        }
    }

    private fun openMailApp() {
        val intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_EMAIL)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            toast(getString(R.string.auth_verify_email_no_mail_app))
        }
    }

    companion object {
        private const val ARG_EMAIL = "arg_email"
        private const val ARG_JUST_SENT = "arg_just_sent"

        fun newInstance(email: String, justSent: Boolean): VerifyEmailFragment {
            return VerifyEmailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_EMAIL, email)
                    putBoolean(ARG_JUST_SENT, justSent)
                }
            }
        }
    }
}
