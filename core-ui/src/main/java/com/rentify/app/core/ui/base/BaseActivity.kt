package com.rentify.app.core.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.rentify.app.core.ui.component.LoadingDialog
import com.rentify.app.core.ui.extension.enableRentifyEdgeToEdge
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

abstract class BaseActivity<VB : ViewBinding>(
    private val inflate: (LayoutInflater) -> VB
) : AppCompatActivity() {

    private var _binding: VB? = null
    protected val binding: VB get() = _binding!!

    private var loadingDialog: LoadingDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableRentifyEdgeToEdge()
        super.onCreate(savedInstanceState)
        _binding = inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        observeData()
    }

    open fun initView() {}
    open fun observeData() {}

    protected fun <T> Flow<T>.collectWhenStarted(action: suspend (T) -> Unit) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                collect { action(it) }
            }
        }
    }

    protected fun showLoading() {
        if (isFinishing || isDestroyed || supportFragmentManager.isStateSaved) return
        if (loadingDialog?.dialog?.isShowing == true || loadingDialog?.isAdded == true) return
        val existingDialog = supportFragmentManager.findFragmentByTag(LoadingDialog.TAG) as? LoadingDialog
        if (existingDialog != null) {
            loadingDialog = existingDialog
            return
        }
        loadingDialog = LoadingDialog.newInstance()
        loadingDialog?.show(supportFragmentManager, LoadingDialog.TAG)
    }

    protected fun hideLoading() {
        if (isFinishing || isDestroyed) return
        val dialog = loadingDialog ?: (supportFragmentManager.findFragmentByTag(LoadingDialog.TAG) as? LoadingDialog)
        dialog?.dismissAllowingStateLoss()
        loadingDialog = null
    }

    override fun onDestroy() {
        hideLoading()
        _binding = null
        super.onDestroy()
    }
}
