package com.rentify.app.landlord.ui.billing

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
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
    private var buildingsList: List<BuildingMyListResponseDto> = emptyList()
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
        billingViewModel.loadInvoices(status = currentStatusFilter)
    }

    private fun issueAllPendingInvoices() {
        val pendingInvoices = allLoadedInvoices.filter { it.invoiceStatus == "pending" }
        if (pendingInvoices.isEmpty()) {
            Toast.makeText(this, "Không có hóa đơn nào đang chờ phát hành", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this, "Vui lòng chọn tòa nhà", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (period.isBlank()) {
                    Toast.makeText(this, "Vui lòng nhập kỳ lập hóa đơn", Toast.LENGTH_SHORT).show()
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
                                Toast.makeText(this@LandlordInvoicesActivity, state.message, Toast.LENGTH_SHORT).show()
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
                                Toast.makeText(this@LandlordInvoicesActivity, state.message, Toast.LENGTH_SHORT).show()
                                billingViewModel.resetActionState()
                                loadInvoices()
                            }
                            is BillingActionUiState.Error -> {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(this@LandlordInvoicesActivity, state.message, Toast.LENGTH_LONG).show()
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

