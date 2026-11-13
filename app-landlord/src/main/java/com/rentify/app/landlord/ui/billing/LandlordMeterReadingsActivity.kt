package com.rentify.app.landlord.ui.billing

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
import com.rentify.app.core.network.dto.meter.BulkMeterReadingCreateRequestDto
import com.rentify.app.core.network.dto.meter.BulkMeterReadingItemDto
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.landlord.databinding.ActivityLandlordMeterReadingsBinding
import com.rentify.app.landlord.ui.property.BuildingListUiState
import com.rentify.app.landlord.ui.property.BuildingViewModel
import com.rentify.app.landlord.ui.property.BuildingViewModelFactory
import com.rentify.app.landlord.ui.property.RoomListUiState
import com.rentify.app.landlord.ui.property.RoomViewModel
import com.rentify.app.landlord.ui.property.RoomViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LandlordMeterReadingsActivity : BaseActivity<ActivityLandlordMeterReadingsBinding>(
    ActivityLandlordMeterReadingsBinding::inflate
) {
    private val billingViewModel: BillingViewModel by viewModels {
        BillingViewModelFactory(this)
    }

    private val buildingViewModel: BuildingViewModel by viewModels {
        BuildingViewModelFactory(this)
    }

    private val roomViewModel: RoomViewModel by viewModels {
        RoomViewModelFactory(this)
    }

    private lateinit var adapter: BulkMeterAdapter
    private var buildingsList: List<BuildingMyListResponseDto> = emptyList()
    private var selectedBuildingId: String? = null
    private val currentPeriod: String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

    override fun initView() {
        binding.rootLayout.applySystemBarsPadding(top = true, bottom = true)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.tvPeriod.text = currentPeriod

        adapter = BulkMeterAdapter()
        binding.rvMeterEntries.layoutManager = LinearLayoutManager(this)
        binding.rvMeterEntries.adapter = adapter

        setupBuildingDropdown()

        binding.btnSubmitBulkMeters.setOnClickListener {
            submitBulkMeters()
        }

        buildingViewModel.loadBuildings(status = "approved")
        showEmptyState(true, "Vui lòng chọn tòa nhà để ghi chỉ số")
    }

    private fun setupBuildingDropdown() {
        binding.actvBuilding.setOnItemClickListener { _, _, position, _ ->
            val building = buildingsList.getOrNull(position) ?: return@setOnItemClickListener
            selectedBuildingId = building.id
            showEmptyState(false)
            binding.progressBar.visibility = View.VISIBLE
            roomViewModel.loadRooms(buildingId = building.id)
        }
    }

    private fun showEmptyState(show: Boolean, message: String = "") {
        binding.emptyStateView.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvMeterEntries.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun submitBulkMeters() {
        val buildingId = selectedBuildingId
        if (buildingId.isNullOrBlank()) {
            Toast.makeText(this, "Vui lòng chọn tòa nhà", Toast.LENGTH_SHORT).show()
            return
        }

        val entries = adapter.getEntries()
        val readings = mutableListOf<BulkMeterReadingItemDto>()

        entries.forEach { entry ->
            if (entry.electricityReading.isNotBlank()) {
                readings.add(
                    BulkMeterReadingItemDto(
                        roomId = entry.roomId,
                        type = "electricity",
                        currentReading = entry.electricityReading
                    )
                )
            }
            if (entry.waterReading.isNotBlank()) {
                readings.add(
                    BulkMeterReadingItemDto(
                        roomId = entry.roomId,
                        type = "water",
                        currentReading = entry.waterReading
                    )
                )
            }
        }

        if (readings.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập ít nhất một chỉ số điện hoặc nước", Toast.LENGTH_SHORT).show()
            return
        }

        billingViewModel.bulkRecordMeters(
            BulkMeterReadingCreateRequestDto(
                buildingId = buildingId,
                recordedMonth = currentPeriod,
                readings = readings
            )
        )
    }

    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    buildingViewModel.buildingListState.collect { state ->
                        when (state) {
                            is BuildingListUiState.Success -> {
                                buildingsList = state.buildings
                                val names = buildingsList.map { it.name }
                                val arrayAdapter = ArrayAdapter(
                                    this@LandlordMeterReadingsActivity,
                                    android.R.layout.simple_dropdown_item_1line,
                                    names
                                )
                                binding.actvBuilding.setAdapter(arrayAdapter)
                            }
                            else -> {}
                        }
                    }
                }

                launch {
                    roomViewModel.roomListState.collect { state ->
                        binding.progressBar.visibility = View.GONE
                        when (state) {
                            is RoomListUiState.Success -> {
                                if (state.rooms.isEmpty()) {
                                    showEmptyState(true, "Tòa nhà này chưa có phòng nào")
                                } else {
                                    showEmptyState(false)
                                    adapter.submitRooms(state.rooms)
                                }
                            }
                            is RoomListUiState.Error -> {
                                showEmptyState(true, state.message)
                                Toast.makeText(this@LandlordMeterReadingsActivity, state.message, Toast.LENGTH_SHORT).show()
                            }
                            RoomListUiState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                            }
                        }
                    }
                }

                launch {
                    billingViewModel.bulkMeterState.collect { state ->
                        when (state) {
                            is BulkMeterUiState.Loading -> {
                                binding.btnSubmitBulkMeters.isEnabled = false
                                binding.btnSubmitBulkMeters.text = "Đang lưu chỉ số..."
                            }
                            is BulkMeterUiState.Success -> {
                                binding.btnSubmitBulkMeters.isEnabled = true
                                binding.btnSubmitBulkMeters.text = "Lưu & Ghi chỉ số hàng loạt"
                                AlertDialog.Builder(this@LandlordMeterReadingsActivity)
                                    .setTitle("Thành công")
                                    .setMessage("Đã ghi nhận chỉ số cho ${state.response.totalRoomsRecorded} phòng.\nTổng điện: ${state.response.totalElectricityUsage} kWh\nTổng nước: ${state.response.totalWaterUsage} m³")
                                    .setPositiveButton("Đóng") { _, _ ->
                                        billingViewModel.resetBulkMeterState()
                                        finish()
                                    }
                                    .setCancelable(false)
                                    .show()
                            }
                            is BulkMeterUiState.Error -> {
                                binding.btnSubmitBulkMeters.isEnabled = true
                                binding.btnSubmitBulkMeters.text = "Lưu & Ghi chỉ số hàng loạt"
                                Toast.makeText(this@LandlordMeterReadingsActivity, state.message, Toast.LENGTH_LONG).show()
                            }
                            BulkMeterUiState.Idle -> {
                                binding.btnSubmitBulkMeters.isEnabled = true
                                binding.btnSubmitBulkMeters.text = "Lưu & Ghi chỉ số hàng loạt"
                            }
                        }
                    }
                }
            }
        }
    }
}

