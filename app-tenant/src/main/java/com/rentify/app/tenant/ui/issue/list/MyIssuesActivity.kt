package com.rentify.app.tenant.ui.issue.list

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.core.ui.extension.toast
import com.rentify.app.core.ui.state.UiState
import com.rentify.app.tenant.R
import com.rentify.app.tenant.databinding.ActivityMyIssuesBinding
import com.rentify.app.tenant.ui.issue.create.CreateIssueActivity
import com.rentify.app.tenant.ui.issue.detail.IssueDetailActivity
import kotlinx.coroutines.launch

class MyIssuesActivity : BaseActivity<ActivityMyIssuesBinding>(ActivityMyIssuesBinding::inflate) {

    private val roomId: String? by lazy {
        intent.getStringExtra(EXTRA_ROOM_ID)
    }

    private val roomName: String? by lazy {
        intent.getStringExtra(EXTRA_ROOM_NAME)
    }

    private val viewModel: MyIssuesViewModel by viewModels {
        MyIssuesViewModelFactory(this, roomId)
    }

    private val adapter by lazy {
        MyIssuesAdapter { issue ->
            IssueDetailActivity.start(this, issue.id)
        }
    }

    private val createIssueLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.loadIssues(isRefresh = true)
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
        binding.rvIssues.layoutManager = LinearLayoutManager(this)
        binding.rvIssues.adapter = adapter
    }

    private fun setupStatusChips() {
        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            val status = when (checkedIds.firstOrNull()) {
                R.id.chipStatusOpen -> "open"
                R.id.chipStatusInProgress -> "in_progress"
                R.id.chipStatusResolved -> "resolved"
                R.id.chipStatusCancelled -> "cancelled"
                else -> null
            }
            viewModel.setStatusFilter(status)
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadIssues(isRefresh = true)
        }

        binding.fabReportIssue.setOnSingleClickListener {
            val intent = Intent(this, CreateIssueActivity::class.java).apply {
                putExtra("EXTRA_ROOM_ID", roomId.orEmpty())
                putExtra("EXTRA_ROOM_NAME", roomName.orEmpty())
            }
            createIssueLauncher.launch(intent)
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
                            binding.rvIssues.isVisible = list.isNotEmpty()
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
        private const val EXTRA_ROOM_ID = "EXTRA_ROOM_ID"
        private const val EXTRA_ROOM_NAME = "EXTRA_ROOM_NAME"

        fun start(context: Context, roomId: String? = null, roomName: String? = null) {
            val intent = Intent(context, MyIssuesActivity::class.java).apply {
                if (!roomId.isNullOrBlank()) putExtra(EXTRA_ROOM_ID, roomId)
                if (!roomName.isNullOrBlank()) putExtra(EXTRA_ROOM_NAME, roomName)
            }
            context.startActivity(intent)
        }
    }
}
