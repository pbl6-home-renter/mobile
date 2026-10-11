package com.rentify.app.tenant

import androidx.lifecycle.lifecycleScope
import com.rentify.app.core.data.local.datastore.AppDataStore
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.network.dto.auth.AuthLoginResponseDto
import com.rentify.app.core.network.dto.auth.AuthRegisterResponseDto
import com.rentify.app.core.ui.auth.ForgotPasswordFragment
import com.rentify.app.core.ui.auth.LoginFragment
import com.rentify.app.core.ui.auth.RegisterFragment
import com.rentify.app.core.ui.auth.ResetPasswordFragment
import com.rentify.app.core.ui.auth.VerifyOtpFragment
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.toast
import com.rentify.app.core.ui.home.HomePlaceholderFragment
import com.rentify.app.tenant.databinding.ActivityMainBinding
import com.rentify.app.tenant.ui.onboarding.OnboardingFragment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : BaseActivity<ActivityMainBinding>(
    ActivityMainBinding::inflate,
), OnboardingFragment.OnboardingListener,
    LoginFragment.LoginListener,
    RegisterFragment.RegisterListener,
    ForgotPasswordFragment.ForgotPasswordListener,
    VerifyOtpFragment.VerifyOtpListener,
    ResetPasswordFragment.ResetPasswordListener,
    HomePlaceholderFragment.HomePlaceholderListener {

    private val tokenDataStore by lazy { TokenDataStore(applicationContext) }
    private val appDataStore by lazy { AppDataStore(applicationContext) }

    override fun initView() {
        if (supportFragmentManager.findFragmentById(R.id.fragmentContainer) == null) {
            checkAppStartDestination()
        }
    }

    private fun checkAppStartDestination() {
        lifecycleScope.launch {
            val token = tokenDataStore.accessToken.first()
            val isOnboardingCompleted = appDataStore.isOnboardingCompleted.first()

            when {
                !token.isNullOrBlank() -> {
                    showHomeScreen()
                }
                isOnboardingCompleted -> {
                    showLoginScreen()
                }
                else -> {
                    showOnboardingScreen()
                }
            }
        }
    }

    override fun onNavigateToLogin() {
        lifecycleScope.launch {
            appDataStore.setOnboardingCompleted(true)
        }
        showLoginScreen()
    }

    override fun onLoginSuccess(response: AuthLoginResponseDto) {
        toast("Đăng nhập thành công! Chào mừng ${response.profile.name}")
        showHomeScreen()
    }

    override fun onLogoutClicked() {
        lifecycleScope.launch {
            tokenDataStore.clearTokens()
            toast("Đã đăng xuất thành công")
            showLoginScreen()
        }
    }

    override fun onNavigateToRegister() {
        showRegisterScreen()
    }

    override fun onNavigateToForgotPassword() {
        showForgotPasswordScreen()
    }

    override fun onRegisterSuccess(response: AuthRegisterResponseDto) {
        toast("Đăng ký thành công! Vui lòng đăng nhập")
        showLoginScreen()
    }

    override fun onRequestResetSuccess(email: String) {
        toast("Đã gửi yêu cầu cho $email! Vui lòng kiểm tra mã xác thực")
        showVerifyOtpScreen(email)
    }

    override fun onVerifyOtpSuccess(token: String) {
        showResetPasswordScreen(token)
    }

    override fun onResetPasswordSuccess() {
        toast("Đặt lại mật khẩu thành công! Vui lòng đăng nhập lại")
        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        showLoginScreen()
    }

    private fun showOnboardingScreen() {
        val onboardingFragment = OnboardingFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, onboardingFragment)
            .commit()
    }

    private fun showLoginScreen() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, LoginFragment.newInstance())
            .commit()
    }

    private fun showHomeScreen() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, HomePlaceholderFragment.newInstance())
            .commit()
    }

    private fun showRegisterScreen() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, RegisterFragment.newInstance(role = "TENANT"))
            .commit()
    }

    private fun showForgotPasswordScreen() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, ForgotPasswordFragment.newInstance())
            .commit()
    }

    private fun showVerifyOtpScreen(email: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, VerifyOtpFragment.newInstance(email))
            .addToBackStack(null)
            .commit()
    }

    private fun showResetPasswordScreen(token: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, ResetPasswordFragment.newInstance(token))
            .addToBackStack(null)
            .commit()
    }
}