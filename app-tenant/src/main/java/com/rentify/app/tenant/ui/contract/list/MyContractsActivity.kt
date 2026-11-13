package com.rentify.app.tenant.ui.contract.list

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.toast
import com.rentify.app.core.ui.state.UiState
import com.rentify.app.tenant.R
import com.rentify.app.tenant.databinding.ActivityMyContractsBinding
import com.rentify.app.tenant.ui.contract.detail.ContractDetailActivity
import kotlinx.coroutines.launch

class MyContractsActivity : BaseActivity<ActivityMyContractsBinding>(ActivityMyContractsBinding::inflate) {

    private val viewModel: MyContractsViewModel by viewModels {
        MyContractsViewModelFactory(this)
    }

    private val adapter by lazy {
        MyContractsAdapter { contract ->
            ContractDetailActivity.start(this, contract.id)
        }
    }

    override fun initView() {
        binding.root.applySystemBarsPadding(top = true, bottom = true)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        setupRecyclerView()
        setupStatusChips()
        setupListeners()
    }

    private fun setupRecyclerView() {
        binding.rvContracts.layoutManager = LinearLayoutManager(this)
        binding.rvContracts.adapter = adapter
    }

    private fun setupStatusChips() {
        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            val status = when (checkedIds.firstOrNull()) {
                R.id.chipStatusActive -> "active"
                R.id.chipStatusSigned -> "signed"
                R.id.chipStatusExpired -> "expired"
                R.id.chipStatusTerminated -> "terminated"
                else -> null
            }
            viewModel.setStatusFilter(status)
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadContracts(isRefresh = true)
        }
    }

    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is UiState.Loading -> {
                            if (!binding.swipeRefresh.isRefreshing && adapter.itemCount == 0) {
                                binding.progressBar.isVisible = true
                            }
                        }
                        is UiState.Success -> {
                            binding.progressBar.isVisible = false
                            binding.swipeRefresh.isRefreshing = false
                            val list = state.data
                            adapter.submitList(list)
                            binding.layoutEmpty.isVisible = list.isEmpty()
                            binding.rvContracts.isVisible = list.isNotEmpty()
                        }
                        is UiState.Error -> {
                            binding.progressBar.isVisible = false
                            binding.swipeRefresh.isRefreshing = false
                            toast(state.message)
                        }
                        is UiState.Idle -> {
                            binding.progressBar.isVisible = false
                            binding.swipeRefresh.isRefreshing = false
                        }
                    }
                }
            }
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, MyContractsActivity::class.java)
            context.startActivity(intent)
        }
    }
}

