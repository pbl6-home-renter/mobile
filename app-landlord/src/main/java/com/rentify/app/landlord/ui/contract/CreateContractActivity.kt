package com.rentify.app.landlord.ui.contract

import android.app.DatePickerDialog
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import com.rentify.app.core.network.dto.building.BuildingMyListResponseDto
import com.rentify.app.core.network.dto.contract.ContractCreateRequestDto
import com.rentify.app.core.network.dto.room.RoomMyListResponseDto
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.landlord.databinding.ActivityCreateContractBinding
import com.rentify.app.landlord.ui.property.BuildingViewModel
import com.rentify.app.landlord.ui.property.BuildingViewModelFactory
import com.rentify.app.landlord.ui.property.RoomListUiState
import com.rentify.app.landlord.ui.property.RoomViewModel
import com.rentify.app.landlord.ui.property.RoomViewModelFactory
import java.util.Calendar

class CreateContractActivity : BaseActivity<ActivityCreateContractBinding>(
    ActivityCreateContractBinding::inflate
) {
    private val contractViewModel: ContractViewModel by viewModels {
        ContractViewModelFactory(this)
    }

    private val buildingViewModel: BuildingViewModel by viewModels {
        BuildingViewModelFactory(this)
    }

    private val roomViewModel: RoomViewModel by viewModels {
        RoomViewModelFactory(this)
    }

    private var buildingsList: List<BuildingMyListResponseDto> = emptyList()
    private var roomsList: List<RoomMyListResponseDto> = emptyList()

    private var selectedRoomId: String? = null

    override fun initView() {
        binding.rootLayout.applySystemBarsPadding(top = true, bottom = true)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        setupDropdowns()
        setupDatePickers()

        binding.btnSubmitContract.setOnClickListener {
            submitContract()
        }

        buildingViewModel.loadBuildings(status = "approved")
    }

    private fun setupDropdowns() {
        binding.actvBuilding.setOnItemClickListener { _, _, position, _ ->
            val building = buildingsList.getOrNull(position) ?: return@setOnItemClickListener
            selectedRoomId = null
            binding.actvRoom.setText("", false)
            binding.actvRoom.isEnabled = true
            roomViewModel.loadRooms(buildingId = building.id, status = "available")
        }

        binding.actvRoom.setOnItemClickListener { _, _, position, _ ->
            val room = roomsList.getOrNull(position) ?: return@setOnItemClickListener
            selectedRoomId = room.id
            binding.etRentPrice.setText(room.rentPrice.toString())
            binding.etDepositAmount.setText(room.rentPrice.toString())
        }
    }

    private fun setupDatePickers() {
        binding.etStartDate.setOnClickListener {
            showDatePicker { dateStr -> binding.etStartDate.setText(dateStr) }
        }
        binding.etEndDate.setOnClickListener {
            showDatePicker { dateStr -> binding.etEndDate.setText(dateStr) }
        }
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val formatted = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
            onDateSelected(formatted)
        }, year, month, day).show()
    }

    override fun observeData() {
        buildingViewModel.buildingListState.collectWhenStarted { state ->
            if (state is com.rentify.app.landlord.ui.property.BuildingListUiState.Success) {
                buildingsList = state.buildings
                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    state.buildings.map { it.name }
                )
                binding.actvBuilding.setAdapter(adapter)
            }
        }

        roomViewModel.roomListState.collectWhenStarted { state ->
            if (state is RoomListUiState.Success) {
                roomsList = state.rooms
                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    state.rooms.map { "Phòng ${it.name}" }
                )
                binding.actvRoom.setAdapter(adapter)
            }
        }

        contractViewModel.actionState.collectWhenStarted { state ->
            when (state) {
                is ContractActionUiState.Loading -> {
                    binding.btnSubmitContract.isEnabled = false
                    binding.progressBar.visibility = View.VISIBLE
                }
                is ContractActionUiState.Success -> {
                    binding.btnSubmitContract.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    contractViewModel.resetActionState()
                    setResult(RESULT_OK)
                    finish()
                }
                is ContractActionUiState.Error -> {
                    binding.btnSubmitContract.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    contractViewModel.resetActionState()
                }
                is ContractActionUiState.Idle -> {
                    binding.btnSubmitContract.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun submitContract() {
        val roomId = selectedRoomId
        if (roomId.isNullOrEmpty()) {
            binding.tilRoom.error = "Vui lòng chọn phòng trống"
            return
        } else {
            binding.tilRoom.error = null
        }

        val tenantName = binding.etTenantName.text?.toString()?.trim() ?: ""
        if (tenantName.isEmpty()) {
            binding.tilTenantName.error = "Vui lòng nhập họ tên người thuê"
            return
        } else {
            binding.tilTenantName.error = null
        }

        val tenantPhone = binding.etTenantPhone.text?.toString()?.trim() ?: ""
        if (tenantPhone.isEmpty()) {
            binding.tilTenantPhone.error = "Vui lòng nhập số điện thoại"
            return
        } else {
            binding.tilTenantPhone.error = null
        }

        val rentPrice = binding.etRentPrice.text?.toString()?.trim()?.toDoubleOrNull()
        if (rentPrice == null || rentPrice < 0) {
            binding.tilRentPrice.error = "Vui lòng nhập giá thuê hợp lệ"
            return
        } else {
            binding.tilRentPrice.error = null
        }

        val depositAmount = binding.etDepositAmount.text?.toString()?.trim()?.toDoubleOrNull()
        if (depositAmount == null || depositAmount < 0) {
            binding.tilDepositAmount.error = "Vui lòng nhập tiền cọc hợp lệ"
            return
        } else {
            binding.tilDepositAmount.error = null
        }

        val startDate = binding.etStartDate.text?.toString()?.trim() ?: ""
        if (startDate.isEmpty()) {
            binding.tilStartDate.error = "Vui lòng chọn ngày bắt đầu"
            return
        } else {
            binding.tilStartDate.error = null
        }

        val endDate = binding.etEndDate.text?.toString()?.trim() ?: ""
        if (endDate.isEmpty()) {
            binding.tilEndDate.error = "Vui lòng chọn ngày kết thúc"
            return
        } else {
            binding.tilEndDate.error = null
        }

        val vehicleCount = binding.etVehicleCount.text?.toString()?.trim()?.toIntOrNull() ?: 1

        val request = ContractCreateRequestDto(
            roomId = roomId,
            tenantName = tenantName,
            tenantPhone = tenantPhone,
            rentPrice = rentPrice,
            depositAmount = depositAmount,
            startDate = startDate,
            endDate = endDate,
            vehicleCount = vehicleCount
        )

        contractViewModel.createContract(request)
    }
}

