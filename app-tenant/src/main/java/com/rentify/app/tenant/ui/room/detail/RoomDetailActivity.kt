package com.rentify.app.tenant.ui.room.detail

import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.chip.ChipGroup
import com.rentify.app.core.network.dto.room.RoomDetailResponseDto
import com.rentify.app.core.ui.R as CoreUiR
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.core.ui.extension.toast
import com.rentify.app.core.ui.state.UiState
import com.rentify.app.tenant.MainActivity
import com.rentify.app.tenant.R
import com.rentify.app.tenant.databinding.ActivityRoomDetailBinding
import com.rentify.app.tenant.ui.chat.detail.ChatDetailActivity
import com.rentify.app.tenant.ui.dialog.showLoginRequiredSheet
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

class RoomDetailActivity : BaseActivity<ActivityRoomDetailBinding>(ActivityRoomDetailBinding::inflate) {

    private val roomId: String by lazy {
        intent.getStringExtra(EXTRA_ROOM_ID).orEmpty()
    }

    private val initialIsFavorited: Boolean by lazy {
        intent.getBooleanExtra(EXTRA_IS_FAVORITED, false)
    }

    private val fallbackAddress: String? by lazy {
        intent.getStringExtra(EXTRA_FALLBACK_ADDRESS)
    }

    private val viewModel: RoomDetailViewModel by viewModels {
        RoomDetailViewModelFactory(this, roomId, initialIsFavorited)
    }

    private val imageAdapter by lazy {
        RoomImageSliderAdapter()
    }

    private var isDescriptionExpanded = false
    private var isUserDragging = false
    private var autoScrollJob: Job? = null

    override fun initView() {
        binding.root.applySystemBarsPadding(top = false, bottom = true)
        setupImageSlider()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        if (imageAdapter.itemCount > 1) {
            startAutoScroll()
        }
    }

    override fun onPause() {
        super.onPause()
        stopAutoScroll()
    }

    private fun startAutoScroll() {
        autoScrollJob?.cancel()
        autoScrollJob = lifecycleScope.launch {
            while (isActive) {
                delay(3500)
                val count = imageAdapter.itemCount
                if (count > 1 && !isUserDragging) {
                    val nextItem = (binding.vpImageSlider.currentItem + 1) % count
                    binding.vpImageSlider.setCurrentItem(nextItem, true)
                }
            }
        }
    }

    private fun stopAutoScroll() {
        autoScrollJob?.cancel()
        autoScrollJob = null
    }

    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        handleUiState(state)
                    }
                }

                launch {
                    viewModel.isFavorited.collect { isFav ->
                        updateFavoriteButton(isFav)
                    }
                }

                launch {
                    viewModel.isContacting.collect { isContacting ->
                        binding.btnBottomChatLandlord.isEnabled = !isContacting
                        binding.btnBottomChatLandlord.text = if (isContacting) "Đang mở..." else "Nhắn tin"
                    }
                }

                launch {
                    viewModel.uiEvent.collect { event ->
                        handleUiEvent(event)
                    }
                }
            }
        }
    }

    private fun setupImageSlider() {
        binding.vpImageSlider.adapter = imageAdapter
        binding.vpImageSlider.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateDotsIndicator(position)
            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                isUserDragging = (state == ViewPager2.SCROLL_STATE_DRAGGING)
            }
        })
    }

    private fun setupDotsIndicator(count: Int) {
        binding.layoutDotsIndicator.removeAllViews()
        if (count <= 1) {
            binding.layoutDotsIndicator.isVisible = false
            return
        }
        binding.layoutDotsIndicator.isVisible = true
        val density = resources.displayMetrics.density
        val activeW = (16f * density).roundToInt()
        val inactiveW = (5f * density).roundToInt()
        val dotH = (5f * density).roundToInt()
        val marginH = (3f * density).roundToInt()

        for (i in 0 until count) {
            val isSelected = i == 0
            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    if (isSelected) activeW else inactiveW,
                    dotH
                ).apply {
                    setMargins(marginH, 0, marginH, 0)
                }
                background = createDotDrawable(isSelected)
            }
            binding.layoutDotsIndicator.addView(dot)
        }
    }

    private fun updateDotsIndicator(selectedPosition: Int) {
        val density = resources.displayMetrics.density
        val activeW = (16f * density).roundToInt()
        val inactiveW = (5f * density).roundToInt()
        val dotH = (5f * density).roundToInt()

        for (i in 0 until binding.layoutDotsIndicator.childCount) {
            val dot = binding.layoutDotsIndicator.getChildAt(i)
            val isSelected = i == selectedPosition
            val params = dot.layoutParams as LinearLayout.LayoutParams
            params.width = if (isSelected) activeW else inactiveW
            params.height = dotH
            dot.layoutParams = params
            dot.background = createDotDrawable(isSelected)
        }
    }

    private fun createDotDrawable(isSelected: Boolean): GradientDrawable {
        val density = resources.displayMetrics.density
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f * density
            setColor(
                ContextCompat.getColor(
                    this@RoomDetailActivity,
                    if (isSelected) CoreUiR.color.rentify_surface else CoreUiR.color.rentify_dot_inactive
                )
            )
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnSingleClickListener {
            finish()
        }

        binding.btnShare.setOnSingleClickListener {
            val currentData = (viewModel.uiState.value as? UiState.Success)?.data ?: return@setOnSingleClickListener
            val shareText = "Phòng ${currentData.name}\n" +
                "Giá thuê: ${formatVndVi(currentData.rentPrice)}/tháng\n" +
                "Tiền cọc: ${formatVndVi(currentData.depositAmount)}\n" +
                "Xem phòng trên ứng dụng Rentify!"
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            startActivity(Intent.createChooser(sendIntent, "Chia sẻ phòng"))
        }

        binding.btnFavorite.setOnSingleClickListener {
            viewModel.toggleFavorite()
        }

        binding.layoutError.btnRetry.setOnSingleClickListener {
            viewModel.loadDetail()
        }

        binding.btnToggleDescription.setOnSingleClickListener {
            isDescriptionExpanded = !isDescriptionExpanded
            if (isDescriptionExpanded) {
                binding.tvDescription.maxLines = Int.MAX_VALUE
                binding.btnToggleDescription.text = "Thu gọn"
            } else {
                binding.tvDescription.maxLines = 4
                binding.btnToggleDescription.text = "Xem thêm"
            }
        }

        binding.btnCallLandlord.setOnSingleClickListener {
            viewModel.callLandlord()
        }

        binding.btnBottomChatLandlord.setOnSingleClickListener {
            viewModel.contactLandlord()
        }
    }

    private fun handleUiState(state: UiState<RoomDetailResponseDto>) {
        when (state) {
            is UiState.Loading -> {
                binding.progressBar.isVisible = true
                binding.scrollView.isVisible = false
                binding.layoutError.root.isVisible = false
                binding.layoutBottomBar.isVisible = false
            }
            is UiState.Error -> {
                binding.progressBar.isVisible = false
                binding.scrollView.isVisible = false
                binding.layoutError.root.isVisible = true
                binding.layoutBottomBar.isVisible = false
            }
            is UiState.Success -> {
                binding.progressBar.isVisible = false
                binding.layoutError.root.isVisible = false
                binding.scrollView.isVisible = true
                binding.layoutBottomBar.isVisible = true
                bindRoomDetail(state.data)
            }
            is UiState.Idle -> Unit
        }
    }

    private fun bindRoomDetail(room: RoomDetailResponseDto) {
        // 2. Ảnh
        imageAdapter.submitList(room.photoUrls)
        val totalImages = imageAdapter.itemCount
        setupDotsIndicator(totalImages)
        if (totalImages > 1) {
            startAutoScroll()
        }

        // 3. Trạng thái, ngày đăng, tên, địa chỉ
        bindStatusBadge(room.status)
        bindPostedAgo(room.createdAt)
        binding.tvRoomTitle.text = room.name.ifBlank { "Phòng cho thuê" }
        bindRoomAddress(room)

        // 4. Thông số (Diện tích, Tầng, Tối đa, Giới tính)
        val parsedArea = room.area?.trim()?.toDoubleOrNull()
        binding.tvSpecArea.text = if (parsedArea != null && parsedArea > 0.0) {
            if (parsedArea % 1.0 == 0.0) "${parsedArea.toInt()} m²" else "$parsedArea m²"
        } else {
            "Chưa cập nhật"
        }

        val floor = room.floorNumber
        binding.tvSpecFloor.text = if (floor != null && floor > 0) {
            floor.toString()
        } else {
            "Chưa cập nhật"
        }

        val maxOcc = room.maxOccupancy
        binding.tvSpecOccupancy.text = when {
            maxOcc == null -> "Không giới hạn"
            maxOcc <= 0 -> "Chưa cập nhật"
            else -> "$maxOcc người"
        }

        binding.tvSpecGender.text = when (room.genderPolicy?.lowercase(Locale.ROOT)) {
            "male" -> "Nam"
            "female" -> "Nữ"
            else -> "Nam, Nữ"
        }

        // 5. Tiện nghi
        bindAmenities(room)

        // 6. Điện và nước
        binding.tvElectricityPolicy.text = formatUtilityPolicy(
            raw = room.effectiveRatePolicySummary?.electricity,
            unit = "kWh",
            defaultMeterLabel = "Theo công tơ"
        )
        binding.tvWaterPolicy.text = formatUtilityPolicy(
            raw = room.effectiveRatePolicySummary?.water,
            unit = "m³",
            defaultMeterLabel = "Theo khối tiêu thụ"
        )

        // 7. Mô tả
        isDescriptionExpanded = false
        binding.tvDescription.maxLines = 4
        binding.btnToggleDescription.text = "Xem thêm"
        val desc = room.description?.trim()
        if (!desc.isNullOrEmpty()) {
            binding.tvDescription.text = desc
            binding.tvDescription.post {
                val layout = binding.tvDescription.layout
                val isTruncated = if (layout != null && layout.lineCount > 0) {
                    layout.lineCount > 4 || layout.getEllipsisCount(layout.lineCount - 1) > 0
                } else {
                    desc.length > 160
                }
                binding.btnToggleDescription.isVisible = isTruncated
            }
        } else {
            binding.tvDescription.text = "Chưa có mô tả cho phòng trọ này."
            binding.btnToggleDescription.isVisible = false
        }

        // 7b. Chủ trọ
        val landlord = room.landlord
        binding.tvLandlordSectionTitle.isVisible = landlord != null
        binding.layoutLandlordCard.isVisible = landlord != null
        if (landlord != null) {
            val landlordName = landlord.name.ifBlank { "Chủ trọ" }
            binding.tvLandlordName.text = landlordName
            binding.landlordAvatarView.bindPersonal(
                userId = landlord.id.ifBlank { landlordName },
                displayName = landlordName,
                avatarUrl = landlord.avatarUrl
            )
        }

        // 8. Thanh nổi dưới cùng
        val priceFormatted = formatVndVi(room.rentPrice)
        val depositFormatted = formatVndVi(room.depositAmount)
        binding.tvBottomRentPrice.text = priceFormatted
        binding.tvBottomDepositAmount.text = "Cọc $depositFormatted"

        val screenWidthDp = resources.configuration.screenWidthDp
        val priceSp = if (screenWidthDp <= 360 || priceFormatted.length >= 12) 15f else 16f
        binding.tvBottomRentPrice.setTextSize(TypedValue.COMPLEX_UNIT_SP, priceSp)
    }

    /**
     * 3. Bảng ánh xạ trạng thái phòng từ API sang 3 nhóm hiển thị (lấy từ strings.xml):
     * - "available" -> @string/room_status_available ("Còn trống")
     * - "occupied" -> @string/room_status_unavailable ("Hết phòng")
     * - "cleaning", "pending_cleanup", "maintenance" -> @string/room_status_preparing ("Sắp trống")
     */
    private fun bindStatusBadge(status: String) {
        when (status.lowercase(Locale.ROOT).trim()) {
            "available" -> {
                binding.tvRoomStatus.setText(R.string.room_status_available)
                binding.tvRoomStatus.setBackgroundResource(R.drawable.bg_room_status_available)
                binding.tvRoomStatus.setTextColor(ContextCompat.getColor(this, CoreUiR.color.rentify_teal))
            }
            "occupied" -> {
                binding.tvRoomStatus.setText(R.string.room_status_unavailable)
                binding.tvRoomStatus.setBackgroundResource(R.drawable.bg_room_status_unavailable)
                binding.tvRoomStatus.setTextColor(ContextCompat.getColor(this, CoreUiR.color.rentify_text_secondary))
            }
            "cleaning", "pending_cleanup", "maintenance" -> {
                binding.tvRoomStatus.setText(R.string.room_status_preparing)
                binding.tvRoomStatus.setBackgroundResource(R.drawable.bg_room_status_preparing)
                binding.tvRoomStatus.setTextColor(ContextCompat.getColor(this, CoreUiR.color.rentify_warning_text))
            }
            else -> {
                binding.tvRoomStatus.setText(R.string.room_status_unavailable)
                binding.tvRoomStatus.setBackgroundResource(R.drawable.bg_room_status_unavailable)
                binding.tvRoomStatus.setTextColor(ContextCompat.getColor(this, CoreUiR.color.rentify_text_secondary))
            }
        }
    }

    private fun bindPostedAgo(createdAt: String?) {
        if (createdAt.isNullOrBlank()) {
            binding.tvPostedAgo.isVisible = false
            return
        }
        val date = try {
            val cleanIso = createdAt.substringBefore("Z").substringBefore("+")
            val base = cleanIso.substringBefore(".")
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(base)
        } catch (_: Exception) {
            null
        }
        if (date == null) {
            binding.tvPostedAgo.isVisible = false
            return
        }
        val diffDays = ((System.currentTimeMillis() - date.time) / (1000L * 60 * 60 * 24)).coerceAtLeast(0L)
        binding.tvPostedAgo.isVisible = true
        binding.tvPostedAgo.text = if (diffDays == 0L) "Đăng hôm nay" else "Đăng $diffDays ngày trước"
    }

    private fun bindRoomAddress(room: RoomDetailResponseDto) {
        val apiAddress = listOfNotNull(room.address, room.ward, room.province)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(", ")

        val resolvedAddress = when {
            apiAddress.isNotEmpty() -> apiAddress
            !fallbackAddress.isNullOrBlank() -> fallbackAddress!!.trim()
            else -> "Chưa cập nhật địa chỉ"
        }
        binding.tvRoomAddress.text = resolvedAddress
    }

    private fun formatUtilityPolicy(raw: String?, unit: String, defaultMeterLabel: String): String {
        val cleaned = raw?.trim().orEmpty()
        val numericRate = cleaned.toLongOrNull()
        if (numericRate != null && numericRate > 0L) {
            return "${formatVndVi(numericRate)}/$unit"
        }
        return when (cleaned.lowercase(Locale.ROOT)) {
            "flat" -> "Giá cố định"
            else -> defaultMeterLabel
        }
    }

    private data class AmenityItem(val label: String, val iconRes: Int)

    private fun bindAmenities(room: RoomDetailResponseDto) {
        binding.chipGroupAmenities.removeAllViews()
        val amenities = room.amenities
        val list = mutableListOf<AmenityItem>()
        if (amenities?.aircon == true) list.add(AmenityItem("Điều hòa", CoreUiR.drawable.ic_amenity_aircon))
        if (amenities?.fridge == true) list.add(AmenityItem("Tủ lạnh", CoreUiR.drawable.ic_amenity_fridge))
        if (amenities?.wcInside == true) list.add(AmenityItem("WC khép kín", CoreUiR.drawable.ic_issue_plumbing))
        if (amenities?.waterHeater == true) list.add(AmenityItem("Bình nóng lạnh", CoreUiR.drawable.ic_amenity_water_heater))
        if (amenities?.mezzanine == true) list.add(AmenityItem("Gác lửng", CoreUiR.drawable.ic_spec_floor))
        if (amenities?.table == true) list.add(AmenityItem("Bàn làm việc", CoreUiR.drawable.ic_amenity_furniture))
        if (amenities?.chair == true) list.add(AmenityItem("Ghế ngồi", CoreUiR.drawable.ic_amenity_furniture))

        if (list.isEmpty()) {
            binding.layoutAmenitiesSection.isVisible = false
            return
        }

        binding.layoutAmenitiesSection.isVisible = true
        val density = resources.displayMetrics.density
        val padH = (12f * density).roundToInt()
        val padV = (8f * density).roundToInt()
        val iconSize = (16f * density).roundToInt()
        val iconGap = (6f * density).roundToInt()
        val fontRegular = ResourcesCompat.getFont(this, CoreUiR.font.plus_jakarta_sans)
        val iconColor = ContextCompat.getColor(this, CoreUiR.color.rentify_icon_slate)
        val textColor = ContextCompat.getColor(this, CoreUiR.color.rentify_text)

        for (item in list) {
            val pillLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundResource(R.drawable.bg_room_amenity_chip)
                setPadding(padH, padV, padH, padV)
                layoutParams = ChipGroup.LayoutParams(
                    ChipGroup.LayoutParams.WRAP_CONTENT,
                    ChipGroup.LayoutParams.WRAP_CONTENT
                )
            }
            val iconView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
                setImageResource(item.iconRes)
                setColorFilter(iconColor)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            val labelView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = iconGap
                }
                text = item.label
                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                typeface = fontRegular
                includeFontPadding = false
            }
            pillLayout.addView(iconView)
            pillLayout.addView(labelView)
            binding.chipGroupAmenities.addView(pillLayout)
        }
    }

    private fun updateFavoriteButton(isFav: Boolean) {
        if (isFav) {
            binding.btnFavorite.setImageResource(CoreUiR.drawable.ic_heart_filled)
            binding.btnFavorite.setColorFilter(ContextCompat.getColor(this, CoreUiR.color.rentify_heart_saved))
            binding.btnFavorite.contentDescription = "Bỏ lưu phòng"
        } else {
            binding.btnFavorite.setImageResource(CoreUiR.drawable.ic_heart_outline)
            binding.btnFavorite.setColorFilter(ContextCompat.getColor(this, CoreUiR.color.rentify_text))
            binding.btnFavorite.contentDescription = "Lưu phòng"
        }
    }

    private fun formatVndVi(amount: Long): String {
        val numberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
        return "${numberFormat.format(amount.coerceAtLeast(0L))} đ"
    }

    private fun handleUiEvent(event: RoomDetailUiEvent) {
        when (event) {
            is RoomDetailUiEvent.ShowToast -> {
                toast(event.message)
            }
            is RoomDetailUiEvent.ShowLoginRequired -> {
                showLoginRequiredSheet(event.feature) {
                    startActivity(MainActivity.createLoginIntent(this))
                }
            }
            is RoomDetailUiEvent.CallPhone -> {
                val callIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:${event.phone.trim()}")
                }
                startActivity(callIntent)
            }
            is RoomDetailUiEvent.NavigateToChat -> {
                val chatIntent = ChatDetailActivity.createIntent(
                    context = this,
                    conversationId = event.conversationId,
                    recipientName = event.recipientName
                )
                startActivity(chatIntent)
            }
        }
    }

    companion object {
        const val EXTRA_ROOM_ID = "extra_room_id"
        const val EXTRA_IS_FAVORITED = "extra_is_favorited"
        const val EXTRA_FALLBACK_ADDRESS = "extra_fallback_address"

        fun start(
            context: Context,
            roomId: String,
            isFavorited: Boolean = false,
            fallbackAddress: String? = null
        ) {
            val intent = Intent(context, RoomDetailActivity::class.java).apply {
                putExtra(EXTRA_ROOM_ID, roomId)
                putExtra(EXTRA_IS_FAVORITED, isFavorited)
                putExtra(EXTRA_FALLBACK_ADDRESS, fallbackAddress)
            }
            context.startActivity(intent)
        }
    }
}
