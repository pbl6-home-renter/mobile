package com.rentify.app.core.ui.auth

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.ViewModelProvider
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.data.repository.auth.AuthRepositoryImpl
import com.rentify.app.core.network.api.AuthApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.core.ui.R
import com.rentify.app.core.ui.base.BaseFragment
import com.rentify.app.core.ui.databinding.FragmentVerifyOtpBinding
import com.rentify.app.core.ui.extension.toast

class VerifyOtpFragment : BaseFragment<FragmentVerifyOtpBinding>(FragmentVerifyOtpBinding::inflate) {

    interface VerifyOtpListener {
        fun onVerifyOtpSuccess(token: String)
    }

    private var listener: VerifyOtpListener? = null

    private var countDownTimer: CountDownTimer? = null
    private var isResendEnabled: Boolean = false

    val targetEmail: String
        get() = arguments?.getString(ARG_EMAIL).orEmpty()

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
        listener = context as? VerifyOtpListener
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun initView() {
        binding.tvSubtitle.text = getString(R.string.auth_verify_otp_subtitle, targetEmail)

        binding.etOtp.doOnTextChanged { _, _, _, _ ->
            binding.tilOtp.error = null
            hideInlineError()
        }

        binding.btnVerify.setOnClickListener {
            performVerify()
        }

        binding.tvResendAction.setOnClickListener {
            if (isResendEnabled) {
                performResendCode()
            }
        }

        startCountDownTimer()
    }

    private fun performVerify() {
        val token = binding.etOtp.text?.toString().orEmpty().trim()

        if (token.isBlank()) {
            binding.tilOtp.error = getString(R.string.auth_verify_otp_error_empty)
            return
        }

        binding.tilOtp.error = null
        hideInlineError()
        listener?.onVerifyOtpSuccess(token)
    }

    private fun performResendCode() {
        if (targetEmail.isNotBlank()) {
            viewModel.requestPasswordReset(targetEmail)
            toast(getString(R.string.auth_verify_otp_resend_success))
            startCountDownTimer()
        }
    }

    private fun startCountDownTimer() {
        countDownTimer?.cancel()
        isResendEnabled = false
        binding.tvResendAction.isEnabled = false

        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt()
                if (isAdded) {
                    binding.tvResendAction.text = getString(R.string.auth_verify_otp_resend_timer, seconds)
                    context?.let { ctx ->
                        binding.tvResendAction.setTextColor(ContextCompat.getColor(ctx, R.color.rentify_neutral))
                    }
                }
            }

            override fun onFinish() {
                if (isAdded) {
                    isResendEnabled = true
                    binding.tvResendAction.isEnabled = true
                    binding.tvResendAction.text = getString(R.string.auth_verify_otp_resend_action)
                    context?.let { ctx ->
                        binding.tvResendAction.setTextColor(ContextCompat.getColor(ctx, R.color.rentify_primary_gradient_start))
                    }
                }
            }
        }.start()
    }

    private fun hideInlineError() {
        binding.tvError.visibility = View.GONE
    }

    override fun onDestroyView() {
        countDownTimer?.cancel()
        countDownTimer = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_EMAIL = "arg_email"

        fun newInstance(email: String): VerifyOtpFragment {
            return VerifyOtpFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_EMAIL, email)
                }
            }
        }
    }
}
