package com.rentify.app.landlord.ui.property

import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import com.rentify.app.core.network.dto.administrative.AdministrativeProvinceDto
import com.rentify.app.core.network.dto.administrative.AdministrativeWardDto
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.landlord.databinding.ActivityCreateBuildingBinding

class CreateBuildingActivity : BaseActivity<ActivityCreateBuildingBinding>(
    ActivityCreateBuildingBinding::inflate
) {
    private val viewModel: BuildingViewModel by viewModels {
        BuildingViewModelFactory(this)
    }

    private var provincesList: List<AdministrativeProvinceDto> = emptyList()
    private var wardsList: List<AdministrativeWardDto> = emptyList()

    private var selectedProvince: AdministrativeProvinceDto? = null
    private var selectedWard: AdministrativeWardDto? = null

    override fun initView() {
        binding.rootLayout.applySystemBarsPadding(top = true, bottom = true)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.actvProvince.setOnItemClickListener { _, _, position, _ ->
            val province = provincesList.getOrNull(position) ?: return@setOnItemClickListener
            selectedProvince = province
            selectedWard = null
            binding.actvWard.setText("", false)
            binding.actvWard.isEnabled = true
            viewModel.loadWards(province.code)
        }

        binding.actvWard.setOnItemClickListener { _, _, position, _ ->
            selectedWard = wardsList.getOrNull(position)
        }

        binding.btnSubmit.setOnClickListener {
            submitBuilding()
        }

        viewModel.loadProvinces()
    }

    override fun observeData() {
        viewModel.provincesState.collectWhenStarted { provinces ->
            provincesList = provinces
            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                provinces.map { it.name }
            )
            binding.actvProvince.setAdapter(adapter)
        }

        viewModel.wardsState.collectWhenStarted { wards ->
            wardsList = wards
            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                wards.map { it.name }
            )
            binding.actvWard.setAdapter(adapter)
        }

        viewModel.actionState.collectWhenStarted { state ->
            when (state) {
                is BuildingActionUiState.Loading -> {
                    binding.btnSubmit.isEnabled = false
                    binding.progressBar.visibility = android.view.View.VISIBLE
                }
                is BuildingActionUiState.Success -> {
                    binding.btnSubmit.isEnabled = true
                    binding.progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    viewModel.resetActionState()
                    setResult(RESULT_OK)
                    finish()
                }
                is BuildingActionUiState.Error -> {
                    binding.btnSubmit.isEnabled = true
                    binding.progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    viewModel.resetActionState()
                }
                is BuildingActionUiState.Idle -> {
                    binding.btnSubmit.isEnabled = true
                    binding.progressBar.visibility = android.view.View.GONE
                }
            }
        }
    }

    private fun submitBuilding() {
        val name = binding.etBuildingName.text?.toString()?.trim() ?: ""
        val province = selectedProvince?.name ?: ""
        val ward = selectedWard?.name ?: ""
        val address = binding.etAddress.text?.toString()?.trim() ?: ""

        if (name.isEmpty()) {
            binding.tilBuildingName.error = "Vui lòng nhập tên tòa nhà"
            return
        } else {
            binding.tilBuildingName.error = null
        }

        if (province.isEmpty()) {
            binding.tilProvince.error = "Vui lòng chọn Tỉnh/Thành phố"
            return
        } else {
            binding.tilProvince.error = null
        }

        if (ward.isEmpty()) {
            binding.tilWard.error = "Vui lòng chọn Phường/Xã"
            return
        } else {
            binding.tilWard.error = null
        }

        if (address.isEmpty()) {
            binding.tilAddress.error = "Vui lòng nhập địa chỉ cụ thể"
            return
        } else {
            binding.tilAddress.error = null
        }

        viewModel.createBuilding(
            name = name,
            province = province,
            ward = ward,
            address = address
        )
    }
}
