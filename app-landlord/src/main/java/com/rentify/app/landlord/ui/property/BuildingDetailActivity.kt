package com.rentify.app.landlord.ui.property

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.rentify.app.core.network.client.NetworkConfig
import com.rentify.app.core.network.dto.billing.BillingSettingResponseDto
import com.rentify.app.core.network.dto.billing.RateItemDto
import com.rentify.app.core.network.dto.building.BuildingMyDetailResponseDto
import com.rentify.app.core.network.dto.room.RoomMyListResponseDto
import com.rentify.app.core.ui.R as CoreUiR
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.landlord.R
import com.rentify.app.landlord.databinding.ActivityBuildingDetailBinding
import kotlin.math.roundToInt

class BuildingDetailActivity : BaseActivity<ActivityBuildingDetailBinding>(
    ActivityBuildingDetailBinding::inflate
) {
    private val viewModel: BuildingViewModel by viewModels {
        BuildingViewModelFactory(this)
    }

    private val roomViewModel: RoomViewModel by viewModels {
        RoomViewModelFactory(this)
    }

    private var buildingId: String = ""
    private var currentDetail: BuildingMyDetailResponseDto? = null
    private var hasMutated: Boolean = false

    private lateinit var roomAdapter: LandlordRoomAdapter
    private var allBuildingRooms: List<RoomMyListResponseDto> = emptyList()
    private var selectedRoomStatus: String? = null
    private var selectedFloorFilter: Int? = null

    private val createRoomLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            hasMutated = true
            setResult(RESULT_OK)
            viewModel.loadBuildingDetail(buildingId)
            roomViewModel.loadRooms(buildingId = buildingId, status = selectedRoomStatus)
        }
    }

    private val roomDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            hasMutated = true
            setResult(RESULT_OK)
            viewModel.loadBuildingDetail(buildingId)
            roomViewModel.loadRooms(buildingId = buildingId, status = selectedRoomStatus)
        }
    }

    private val pickOwnershipDocLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && buildingId.isNotEmpty()) {
            uploadSelectedOwnershipDoc(uri)
        }
    }

    override fun initView() {
        binding.rootLayout.applySystemBarsPadding(top = true, bottom = true)

        buildingId = intent.getStringExtra(EXTRA_BUILDING_ID) ?: ""
        if (buildingId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy mã tòa nhà", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.toolbar.setNavigationOnClickListener {
            if (hasMutated) setResult(RESULT_OK)
            finish()
        }

        setupRoomsSection()

        binding. swipeRefresh.setOnRefreshListener {
            viewModel.loadBuildingDetail(buildingId)
            viewModel.loadBuildingPoliciesAndSettings(buildingId)
            roomViewModel.loadRooms(buildingId = buildingId, status = selectedRoomStatus)
        }

        binding.btnConfigVietQr.setOnClickListener {
            showVietQrConfigBottomSheet()
        }

        binding.btnConfigBuildingRates.setOnClickListener {
            val activeRates = viewModel.ratePolicyState.value?.rates.orEmpty()
            val initialRates = if (activeRates.isNotEmpty()) {
                activeRates
            } else {
                currentDetail?.ratePolicies?.map {
                    RateItemDto(
                        type = it.category.ifBlank { "other" },
                        name = it.name.ifBlank { "Dịch vụ" },
                        rateKind = it.rateKind.ifBlank { "flat" },
                        unitPrice = (it.unitPrice ?: 0).toString(),
                        billingUnit = it.unit.ifBlank { "kwh" }
                    )
                }.orEmpty()
            }
            PolicySettingBottomSheetHelper.showRatePoliciesSheet(
                context = this,
                initialRates = initialRates,
                canInheritBuilding = false
            ) { rates ->
                viewModel.saveBuildingRatePolicies(buildingId, rates)
            }
        }

        binding.btnConfigBuildingBilling.setOnClickListener {
            PolicySettingBottomSheetHelper.showBillingSettingsSheet(
                context = this,
                currentSetting = viewModel.billingSettingState.value,
                canInheritBuilding = false
            ) { dueDay, remindDay, botEnabled ->
                viewModel.saveBuildingBillingSetting(buildingId, dueDay, remindDay, botEnabled)
            }
        }

        binding.btnEditBuilding.setOnClickListener {
            showEditBuildingBottomSheet()
        }

        binding.btnDeleteBuilding.setOnClickListener {
            showDeleteConfirmDialog()
        }

        binding.btnUploadOwnershipDoc.setOnClickListener {
            pickOwnershipDocLauncher.launch("image/*")
        }

        binding.btnAddRoomInBuilding.setOnClickListener {
            val detail = currentDetail
            if (detail != null && !detail.status.equals("approved", ignoreCase = true)) {
                Toast.makeText(
                    this,
                    "Tòa nhà cần được phê duyệt trước khi thêm phòng mới.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            val intent = CreateRoomActivity.newIntent(this, buildingId)
            createRoomLauncher.launch(intent)
        }

        viewModel.loadBuildingDetail(buildingId)
        viewModel.loadBuildingPoliciesAndSettings(buildingId)
        roomViewModel.loadRooms(buildingId = buildingId, status = null)
    }

    private fun setupRoomsSection() {
        roomAdapter = LandlordRoomAdapter(
            onRoomClick = { room ->
                val intent = LandlordRoomDetailActivity.newIntent(this, room.id)
                roomDetailLauncher.launch(intent)
            },
            onMarkReadyClick = { room ->
                hasMutated = true
                roomViewModel.markRoomReady(room.id)
            }
        )

        binding.rvBuildingRooms.apply {
            layoutManager = LinearLayoutManager(this@BuildingDetailActivity)
            adapter = roomAdapter
        }

        binding.chipGroupBuildingRoomStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedRoomStatus = when {
                checkedIds.contains(R.id.chipBldRoomAvailable) -> "available"
                checkedIds.contains(R.id.chipBldRoomOccupied) -> "occupied"
                checkedIds.contains(R.id.chipBldRoomMaintenance) -> "maintenance"
                else -> null
            }
            roomViewModel.loadRooms(buildingId = buildingId, status = selectedRoomStatus)
        }
    }

    override fun observeData() {
        viewModel.buildingDetailState.collectWhenStarted { state ->
            binding.swipeRefresh.isRefreshing = false
            when (state) {
                is BuildingDetailUiState.Loading -> {
                    binding.detailProgressBar.visibility = View.VISIBLE
                }
                is BuildingDetailUiState.Success -> {
                    binding.detailProgressBar.visibility = View.GONE
                    currentDetail = state.building
                    bindBuildingDetail(state.building)
                }
                is BuildingDetailUiState.Error -> {
                    binding.detailProgressBar.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                is BuildingDetailUiState.Idle -> {
                    binding.detailProgressBar.visibility = View.GONE
                }
            }
        }

        viewModel.ratePolicyState.collectWhenStarted { policy ->
            val rates = policy?.rates.orEmpty()
            bindBuildingRatesSection(rates)
        }

        viewModel.billingSettingState.collectWhenStarted { setting ->
            bindBuildingBillingSection(setting)
        }

        viewModel.actionState.collectWhenStarted { state ->
            when (state) {
                is BuildingActionUiState.Loading -> {
                    binding.detailProgressBar.visibility = View.VISIBLE
                }
                is BuildingActionUiState.Success -> {
                    binding.detailProgressBar.visibility = View.GONE
                    hasMutated = true
                    setResult(RESULT_OK)
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    viewModel.resetActionState()
                    if (state.message.contains("Xóa")) {
                        finish()
                    }
                }
                is BuildingActionUiState.Error -> {
                    binding.detailProgressBar.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    viewModel.resetActionState()
                }
                is BuildingActionUiState.Idle -> {
                    binding.detailProgressBar.visibility = View.GONE
                }
            }
        }

        roomViewModel.roomListState.collectWhenStarted { state ->
            when (state) {
                is RoomListUiState.Loading -> Unit
                is RoomListUiState.Success -> {
                    allBuildingRooms = state.rooms
                    rebuildFloorChips(state.rooms)
                    applyFloorFilter()
                }
                is RoomListUiState.Error -> {
                    binding.tvEmptyBuildingRooms.visibility = View.VISIBLE
                    binding.tvEmptyBuildingRooms.text = state.message
                }
            }
        }

        roomViewModel.actionState.collectWhenStarted { state ->
            when (state) {
                is RoomActionUiState.Loading -> {
                    binding.detailProgressBar.visibility = View.VISIBLE
                }
                is RoomActionUiState.Success -> {
                    binding.detailProgressBar.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    roomViewModel.resetActionState()
                    viewModel.loadBuildingDetail(buildingId)
                    roomViewModel.loadRooms(buildingId = buildingId, status = selectedRoomStatus)
                }
                is RoomActionUiState.Error -> {
                    binding.detailProgressBar.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    roomViewModel.resetActionState()
                }
                is RoomActionUiState.Idle -> Unit
            }
        }
    }

    private fun rebuildFloorChips(rooms: List<RoomMyListResponseDto>) {
        val floors = rooms.mapNotNull { it.floorNumber }.filter { it > 0 }.distinct().sorted()
        if (floors.isEmpty()) {
            binding.scrollBuildingRoomFloors.visibility = View.GONE
            selectedFloorFilter = null
            return
        }
        if (selectedFloorFilter != null && !floors.contains(selectedFloorFilter)) {
            selectedFloorFilter = null
        }

        binding.scrollBuildingRoomFloors.visibility = View.VISIBLE
        binding.chipGroupBuildingRoomFloor.setOnCheckedStateChangeListener(null)
        binding.chipGroupBuildingRoomFloor.removeAllViews()

        val allChip = Chip(this).apply {
            id = View.generateViewId()
            text = "Tất cả tầng"
            isCheckable = true
            isChecked = (selectedFloorFilter == null)
            textSize = 12f
        }
        binding.chipGroupBuildingRoomFloor.addView(allChip)

        val floorIdMap = mutableMapOf<Int, Int>()
        for (floor in floors) {
            val chip = Chip(this).apply {
                id = View.generateViewId()
                text = "Tầng $floor"
                isCheckable = true
                isChecked = (selectedFloorFilter == floor)
                textSize = 12f
            }
            floorIdMap[chip.id] = floor
            binding.chipGroupBuildingRoomFloor.addView(chip)
        }

        binding.chipGroupBuildingRoomFloor.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            selectedFloorFilter = if (checkedId != null) floorIdMap[checkedId] else null
            applyFloorFilter()
        }
    }

    private fun applyFloorFilter() {
        val filtered = if (selectedFloorFilter == null) {
            allBuildingRooms
        } else {
            allBuildingRooms.filter { it.floorNumber == selectedFloorFilter }
        }
        roomAdapter.submitList(filtered)
        binding.tvEmptyBuildingRooms.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.tvBuildingRoomsSubtitle.text = "${filtered.size} phòng được hiển thị"
    }

    private fun bindBuildingDetail(building: BuildingMyDetailResponseDto) {
        binding.toolbar.title = building.name.ifBlank { "Chi tiết Tòa nhà" }
        binding.tvDetailName.text = building.name

        val fullAddr = buildString {
            append(building.address)
            if (building.ward.isNotBlank()) append(", ${building.ward}")
            if (building.province.isNotBlank()) append(", ${building.province}")
        }
        binding.tvDetailFullAddress.text = fullAddr

        val floorsCount = building.totalFloors ?: 1
        val totalRooms = if (building.totalRooms > 0) building.totalRooms else allBuildingRooms.size
        val createdDate = if (building.createdAt.length >= 10) building.createdAt.substring(0, 10) else building.createdAt
        binding.tvDetailSubMeta.text = listOfNotNull(
            "$floorsCount tầng lầu",
            "$totalRooms phòng độc lập",
            createdDate.takeIf { it.isNotBlank() }?.let { "Khởi tạo: $it" }
        ).joinToString(" • ")

        binding.tvDetailOccupiedRooms.text = "${building.occupiedRooms} / $totalRooms phòng đang thuê"
        binding.tvDetailTotalFloors.text = "Số tầng: $floorsCount"
        binding.tvDetailOccupancyRate.text = building.occupancyRate

        val rateNumeric = building.occupancyRate.replace("%", "").trim().toDoubleOrNull()?.roundToInt() ?: 0
        binding.pbOccupancy.progress = rateNumeric.coerceIn(0, 100)

        val (badgeText, textColor, bgColor) = when (building.status.lowercase()) {
            "approved" -> Triple("Đã duyệt", Color.parseColor("#16A34A"), Color.parseColor("#DCFCE7"))
            "rejected" -> Triple("Bị từ chối", Color.parseColor("#DC2626"), Color.parseColor("#FEE2E2"))
            else -> Triple("Chờ duyệt", Color.parseColor("#D97706"), Color.parseColor("#FEF3C7"))
        }

        binding.tvDetailStatusBadge.text = badgeText
        binding.tvDetailStatusBadge.setTextColor(textColor)
        binding.tvDetailStatusBadge.background = GradientDrawable().apply {
            setColor(bgColor)
            cornerRadius = 16f
        }

        if (building.status.equals("rejected", ignoreCase = true) && !building.reviewNote.isNullOrBlank()) {
            binding.layoutDetailReviewNote.visibility = View.VISIBLE
            binding.tvDetailReviewNote.text = building.reviewNote
            binding.layoutDetailReviewNote.background = GradientDrawable().apply {
                setColor(Color.parseColor("#FEE2E2"))
                cornerRadius = 12f
            }
        } else {
            binding.layoutDetailReviewNote.visibility = View.GONE
        }

        // Building Amenities
        val am = building.amenities
        val amenityNames = mutableListOf<String>()
        if (am?.elevator == true) amenityNames.add("Thang máy")
        if (am?.parking == true) amenityNames.add("Chỗ để xe")
        if (am?.securityCamera == true) amenityNames.add("Camera an ninh")
        if (am?.fingerprintLock == true) amenityNames.add("Khóa vân tay")

        binding.tvBuildingAmenitiesList.text = if (amenityNames.isEmpty()) {
            "Chưa thiết lập tiện ích"
        } else {
            amenityNames.joinToString(" • ")
        }

        // Ownership Documents
        bindOwnershipCertificates(building.ownershipCertificateUrls)

        // Fallback RatePolicies if ratePolicyState not yet populated
        if (viewModel.ratePolicyState.value == null) {
            val mappedRates = building.ratePolicies.map {
                RateItemDto(
                    type = it.category.ifBlank { "other" },
                    name = it.name.ifBlank { "Dịch vụ" },
                    rateKind = it.rateKind.ifBlank { "flat" },
                    unitPrice = (it.unitPrice ?: 0).toString(),
                    billingUnit = it.unit.ifBlank { "kwh" }
                )
            }
            bindBuildingRatesSection(mappedRates)
        }

        // VietQR
        if (building.vietQrConfigured) {
            binding.tvVietQrStatus.text = "✓ Đã cấu hình tài khoản nhận tiền VietQR"
            binding.tvVietQrStatus.setTextColor(Color.parseColor("#16A34A"))
            binding.btnConfigVietQr.text = "Cập nhật tài khoản VietQR"
        } else {
            binding.tvVietQrStatus.text = "Chưa cấu hình tài khoản nhận tiền VietQR"
            binding.tvVietQrStatus.setTextColor(Color.parseColor("#475569"))
            binding.btnConfigVietQr.text = "Cấu hình tài khoản nhận tiền"
        }
    }

    private fun bindBuildingRatesSection(rates: List<RateItemDto>) {
        val hasElectricity = rates.any { it.type.equals("electricity", ignoreCase = true) }
        val hasWater = rates.any { it.type.equals("water", ignoreCase = true) }

        if (rates.isEmpty() || !hasElectricity || !hasWater) {
            binding.layoutRatePolicyWarning.visibility = View.VISIBLE
        } else {
            binding.layoutRatePolicyWarning.visibility = View.GONE
        }

        binding.layoutBuildingRatesList.removeAllViews()
        if (rates.isEmpty()) {
            binding.layoutBuildingRatesList.visibility = View.GONE
            return
        }
        binding.layoutBuildingRatesList.visibility = View.VISIBLE
        val density = resources.displayMetrics.density
        val padV = (4f * density).roundToInt()
        val fontRegular = ResourcesCompat.getFont(this, CoreUiR.font.plus_jakarta_sans)

        for (rate in rates) {
            val tv = TextView(this).apply {
                text = "• ${PolicySettingBottomSheetHelper.formatRateSummary(rate)}"
                setTextColor(ContextCompat.getColor(this@BuildingDetailActivity, CoreUiR.color.rentify_text_primary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                typeface = fontRegular
                setPadding(0, padV, 0, padV)
            }
            binding.layoutBuildingRatesList.addView(tv)
        }
    }

    private fun bindBuildingBillingSection(setting: BillingSettingResponseDto?) {
        val dueDay = setting?.paymentDueDay ?: 5
        val remindDay = setting?.remindDay
        val botOn = setting?.botEnabled ?: true

        binding.tvBuildingDueDay.text = "Ngày $dueDay hàng tháng"
        binding.tvBuildingRemindDay.text = if (remindDay != null) "Ngày $remindDay" else "Không nhắc"
        binding.tvBuildingBotStatus.text = if (botOn) "Đang bật" else "Đang tắt"
        binding.tvBuildingBotStatus.setTextColor(
            ContextCompat.getColor(
                this,
                if (botOn) CoreUiR.color.rentify_success else CoreUiR.color.rentify_text_muted
            )
        )
    }

    private fun bindOwnershipCertificates(urls: List<String>) {
        binding.tvOwnershipDocsCount.text = "${urls.size} tệp đính kèm"
        binding.layoutOwnershipContainer.removeAllViews()
        if (urls.isEmpty()) {
            binding.scrollOwnershipPreviews.visibility = View.GONE
            return
        }
        binding.scrollOwnershipPreviews.visibility = View.VISIBLE
        val density = resources.displayMetrics.density
        val sizePx = (88f * density).roundToInt()
        val marginPx = (8f * density).roundToInt()
        val radiusPx = 12f * density
        val gatewayOrigin = NetworkConfig.DEFAULT_BASE_URL.substringBefore("/api/")

        for (rawUrl in urls) {
            val resolvedUrl = if (rawUrl.startsWith("http")) rawUrl else "$gatewayOrigin/${rawUrl.trimStart('/')}"
            val card = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                    marginEnd = marginPx
                }
                radius = radiusPx
                cardElevation = 0f
                strokeWidth = (1f * density).roundToInt()
                strokeColor = ContextCompat.getColor(this@BuildingDetailActivity, CoreUiR.color.rentify_divider)
            }
            val iv = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                load(resolvedUrl) {
                    crossfade(true)
                    placeholder(CoreUiR.drawable.ic_file_text)
                    error(CoreUiR.drawable.ic_file_text)
                }
            }
            card.addView(iv)
            binding.layoutOwnershipContainer.addView(card)
        }
    }

    private fun showVietQrConfigBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_vietqr_config, null)
        dialog.setContentView(sheetView)

        val etBankCode = sheetView.findViewById<TextInputEditText>(R.id.etBankCode)
        val etBankName = sheetView.findViewById<TextInputEditText>(R.id.etBankName)
        val etAccountNumber = sheetView.findViewById<TextInputEditText>(R.id.etAccountNumber)
        val etAccountHolderName = sheetView.findViewById<TextInputEditText>(R.id.etAccountHolderName)
        val etBranch = sheetView.findViewById<TextInputEditText>(R.id.etBranch)
        val btnSave = sheetView.findViewById<Button>(R.id.btnSaveVietQr)
        val pbLoading = sheetView.findViewById<ProgressBar>(R.id.pbVietQrLoading)

        btnSave.setOnClickListener {
            val bankCode = etBankCode.text?.toString()?.trim() ?: ""
            val bankName = etBankName.text?.toString()?.trim() ?: ""
            val accountNumber = etAccountNumber.text?.toString()?.trim() ?: ""
            val accountHolderName = etAccountHolderName.text?.toString()?.trim()?.uppercase() ?: ""
            val branch = etBranch.text?.toString()?.trim()

            if (bankCode.isEmpty() || bankName.isEmpty() || accountNumber.isEmpty() || accountHolderName.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đủ các trường bắt buộc (*)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            pbLoading.visibility = View.VISIBLE

            viewModel.configureVietQr(
                buildingId = buildingId,
                bankCode = bankCode,
                bankName = bankName,
                accountNumber = accountNumber,
                accountHolderName = accountHolderName,
                branch = branch
            )
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showEditBuildingBottomSheet() {
        val current = currentDetail ?: return
        val dialog = BottomSheetDialog(this)
        val sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_edit_building, null)
        dialog.setContentView(sheetView)

        val tilName = sheetView.findViewById<TextInputLayout>(R.id.tilEditBuildingName)
        val etName = sheetView.findViewById<TextInputEditText>(R.id.etEditBuildingName)
        val etProvinceFixed = sheetView.findViewById<TextInputEditText>(R.id.etEditProvinceFixed)
        val etWardFixed = sheetView.findViewById<TextInputEditText>(R.id.etEditWardFixed)
        val tilAddress = sheetView.findViewById<TextInputLayout>(R.id.tilEditBuildingAddress)
        val etAddress = sheetView.findViewById<TextInputEditText>(R.id.etEditBuildingAddress)
        val btnCancel = sheetView.findViewById<MaterialButton>(R.id.btnCancelEditBuilding)
        val btnSave = sheetView.findViewById<MaterialButton>(R.id.btnSaveEditBuilding)

        etName.setText(current.name)
        etProvinceFixed.setText(current.province.ifBlank { "---" })
        etWardFixed.setText(current.ward.ifBlank { "---" })
        etAddress.setText(current.address)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val newName = etName.text?.toString()?.trim().orEmpty()
            val newAddress = etAddress.text?.toString()?.trim().orEmpty()

            if (newName.isEmpty()) {
                tilName.error = "Vui lòng nhập tên tòa nhà"
                return@setOnClickListener
            } else {
                tilName.error = null
            }

            if (newAddress.isEmpty()) {
                tilAddress.error = "Vui lòng nhập địa chỉ chi tiết"
                return@setOnClickListener
            } else {
                tilAddress.error = null
            }

            viewModel.updateBuilding(buildingId, newName, newAddress)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("Xác nhận xóa tòa nhà")
            .setMessage("Bạn có chắc chắn muốn xóa tòa nhà này không? Hành động này không thể hoàn tác.")
            .setPositiveButton("Xóa") { _, _ ->
                viewModel.deleteBuilding(buildingId)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun uploadSelectedOwnershipDoc(uri: Uri) {
        try {
            val contentType = contentResolver.getType(uri) ?: "image/jpeg"
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null || bytes.isEmpty()) {
                Toast.makeText(this, "Không thể đọc tệp ảnh minh chứng", Toast.LENGTH_SHORT).show()
                return
            }
            val ext = when {
                contentType.contains("png") -> "png"
                contentType.contains("webp") -> "webp"
                else -> "jpg"
            }
            val fileName = "ownership_${buildingId.take(6)}_${System.currentTimeMillis()}.$ext"
            viewModel.uploadOwnershipProof(buildingId, bytes, fileName, contentType)
        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi đọc tệp: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val EXTRA_BUILDING_ID = "extra_building_id"

        fun newIntent(context: Context, buildingId: String): Intent {
            return Intent(context, BuildingDetailActivity::class.java).apply {
                putExtra(EXTRA_BUILDING_ID, buildingId)
            }
        }
    }
}
