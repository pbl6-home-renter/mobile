package com.rentify.app.landlord.ui.contract

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.rentify.app.core.network.dto.contract.ContractDetailResponseDto
import com.rentify.app.core.network.dto.contract.ContractMemberAddRequestDto
import com.rentify.app.core.network.dto.contract.ContractMemberLeaveRequestDto
import com.rentify.app.core.network.dto.contract.DepositSettlementDeductionDto
import com.rentify.app.core.network.dto.contract.DepositSettlementRequestDto
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.landlord.R
import com.rentify.app.landlord.databinding.ActivityLandlordContractDetailBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class LandlordContractDetailActivity : BaseActivity<ActivityLandlordContractDetailBinding>(
    ActivityLandlordContractDetailBinding::inflate
) {
    private val viewModel: ContractViewModel by viewModels {
        ContractViewModelFactory(this)
    }

    private var contractId: String = ""
    private var currentContract: ContractDetailResponseDto? = null
    private lateinit var memberAdapter: ContractMemberAdapter

    override fun initView() {
        binding.rootLayout.applySystemBarsPadding(top = true, bottom = true)

        contractId = intent.getStringExtra(EXTRA_CONTRACT_ID) ?: ""
        if (contractId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy mã hợp đồng", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        setupMembersRecyclerView()

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadContractDetail(contractId)
            viewModel.loadContractMembers(contractId)
        }

        binding.btnAddMember.setOnClickListener {
            showAddMemberBottomSheet()
        }

        binding.btnActivateContract.setOnClickListener {
            showActivateConfirmDialog()
        }

        binding.btnCheckoutAndSettle.setOnClickListener {
            showDepositSettlementBottomSheet()
        }

        viewModel.loadContractDetail(contractId)
        viewModel.loadContractMembers(contractId)
    }

    private fun setupMembersRecyclerView() {
        memberAdapter = ContractMemberAdapter { member ->
            val tenantId = member.tenantId ?: return@ContractMemberAdapter
            showConfirmLeaveDialog(tenantId, member.tenantName ?: "thành viên")
        }

        binding.rvContractMembers.apply {
            layoutManager = LinearLayoutManager(this@LandlordContractDetailActivity)
            adapter = memberAdapter
        }
    }

    override fun observeData() {
        viewModel.contractDetailState.collectWhenStarted { state ->
            binding.swipeRefresh.isRefreshing = false
            when (state) {
                is ContractDetailUiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is ContractDetailUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    currentContract = state.contract
                    bindContractDetail(state.contract)
                }
                is ContractDetailUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                is ContractDetailUiState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }

        viewModel.contractMembersState.collectWhenStarted { state ->
            when (state) {
                is ContractMembersUiState.Success -> {
                    memberAdapter.submitList(state.members)
                    binding.tvEmptyMembers.visibility = if (state.members.isEmpty()) View.VISIBLE else View.GONE
                }
                else -> {}
            }
        }

        viewModel.actionState.collectWhenStarted { state ->
            when (state) {
                is ContractActionUiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is ContractActionUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    viewModel.resetActionState()
                    viewModel.loadContractDetail(contractId)
                }
                is ContractActionUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    viewModel.resetActionState()
                }
                is ContractActionUiState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun bindContractDetail(contract: ContractDetailResponseDto) {
        binding.tvDetailContractCode.text = "HĐ-${contract.id.take(6)}"

        val (badgeText, textColor, bgColor) = when (contract.status.lowercase()) {
            "active" -> Triple("Đang hiệu lực", Color.parseColor("#16A34A"), Color.parseColor("#DCFCE7"))
            "signed" -> Triple("Đã ký", Color.parseColor("#2563EB"), Color.parseColor("#EFF6FF"))
            "draft" -> Triple("Nháp", Color.parseColor("#64748B"), Color.parseColor("#F1F5F9"))
            "suspended" -> Triple("Tạm dừng", Color.parseColor("#D97706"), Color.parseColor("#FEF3C7"))
            "expired" -> Triple("Hết hạn", Color.parseColor("#7C3AED"), Color.parseColor("#F3E8FF"))
            "terminated" -> Triple("Đã thanh lý", Color.parseColor("#DC2626"), Color.parseColor("#FEE2E2"))
            else -> Triple(contract.status, Color.parseColor("#64748B"), Color.parseColor("#F1F5F9"))
        }

        binding.tvDetailStatusBadge.text = badgeText
        binding.tvDetailStatusBadge.setTextColor(textColor)
        binding.tvDetailStatusBadge.background = GradientDrawable().apply {
            setColor(bgColor)
            cornerRadius = 16f
        }

        binding.tvDetailRoomName.text = "Phòng (Mã: ${contract.roomId.take(8)})"
        val name = contract.tenantName ?: "Chưa rõ tên"
        val phone = contract.tenantPhone ?: "Chưa rõ SĐT"
        binding.tvDetailTenantInfo.text = "Khách thuê: $name • SĐT: $phone"

        val priceStr = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(contract.rentPrice.toLong())
        val depositStr = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(contract.depositAmount.toLong())
        binding.tvDetailRentPrice.text = "$priceStr đ"
        binding.tvDetailDepositAmount.text = "$depositStr đ"

        binding.tvDetailDuration.text = "Thời hạn: ${contract.startDate} → ${contract.endDate}"
        binding.tvDetailVehicleCount.text = "Số xe máy gửi: ${contract.vehicleCount ?: 0} xe"

        when (contract.status.lowercase()) {
            "draft", "signed" -> {
                binding.btnActivateContract.visibility = View.VISIBLE
                binding.btnCheckoutAndSettle.visibility = View.GONE
            }
            "active" -> {
                binding.btnActivateContract.visibility = View.GONE
                binding.btnCheckoutAndSettle.visibility = View.VISIBLE
            }
            else -> {
                binding.btnActivateContract.visibility = View.GONE
                binding.btnCheckoutAndSettle.visibility = View.GONE
            }
        }
    }

    private fun showActivateConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("Kích hoạt Hợp đồng")
            .setMessage("Xác nhận đã hoàn tất ký hợp đồng với khách thuê và đưa phòng vào trạng thái đang thuê?")
            .setPositiveButton("Kích hoạt") { _, _ ->
                val mockMediaId = UUID.randomUUID().toString()
                viewModel.activateContract(contractId, signedContractMediaId = mockMediaId)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showAddMemberBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_add_member, null)
        dialog.setContentView(sheetView)

        val etName = sheetView.findViewById<TextInputEditText>(R.id.etMemberName)
        val etPhone = sheetView.findViewById<TextInputEditText>(R.id.etMemberPhone)
        val etJoinedAt = sheetView.findViewById<TextInputEditText>(R.id.etMemberJoinedAt)
        val btnSubmit = sheetView.findViewById<Button>(R.id.btnSubmitAddMember)
        val pbLoading = sheetView.findViewById<ProgressBar>(R.id.pbAddMemberLoading)

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        etJoinedAt.setText(today)

        btnSubmit.setOnClickListener {
            val name = etName.text?.toString()?.trim() ?: ""
            val phone = etPhone.text?.toString()?.trim() ?: ""
            val joinedAt = etJoinedAt.text?.toString()?.trim() ?: ""

            if (name.isEmpty() || joinedAt.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên và ngày vào ở", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSubmit.isEnabled = false
            pbLoading.visibility = View.VISIBLE

            val req = ContractMemberAddRequestDto(
                contractId = contractId,
                tenantName = name,
                tenantPhone = phone.ifEmpty { null },
                joinedAt = joinedAt
            )
            viewModel.addContractMember(req)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showConfirmLeaveDialog(tenantId: String, memberName: String) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        AlertDialog.Builder(this)
            .setTitle("Xác nhận rời phòng")
            .setMessage("Ghi nhận thành viên '$memberName' rời khỏi phòng vào ngày hôm nay ($today)?")
            .setPositiveButton("Đồng ý") { _, _ ->
                viewModel.leaveContract(tenantId, ContractMemberLeaveRequestDto(contractId, leftOn = today))
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showDepositSettlementBottomSheet() {
        val contract = currentContract ?: return
        val dialog = BottomSheetDialog(this)
        val sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_deposit_settlement, null)
        dialog.setContentView(sheetView)

        val etDeposit = sheetView.findViewById<TextInputEditText>(R.id.etOriginalDeposit)
        val etDeductions = sheetView.findViewById<TextInputEditText>(R.id.etDeductionsAmount)
        val etReason = sheetView.findViewById<TextInputEditText>(R.id.etDeductionsReason)
        val etRefund = sheetView.findViewById<TextInputEditText>(R.id.etRefundAmount)
        val actvPayment = sheetView.findViewById<AutoCompleteTextView>(R.id.actvPaymentMethod)
        val etNote = sheetView.findViewById<TextInputEditText>(R.id.etSettlementNote)
        val btnSubmit = sheetView.findViewById<Button>(R.id.btnSubmitSettlement)
        val pbLoading = sheetView.findViewById<ProgressBar>(R.id.pbSettlementLoading)

        etDeposit.setText(contract.depositAmount.toLong().toString())
        etRefund.setText(contract.depositAmount.toLong().toString())

        val paymentOptions = listOf("Tiền mặt" to "CASH", "Chuyển khoản" to "BANK_TRANSFER")
        val paymentAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, paymentOptions.map { it.first })
        actvPayment.setAdapter(paymentAdapter)
        actvPayment.setText(paymentOptions[0].first, false)

        btnSubmit.setOnClickListener {
            val deposit = etDeposit.text?.toString()?.trim()?.toDoubleOrNull() ?: contract.depositAmount
            val deductions = etDeductions.text?.toString()?.trim()?.toDoubleOrNull() ?: 0.0
            val reason = etReason.text?.toString()?.trim() ?: "Khấu trừ chi phí phòng"
            val refund = etRefund.text?.toString()?.trim()?.toDoubleOrNull() ?: (deposit - deductions)
            val note = etNote.text?.toString()?.trim()

            btnSubmit.isEnabled = false
            pbLoading.visibility = View.VISIBLE

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            viewModel.completeCheckout(contractId, completedOn = today)

            val settlementReq = DepositSettlementRequestDto(
                contractId = contractId,
                originalDeposit = deposit,
                deductions = if (deductions > 0) listOf(DepositSettlementDeductionDto("other", deductions, reason)) else emptyList(),
                refundAmount = refund,
                paymentMethod = "CASH",
                note = note
            )
            viewModel.depositSettlement(contractId, settlementReq)
            dialog.dismiss()
        }

        dialog.show()
    }

    companion object {
        const val EXTRA_CONTRACT_ID = "extra_contract_id"

        fun newIntent(context: Context, contractId: String): Intent {
            return Intent(context, LandlordContractDetailActivity::class.java).apply {
                putExtra(EXTRA_CONTRACT_ID, contractId)
            }
        }
    }
}

