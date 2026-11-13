package com.rentify.app.landlord.ui.property

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.rentify.app.core.network.dto.building.BuildingMyDetailResponseDto
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.landlord.R
import com.rentify.app.landlord.databinding.ActivityBuildingDetailBinding

class BuildingDetailActivity : BaseActivity<ActivityBuildingDetailBinding>(
    ActivityBuildingDetailBinding::inflate
) {
    private val viewModel: BuildingViewModel by viewModels {
        BuildingViewModelFactory(this)
    }

    private var buildingId: String = ""
    private var currentDetail: BuildingMyDetailResponseDto? = null

    override fun initView() {
        binding.rootLayout.applySystemBarsPadding(top = true, bottom = true)

        buildingId = intent.getStringExtra(EXTRA_BUILDING_ID) ?: ""
        if (buildingId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy mã tòa nhà", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadBuildingDetail(buildingId)
        }

        binding.btnConfigVietQr.setOnClickListener {
            showVietQrConfigBottomSheet()
        }

        binding.btnEditBuilding.setOnClickListener {
            showEditBuildingDialog()
        }

        binding.btnDeleteBuilding.setOnClickListener {
            showDeleteConfirmDialog()
        }

        viewModel.loadBuildingDetail(buildingId)
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

        viewModel.actionState.collectWhenStarted { state ->
            when (state) {
                is BuildingActionUiState.Loading -> {
                    binding.detailProgressBar.visibility = View.VISIBLE
                }
                is BuildingActionUiState.Success -> {
                    binding.detailProgressBar.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    viewModel.resetActionState()
                    if (state.message.contains("Xóa")) {
                        setResult(RESULT_OK)
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
    }

    private fun bindBuildingDetail(building: BuildingMyDetailResponseDto) {
        binding.tvDetailName.text = building.name
        val fullAddr = buildString {
            append(building.address)
            if (building.ward.isNotBlank()) append(", ${building.ward}")
            if (building.province.isNotBlank()) append(", ${building.province}")
        }
        binding.tvDetailFullAddress.text = fullAddr
        binding.tvDetailOccupiedRooms.text = "${building.occupiedRooms} phòng"
        binding.tvDetailOccupancyRate.text = building.occupancyRate

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

    private fun showEditBuildingDialog() {
        val current = currentDetail ?: return
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Chỉnh sửa Tòa nhà")

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        val etName = EditText(this).apply {
            hint = "Tên tòa nhà"
            setText(current.name)
        }
        val etAddress = EditText(this).apply {
            hint = "Địa chỉ chi tiết"
            setText(current.address)
        }

        layout.addView(etName)
        layout.addView(etAddress)
        builder.setView(layout)

        builder.setPositiveButton("Lưu") { _, _ ->
            val newName = etName.text.toString().trim()
            val newAddress = etAddress.text.toString().trim()
            viewModel.updateBuilding(buildingId, newName, newAddress)
        }
        builder.setNegativeButton("Hủy", null)
        builder.show()
    }

    private fun showDeleteConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc chắn muốn xóa tòa nhà này không? Hành động này không thể hoàn tác.")
            .setPositiveButton("Xóa") { _, _ ->
                viewModel.deleteBuilding(buildingId)
            }
            .setNegativeButton("Hủy", null)
            .show()
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
