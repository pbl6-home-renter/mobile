package com.rentify.app.landlord.ui.property

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import coil.load
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.rentify.app.core.network.client.NetworkConfig
import com.rentify.app.core.network.dto.billing.BillingSettingResponseDto
import com.rentify.app.core.network.dto.billing.RateItemDto
import com.rentify.app.core.network.dto.room.RoomAmenitiesDto
import com.rentify.app.core.network.dto.room.RoomDetailResponseDto
import com.rentify.app.core.network.dto.room.RoomMemberDto
import com.rentify.app.core.network.dto.room.RoomUpdateRequestDto
import com.rentify.app.core.ui.R as CoreUiR
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.view.RentifyAvatarView
import com.rentify.app.landlord.R
import com.rentify.app.landlord.databinding.ActivityLandlordRoomDetailBinding
import com.rentify.app.landlord.ui.contract.LandlordContractDetailActivity
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

class LandlordRoomDetailActivity : BaseActivity<ActivityLandlordRoomDetailBinding>(
    ActivityLandlordRoomDetailBinding::inflate
) {
    private val viewModel: RoomViewModel by viewModels {
        RoomViewModelFactory(this)
    }

    private var roomId: String = ""
    private var currentRoom: RoomDetailResponseDto? = null
    private var hasMutated: Boolean = false

    private val genderPolicyOptions = listOf(
        "Tất cả (Không giới hạn)" to "any",
        "Nam" to "male",
        "Nữ" to "female"
    )

    private val pickRoomPhotoLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && roomId.isNotEmpty()) {
            uploadSelectedPhoto(uri)
        }
    }

    override fun initView() {
        binding.rootLayout.applySystemBarsPadding(top = true, bottom = true)

        roomId = intent.getStringExtra(EXTRA_ROOM_ID) ?: ""
        if (roomId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy mã phòng", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.toolbar.setNavigationOnClickListener {
            if (hasMutated) setResult(RESULT_OK)
            finish()
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadRoomDetail(roomId)
            currentRoom?.buildingId?.let { bId ->
                viewModel.loadRoomPoliciesAndSettings(roomId, bId)
            }
        }

        binding.btnDetailMarkReady.setOnClickListener {
            hasMutated = true
            viewModel.markRoomReady(roomId, reloadDetail = true)
        }

        binding.btnEditRoom.setOnClickListener {
            showEditRoomBottomSheet()
        }

        binding.btnDeleteRoom.setOnClickListener {
            showDeleteRoomDialog()
        }

        binding.btnUploadRoomPhoto.setOnClickListener {
            pickRoomPhotoLauncher.launch("image/*")
        }

        binding.btnConfigRoomRates.setOnClickListener {
            val room = currentRoom ?: return@setOnClickListener
            val roomPolicy = viewModel.roomRatePolicyState.value
            val bldPolicy = viewModel.buildingRatePolicyState.value
            val hasCustomActive = roomPolicy?.isActive == true
            val initialRates = when {
                hasCustomActive && roomPolicy!!.rates.isNotEmpty() -> roomPolicy.rates
                bldPolicy != null && bldPolicy.rates.isNotEmpty() -> bldPolicy.rates
                else -> emptyList()
            }
            PolicySettingBottomSheetHelper.showRatePoliciesSheet(
                context = this,
                initialRates = initialRates,
                canInheritBuilding = hasCustomActive,
                onInheritBuilding = {
                    viewModel.inheritBuildingRatePolicies(roomId, room.buildingId)
                },
                onSave = { rates ->
                    viewModel.saveRoomRatePolicies(roomId, room.buildingId, rates)
                }
            )
        }

        binding.btnConfigRoomBilling.setOnClickListener {
            val room = currentRoom ?: return@setOnClickListener
            val roomSetting = viewModel.roomBillingSettingState.value
            val bldSetting = viewModel.buildingBillingSettingState.value
            val hasCustomActive = roomSetting?.isActive == true
            val effectiveSetting = if (hasCustomActive) roomSetting else bldSetting
            PolicySettingBottomSheetHelper.showBillingSettingsSheet(
                context = this,
                currentSetting = effectiveSetting,
                canInheritBuilding = hasCustomActive,
                onInheritBuilding = {
                    viewModel.inheritBuildingBillingSetting(roomId, room.buildingId)
                },
                onSave = { dueDay, remindDay, botEnabled ->
                    viewModel.saveRoomBillingSetting(roomId, room.buildingId, dueDay, remindDay, botEnabled)
                }
            )
        }

        viewModel.loadRoomDetail(roomId)
    }

    override fun observeData() {
        viewModel.roomDetailState.collectWhenStarted { state ->
            binding.swipeRefresh.isRefreshing = false
            when (state) {
                is RoomDetailUiState.Loading -> {
                    binding.roomDetailProgressBar.visibility = View.VISIBLE
                }
                is RoomDetailUiState.Success -> {
                    binding.roomDetailProgressBar.visibility = View.GONE
                    val isFirstLoad = currentRoom == null
                    currentRoom = state.room
                    bindRoomDetail(state.room)
                    if (isFirstLoad && state.room.buildingId.isNotBlank()) {
                        viewModel.loadRoomPoliciesAndSettings(roomId, state.room.buildingId)
                    }
                }
                is RoomDetailUiState.Error -> {
                    binding.roomDetailProgressBar.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                is RoomDetailUiState.Idle -> {
                    binding.roomDetailProgressBar.visibility = View.GONE
                }
            }
        }

        viewModel.roomRatePolicyState.collectWhenStarted {
            refreshRoomRatesUi()
        }

        viewModel.buildingRatePolicyState.collectWhenStarted {
            refreshRoomRatesUi()
        }

        viewModel.roomBillingSettingState.collectWhenStarted {
            refreshRoomBillingUi()
        }

        viewModel.buildingBillingSettingState.collectWhenStarted {
            refreshRoomBillingUi()
        }

        viewModel.actionState.collectWhenStarted { state ->
            when (state) {
                is RoomActionUiState.Loading -> {
                    binding.roomDetailProgressBar.visibility = View.VISIBLE
                }
                is RoomActionUiState.Success -> {
                    binding.roomDetailProgressBar.visibility = View.GONE
                    hasMutated = true
                    setResult(RESULT_OK)
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    viewModel.resetActionState()
                    if (state.message.contains("Xóa")) {
                        finish()
                    }
                }
                is RoomActionUiState.Error -> {
                    binding.roomDetailProgressBar.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    viewModel.resetActionState()
                }
                is RoomActionUiState.Idle -> {
                    binding.roomDetailProgressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun bindRoomDetail(room: RoomDetailResponseDto) {
        binding.toolbar.title = room.name.ifBlank { "Chi tiết Phòng" }
        binding.tvRoomDetailName.text = room.name.ifBlank { "Phòng trọ" }

        val floorStr = room.floorNumber?.takeIf { it > 0 }?.let { "Tầng $it" } ?: "Chưa rõ tầng"
        val areaStr = room.area?.takeIf { it.isNotBlank() }?.let { "$it m²" } ?: "Chưa rõ DT"
        val buildingPart = room.buildingName.takeIf { it.isNotBlank() }
        binding.tvRoomDetailSubtitle.text = listOfNotNull(buildingPart, floorStr, areaStr).joinToString(" • ")

        val fullAddr = listOfNotNull(room.address, room.ward, room.province)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(", ")
        if (fullAddr.isNotEmpty()) {
            binding.tvRoomDetailAddress.visibility = View.VISIBLE
            binding.tvRoomDetailAddress.text = fullAddr
        } else {
            binding.tvRoomDetailAddress.visibility = View.GONE
        }

        val (badgeText, textColor, bgColor) = when (room.status.lowercase(Locale.ROOT)) {
            "occupied" -> Triple("Đang thuê", Color.parseColor("#2563EB"), Color.parseColor("#EFF6FF"))
            "maintenance" -> Triple("Đang bảo trì", Color.parseColor("#D97706"), Color.parseColor("#FEF3C7"))
            "cleaning" -> Triple("Đang dọn dẹp", Color.parseColor("#D97706"), Color.parseColor("#FEF3C7"))
            "pending_cleanup" -> Triple("Chờ dọn dẹp", Color.parseColor("#D97706"), Color.parseColor("#FEF3C7"))
            else -> Triple("Phòng trống", Color.parseColor("#16A34A"), Color.parseColor("#DCFCE7"))
        }

        binding.tvRoomDetailStatusBadge.text = badgeText
        binding.tvRoomDetailStatusBadge.setTextColor(textColor)
        binding.tvRoomDetailStatusBadge.background = GradientDrawable().apply {
            setColor(bgColor)
            cornerRadius = 16f
        }

        binding.btnDetailMarkReady.visibility = if (room.status.equals("maintenance", ignoreCase = true)) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // 2. Photos
        bindRoomPhotos(room.photoUrls)

        // 3. Technical Specs & Price
        val numberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
        binding.tvSpecRentPrice.text = "${numberFormat.format(room.rentPrice)} đ"
        binding.tvSpecDepositAmount.text = "${numberFormat.format(room.depositAmount)} đ"
        binding.tvSpecArea.text = areaStr
        binding.tvSpecFloor.text = floorStr
        binding.tvSpecMaxOccupancy.text = when {
            room.maxOccupancy == null -> "Không giới hạn"
            room.maxOccupancy!! <= 0 -> "Không giới hạn"
            else -> "${room.maxOccupancy} người"
        }
        binding.tvSpecGenderPolicy.text = when (room.genderPolicy?.lowercase(Locale.ROOT)) {
            "male" -> "Nam"
            "female" -> "Nữ"
            else -> "Tất cả (Không giới hạn)"
        }

        bindRoomAmenities(room.amenities)
        refreshRoomRatesUi()
        refreshRoomBillingUi()

        // 4. Contract & Members
        val contract = room.currentContract
        if (contract != null && contract.id.isNotBlank()) {
            binding.layoutActiveContractContent.visibility = View.VISIBLE
            binding.tvNoActiveContract.visibility = View.GONE
            binding.tvContractCode.text = contract.contractCode?.takeIf { it.isNotBlank() } ?: "HĐ-${contract.id.take(6).uppercase()}"

            val (cStatusText, cTextColor, cBgColor) = when (contract.status?.lowercase(Locale.ROOT)) {
                "active" -> Triple("Đang hiệu lực", Color.parseColor("#16A34A"), Color.parseColor("#DCFCE7"))
                "draft" -> Triple("Bản nháp", Color.parseColor("#D97706"), Color.parseColor("#FEF3C7"))
                else -> Triple(contract.status ?: "Đang hiệu lực", Color.parseColor("#2563EB"), Color.parseColor("#EFF6FF"))
            }
            binding.tvContractStatusBadge.text = cStatusText
            binding.tvContractStatusBadge.setTextColor(cTextColor)
            binding.tvContractStatusBadge.background = GradientDrawable().apply {
                setColor(cBgColor)
                cornerRadius = 12f
            }

            val start = contract.startDate?.take(10) ?: "---"
            val end = contract.endDate?.take(10) ?: "---"
            binding.tvContractDuration.text = "Thời hạn thuê: $start → $end"

            binding.cardContractSummaryBox.setOnClickListener {
                val intent = LandlordContractDetailActivity.newIntent(this, contract.id, room.name)
                startActivity(intent)
            }

            bindCurrentMembers(room.currentMembers)
        } else {
            binding.layoutActiveContractContent.visibility = View.GONE
            binding.tvNoActiveContract.visibility = View.VISIBLE
        }

        // 5. Meter Readings
        val meters = room.latestMeterReadings
        val elecVal = meters?.electricity?.takeIf { it.isNotBlank() } ?: "0"
        val waterVal = meters?.water?.takeIf { it.isNotBlank() } ?: "0"
        binding.tvLatestElectricity.text = "$elecVal kWh"
        binding.tvLatestWater.text = "$waterVal m³"
        if (!meters?.recordedAt.isNullOrBlank()) {
            binding.tvMeterRecordedAt.visibility = View.VISIBLE
            binding.tvMeterRecordedAt.text = "Cập nhật gần nhất: ${meters?.recordedAt?.take(10)}"
        } else {
            binding.tvMeterRecordedAt.visibility = View.GONE
        }
    }

    private fun refreshRoomRatesUi() {
        val roomPolicy = viewModel.roomRatePolicyState.value
        val bldPolicy = viewModel.buildingRatePolicyState.value
        val isCustom = roomPolicy?.isActive == true
        binding.tvRoomRateScopeBadge.text = if (isCustom) "Biểu giá riêng" else "Kế thừa tòa nhà"

        val rates = when {
            isCustom -> roomPolicy?.rates.orEmpty()
            bldPolicy != null -> bldPolicy.rates
            else -> emptyList()
        }

        binding.layoutRoomRatesList.removeAllViews()
        val density = resources.displayMetrics.density
        val padV = (4f * density).roundToInt()
        val fontRegular = ResourcesCompat.getFont(this, CoreUiR.font.plus_jakarta_sans)

        if (rates.isNotEmpty()) {
            binding.tvRoomNoRatesNote.visibility = View.GONE
            for (rate in rates) {
                val tv = TextView(this).apply {
                    text = "• ${PolicySettingBottomSheetHelper.formatRateSummary(rate)}"
                    setTextColor(ContextCompat.getColor(this@LandlordRoomDetailActivity, CoreUiR.color.rentify_text_primary))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    typeface = fontRegular
                    setPadding(0, padV, 0, padV)
                }
                binding.layoutRoomRatesList.addView(tv)
            }
        } else {
            val summary = currentRoom?.effectiveRatePolicySummary
            val elec = summary?.electricity?.takeIf { it.isNotBlank() }
            val water = summary?.water?.takeIf { it.isNotBlank() }
            if (elec != null || water != null) {
                binding.tvRoomNoRatesNote.visibility = View.GONE
                if (elec != null) {
                    val tvE = TextView(this).apply {
                        text = "• Điện: $elec"
                        setTextColor(ContextCompat.getColor(this@LandlordRoomDetailActivity, CoreUiR.color.rentify_text_primary))
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                        typeface = fontRegular
                        setPadding(0, padV, 0, padV)
                    }
                    binding.layoutRoomRatesList.addView(tvE)
                }
                if (water != null) {
                    val tvW = TextView(this).apply {
                        text = "• Nước: $water"
                        setTextColor(ContextCompat.getColor(this@LandlordRoomDetailActivity, CoreUiR.color.rentify_text_primary))
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                        typeface = fontRegular
                        setPadding(0, padV, 0, padV)
                    }
                    binding.layoutRoomRatesList.addView(tvW)
                }
            } else {
                binding.tvRoomNoRatesNote.visibility = View.VISIBLE
            }
        }
    }

    private fun refreshRoomBillingUi() {
        val roomSetting = viewModel.roomBillingSettingState.value
        val bldSetting = viewModel.buildingBillingSettingState.value
        val isCustom = roomSetting?.isActive == true
        binding.tvRoomBillingScopeBadge.text = if (isCustom) "Cài đặt riêng" else "Kế thừa tòa nhà"

        val effective: BillingSettingResponseDto? = if (isCustom) roomSetting else bldSetting
        val dueDay = effective?.paymentDueDay ?: 5
        val remindDay = effective?.remindDay
        val botOn = effective?.botEnabled ?: true

        binding.tvRoomDueDay.text = "Ngày $dueDay hàng tháng"
        binding.tvRoomRemindDay.text = if (remindDay != null) "Ngày $remindDay" else "Không nhắc"
        binding.tvRoomBotStatus.text = if (botOn) "Đang bật" else "Đang tắt"
        binding.tvRoomBotStatus.setTextColor(
            ContextCompat.getColor(
                this,
                if (botOn) CoreUiR.color.rentify_success else CoreUiR.color.rentify_text_muted
            )
        )
    }

    private fun bindRoomPhotos(photoUrls: List<String>) {
        binding.layoutRoomPhotosContainer.removeAllViews()
        if (photoUrls.isEmpty()) {
            binding.tvRoomPhotosCount.text = "Chưa có ảnh phòng"
            binding.scrollRoomPhotos.visibility = View.GONE
            return
        }
        binding.tvRoomPhotosCount.text = "${photoUrls.size} ảnh đã tải lên"
        binding.scrollRoomPhotos.visibility = View.VISIBLE

        val density = resources.displayMetrics.density
        val sizePx = (96f * density).roundToInt()
        val marginPx = (8f * density).roundToInt()
        val radiusPx = 12f * density

        val gatewayOrigin = NetworkConfig.DEFAULT_BASE_URL.substringBefore("/api/")

        for (rawUrl in photoUrls) {
            val resolvedUrl = if (rawUrl.startsWith("http")) rawUrl else "$gatewayOrigin/${rawUrl.trimStart('/')}"
            val card = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                    marginEnd = marginPx
                }
                radius = radiusPx
                cardElevation = 0f
                strokeWidth = (1f * density).roundToInt()
                strokeColor = ContextCompat.getColor(this@LandlordRoomDetailActivity, CoreUiR.color.rentify_divider)
            }
            val iv = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                load(resolvedUrl) {
                    crossfade(true)
                    placeholder(CoreUiR.drawable.ic_nav_properties)
                    error(CoreUiR.drawable.ic_nav_properties)
                }
            }
            card.addView(iv)
            binding.layoutRoomPhotosContainer.addView(card)
        }
    }

    private fun bindRoomAmenities(amenities: RoomAmenitiesDto?) {
        binding.chipGroupRoomAmenities.removeAllViews()
        val items = mutableListOf<Pair<String, Int>>()
        if (amenities?.aircon == true) items.add("Điều hòa" to CoreUiR.drawable.ic_amenity_aircon)
        if (amenities?.fridge == true) items.add("Tủ lạnh" to CoreUiR.drawable.ic_amenity_fridge)
        if (amenities?.wcInside == true) items.add("WC khép kín" to CoreUiR.drawable.ic_issue_plumbing)
        if (amenities?.waterHeater == true) items.add("Bình nóng lạnh" to CoreUiR.drawable.ic_amenity_water_heater)
        if (amenities?.mezzanine == true) items.add("Gác lửng" to CoreUiR.drawable.ic_spec_floor)
        if (amenities?.table == true) items.add("Bàn làm việc" to CoreUiR.drawable.ic_amenity_furniture)
        if (amenities?.chair == true) items.add("Ghế ngồi" to CoreUiR.drawable.ic_amenity_furniture)

        if (items.isEmpty()) {
            binding.chipGroupRoomAmenities.visibility = View.GONE
            binding.tvNoRoomAmenities.visibility = View.VISIBLE
            return
        }

        binding.chipGroupRoomAmenities.visibility = View.VISIBLE
        binding.tvNoRoomAmenities.visibility = View.GONE

        val density = resources.displayMetrics.density
        val padH = (12f * density).roundToInt()
        val padV = (8f * density).roundToInt()
        val iconSize = (16f * density).roundToInt()
        val iconGap = (6f * density).roundToInt()
        val fontRegular = ResourcesCompat.getFont(this, CoreUiR.font.plus_jakarta_sans)
        val iconColor = ContextCompat.getColor(this, CoreUiR.color.rentify_icon_slate)
        val textColor = ContextCompat.getColor(this, CoreUiR.color.rentify_text_primary)
        val pillBgColor = ContextCompat.getColor(this, CoreUiR.color.rentify_soft_surface)

        for ((label, iconRes) in items) {
            val pillLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable().apply {
                    setColor(pillBgColor)
                    cornerRadius = 12f * density
                }
                setPadding(padH, padV, padH, padV)
                layoutParams = ChipGroup.LayoutParams(
                    ChipGroup.LayoutParams.WRAP_CONTENT,
                    ChipGroup.LayoutParams.WRAP_CONTENT
                )
            }
            val iconView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
                setImageResource(iconRes)
                setColorFilter(iconColor)
            }
            val labelView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = iconGap
                }
                text = label
                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = fontRegular
            }
            pillLayout.addView(iconView)
            pillLayout.addView(labelView)
            binding.chipGroupRoomAmenities.addView(pillLayout)
        }
    }

    private fun bindCurrentMembers(members: List<RoomMemberDto>) {
        binding.layoutCurrentMembersContainer.removeAllViews()
        if (members.isEmpty()) {
            binding.tvMembersSectionLabel.visibility = View.GONE
            return
        }
        binding.tvMembersSectionLabel.visibility = View.VISIBLE
        val density = resources.displayMetrics.density
        val avatarSizePx = (40f * density).roundToInt()
        val padV = (8f * density).roundToInt()
        val fontRegular = ResourcesCompat.getFont(this, CoreUiR.font.plus_jakarta_sans)

        for (member in members) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, padV, 0, padV)
            }

            val avatarView = RentifyAvatarView(this).apply {
                layoutParams = LinearLayout.LayoutParams(avatarSizePx, avatarSizePx)
                bindPersonal(
                    userId = member.id.ifBlank { member.name },
                    displayName = member.name.ifBlank { "Khách thuê" },
                    avatarUrl = member.avatarUrl
                )
            }

            val infoCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = (12f * density).roundToInt()
                }
            }

            val nameRow = TextView(this).apply {
                val repSuffix = if (member.isRepresentative) " (Người đại diện)" else ""
                text = "${member.name}$repSuffix"
                setTextColor(ContextCompat.getColor(this@LandlordRoomDetailActivity, CoreUiR.color.rentify_text_primary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                typeface = fontRegular
                paint.isFakeBoldText = true
            }

            val phoneRow = TextView(this).apply {
                text = member.phone?.takeIf { it.isNotBlank() } ?: "Chưa có SĐT"
                setTextColor(ContextCompat.getColor(this@LandlordRoomDetailActivity, CoreUiR.color.rentify_text_secondary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = fontRegular
            }

            infoCol.addView(nameRow)
            infoCol.addView(phoneRow)
            row.addView(avatarView)
            row.addView(infoCol)

            if (!member.phone.isNullOrBlank()) {
                val callBtn = ImageButton(this).apply {
                    val btnSize = (36f * density).roundToInt()
                    layoutParams = LinearLayout.LayoutParams(btnSize, btnSize)
                    setImageResource(CoreUiR.drawable.ic_phone_outline)
                    setColorFilter(ContextCompat.getColor(this@LandlordRoomDetailActivity, CoreUiR.color.rentify_primary))
                    background = GradientDrawable().apply {
                        setColor(ContextCompat.getColor(this@LandlordRoomDetailActivity, CoreUiR.color.profile_icon_blue_bg))
                        cornerRadius = 18f * density
                    }
                    setOnClickListener {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${member.phone?.trim()}"))
                        startActivity(dialIntent)
                    }
                }
                row.addView(callBtn)
            }

            binding.layoutCurrentMembersContainer.addView(row)
        }
    }

    private fun showEditRoomBottomSheet() {
        val room = currentRoom ?: return
        val dialog = BottomSheetDialog(this)
        val sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_edit_room, null)
        dialog.setContentView(sheetView)

        val tvTitle = sheetView.findViewById<TextView>(R.id.tvEditRoomTitle)
        val tilName = sheetView.findViewById<TextInputLayout>(R.id.tilEditRoomName)
        val etName = sheetView.findViewById<TextInputEditText>(R.id.etEditRoomName)
        val tilFloor = sheetView.findViewById<TextInputLayout>(R.id.tilEditFloor)
        val etFloor = sheetView.findViewById<TextInputEditText>(R.id.etEditFloor)
        val tilArea = sheetView.findViewById<TextInputLayout>(R.id.tilEditArea)
        val etArea = sheetView.findViewById<TextInputEditText>(R.id.etEditArea)
        val tilRentPrice = sheetView.findViewById<TextInputLayout>(R.id.tilEditRentPrice)
        val etRentPrice = sheetView.findViewById<TextInputEditText>(R.id.etEditRentPrice)
        val etMaxOccupancy = sheetView.findViewById<TextInputEditText>(R.id.etEditMaxOccupancy)
        val actvGender = sheetView.findViewById<AutoCompleteTextView>(R.id.actvEditGenderPolicy)
        val etDescription = sheetView.findViewById<TextInputEditText>(R.id.etEditDescription)

        val cbAircon = sheetView.findViewById<MaterialCheckBox>(R.id.cbEditAircon)
        val cbFridge = sheetView.findViewById<MaterialCheckBox>(R.id.cbEditFridge)
        val cbWcInside = sheetView.findViewById<MaterialCheckBox>(R.id.cbEditWcInside)
        val cbWaterHeater = sheetView.findViewById<MaterialCheckBox>(R.id.cbEditWaterHeater)
        val cbMezzanine = sheetView.findViewById<MaterialCheckBox>(R.id.cbEditMezzanine)
        val cbTable = sheetView.findViewById<MaterialCheckBox>(R.id.cbEditTable)
        val cbChair = sheetView.findViewById<MaterialCheckBox>(R.id.cbEditChair)

        val btnCancel = sheetView.findViewById<MaterialButton>(R.id.btnCancelEditRoom)
        val btnSave = sheetView.findViewById<MaterialButton>(R.id.btnSaveEditRoom)

        tvTitle.text = "Chỉnh sửa thông tin phòng ${room.name}"
        etName.setText(room.name)
        etFloor.setText((room.floorNumber ?: 1).toString())
        etArea.setText(room.area ?: "")
        etRentPrice.setText(room.rentPrice.toString())
        etMaxOccupancy.setText(room.maxOccupancy?.takeIf { it > 0 }?.toString() ?: "")
        etDescription.setText(room.description ?: "")

        var selectedGenderCode = room.genderPolicy?.lowercase(Locale.ROOT) ?: "any"
        val genderAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            genderPolicyOptions.map { it.first }
        )
        actvGender.setAdapter(genderAdapter)
        val currentGenderLabel = genderPolicyOptions.firstOrNull { it.second == selectedGenderCode }?.first
            ?: genderPolicyOptions[0].first
        actvGender.setText(currentGenderLabel, false)
        actvGender.setOnItemClickListener { _, _, position, _ ->
            selectedGenderCode = genderPolicyOptions.getOrNull(position)?.second ?: "any"
        }

        cbAircon.isChecked = room.amenities?.aircon == true
        cbFridge.isChecked = room.amenities?.fridge == true
        cbWcInside.isChecked = room.amenities?.wcInside == true
        cbWaterHeater.isChecked = room.amenities?.waterHeater == true
        cbMezzanine.isChecked = room.amenities?.mezzanine == true
        cbTable.isChecked = room.amenities?.table == true
        cbChair.isChecked = room.amenities?.chair == true

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val newName = etName.text?.toString()?.trim().orEmpty()
            if (newName.isEmpty()) {
                tilName.error = "Vui lòng nhập tên phòng"
                return@setOnClickListener
            } else {
                tilName.error = null
            }

            val newFloor = etFloor.text?.toString()?.trim()?.toIntOrNull()
            if (newFloor == null || newFloor < 1) {
                tilFloor.error = "Số tầng không hợp lệ"
                return@setOnClickListener
            } else {
                tilFloor.error = null
            }

            val newArea = etArea.text?.toString()?.trim()?.toDoubleOrNull()
            if (newArea == null || newArea <= 0.0) {
                tilArea.error = "Diện tích không hợp lệ"
                return@setOnClickListener
            } else {
                tilArea.error = null
            }

            val newRentPrice = etRentPrice.text?.toString()?.trim()?.toLongOrNull()
            if (newRentPrice == null || newRentPrice < 0L) {
                tilRentPrice.error = "Giá thuê không hợp lệ"
                return@setOnClickListener
            } else {
                tilRentPrice.error = null
            }

            val newMaxOcc = etMaxOccupancy.text?.toString()?.trim()?.toIntOrNull()
            val newDesc = etDescription.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }

            val newAmenities = RoomAmenitiesDto(
                aircon = cbAircon.isChecked,
                fridge = cbFridge.isChecked,
                wcInside = cbWcInside.isChecked,
                waterHeater = cbWaterHeater.isChecked,
                mezzanine = cbMezzanine.isChecked,
                table = cbTable.isChecked,
                chair = cbChair.isChecked
            )

            val updateReq = RoomUpdateRequestDto(
                name = newName,
                floorNumber = newFloor,
                description = newDesc,
                area = newArea,
                amenities = newAmenities,
                maxOccupancy = newMaxOcc,
                genderPolicy = selectedGenderCode,
                rentPrice = newRentPrice
            )

            viewModel.updateRoom(roomId, updateReq)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteRoomDialog() {
        val room = currentRoom
        if (room != null && room.status.equals("occupied", ignoreCase = true)) {
            Toast.makeText(
                this,
                "Phòng đang có khách thuê / hợp đồng hiệu lực, không thể xóa.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Xác nhận xóa phòng")
            .setMessage("Bạn có chắc chắn muốn xóa phòng ${room?.name ?: ""} không? Hành động này không thể hoàn tác.")
            .setPositiveButton("Xóa") { _, _ ->
                viewModel.deleteRoom(roomId)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun uploadSelectedPhoto(uri: Uri) {
        try {
            val contentType = contentResolver.getType(uri) ?: "image/jpeg"
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null || bytes.isEmpty()) {
                Toast.makeText(this, "Không thể đọc tệp ảnh đã chọn", Toast.LENGTH_SHORT).show()
                return
            }
            val ext = when {
                contentType.contains("png") -> "png"
                contentType.contains("webp") -> "webp"
                else -> "jpg"
            }
            val fileName = "room_${roomId.take(6)}_${System.currentTimeMillis()}.$ext"
            viewModel.uploadRoomPhoto(roomId, bytes, fileName, contentType)
        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi đọc ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val EXTRA_ROOM_ID = "extra_room_id"

        fun newIntent(context: Context, roomId: String): Intent {
            return Intent(context, LandlordRoomDetailActivity::class.java).apply {
                putExtra(EXTRA_ROOM_ID, roomId)
            }
        }
    }
}
