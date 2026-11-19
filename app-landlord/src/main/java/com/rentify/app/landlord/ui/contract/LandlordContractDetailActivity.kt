package com.rentify.app.landlord.ui.contract

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.rentify.app.core.network.dto.contract.ContractDetailResponseDto
import com.rentify.app.core.network.dto.contract.ContractMemberAddRequestDto
import com.rentify.app.core.network.dto.contract.ContractMemberLeaveRequestDto
import com.rentify.app.core.network.dto.contract.ContractTemplateItemDto
import com.rentify.app.core.network.dto.contract.DepositSettlementDeductionDto
import com.rentify.app.core.network.dto.contract.DepositSettlementPayoutAccountDto
import com.rentify.app.core.network.dto.contract.DepositSettlementRequestDto
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.toDisplayDate
import com.rentify.app.landlord.R
import com.rentify.app.landlord.databinding.ActivityLandlordContractDetailBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LandlordContractDetailActivity : BaseActivity<ActivityLandlordContractDetailBinding>(
    ActivityLandlordContractDetailBinding::inflate
) {
    private val viewModel: ContractViewModel by viewModels {
        ContractViewModelFactory(this)
    }

    private var contractId: String = ""
    private var roomName: String = ""
    private var currentContract: ContractDetailResponseDto? = null
    private var hasMutated: Boolean = false
    private lateinit var memberAdapter: ContractMemberAdapter

    private var templatesList: List<ContractTemplateItemDto> = emptyList()
    private var selectedTemplateKey: String? = null
    private var uploadedSignedContractMediaId: String? = null
    private var pendingDraftPdfBytes: ByteArray? = null

    private val pickSignedFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null && contractId.isNotEmpty()) {
            handleSelectedSignedFile(uri)
        }
    }

    private val saveDraftPdfLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        val bytes = pendingDraftPdfBytes
        if (uri != null && bytes != null) {
            try {
                contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(bytes)
                }
                Toast.makeText(this, "Đã lưu file bản nháp PDF thành công!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Lỗi lưu file PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        pendingDraftPdfBytes = null
    }

    override fun initView() {
        binding.rootLayout.applySystemBarsPadding(top = true, bottom = true)

        contractId = intent.getStringExtra(EXTRA_CONTRACT_ID) ?: ""
        roomName = intent.getStringExtra(EXTRA_ROOM_NAME).orEmpty()
        if (contractId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy mã hợp đồng", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.toolbar.setNavigationOnClickListener {
            if (hasMutated) setResult(RESULT_OK)
            finish()
        }

        setupMembersRecyclerView()
        setupDocumentActions()

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadContractDetail(contractId)
            viewModel.loadContractMembers(contractId)
            viewModel.loadContractTemplates()
        }

        binding.btnEditContractMeta.setOnClickListener {
            showEditContractBottomSheet()
        }

        binding.btnAddMember.setOnClickListener {
            showAddMemberBottomSheet()
        }

        binding.btnCheckoutAndSettle.setOnClickListener {
            showDepositSettlementBottomSheet()
        }

        viewModel.loadContractDetail(contractId)
        viewModel.loadContractMembers(contractId)
        viewModel.loadContractTemplates()
    }

    private fun setupDocumentActions() {
        binding.actvContractTemplate.setOnItemClickListener { _, _, position, _ ->
            selectedTemplateKey = templatesList.getOrNull(position)?.templateKey
        }

        binding.btnApplyTemplate.setOnClickListener {
            val key = selectedTemplateKey
            if (key.isNullOrBlank()) {
                Toast.makeText(
                    this,
                    if (templatesList.isEmpty()) "Chưa có mẫu hợp đồng nào trên hệ thống" else "Vui lòng chọn một mẫu hợp đồng",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            viewModel.applyContractTemplate(contractId, key)
        }

        binding.btnGenerateDraftPdf.setOnClickListener {
            val appliedKey = currentContract?.templateKey
            if (appliedKey.isNullOrBlank()) {
                Toast.makeText(this, "Hãy áp dụng mẫu trước khi tạo bản nháp.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.generateDraftDocumentPdf(contractId) { pdfBytes ->
                pendingDraftPdfBytes = pdfBytes
                saveDraftPdfLauncher.launch("hop_dong_nhap_${contractId.take(6)}.pdf")
            }
        }

        binding.btnSelectSignedFile.setOnClickListener {
            pickSignedFileLauncher.launch(
                arrayOf("application/pdf", "image/jpeg", "image/png", "image/webp")
            )
        }

        binding.btnActivateContract.setOnClickListener {
            val mediaId = uploadedSignedContractMediaId
            if (mediaId.isNullOrBlank()) {
                Toast.makeText(
                    this,
                    "Vui lòng chọn và tải lên bản hợp đồng đã ký trước khi kích hoạt.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            showActivateConfirmDialog(mediaId)
        }
    }

    private fun setupMembersRecyclerView() {
        memberAdapter = ContractMemberAdapter { member ->
            val tenantId = member.tenantId
            if (tenantId.isNullOrBlank()) {
                Toast.makeText(
                    this,
                    "Thành viên chưa liên kết tài khoản hệ thống; vui lòng cập nhật qua ghi chú hợp đồng.",
                    Toast.LENGTH_LONG
                ).show()
                return@ContractMemberAdapter
            }
            showConfirmLeaveDialog(tenantId, member.tenantName ?: "thành viên")
        }

        binding.rvContractMembers.apply {
            layoutManager = LinearLayoutManager(this@LandlordContractDetailActivity)
            adapter = memberAdapter
        }
    }

    override fun observeData() {
        viewModel.contractTemplates.collectWhenStarted { templates ->
            templatesList = templates
            if (templates.isEmpty()) {
                val emptyAdapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    listOf("Không tìm thấy mẫu hợp đồng phù hợp")
                )
                binding.actvContractTemplate.setAdapter(emptyAdapter)
                selectedTemplateKey = null
            } else {
                val labels = templates.map { "${it.title} (${it.version})".trim() }
                val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, labels)
                binding.actvContractTemplate.setAdapter(adapter)
                val currentKey = currentContract?.templateKey
                val matchedIdx = templates.indexOfFirst { it.templateKey == currentKey }
                if (matchedIdx >= 0) {
                    binding.actvContractTemplate.setText(labels[matchedIdx], false)
                    selectedTemplateKey = templates[matchedIdx].templateKey
                } else if (selectedTemplateKey == null) {
                    binding.actvContractTemplate.setText(labels[0], false)
                    selectedTemplateKey = templates[0].templateKey
                }
            }
        }

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
                    val activeCount = state.members.count { it.status.equals("active", ignoreCase = true) }
                    binding.tvMembersTitleCount.text = "Thành viên hợp đồng • Đang ở ($activeCount)"
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
                    hasMutated = true
                    setResult(RESULT_OK)
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    viewModel.resetActionState()
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
        val codeText = "HĐ-${contract.id.take(6).uppercase()}"
        binding.toolbar.title = codeText
        binding.tvDetailContractCode.text = codeText

        val (badgeText, textColor, bgColor) = when (contract.status.lowercase()) {
            "active" -> Triple("Đang hiệu lực", Color.parseColor("#16A34A"), Color.parseColor("#DCFCE7"))
            "signed" -> Triple("Đã ký", Color.parseColor("#2563EB"), Color.parseColor("#EFF6FF"))
            "draft" -> Triple("Bản nháp", Color.parseColor("#D97706"), Color.parseColor("#FEF3C7"))
            "suspended" -> Triple("Tạm dừng", Color.parseColor("#D97706"), Color.parseColor("#FEF3C7"))
            "expired" -> Triple("Hết hạn", Color.parseColor("#7C3AED"), Color.parseColor("#F3E8FF"))
            "terminated" -> Triple("Đã chấm dứt", Color.parseColor("#DC2626"), Color.parseColor("#FEE2E2"))
            else -> Triple(contract.status, Color.parseColor("#64748B"), Color.parseColor("#F1F5F9"))
        }

        binding.tvDetailStatusBadge.text = badgeText
        binding.tvDetailStatusBadge.setTextColor(textColor)
        binding.tvDetailStatusBadge.background = GradientDrawable().apply {
            setColor(bgColor)
            cornerRadius = 16f
        }

        binding.tvDetailRoomName.text = roomName.takeIf { it.isNotBlank() }?.let { "Phòng $it" } ?: "Phòng (Mã: ${contract.roomId.take(8)})"
        val name = contract.tenantName ?: "Chưa rõ tên"
        val phone = contract.tenantPhone ?: "Chưa rõ SĐT"
        binding.tvDetailTenantInfo.text = "Người đại diện: $name • SĐT: $phone"

        val priceStr = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(contract.rentPrice.toLong())
        val depositStr = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(contract.depositAmount.toLong())
        binding.tvDetailRentPrice.text = "$priceStr đ"
        binding.tvDetailDepositAmount.text = "$depositStr đ"

        binding.tvDetailDuration.text = "Thời hạn hợp đồng: ${contract.startDate.toDisplayDate()} → ${contract.endDate.toDisplayDate()}"
        binding.tvDetailVehicleCount.text = "Số xe gửi: ${contract.vehicleCount ?: 0} xe"
        binding.tvDetailContractNote.text = "Ghi chú: ${contract.description?.takeIf { it.isNotBlank() } ?: "Không có ghi chú"}"

        val hasTemplate = !contract.templateKey.isNullOrBlank()
        binding.tvAppliedTemplateInfo.text = if (hasTemplate) {
            "Mẫu đã áp dụng: ${contract.templateKey}"
        } else {
            "Mẫu hiện tại: Chưa áp dụng mẫu hợp đồng"
        }
        binding.btnGenerateDraftPdf.isEnabled = hasTemplate
        binding.tvDraftPdfHint.text = if (hasTemplate) {
            "Bấm nút bên dưới để sinh và lưu file PDF bản nháp hợp đồng."
        } else {
            "Hãy áp dụng mẫu trước khi tạo bản nháp."
        }

        val isActive = contract.status.equals("active", ignoreCase = true)
        val isDraftOrSigned = contract.status.equals("draft", ignoreCase = true) ||
            contract.status.equals("signed", ignoreCase = true)

        binding.btnEditContractMeta.text = if (isDraftOrSigned) "Sửa hợp đồng" else "Sửa số xe & ghi chú"
        binding.btnCheckoutAndSettle.visibility = if (isActive) View.VISIBLE else View.GONE

        if (isActive) {
            binding.tvContractActiveNotice.visibility = View.VISIBLE
            binding.layoutUploadAndActivate.visibility = View.GONE
        } else if (isDraftOrSigned) {
            binding.tvContractActiveNotice.visibility = View.GONE
            binding.layoutUploadAndActivate.visibility = View.VISIBLE
        } else {
            binding.tvContractActiveNotice.visibility = View.GONE
            binding.layoutUploadAndActivate.visibility = View.GONE
        }
    }

    private fun showEditContractBottomSheet() {
        val contract = currentContract ?: return
        val dialog = BottomSheetDialog(this)
        val sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_edit_contract, null)
        dialog.setContentView(sheetView)

        val etVehicle = sheetView.findViewById<TextInputEditText>(R.id.etEditVehicleCount)
        val etNote = sheetView.findViewById<TextInputEditText>(R.id.etEditContractNote)
        val btnCancel = sheetView.findViewById<MaterialButton>(R.id.btnCancelEditContract)
        val btnSave = sheetView.findViewById<MaterialButton>(R.id.btnSaveEditContract)

        etVehicle.setText((contract.vehicleCount ?: 0).toString())
        etNote.setText(contract.description ?: "")

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val vehicleCount = etVehicle.text?.toString()?.trim()?.toIntOrNull()?.coerceAtLeast(0) ?: 0
            val description = etNote.text?.toString()?.trim()
            viewModel.updateContract(contractId, vehicleCount, description)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun handleSelectedSignedFile(uri: Uri) {
        try {
            val contentType = contentResolver.getType(uri) ?: "application/pdf"
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null || bytes.isEmpty()) {
                Toast.makeText(this, "Không thể đọc tệp hợp đồng đã chọn", Toast.LENGTH_SHORT).show()
                return
            }
            if (bytes.size > 10 * 1024 * 1024) {
                Toast.makeText(this, "Tệp vượt quá giới hạn 10 MB", Toast.LENGTH_LONG).show()
                return
            }
            val ext = when {
                contentType.contains("pdf") -> "pdf"
                contentType.contains("png") -> "png"
                contentType.contains("webp") -> "webp"
                else -> "jpg"
            }
            val fileName = "signed_contract_${contractId.take(6)}_${System.currentTimeMillis()}.$ext"
            viewModel.uploadSignedContractFile(
                contractId = contractId,
                fileBytes = bytes,
                fileName = fileName,
                contentType = contentType
            ) { mediaId ->
                uploadedSignedContractMediaId = mediaId
                binding.tvSignedFileStatus.text = "✓ Đã tải lên tệp hợp đồng đã ký ($fileName)"
                binding.tvSignedFileStatus.setTextColor(Color.parseColor("#16A34A"))
                binding.btnSelectSignedFile.text = "Đổi file bản ký khác"
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi đọc tệp: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showActivateConfirmDialog(mediaId: String) {
        AlertDialog.Builder(this)
            .setTitle("Kích hoạt Hợp đồng")
            .setMessage("Xác nhận kích hoạt hợp đồng này với bản ký đã tải lên và chuyển phòng sang trạng thái Đang thuê?")
            .setPositiveButton("Kích hoạt") { _, _ ->
                viewModel.activateContract(contractId, signedContractMediaId = mediaId)
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
                Toast.makeText(this, "Vui lòng nhập họ tên và ngày vào ở", Toast.LENGTH_SHORT).show()
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
        val layoutBankFields = sheetView.findViewById<LinearLayout>(R.id.layoutRefundBankFields)
        val tilBankCode = sheetView.findViewById<TextInputLayout>(R.id.tilRefundBankCode)
        val etBankCode = sheetView.findViewById<TextInputEditText>(R.id.etRefundBankCode)
        val tilAccNumber = sheetView.findViewById<TextInputLayout>(R.id.tilRefundAccountNumber)
        val etAccNumber = sheetView.findViewById<TextInputEditText>(R.id.etRefundAccountNumber)
        val tilAccHolder = sheetView.findViewById<TextInputLayout>(R.id.tilRefundAccountHolder)
        val etAccHolder = sheetView.findViewById<TextInputEditText>(R.id.etRefundAccountHolder)
        val etNote = sheetView.findViewById<TextInputEditText>(R.id.etSettlementNote)
        val btnSubmit = sheetView.findViewById<Button>(R.id.btnSubmitSettlement)
        val pbLoading = sheetView.findViewById<ProgressBar>(R.id.pbSettlementLoading)

        etDeposit.setText(contract.depositAmount.toLong().toString())
        etRefund.setText(contract.depositAmount.toLong().toString())

        val paymentOptions = listOf("Tiền mặt" to "CASH", "Chuyển khoản" to "BANK_TRANSFER")
        var selectedPaymentMethod = "CASH"
        val paymentAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, paymentOptions.map { it.first })
        actvPayment.setAdapter(paymentAdapter)
        actvPayment.setText(paymentOptions[0].first, false)
        actvPayment.setOnItemClickListener { _, _, position, _ ->
            selectedPaymentMethod = paymentOptions.getOrNull(position)?.second ?: "CASH"
            layoutBankFields.visibility = if (selectedPaymentMethod == "BANK_TRANSFER") View.VISIBLE else View.GONE
        }

        btnSubmit.setOnClickListener {
            val deposit = etDeposit.text?.toString()?.trim()?.toDoubleOrNull() ?: contract.depositAmount
            val deductions = etDeductions.text?.toString()?.trim()?.toDoubleOrNull() ?: 0.0
            val reason = etReason.text?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: "Khấu trừ chi phí phát sinh"
            val refund = etRefund.text?.toString()?.trim()?.toDoubleOrNull() ?: (deposit - deductions)
            val note = etNote.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }

            val payoutAccount: DepositSettlementPayoutAccountDto? = if (selectedPaymentMethod == "BANK_TRANSFER") {
                val bankCode = etBankCode.text?.toString()?.trim().orEmpty()
                val accNumber = etAccNumber.text?.toString()?.trim().orEmpty()
                val accHolder = etAccHolder.text?.toString()?.trim()?.uppercase().orEmpty()

                if (bankCode.isEmpty()) {
                    tilBankCode.error = "Vui lòng nhập mã ngân hàng"
                    return@setOnClickListener
                } else tilBankCode.error = null

                if (accNumber.isEmpty()) {
                    tilAccNumber.error = "Vui lòng nhập số tài khoản"
                    return@setOnClickListener
                } else tilAccNumber.error = null

                if (accHolder.isEmpty()) {
                    tilAccHolder.error = "Vui lòng nhập tên chủ tài khoản"
                    return@setOnClickListener
                } else tilAccHolder.error = null

                DepositSettlementPayoutAccountDto(
                    bankCode = bankCode,
                    accountNumber = accNumber,
                    accountHolderName = accHolder
                )
            } else {
                null
            }

            btnSubmit.isEnabled = false
            pbLoading.visibility = View.VISIBLE

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            viewModel.completeCheckout(contractId, completedOn = today)

            val settlementReq = DepositSettlementRequestDto(
                contractId = contractId,
                originalDeposit = deposit,
                deductions = if (deductions > 0) {
                    listOf(
                        DepositSettlementDeductionDto(
                            category = "OTHER",
                            amount = deductions,
                            description = reason
                        )
                    )
                } else {
                    emptyList()
                },
                refundAmount = refund,
                paymentMethod = selectedPaymentMethod,
                refundPayoutAccount = payoutAccount,
                note = note
            )
            viewModel.depositSettlement(contractId, settlementReq)
            dialog.dismiss()
        }

        dialog.show()
    }

    companion object {
        const val EXTRA_CONTRACT_ID = "extra_contract_id"
        const val EXTRA_ROOM_NAME = "extra_room_name"

        fun newIntent(context: Context, contractId: String, roomName: String): Intent {
            return Intent(context, LandlordContractDetailActivity::class.java).apply {
                putExtra(EXTRA_CONTRACT_ID, contractId)
                putExtra(EXTRA_ROOM_NAME, roomName)
            }
        }
    }
}
