package com.rentify.app.landlord.ui.billing

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.rentify.app.core.network.dto.building.BuildingMyListResponseDto
import com.rentify.app.core.network.dto.invoice.InvoiceListResponseDto
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.showError
import com.rentify.app.core.ui.extension.showMessage
import com.rentify.app.core.ui.extension.showSuccess
import com.rentify.app.landlord.R
import com.rentify.app.landlord.databinding.ActivityLandlordInvoicesBinding
import com.rentify.app.landlord.databinding.DialogGenerateInvoicesBinding
import com.rentify.app.landlord.ui.property.BuildingListUiState
import com.rentify.app.landlord.ui.property.BuildingViewModel
import com.rentify.app.landlord.ui.property.BuildingViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LandlordInvoicesActivity : BaseActivity<ActivityLandlordInvoicesBinding>(
    ActivityLandlordInvoicesBinding::inflate
) {
    private val billingViewModel: BillingViewModel by viewModels {
        BillingViewModelFactory(this)
    }

    private val buildingViewModel: BuildingViewModel by viewModels {
        BuildingViewModelFactory(this)
    }

    private lateinit var adapter: LandlordInvoiceAdapter
    private var currentStatusFilter: String? = null
    private var currentBuildingFilter: String? = null
    private var currentPeriodFilter: String? = null
    private var buildingsList: List<BuildingMyListResponseDto> = emptyList()
    private var periodsList: List<String> = emptyList()
    private var allLoadedInvoices: List<InvoiceListResponseDto> = emptyList()

    override fun initView() {
        binding.rootLayout.applySystemBarsPadding(top = true, bottom = true)

        binding.btnBack.setOnClickListener {
            finish()
        }

        adapter = LandlordInvoiceAdapter { invoice ->
            val intent = Intent(this, LandlordInvoiceDetailActivity::class.java).apply {
                putExtra(LandlordInvoiceDetailActivity.EXTRA_INVOICE_ID, invoice.id)
            }
            startActivity(intent)
        }

        binding.rvInvoices.layoutManager = LinearLayoutManager(this)
        binding.rvInvoices.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            loadInvoices()
        }

        setupFilterChips()

        binding.btnGenerateInvoices.setOnClickListener {
            showGenerateInvoicesDialog()
        }

        binding.btnIssueAll.setOnClickListener {
            issueAllPendingInvoices()
        }

        buildingViewModel.loadBuildings(status = "approved")
        loadInvoices()
    }

    private fun setupFilterChips() {
        val cal = java.util.Calendar.getInstance()
        val fmt = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val pList = mutableListOf<String>()
        for (offset in 1 downTo -5) {
            val c = cal.clone() as java.util.Calendar
            c.add(java.util.Calendar.MONTH, offset)
            pList.add(fmt.format(c.time))
        }
        periodsList = pList
        val periodLabels = listOf("Tất cả kỳ") + pList
        binding.actvFilterInvoicePeriod.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, periodLabels)
        )
        binding.actvFilterInvoicePeriod.setText("Tất cả kỳ", false)
        binding.actvFilterInvoicePeriod.setOnItemClickListener { _, _, position, _ ->
            currentPeriodFilter = if (position == 0) null else periodsList.getOrNull(position - 1)
            loadInvoices()
        }

        binding.actvFilterInvoiceBuilding.setOnItemClickListener { _, _, position, _ ->
            currentBuildingFilter = if (position == 0) null else buildingsList.getOrNull(position - 1)?.id
            loadInvoices()
        }

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: R.id.chipAll
            currentStatusFilter = when (checkedId) {
                R.id.chipPending -> "pending"
                R.id.chipIssued -> "issued"
                R.id.chipPaid -> "paid"
                R.id.chipVoid -> "void"
                else -> null
            }
            loadInvoices()
        }
    }

    private fun loadInvoices() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyStateView.visibility = View.GONE
        billingViewModel.loadInvoices(
            status = currentStatusFilter,
            buildingId = currentBuildingFilter,
            period = currentPeriodFilter
        )
    }

    private fun issueAllPendingInvoices() {
        val pendingInvoices = allLoadedInvoices.filter { it.invoiceStatus == "pending" }
        if (pendingInvoices.isEmpty()) {
            showMessage("Không có hóa đơn nào đang chờ phát hành")
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Phát hành hóa đơn")
            .setMessage("Bạn có chắc chắn muốn phát hành ${pendingInvoices.size} hóa đơn đang chờ?")
            .setPositiveButton("Phát hành") { _, _ ->
                billingViewModel.issueInvoices(pendingInvoices.map { it.id })
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showGenerateInvoicesDialog() {
        val dialogBinding = DialogGenerateInvoicesBinding.inflate(LayoutInflater.from(this))
        val currentPeriod = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        dialogBinding.etDialogPeriod.setText(currentPeriod)

        var selectedBuildingId: String? = null
        val buildingNames = buildingsList.map { it.name }
        val arrayAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            buildingNames
        )
        dialogBinding.actvDialogBuilding.setAdapter(arrayAdapter)
        dialogBinding.actvDialogBuilding.setOnItemClickListener { _, _, position, _ ->
            selectedBuildingId = buildingsList.getOrNull(position)?.id
        }

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Lập hóa đơn") { _, _ ->
                val buildingId = selectedBuildingId
                val period = dialogBinding.etDialogPeriod.text?.toString()?.trim() ?: ""

                if (buildingId.isNullOrBlank()) {
                    showMessage("Vui lòng chọn tòa nhà")
                    return@setPositiveButton
                }
                if (period.isBlank()) {
                    showMessage("Vui lòng nhập kỳ lập hóa đơn")
                    return@setPositiveButton
                }

                billingViewModel.generateInvoices(buildingId = buildingId, period = period)
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    buildingViewModel.buildingListState.collect { state ->
                        if (state is BuildingListUiState.Success) {
                            buildingsList = state.buildings
                            val labels = listOf("Tất cả tòa nhà") + buildingsList.map { it.name }
                            binding.actvFilterInvoiceBuilding.setAdapter(
                                ArrayAdapter(
                                    this@LandlordInvoicesActivity,
                                    android.R.layout.simple_dropdown_item_1line,
                                    labels
                                )
                            )
                        }
                    }
                }

                launch {
                    billingViewModel.invoicesState.collect { state ->
                        binding.swipeRefresh.isRefreshing = false
                        binding.progressBar.visibility = View.GONE

                        when (state) {
                            is InvoicesUiState.Success -> {
                                allLoadedInvoices = state.invoices
                                val pendingCount = state.invoices.count { it.invoiceStatus == "pending" }
                                binding.tvPendingCount.text = "Hóa đơn chờ phát hành: $pendingCount"
                                binding.btnIssueAll.isEnabled = pendingCount > 0

                                if (state.invoices.isEmpty()) {
                                    binding.emptyStateView.visibility = View.VISIBLE
                                    binding.rvInvoices.visibility = View.GONE
                                } else {
                                    binding.emptyStateView.visibility = View.GONE
                                    binding.rvInvoices.visibility = View.VISIBLE
                                    adapter.submitList(state.invoices)
                                }
                            }
                            is InvoicesUiState.Error -> {
                                binding.emptyStateView.visibility = View.VISIBLE
                                binding.rvInvoices.visibility = View.GONE
                                binding.tvEmptyMessage.text = state.message
                                showError(state.message)
                            }
                            InvoicesUiState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                            }
                        }
                    }
                }

                launch {
                    billingViewModel.actionState.collect { state ->
                        when (state) {
                            is BillingActionUiState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                            }
                            is BillingActionUiState.Success -> {
                                binding.progressBar.visibility = View.GONE
                                showSuccess(state.message)
                                billingViewModel.resetActionState()
                                loadInvoices()
                            }
                            is BillingActionUiState.Error -> {
                                binding.progressBar.visibility = View.GONE
                                showError(state.message)
                                billingViewModel.resetActionState()
                            }
                            BillingActionUiState.Idle -> {}
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadInvoices()
    }
}

