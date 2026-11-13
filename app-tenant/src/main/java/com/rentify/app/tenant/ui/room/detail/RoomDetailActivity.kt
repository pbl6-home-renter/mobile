package com.rentify.app.tenant.ui.room.detail

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import android.view.View
import android.widget.LinearLayout
import com.google.android.material.chip.Chip
import com.rentify.app.core.network.dto.room.RoomDetailResponseDto
import com.rentify.app.core.ui.R as CoreUiR
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.core.ui.extension.toast
import com.rentify.app.core.ui.extension.toVnd
import com.rentify.app.core.ui.state.UiState
import com.rentify.app.tenant.MainActivity
import com.rentify.app.tenant.databinding.ActivityRoomDetailBinding
import com.rentify.app.tenant.ui.chat.detail.ChatDetailActivity
import com.rentify.app.tenant.ui.dialog.showRequireLoginDialog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RoomDetailActivity : BaseActivity<ActivityRoomDetailBinding>(ActivityRoomDetailBinding::inflate) {

    private val roomId: String by lazy {
        intent.getStringExtra(EXTRA_ROOM_ID).orEmpty()
    }

    private val initialIsFavorited: Boolean by lazy {
        intent.getBooleanExtra(EXTRA_IS_FAVORITED, false)
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
                        binding.btnChatLandlord.isEnabled = !isContacting
                        binding.btnChatLandlord.text = if (isContacting) "Đang kết nối..." else "Nhắn tin"
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
                val total = maxOf(1, imageAdapter.itemCount)
                binding.tvImageIndicator.text = "${position + 1} / $total"
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

        for (i in 0 until count) {
            val dot = View(this).apply {
                val params = LinearLayout.LayoutParams(
                    if (i == 0) (16 * density).toInt() else (6 * density).toInt(),
                    (6 * density).toInt()
                ).apply {
                    setMargins((2 * density).toInt(), 0, (2 * density).toInt(), 0)
                }
                layoutParams = params
                background = ContextCompat.getDrawable(
                    this@RoomDetailActivity,
                    if (i == 0) CoreUiR.drawable.bg_pill_badge_role else CoreUiR.drawable.bg_circle_icon_button
                )
                backgroundTintList = ColorStateList.valueOf(
                    if (i == 0) Color.WHITE else Color.parseColor("#80FFFFFF")
                )
            }
            binding.layoutDotsIndicator.addView(dot)
        }
    }

    private fun updateDotsIndicator(selectedPosition: Int) {
        val density = resources.displayMetrics.density
        for (i in 0 until binding.layoutDotsIndicator.childCount) {
            val dot = binding.layoutDotsIndicator.getChildAt(i)
            val isSelected = i == selectedPosition
            val params = dot.layoutParams as LinearLayout.LayoutParams
            params.width = if (isSelected) (16 * density).toInt() else (6 * density).toInt()
            dot.layoutParams = params
            dot.background = ContextCompat.getDrawable(
                this,
                if (isSelected) CoreUiR.drawable.bg_pill_badge_role else CoreUiR.drawable.bg_circle_icon_button
            )
            dot.backgroundTintList = ColorStateList.valueOf(
                if (isSelected) Color.WHITE else Color.parseColor("#80FFFFFF")
            )
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnSingleClickListener {
            finish()
        }

        binding.btnFavorite.setOnSingleClickListener {
            viewModel.toggleFavorite()
        }

        binding.btnRetry.setOnSingleClickListener {
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

        binding.btnChatLandlord.setOnSingleClickListener {
            viewModel.contactLandlord()
        }
    }

    private fun handleUiState(state: UiState<RoomDetailResponseDto>) {
        when (state) {
            is UiState.Loading -> {
                binding.progressBar.isVisible = true
                binding.layoutContent.isVisible = false
                binding.layoutError.isVisible = false
            }
            is UiState.Error -> {
                binding.progressBar.isVisible = false
                binding.layoutContent.isVisible = false
                binding.layoutError.isVisible = true
                binding.tvErrorMessage.text = state.message
            }
            is UiState.Success -> {
                binding.progressBar.isVisible = false
                binding.layoutError.isVisible = false
                binding.layoutContent.isVisible = true
                bindRoomDetail(state.data)
            }
            is UiState.Idle -> Unit
        }
    }

    private fun bindRoomDetail(room: RoomDetailResponseDto) {
        // Image carousel
        imageAdapter.submitList(room.photoUrls)
        val totalImages = imageAdapter.itemCount
        binding.tvImageIndicator.text = "1 / $totalImages"
        setupDotsIndicator(totalImages)
        if (totalImages > 1) {
            startAutoScroll()
        }

        // Status badge
        bindStatusBadge(room.status)

        // Header info
        binding.tvBuildingNameHeader.text = room.buildingName
        binding.tvRoomTitle.text = room.name
        binding.tvRentPrice.text = room.rentPrice.toVnd()
        binding.tvDepositAmount.text = "Cọc: ${room.depositAmount.toVnd()}"

        // Specs
        val parsedArea = room.area?.trim()?.toDoubleOrNull()
        val displayArea = if (parsedArea != null) {
            if (parsedArea % 1.0 == 0.0) "${parsedArea.toInt()} m²" else "$parsedArea m²"
        } else {
            "25 m²"
        }
        binding.tvSpecArea.text = displayArea
        binding.tvSpecFloor.text = if (room.floorNumber != null) "Tầng ${room.floorNumber}" else "Đang cập nhật"
        binding.tvSpecOccupancy.text = if (room.maxOccupancy != null) "Tối đa ${room.maxOccupancy} người" else "Không giới hạn"
        binding.tvSpecGender.text = when (room.genderPolicy?.lowercase()) {
            "male" -> "Chỉ nam"
            "female" -> "Chỉ nữ"
            else -> "Nam & Nữ"
        }

        // Amenities chips
        bindAmenities(room)

        // Rates & utilities
        binding.tvElectricityPolicy.text = when (room.effectiveRatePolicySummary?.electricity?.lowercase()) {
            "flat" -> "Giá cố định"
            else -> "Theo công tơ"
        }
        binding.tvWaterPolicy.text = when (room.effectiveRatePolicySummary?.water?.lowercase()) {
            "flat" -> "Giá cố định"
            else -> "Theo khối tiêu thụ"
        }

        // Description
        val desc = room.description
        if (!desc.isNullOrBlank()) {
            binding.tvDescription.text = desc
            binding.btnToggleDescription.isVisible = desc.length > 150
        } else {
            binding.tvDescription.text = "Chưa có mô tả chi tiết cho phòng trọ này."
            binding.btnToggleDescription.isVisible = false
        }

        // Landlord card
        binding.tvLandlordName.text = room.landlord.name.ifBlank { "Chủ trọ" }
        val initial = room.landlord.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "C"
        binding.tvLandlordAvatar.text = initial
    }

    private fun bindStatusBadge(status: String) {
        val (text, bgColor, textColor) = when (status.lowercase()) {
            "available" -> Triple("Còn trống", Color.parseColor("#E8F5E9"), Color.parseColor("#2E7D32"))
            "occupied" -> Triple("Đã cho thuê", Color.parseColor("#ECEFF1"), Color.parseColor("#546E7A"))
            "maintenance" -> Triple("Đang bảo trì", Color.parseColor("#FFF3E0"), Color.parseColor("#E65100"))
            "cleaning", "pending_cleanup" -> Triple("Chờ dọn dẹp", Color.parseColor("#E3F2FD"), Color.parseColor("#1565C0"))
            else -> Triple(status, Color.parseColor("#ECEFF1"), Color.parseColor("#546E7A"))
        }
        binding.tvRoomStatus.text = text
        binding.layoutRoomStatusBadge.backgroundTintList = ColorStateList.valueOf(bgColor)
        binding.tvRoomStatus.setTextColor(textColor)
        binding.ivStatusDot.imageTintList = ColorStateList.valueOf(textColor)
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
            list.add(AmenityItem("Tiện nghi cơ bản", CoreUiR.drawable.ic_ai_sparkle))
        }

        for (item in list) {
            val chip = Chip(this).apply {
                text = item.label
                isClickable = false
                isCheckable = false
                setChipIconResource(item.iconRes)
                chipIconTint = ColorStateList.valueOf(ContextCompat.getColor(this@RoomDetailActivity, CoreUiR.color.rentify_primary))
                chipIconSize = resources.displayMetrics.density * 18f
                iconStartPadding = resources.displayMetrics.density * 8f
                chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(this@RoomDetailActivity, CoreUiR.color.rentify_surface))
                setTextColor(ContextCompat.getColor(this@RoomDetailActivity, CoreUiR.color.rentify_text_primary))
                chipStrokeColor = ColorStateList.valueOf(ContextCompat.getColor(this@RoomDetailActivity, CoreUiR.color.rentify_divider))
                chipStrokeWidth = 1f
                textSize = 13f
            }
            binding.chipGroupAmenities.addView(chip)
        }
    }

    private fun updateFavoriteButton(isFav: Boolean) {
        if (isFav) {
            binding.btnFavorite.setImageResource(CoreUiR.drawable.ic_heart_filled)
            binding.btnFavorite.setColorFilter(ContextCompat.getColor(this, CoreUiR.color.rentify_card_saved_icon))
        } else {
            binding.btnFavorite.setImageResource(CoreUiR.drawable.ic_heart_outline)
            binding.btnFavorite.setColorFilter(ContextCompat.getColor(this, CoreUiR.color.rentify_neutral))
        }
    }

    private fun handleUiEvent(event: RoomDetailUiEvent) {
        when (event) {
            is RoomDetailUiEvent.ShowToast -> {
                toast(event.message)
            }
            is RoomDetailUiEvent.ShowLoginRequired -> {
                showRequireLoginDialog("Vui lòng đăng nhập để thực hiện tính năng này.") {
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

        fun start(context: Context, roomId: String, isFavorited: Boolean = false) {
            val intent = Intent(context, RoomDetailActivity::class.java).apply {
                putExtra(EXTRA_ROOM_ID, roomId)
                putExtra(EXTRA_IS_FAVORITED, isFavorited)
            }
            context.startActivity(intent)
        }
    }
}
