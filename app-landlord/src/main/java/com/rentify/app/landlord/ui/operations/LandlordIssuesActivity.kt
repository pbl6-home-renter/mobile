package com.rentify.app.landlord.ui.operations

import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.landlord.R
import com.rentify.app.landlord.databinding.ActivityLandlordIssuesBinding
import kotlinx.coroutines.launch

class LandlordIssuesActivity : BaseActivity<ActivityLandlordIssuesBinding>(
    ActivityLandlordIssuesBinding::inflate
) {
    private val issueViewModel: IssueViewModel by viewModels {
        IssueViewModelFactory(this)
    }

    private lateinit var adapter: LandlordIssueAdapter
    private var currentStatusFilter: String? = null

    override fun initView() {
        binding.rootLayout.applySystemBarsPadding(top = true, bottom = true)

        binding.btnBack.setOnClickListener {
            finish()
        }

        adapter = LandlordIssueAdapter { issue ->
            val intent = Intent(this, LandlordIssueDetailActivity::class.java).apply {
                putExtra(LandlordIssueDetailActivity.EXTRA_ISSUE_ID, issue.id)
            }
            startActivity(intent)
        }

        binding.rvIssues.layoutManager = LinearLayoutManager(this)
        binding.rvIssues.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            loadIssues()
        }

        setupFilterChips()
        loadIssues()
    }

    private fun setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: R.id.chipAll
            currentStatusFilter = when (checkedId) {
                R.id.chipOpen -> "open"
                R.id.chipInProgress -> "in_progress"
                R.id.chipResolved -> "resolved"
                R.id.chipCancelled -> "cancelled"
                else -> null
            }
            loadIssues()
        }
    }

    private fun loadIssues() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyStateView.visibility = View.GONE
        issueViewModel.loadIssues(status = currentStatusFilter)
    }

    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    issueViewModel.issueListState.collect { state ->
                        binding.swipeRefresh.isRefreshing = false
                        binding.progressBar.visibility = View.GONE

                        when (state) {
                            is IssueListUiState.Success -> {
                                if (state.issues.isEmpty()) {
                                    binding.emptyStateView.visibility = View.VISIBLE
                                    binding.rvIssues.visibility = View.GONE
                                } else {
                                    binding.emptyStateView.visibility = View.GONE
                                    binding.rvIssues.visibility = View.VISIBLE
                                    adapter.submitList(state.issues)
                                }
                            }
                            is IssueListUiState.Error -> {
                                binding.emptyStateView.visibility = View.VISIBLE
                                binding.rvIssues.visibility = View.GONE
                                binding.tvEmptyMessage.text = state.message
                                Toast.makeText(this@LandlordIssuesActivity, state.message, Toast.LENGTH_SHORT).show()
                            }
                            IssueListUiState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadIssues()
    }
}

