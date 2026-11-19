package com.rentify.app.tenant

import androidx.lifecycle.lifecycleScope
import com.rentify.app.core.data.local.datastore.AppDataStore
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.network.dto.auth.AuthLoginResponseDto
import com.rentify.app.core.network.dto.auth.AuthRegisterResponseDto
import com.rentify.app.core.ui.auth.ForgotPasswordFragment
import com.rentify.app.core.ui.auth.LoginFragment
import com.rentify.app.core.ui.auth.RegisterFragment
import com.rentify.app.core.network.socket.CommunitySocketClient
import com.rentify.app.core.ui.auth.ResetPasswordFragment
import com.rentify.app.core.ui.auth.VerifyEmailFragment
import com.rentify.app.core.ui.auth.VerifyOtpFragment
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.showSuccess
import com.rentify.app.tenant.databinding.ActivityMainBinding
import com.rentify.app.tenant.ui.main.TenantMainFragment
import com.rentify.app.tenant.ui.onboarding.OnboardingFragment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : BaseActivity<ActivityMainBinding>(
    ActivityMainBinding::inflate,
), OnboardingFragment.OnboardingListener,
    LoginFragment.LoginListener,
    RegisterFragment.RegisterListener,
    ForgotPasswordFragment.ForgotPasswordListener,
    VerifyEmailFragment.VerifyEmailListener,
    VerifyOtpFragment.VerifyOtpListener,
    ResetPasswordFragment.ResetPasswordListener,
    TenantMainFragment.TenantMainListener {

    private val tokenDataStore by lazy { TokenDataStore(applicationContext) }
    private val appDataStore by lazy { AppDataStore(applicationContext) }

    private var pendingTargetTabId: Int = R.id.nav_explore

    override fun initView() {
        val targetTabFromIntent = intent.getIntExtra(EXTRA_TARGET_TAB_ID, R.id.nav_explore)
        if (intent.getBooleanExtra(EXTRA_NAVIGATE_LOGIN, false)) {
            pendingTargetTabId = targetTabFromIntent
            showLoginScreen()
            return
        }

        if (supportFragmentManager.findFragmentById(R.id.fragmentContainer) == null) {
            checkAppStartDestination(targetTabFromIntent)
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val targetTabFromIntent = intent.getIntExtra(EXTRA_TARGET_TAB_ID, R.id.nav_explore)
        if (intent.getBooleanExtra(EXTRA_NAVIGATE_LOGIN, false)) {
            pendingTargetTabId = targetTabFromIntent
            showLoginScreen()
        } else if (targetTabFromIntent != R.id.nav_explore) {
            showHomeScreen(initialTabId = targetTabFromIntent)
        }
    }

    private fun checkAppStartDestination(initialTabId: Int = R.id.nav_explore) {
        lifecycleScope.launch {
            val isOnboardingCompleted = appDataStore.isOnboardingCompleted.first()

            if (isOnboardingCompleted) {
                showHomeScreen(initialTabId = initialTabId)
            } else {
                showOnboardingScreen()
            }
        }
    }

    override fun onNavigateToLogin() {
        lifecycleScope.launch {
            appDataStore.setOnboardingCompleted(true)
        }
        showHomeScreen()
    }

    override fun onLoginSuccess(response: AuthLoginResponseDto) {
        showSuccess("Đăng nhập thành công! Chào mừng ${response.profile.name}")
        val targetTab = pendingTargetTabId
        pendingTargetTabId = R.id.nav_explore
        showHomeScreen(initialTabId = targetTab)
    }

    override fun onTenantLogoutRequested() {
        lifecycleScope.launch {
            tokenDataStore.clearTokens()
            CommunitySocketClient.disconnect()
            showSuccess("Đã đăng xuất thành công")
            pendingTargetTabId = R.id.nav_explore
            showHomeScreen()
        }
    }

    override fun onTenantLoginRequested(targetTabId: Int) {
        pendingTargetTabId = targetTabId
        showLoginScreen()
    }

    override fun onNavigateToRegister() {
        showRegisterScreen()
    }

    override fun onNavigateToForgotPassword() {
        showForgotPasswordScreen()
    }

    override fun onRegisterSuccess(response: AuthRegisterResponseDto) {
        if (response.verificationRequired) {
            showVerifyEmailScreen(response.email, justSent = true)
        } else {
            showSuccess("Đăng ký thành công! Vui lòng đăng nhập")
            showLoginScreen()
        }
    }

    override fun onNavigateToVerifyEmail(email: String) {
        showVerifyEmailScreen(email, justSent = false)
    }

    override fun onVerifyEmailLoginRequested() {
        showLoginScreen()
    }

    override fun onRequestResetSuccess(email: String) {
        showSuccess("Đã gửi yêu cầu cho $email! Vui lòng kiểm tra mã xác thực")
        showVerifyOtpScreen(email)
    }

    override fun onVerifyOtpSuccess(token: String) {
        showResetPasswordScreen(token)
    }

    override fun onResetPasswordSuccess() {
        showSuccess("Đặt lại mật khẩu thành công! Vui lòng đăng nhập lại")
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

    private fun showHomeScreen(initialTabId: Int = R.id.nav_explore) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, TenantMainFragment.newInstance(initialTabId = initialTabId))
            .commit()
    }

    private fun showRegisterScreen() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, RegisterFragment.newInstance(role = "TENANT"))
            .commit()
    }

    private fun showVerifyEmailScreen(email: String, justSent: Boolean) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, VerifyEmailFragment.newInstance(email, justSent))
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

    companion object {
        const val EXTRA_NAVIGATE_LOGIN = "extra_navigate_login"
        const val EXTRA_TARGET_TAB_ID = "extra_target_tab_id"

        fun createLoginIntent(
            context: android.content.Context,
            targetTabId: Int = R.id.nav_explore
        ): android.content.Intent {
            return android.content.Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_NAVIGATE_LOGIN, true)
                putExtra(EXTRA_TARGET_TAB_ID, targetTabId)
                flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        }
    }
}
