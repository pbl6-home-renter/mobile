package com.rentify.app.landlord.ui.contract

import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.rentify.app.core.network.dto.contract.ContractListResponseDto
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.landlord.R
import com.rentify.app.landlord.databinding.ActivityLandlordContractsBinding

class LandlordContractsActivity : BaseActivity<ActivityLandlordContractsBinding>(
    ActivityLandlordContractsBinding::inflate
) {
    private val viewModel: ContractViewModel by viewModels {
        ContractViewModelFactory(this)
    }

    private lateinit var adapter: LandlordContractAdapter
    private var currentFilterStatus: String? = null

    private val createContractLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.loadContracts(status = currentFilterStatus)
        }
    }

    private val contractDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.loadContracts(status = currentFilterStatus)
        }
    }

    override fun initView() {
        binding.rootLayout.applySystemBarsPadding(top = true, bottom = true)

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnCreateContract.setOnClickListener {
            val intent = Intent(this, CreateContractActivity::class.java)
            createContractLauncher.launch(intent)
        }

        setupRecyclerView()
        setupFilters()

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadContracts(status = currentFilterStatus)
        }

        viewModel.loadContracts()
    }

    private fun setupRecyclerView() {
        adapter = LandlordContractAdapter { contract ->
            val intent = LandlordContractDetailActivity.newIntent(this, contract.id)
            contractDetailLauncher.launch(intent)
        }

        binding.rvContracts.apply {
            layoutManager = LinearLayoutManager(this@LandlordContractsActivity)
            adapter = this@LandlordContractsActivity.adapter
        }
    }

    private fun setupFilters() {
        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilterStatus = when {
                checkedIds.contains(R.id.chipActive) -> "active"
                checkedIds.contains(R.id.chipPending) -> "signed"
                checkedIds.contains(R.id.chipEnded) -> "terminated"
                else -> null
            }
            viewModel.loadContracts(status = currentFilterStatus)
        }
    }

    override fun observeData() {
        viewModel.contractListState.collectWhenStarted { state ->
            binding.swipeRefresh.isRefreshing = false
            when (state) {
                is ContractListUiState.Loading -> {
                    if (!binding.swipeRefresh.isRefreshing) {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                }
                is ContractListUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    adapter.submitList(state.contracts)
                    if (state.contracts.isEmpty()) {
                        binding.layoutEmpty.visibility = View.VISIBLE
                    } else {
                        binding.layoutEmpty.visibility = View.GONE
                    }
                }
                is ContractListUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    if (adapter.itemCount == 0) {
                        binding.layoutEmpty.visibility = View.VISIBLE
                        binding.tvEmptyTitle.text = "Không thể tải danh sách"
                        binding.tvEmptySubtitle.text = state.message
                    }
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

