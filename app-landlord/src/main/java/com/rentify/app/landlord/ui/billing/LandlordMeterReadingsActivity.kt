package com.rentify.app.landlord.ui.billing

import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.rentify.app.core.network.dto.building.BuildingMyListResponseDto
import com.rentify.app.core.network.dto.meter.BulkMeterReadingCreateRequestDto
import com.rentify.app.core.network.dto.meter.BulkMeterReadingItemDto
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.showError
import com.rentify.app.core.ui.extension.showMessage
import com.rentify.app.core.ui.extension.showSuccess
import com.rentify.app.landlord.R
import com.rentify.app.landlord.databinding.ActivityLandlordMeterReadingsBinding
import com.rentify.app.landlord.ui.property.BuildingListUiState
import com.rentify.app.landlord.ui.property.BuildingViewModel
import com.rentify.app.landlord.ui.property.BuildingViewModelFactory
import com.rentify.app.landlord.ui.property.RoomListUiState
import com.rentify.app.landlord.ui.property.RoomViewModel
import com.rentify.app.landlord.ui.property.RoomViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
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

    private val calendar: Calendar = Calendar.getInstance()
    private var currentPeriod: String = formatCalendarPeriod(calendar)

    // Photo state for currently open Detailed Meter BottomSheet
    private var pendingPhotoBytes: ByteArray? = null
    private var pendingPhotoFileName: String? = null
    private var pendingPhotoContentType: String? = null
    private var onPhotoPickedCallback: ((String) -> Unit)? = null

    private val pickMeterPhotoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val contentType = contentResolver.getType(uri) ?: "image/jpeg"
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null && bytes.isNotEmpty()) {
                    pendingPhotoBytes = bytes
                    pendingPhotoContentType = contentType
                    val ext = when {
                        contentType.contains("png") -> "png"
                        contentType.contains("webp") -> "webp"
                        else -> "jpg"
                    }
                    val fileName = "meter_${System.currentTimeMillis()}.$ext"
                    pendingPhotoFileName = fileName
                    onPhotoPickedCallback?.invoke(fileName)
                }
            } catch (e: Exception) {
                showError("Lỗi đọc ảnh: ${e.message}")
            }
        }
    }

    override fun initView() {
        binding.rootLayout.applySystemBarsPadding(top = true, bottom = true)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        updatePeriodLabel()

        binding.btnPrevMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            currentPeriod = formatCalendarPeriod(calendar)
            updatePeriodLabel()
            reloadBuildingMeters()
        }

        binding.btnNextMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            currentPeriod = formatCalendarPeriod(calendar)
            updatePeriodLabel()
            reloadBuildingMeters()
        }

        adapter = BulkMeterAdapter { entry ->
            showDetailedMeterBottomSheet(entry)
        }
        binding.rvMeterEntries.layoutManager = LinearLayoutManager(this)
        binding.rvMeterEntries.adapter = adapter

        setupBuildingDropdown()

        binding.btnSubmitBulkMeters.setOnClickListener {
            submitBulkMeters()
        }

        buildingViewModel.loadBuildings(status = "approved")
        showEmptyState(true, "Vui lòng chọn tòa nhà để ghi chỉ số")
    }

    private fun formatCalendarPeriod(cal: Calendar): String {
        return SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
    }

    private fun updatePeriodLabel() {
        binding.tvPeriod.text = "Kỳ: $currentPeriod"
    }

    private fun setupBuildingDropdown() {
        binding.actvBuilding.setOnItemClickListener { _, _, position, _ ->
            val building = buildingsList.getOrNull(position) ?: return@setOnItemClickListener
            selectedBuildingId = building.id
            reloadBuildingMeters()
        }
    }

    private fun reloadBuildingMeters() {
        val bId = selectedBuildingId ?: return
        showEmptyState(false)
        binding.progressBar.visibility = View.VISIBLE
        roomViewModel.loadRooms(buildingId = bId)
        billingViewModel.loadMeterReadings(period = currentPeriod)
    }

    private fun showEmptyState(show: Boolean, message: String = "") {
        binding.emptyStateView.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvMeterEntries.visibility = if (show) View.GONE else View.VISIBLE
        if (message.isNotEmpty()) {
            binding.tvEmptyMeterMsg.text = message
        }
    }

    private fun showDetailedMeterBottomSheet(entry: RoomMeterEntry) {
        val dialog = BottomSheetDialog(this)
        val sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_detailed_meter, null)
        dialog.setContentView(sheetView)

        val tvTitle = sheetView.findViewById<TextView>(R.id.tvDetailedMeterTitle)
        val chipGroupType = sheetView.findViewById<ChipGroup>(R.id.chipGroupMeterType)
        val tvContextPeriod = sheetView.findViewById<TextView>(R.id.tvMeterContextPeriod)
        val tvContextPrev = sheetView.findViewById<TextView>(R.id.tvMeterContextPrev)
        val tilReading = sheetView.findViewById<TextInputLayout>(R.id.tilDetailedCurrentReading)
        val etReading = sheetView.findViewById<TextInputEditText>(R.id.etDetailedCurrentReading)
        val cbBaseline = sheetView.findViewById<MaterialCheckBox>(R.id.cbIsBaseline)
        val tvPhotoStatus = sheetView.findViewById<TextView>(R.id.tvMeterPhotoStatus)
        val btnSelectPhoto = sheetView.findViewById<MaterialButton>(R.id.btnSelectMeterPhoto)
        val btnCancel = sheetView.findViewById<MaterialButton>(R.id.btnCancelDetailedMeter)
        val btnSave = sheetView.findViewById<MaterialButton>(R.id.btnSaveDetailedMeter)

        pendingPhotoBytes = null
        pendingPhotoFileName = null
        pendingPhotoContentType = null

        tvTitle.text = "Nhập chỉ số - ${entry.roomName}"
        tvContextPeriod.text = "Kỳ ghi nhận: $currentPeriod"

        var selectedType = "electricity"

        fun syncTypeUi() {
            if (selectedType == "electricity") {
                tvContextPrev.text = "Chỉ số cũ: ${entry.prevElectricityReading} (kWh)"
                tilReading.hint = "Chỉ số mới (kWh) *"
                etReading.setText(entry.electricityReading)
            } else {
                tvContextPrev.text = "Chỉ số cũ: ${entry.prevWaterReading} (m³)"
                tilReading.hint = "Chỉ số mới (m³) *"
                etReading.setText(entry.waterReading)
            }
        }

        syncTypeUi()

        chipGroupType.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedType = if (checkedIds.contains(R.id.chipTypeWater)) "water" else "electricity"
            syncTypeUi()
        }

        onPhotoPickedCallback = { fileName ->
            tvPhotoStatus.text = "✓ Đã chọn ảnh công tơ: $fileName"
            tvPhotoStatus.setTextColor(Color.parseColor("#16A34A"))
        }

        btnSelectPhoto.setOnClickListener {
            pickMeterPhotoLauncher.launch("image/*")
        }

        btnCancel.setOnClickListener {
            onPhotoPickedCallback = null
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val currentVal = etReading.text?.toString()?.trim().orEmpty()
            val num = currentVal.toDoubleOrNull()
            if (num == null || num < 0.0) {
                tilReading.error = "Vui lòng nhập chỉ số hợp lệ"
                return@setOnClickListener
            }
            tilReading.error = null

            val prevVal = if (selectedType == "electricity") entry.prevElectricityReading else entry.prevWaterReading
            if (selectedType == "electricity") {
                entry.electricityReading = currentVal
            } else {
                entry.waterReading = currentVal
            }
            adapter.notifyDataSetChanged()

            billingViewModel.saveDetailedMeterReading(
                roomId = entry.roomId,
                type = selectedType,
                period = currentPeriod,
                currentReading = currentVal,
                previousReading = prevVal,
                isBaseline = cbBaseline.isChecked,
                photoBytes = pendingPhotoBytes,
                photoFileName = pendingPhotoFileName,
                photoContentType = pendingPhotoContentType
            )
            onPhotoPickedCallback = null
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            onPhotoPickedCallback = null
        }

        dialog.show()
    }

    private fun submitBulkMeters() {
        val buildingId = selectedBuildingId
        if (buildingId.isNullOrBlank()) {
            showMessage("Vui lòng chọn tòa nhà")
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
            showMessage("Vui lòng nhập ít nhất một chỉ số điện hoặc nước")
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
                                if (buildingsList.isNotEmpty() && selectedBuildingId == null) {
                                    binding.actvBuilding.setText(buildingsList[0].name, false)
                                    selectedBuildingId = buildingsList[0].id
                                    reloadBuildingMeters()
                                }
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
                                    val currentReadings = (billingViewModel.meterReadingsState.value as? MeterReadingsUiState.Success)?.readings
                                    if (currentReadings != null) {
                                        adapter.applyExistingReadings(currentReadings)
                                    }
                                }
                            }
                            is RoomListUiState.Error -> {
                                showEmptyState(true, state.message)
                                showError(state.message)
                            }
                            RoomListUiState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                            }
                        }
                    }
                }

                launch {
                    billingViewModel.meterReadingsState.collect { state ->
                        if (state is MeterReadingsUiState.Success) {
                            adapter.applyExistingReadings(state.readings)
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

                launch {
                    billingViewModel.bulkMeterState.collect { state ->
                        when (state) {
                            is BulkMeterUiState.Loading -> {
                                binding.btnSubmitBulkMeters.isEnabled = false
                                binding.btnSubmitBulkMeters.text = "Đang lưu chỉ số..."
                            }
                            is BulkMeterUiState.Success -> {
                                binding.btnSubmitBulkMeters.isEnabled = true
                                binding.btnSubmitBulkMeters.text = "Lưu tất cả"
                                AlertDialog.Builder(this@LandlordMeterReadingsActivity)
                                    .setTitle("Thành công")
                                    .setMessage("Đã ghi nhận chỉ số cho ${state.response.totalRoomsRecorded} phòng (Kỳ ${state.response.recordedMonth}).\nTổng điện: ${state.response.totalElectricityUsage} kWh\nTổng nước: ${state.response.totalWaterUsage} m³")
                                    .setPositiveButton("Đóng") { _, _ ->
                                        billingViewModel.resetBulkMeterState()
                                        reloadBuildingMeters()
                                    }
                                    .setCancelable(false)
                                    .show()
                            }
                            is BulkMeterUiState.Error -> {
                                binding.btnSubmitBulkMeters.isEnabled = true
                                binding.btnSubmitBulkMeters.text = "Lưu tất cả"
                                showError(state.message)
                            }
                            BulkMeterUiState.Idle -> {
                                binding.btnSubmitBulkMeters.isEnabled = true
                                binding.btnSubmitBulkMeters.text = "Lưu tất cả"
                            }
                        }
                    }
                }
            }
        }
    }
}
