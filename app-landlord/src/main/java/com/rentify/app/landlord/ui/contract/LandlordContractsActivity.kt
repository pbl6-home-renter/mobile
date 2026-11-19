package com.rentify.app.landlord.ui.contract

import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.rentify.app.core.network.dto.building.BuildingMyListResponseDto
import com.rentify.app.core.network.dto.contract.ContractListResponseDto
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.showError
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
    private var currentFilterBuildingId: String? = null
    private var currentSearchQuery: String = ""
    private var buildingsList: List<BuildingMyListResponseDto> = emptyList()
    private var allContracts: List<ContractListResponseDto> = emptyList()

    private val createContractLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            reloadContracts()
        }
    }

    private val contractDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            reloadContracts()
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
            reloadContracts()
        }

        viewModel.loadApprovedBuildings()
        reloadContracts()
    }

    private fun setupRecyclerView() {
        adapter = LandlordContractAdapter { contract ->
            val intent = LandlordContractDetailActivity.newIntent(this, contract.id, contract.roomName)
            contractDetailLauncher.launch(intent)
        }

        binding.rvContracts.apply {
            layoutManager = LinearLayoutManager(this@LandlordContractsActivity)
            adapter = this@LandlordContractsActivity.adapter
        }
    }

    private fun setupFilters() {
        binding.etSearchContract.doAfterTextChanged { editable ->
            currentSearchQuery = editable?.toString()?.trim().orEmpty()
            applyLocalSearchFilter()
        }

        binding.actvFilterContractBuilding.setOnItemClickListener { _, _, position, _ ->
            currentFilterBuildingId = if (position == 0) {
                null
            } else {
                buildingsList.getOrNull(position - 1)?.id
            }
            reloadContracts()
        }

        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilterStatus = when {
                checkedIds.contains(R.id.chipDraft) -> "draft"
                checkedIds.contains(R.id.chipActive) -> "active"
                checkedIds.contains(R.id.chipEnded) -> "terminated"
                checkedIds.contains(R.id.chipExpired) -> "expired"
                else -> null
            }
            reloadContracts()
        }
    }

    private fun reloadContracts() {
        viewModel.loadContracts(
            status = currentFilterStatus,
            buildingId = currentFilterBuildingId
        )
    }

    private fun applyLocalSearchFilter() {
        val q = currentSearchQuery.lowercase()
        val filtered = if (q.isEmpty()) {
            allContracts
        } else {
            allContracts.filter { item ->
                val code = item.contractCode.ifEmpty { "HĐ-${item.id.take(6)}" }.lowercase()
                val room = item.roomName.lowercase()
                val tenant = item.tenantName.orEmpty().lowercase()
                val phone = item.tenantPhone.orEmpty().lowercase()
                code.contains(q) || room.contains(q) || tenant.contains(q) || phone.contains(q)
            }
        }
        adapter.submitList(filtered)
        binding.layoutEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun observeData() {
        viewModel.approvedBuildings.collectWhenStarted { buildings ->
            buildingsList = buildings
            val labels = mutableListOf("Tất cả tòa nhà")
            labels.addAll(buildings.map { it.name })
            val dropdownAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                labels
            )
            binding.actvFilterContractBuilding.setAdapter(dropdownAdapter)
            val selectedName = currentFilterBuildingId?.let { id ->
                buildings.firstOrNull { it.id == id }?.name
            } ?: "Tất cả tòa nhà"
            binding.actvFilterContractBuilding.setText(selectedName, false)
        }

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
                    allContracts = state.contracts
                    applyLocalSearchFilter()
                }
                is ContractListUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    if (adapter.itemCount == 0) {
                        binding.layoutEmpty.visibility = View.VISIBLE
                        binding.tvEmptyTitle.text = "Không thể tải danh sách"
                        binding.tvEmptySubtitle.text = state.message
                    }
                    showError(state.message)
                }
            }
        }
    }
}
