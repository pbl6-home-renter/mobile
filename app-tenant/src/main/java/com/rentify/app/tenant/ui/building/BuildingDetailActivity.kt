package com.rentify.app.tenant.ui.building

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.rentify.app.core.ui.R as CoreUiR
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.core.ui.extension.toVnd
import com.rentify.app.core.ui.state.UiState
import com.rentify.app.tenant.data.repository.BuildingSummary
import com.rentify.app.tenant.databinding.ActivityBuildingDetailBinding
import com.rentify.app.tenant.ui.explore.RoomForYouAdapter
import com.rentify.app.tenant.ui.room.detail.RoomDetailActivity
import kotlinx.coroutines.launch

class BuildingDetailActivity : BaseActivity<ActivityBuildingDetailBinding>(ActivityBuildingDetailBinding::inflate) {

    private val buildingId: String by lazy { intent.getStringExtra(EXTRA_BUILDING_ID).orEmpty() }

    private val viewModel: BuildingDetailViewModel by viewModels {
        BuildingDetailViewModelFactory(this, buildingId)
    }

    private val roomsAdapter by lazy {
        BuildingRoomsAdapter { room ->
            val fullAddress = listOf(room.address, room.ward, room.province)
                .filter { it.isNotBlank() }
                .joinToString(", ")
            RoomDetailActivity.start(
                context = this,
                roomId = room.id,
                isFavorited = room.isFavorited,
                fallbackAddress = fullAddress
            )
        }
    }

    override fun initView() {
        binding.root.applySystemBarsPadding(top = true, bottom = true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.rvRooms.layoutManager = LinearLayoutManager(this)
        binding.rvRooms.adapter = roomsAdapter
        binding.btnRetry.setOnSingleClickListener { viewModel.load() }
    }

    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state is UiState.Loading
                    binding.scrollContent.isVisible = state is UiState.Success
                    binding.layoutError.isVisible = state is UiState.Error
                    when (state) {
                        is UiState.Success -> bind(state.data)
                        is UiState.Error -> binding.tvError.text = state.message
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun bind(building: BuildingSummary) {
        binding.ivBuildingCover.load(RoomForYouAdapter.resolveImageUrl(building.coverUrl, 0)) {
            crossfade(true)
            placeholder(CoreUiR.drawable.ic_image_placeholder)
            error(CoreUiR.drawable.ic_image_placeholder)
        }
        binding.tvBuildingName.text = building.name
        binding.tvBuildingAddress.text = BuildingTextFormatter.fullAddress(building)
        binding.tvStatRooms.text = "${building.roomCount} phòng"
        binding.tvStatPrice.isVisible = building.roomCount > 0
        binding.tvStatPrice.text = "Từ ${building.minRentPrice.toVnd()}/tháng"
        roomsAdapter.submitList(building.rooms)
        binding.tvNoRooms.isVisible = building.rooms.isEmpty()
    }

    companion object {
        private const val EXTRA_BUILDING_ID = "extra_building_id"

        fun start(context: Context, buildingId: String) {
            context.startActivity(
                Intent(context, BuildingDetailActivity::class.java).putExtra(EXTRA_BUILDING_ID, buildingId)
            )
        }
    }
}
