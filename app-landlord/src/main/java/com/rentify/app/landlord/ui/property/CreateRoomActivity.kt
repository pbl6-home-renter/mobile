package com.rentify.app.landlord.ui.property

import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import com.rentify.app.core.network.dto.building.BuildingMyListResponseDto
import com.rentify.app.core.network.dto.room.RoomAmenitiesDto
import com.rentify.app.core.network.dto.room.RoomCreateRequestDto
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.landlord.databinding.ActivityCreateRoomBinding

class CreateRoomActivity : BaseActivity<ActivityCreateRoomBinding>(
    ActivityCreateRoomBinding::inflate
) {
    private val viewModel: RoomViewModel by viewModels {
        RoomViewModelFactory(this)
    }

    private var buildingsList: List<BuildingMyListResponseDto> = emptyList()
    private var selectedBuildingId: String? = null

    private val genderPolicyOptions = listOf(
        "Tất cả (Không giới tính)" to "any",
        "Chỉ cho Nam thuê" to "male",
        "Chỉ cho Nữ thuê" to "female"
    )
    private var selectedGenderPolicy: String = "any"

    override fun initView() {
        binding.rootLayout.applySystemBarsPadding(top = true, bottom = true)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        setupDropdowns()

        binding.btnSubmitRoom.setOnClickListener {
            submitRoom()
        }

        viewModel.loadApprovedBuildings()
    }

    private fun setupDropdowns() {
        binding.actvBuilding.setOnItemClickListener { _, _, position, _ ->
            selectedBuildingId = buildingsList.getOrNull(position)?.id
        }

        val genderAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            genderPolicyOptions.map { it.first }
        )
        binding.actvGenderPolicy.setAdapter(genderAdapter)
        binding.actvGenderPolicy.setText(genderPolicyOptions[0].first, false)
        binding.actvGenderPolicy.setOnItemClickListener { _, _, position, _ ->
            selectedGenderPolicy = genderPolicyOptions.getOrNull(position)?.second ?: "any"
        }
    }

    override fun observeData() {
        viewModel.approvedBuildings.collectWhenStarted { buildings ->
            buildingsList = buildings
            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                buildings.map { it.name }
            )
            binding.actvBuilding.setAdapter(adapter)
            if (buildings.isNotEmpty() && selectedBuildingId == null) {
                binding.actvBuilding.setText(buildings[0].name, false)
                selectedBuildingId = buildings[0].id
            }
        }

        viewModel.actionState.collectWhenStarted { state ->
            when (state) {
                is RoomActionUiState.Loading -> {
                    binding.btnSubmitRoom.isEnabled = false
                    binding.progressBar.visibility = View.VISIBLE
                }
                is RoomActionUiState.Success -> {
                    binding.btnSubmitRoom.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    viewModel.resetActionState()
                    setResult(RESULT_OK)
                    finish()
                }
                is RoomActionUiState.Error -> {
                    binding.btnSubmitRoom.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    viewModel.resetActionState()
                }
                is RoomActionUiState.Idle -> {
                    binding.btnSubmitRoom.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun submitRoom() {
        val buildingId = selectedBuildingId
        if (buildingId.isNullOrEmpty()) {
            binding.tilBuilding.error = "Vui lòng chọn tòa nhà"
            return
        } else {
            binding.tilBuilding.error = null
        }

        val name = binding.etRoomName.text?.toString()?.trim() ?: ""
        if (name.isEmpty()) {
            binding.tilRoomName.error = "Vui lòng nhập tên/số phòng"
            return
        } else {
            binding.tilRoomName.error = null
        }

        val floorNumberStr = binding.etFloorNumber.text?.toString()?.trim() ?: ""
        val floorNumber = floorNumberStr.toIntOrNull()
        if (floorNumber == null || floorNumber < 1) {
            binding.tilFloorNumber.error = "Vui lòng nhập số tầng hợp lệ"
            return
        } else {
            binding.tilFloorNumber.error = null
        }

        val area = binding.etArea.text?.toString()?.trim() ?: ""
        if (area.isEmpty()) {
            binding.tilArea.error = "Vui lòng nhập diện tích"
            return
        } else {
            binding.tilArea.error = null
        }

        val rentPriceStr = binding.etRentPrice.text?.toString()?.trim() ?: ""
        val rentPrice = rentPriceStr.toLongOrNull()
        if (rentPrice == null || rentPrice < 0) {
            binding.tilRentPrice.error = "Vui lòng nhập giá thuê hợp lệ"
            return
        } else {
            binding.tilRentPrice.error = null
        }

        val depositStr = binding.etDepositAmount.text?.toString()?.trim() ?: ""
        val depositAmount = depositStr.toLongOrNull()
        if (depositAmount == null || depositAmount < 0) {
            binding.tilDepositAmount.error = "Vui lòng nhập tiền cọc hợp lệ"
            return
        } else {
            binding.tilDepositAmount.error = null
        }

        val maxOccupancy = binding.etMaxOccupancy.text?.toString()?.trim()?.toIntOrNull()

        val amenities = RoomAmenitiesDto(
            aircon = binding.cbAircon.isChecked,
            fridge = binding.cbFridge.isChecked,
            wcInside = binding.cbWcInside.isChecked,
            waterHeater = binding.cbWaterHeater.isChecked,
            mezzanine = binding.cbMezzanine.isChecked,
            table = binding.cbTable.isChecked,
            chair = binding.cbChair.isChecked
        )

        val request = RoomCreateRequestDto(
            buildingId = buildingId,
            name = name,
            floorNumber = floorNumber,
            area = area,
            rentPrice = rentPrice,
            depositAmount = depositAmount,
            maxOccupancy = maxOccupancy,
            genderPolicy = selectedGenderPolicy,
            amenities = amenities,
            photoMediaIds = emptyList()
        )

        viewModel.createRoom(request)
    }
}

