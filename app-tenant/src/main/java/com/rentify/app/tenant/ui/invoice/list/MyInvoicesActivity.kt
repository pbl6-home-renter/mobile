package com.rentify.app.tenant.ui.invoice.list

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.toast
import com.rentify.app.core.ui.R as CoreR
import com.rentify.app.tenant.databinding.ActivityMyInvoicesBinding
import com.rentify.app.tenant.ui.invoice.detail.InvoiceDetailActivity
import kotlinx.coroutines.launch

class MyInvoicesActivity : BaseActivity<ActivityMyInvoicesBinding>(ActivityMyInvoicesBinding::inflate) {

    private val viewModel: MyInvoicesViewModel by viewModels {
        MyInvoicesViewModelFactory(this)
    }

    private lateinit var invoicesAdapter: MyInvoicesAdapter
    private lateinit var paymentsAdapter: PaymentHistoryAdapter

    override fun initView() {
        binding.root.applySystemBarsPadding(top = true, bottom = true)

        setupToolbar()
        setupTabs()
        setupChips()
        setupRecyclerViews()
        setupSwipeRefresh()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupTabs() {
        binding.tabLayoutInvoices.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                viewModel.selectTab(tab?.position ?: 0)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupChips() {
        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when (checkedIds.firstOrNull()) {
                binding.chipStatusUnpaid.id -> InvoiceFilterStatus.UNPAID
                binding.chipStatusPaid.id -> InvoiceFilterStatus.PAID
                binding.chipStatusOverdue.id -> InvoiceFilterStatus.OVERDUE
                else -> InvoiceFilterStatus.ALL
            }
            viewModel.setFilter(filter)
        }
    }

    private fun setupRecyclerViews() {
        invoicesAdapter = MyInvoicesAdapter(
            onInvoiceClick = { invoice ->
                InvoiceDetailActivity.start(this, invoice.id)
            }
        )
        binding.rvInvoices.apply {
            layoutManager = LinearLayoutManager(this@MyInvoicesActivity)
            adapter = invoicesAdapter
        }

        paymentsAdapter = PaymentHistoryAdapter()
        binding.rvPayments.apply {
            layoutManager = LinearLayoutManager(this@MyInvoicesActivity)
            adapter = paymentsAdapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(
            CoreR.color.rentify_primary_gradient_start,
            CoreR.color.rentify_primary_gradient_end
        )
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadData(isRefresh = true)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.pbLoading.isVisible = state.isLoading

                    state.errorMessage?.let { error ->
                        toast(error)
                    }

                    // Tab visibility
                    val isTabInvoices = state.selectedTab == 0
                    binding.layoutChipsContainer.isVisible = isTabInvoices

                    if (isTabInvoices) {
                        binding.rvPayments.isVisible = false
                        binding.layoutEmptyPayments.isVisible = false

                        val hasInvoices = state.displayedInvoices.isNotEmpty()
                        binding.rvInvoices.isVisible = hasInvoices
                        binding.layoutEmptyInvoices.isVisible = !hasInvoices
                        invoicesAdapter.submitList(state.displayedInvoices)
                    } else {
                        binding.rvInvoices.isVisible = false
                        binding.layoutEmptyInvoices.isVisible = false

                        val hasPayments = state.payments.isNotEmpty()
                        binding.rvPayments.isVisible = hasPayments
                        binding.layoutEmptyPayments.isVisible = !hasPayments
                        paymentsAdapter.submitList(state.payments)
                    }
                }
            }
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, MyInvoicesActivity::class.java)
            context.startActivity(intent)
        }
    }
}

