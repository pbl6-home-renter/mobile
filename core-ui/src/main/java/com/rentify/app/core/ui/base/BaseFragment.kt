package com.rentify.app.core.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.rentify.app.core.ui.component.LoadingDialog
import com.rentify.app.core.ui.extension.toast
import com.rentify.app.core.ui.state.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

abstract class BaseFragment<VB : ViewBinding>(
    private val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> VB
) : Fragment() {

    private var _binding: VB? = null
    protected val binding: VB get() = _binding!!

    private var loadingDialog: LoadingDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = bindingInflater(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        observeData()
    }

    protected open fun initView() {}
    protected open fun observeData() {}

    protected fun <T> Flow<T>.collectWhenStarted(action: suspend (T) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                collect { action(it) }
            }
        }
    }

    fun showLoading() {
        if (!isAdded || isDetached || childFragmentManager.isStateSaved) return
        if (loadingDialog?.dialog?.isShowing == true || loadingDialog?.isAdded == true) return
        val existingDialog = childFragmentManager.findFragmentByTag(LoadingDialog.TAG) as? LoadingDialog
        if (existingDialog != null) {
            loadingDialog = existingDialog
            return
        }
        loadingDialog = LoadingDialog.newInstance()
        loadingDialog?.show(childFragmentManager, LoadingDialog.TAG)
    }

    fun hideLoading() {
        if (!isAdded || isDetached) return
        val dialog = loadingDialog ?: (childFragmentManager.findFragmentByTag(LoadingDialog.TAG) as? LoadingDialog)
        dialog?.dismissAllowingStateLoss()
        loadingDialog = null
    }

    fun <T> renderState(
        state: UiState<T>,
        onError: ((String) -> Unit)? = { message -> toast(message) },
        onSuccess: (T) -> Unit
    ) {
        when (state) {
            is UiState.Idle -> hideLoading()
            is UiState.Loading -> showLoading()
            is UiState.Success -> {
                hideLoading()
                onSuccess(state.data)
            }
            is UiState.Error -> {
                hideLoading()
                onError?.invoke(state.message)
            }
        }
    }

    override fun onDestroyView() {
        hideLoading()
        _binding = null
        super.onDestroyView()
    }
}
