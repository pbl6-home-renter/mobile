package com.rentify.app.landlord.ui.chat

import android.net.Uri
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.rentify.app.core.network.dto.chat.ConversationDetailResponseDto
import com.rentify.app.core.network.dto.chat.MessageListResponseDto
import com.rentify.app.core.ui.R as CoreUiR
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.view.RentifyAvatarView
import com.rentify.app.landlord.R
import com.rentify.app.landlord.databinding.ActivityLandlordChatDetailBinding
import com.rentify.app.landlord.databinding.DialogSendBotReminderBinding
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class LandlordChatDetailActivity : BaseActivity<ActivityLandlordChatDetailBinding>(
    ActivityLandlordChatDetailBinding::inflate
) {
    private val viewModel: LandlordChatDetailViewModel by viewModels {
        LandlordChatDetailViewModelFactory(this)
    }

    private var conversationId: String? = null
    private var conversationTitle: String? = null
    private var conversationKind: String? = null
    private var replyTargetMessage: MessageListResponseDto? = null
    private lateinit var adapter: LandlordMessageAdapter

    private val pickChatImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val convId = conversationId
        if (uri != null && !convId.isNullOrBlank()) {
            sendSelectedImage(convId, uri)
        }
    }

    override fun initView() {
        binding.rootLayout.applySystemBarsPadding(top = true, bottom = true)

        conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        conversationTitle = intent.getStringExtra(EXTRA_CONVERSATION_TITLE)
        conversationKind = intent.getStringExtra(EXTRA_CONVERSATION_KIND)

        if (conversationId.isNullOrBlank()) {
            Toast.makeText(this, "Không tìm thấy cuộc trò chuyện", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val initialTitle = conversationTitle ?: "Cuộc trò chuyện"
        binding.tvConversationTitle.text = initialTitle
        bindHeaderAvatar(null, initialTitle)

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.layoutHeaderTitleClick.setOnClickListener {
            val detail = viewModel.conversationDetailState.value
            val isGroup = detail?.kind?.equals("group", ignoreCase = true) == true ||
                conversationKind?.equals("group", ignoreCase = true) == true
            if (isGroup && detail != null) {
                showGroupInfoBottomSheet(detail)
            }
        }

        adapter = LandlordMessageAdapter(
            currentUserId = viewModel.currentUserId.value,
            onMessageLongClick = { msg ->
                setReplyTarget(msg)
            }
        )
        val layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvMessages.layoutManager = layoutManager
        binding.rvMessages.adapter = adapter

        binding.btnCancelReply.setOnClickListener {
            clearReplyTarget()
        }

        binding.btnAttachImage.setOnClickListener {
            pickChatImageLauncher.launch("image/*")
        }

        binding.btnSend.setOnClickListener {
            val content = binding.etMessage.text?.toString()?.trim() ?: ""
            if (content.isNotBlank()) {
                val replyId = replyTargetMessage?.id
                binding.etMessage.setText("")
                clearReplyTarget()
                viewModel.sendMessage(conversationId!!, content, replyToMessageId = replyId)
            }
        }

        binding.btnBotReminder.setOnClickListener {
            showBotReminderDialog()
        }

        viewModel.loadMessages(conversationId!!)
        viewModel.loadReminderResources()
    }

    override fun onStart() {
        super.onStart()
        conversationId?.let { viewModel.onRoomActive(it) }
    }

    override fun onStop() {
        conversationId?.let { viewModel.onRoomInactive(it) }
        super.onStop()
    }

    private fun bindHeaderAvatar(detail: ConversationDetailResponseDto?, fallbackTitle: String) {
        val convId = conversationId.orEmpty()
        val isGroup = detail?.kind?.equals("group", ignoreCase = true) == true ||
            conversationKind?.equals("group", ignoreCase = true) == true
        val title = detail?.displayName ?: detail?.title ?: fallbackTitle
        binding.tvConversationTitle.text = title

        if (isGroup) {
            val members = detail?.members.orEmpty()
            val activeMembersCount = members.count { it.status.equals("active", ignoreCase = true) }
            if (activeMembersCount > 0) {
                binding.tvConversationSubtitle.text = "$activeMembersCount thành viên • Chạm xem nhóm"
            }
            val otherMembers = RentifyAvatarView.selectGroupAvatarMembers(
                currentUserId = viewModel.currentUserId.value,
                members = members
            )
            binding.headerAvatarView.bindGroup(
                groupId = convId,
                groupName = title,
                avatarUrl = detail?.avatarUrl,
                otherMembers = otherMembers
            )
        } else {
            binding.tvConversationSubtitle.text = "Đang hoạt động"
            binding.headerAvatarView.bindPersonal(
                userId = convId,
                displayName = title,
                avatarUrl = detail?.avatarUrl
            )
        }
    }

    private fun setReplyTarget(msg: MessageListResponseDto) {
        replyTargetMessage = msg
        binding.layoutReplyPreviewBar.visibility = View.VISIBLE
        binding.tvReplyPreviewSender.text = "Trả lời ${msg.senderName.ifBlank { "Tin nhắn" }}"
        binding.tvReplyPreviewText.text = if (msg.type == "image") "[Hình ảnh]" else (msg.content ?: "")
    }

    private fun clearReplyTarget() {
        replyTargetMessage = null
        binding.layoutReplyPreviewBar.visibility = View.GONE
    }

    private fun sendSelectedImage(convId: String, uri: Uri) {
        try {
            val contentType = contentResolver.getType(uri) ?: "image/jpeg"
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null || bytes.isEmpty()) {
                Toast.makeText(this, "Không thể đọc ảnh đã chọn", Toast.LENGTH_SHORT).show()
                return
            }
            val ext = when {
                contentType.contains("png") -> "png"
                contentType.contains("webp") -> "webp"
                else -> "jpg"
            }
            val fileName = "chat_${System.currentTimeMillis()}.$ext"
            val replyId = replyTargetMessage?.id
            clearReplyTarget()
            viewModel.sendImageMessage(
                conversationId = convId,
                fileBytes = bytes,
                fileName = fileName,
                contentType = contentType,
                replyToMessageId = replyId
            )
        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi đọc ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showGroupInfoBottomSheet(detail: ConversationDetailResponseDto) {
        val dialog = BottomSheetDialog(this)
        val sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_landlord_group_info, null)
        dialog.setContentView(sheetView)

        val etGroupName = sheetView.findViewById<TextInputEditText>(R.id.etEditGroupName)
        val btnSaveName = sheetView.findViewById<MaterialButton>(R.id.btnSaveGroupName)
        val tvMembersHeader = sheetView.findViewById<TextView>(R.id.tvGroupMembersHeader)
        val layoutMembers = sheetView.findViewById<LinearLayout>(R.id.layoutGroupMembersContainer)
        val btnLeave = sheetView.findViewById<MaterialButton>(R.id.btnLeaveGroupChat)

        etGroupName.setText(detail.title ?: detail.displayName ?: "")
        val activeMembers = detail.members.filter { it.status.equals("active", ignoreCase = true) }
        tvMembersHeader.text = "Thành viên nhóm (${activeMembers.size})"

        val myId = viewModel.currentUserId.value
        val density = resources.displayMetrics.density
        val avatarSizePx = (36f * density).roundToInt()
        val padPx = (6f * density).roundToInt()
        val fontRegular = ResourcesCompat.getFont(this, CoreUiR.font.plus_jakarta_sans)

        for (member in activeMembers) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(padPx, padPx, padPx, padPx)
            }

            val avatar = RentifyAvatarView(this).apply {
                layoutParams = LinearLayout.LayoutParams(avatarSizePx, avatarSizePx)
                bindPersonal(
                    userId = member.userId ?: member.displayName,
                    displayName = member.displayName,
                    avatarUrl = member.avatarUrl
                )
            }

            val nameTv = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = (10f * density).roundToInt()
                }
                val roleBadge = if (member.role.equals("admin", ignoreCase = true)) " (Quản trị)" else ""
                text = "${member.displayName}$roleBadge"
                setTextColor(ContextCompat.getColor(this@LandlordChatDetailActivity, CoreUiR.color.rentify_text_primary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                typeface = fontRegular
                paint.isFakeBoldText = true
            }

            row.addView(avatar)
            row.addView(nameTv)

            val mUserId = member.userId
            if (!mUserId.isNullOrBlank() && mUserId != myId) {
                val removeBtn = MaterialButton(
                    this,
                    null,
                    android.R.attr.borderlessButtonStyle
                ).apply {
                    text = "Xóa"
                    setTextColor(ContextCompat.getColor(this@LandlordChatDetailActivity, CoreUiR.color.rentify_error))
                    textSize = 12f
                    setOnClickListener {
                        viewModel.removeGroupMember(detail.id, mUserId)
                        dialog.dismiss()
                    }
                }
                row.addView(removeBtn)
            }

            layoutMembers.addView(row)
        }

        btnSaveName.setOnClickListener {
            val newTitle = etGroupName.text?.toString()?.trim().orEmpty()
            if (newTitle.isNotEmpty()) {
                viewModel.updateGroupTitle(detail.id, newTitle)
                dialog.dismiss()
            }
        }

        btnLeave.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Rời khỏi nhóm")
                .setMessage("Bạn có chắc chắn muốn rời khỏi nhóm trò chuyện này?")
                .setPositiveButton("Rời nhóm") { _, _ ->
                    dialog.dismiss()
                    viewModel.leaveGroupConversation(detail.id) {
                        finish()
                    }
                }
                .setNegativeButton("Hủy", null)
                .show()
        }

        dialog.show()
    }

    private fun showBotReminderDialog() {
        val dialogBinding = DialogSendBotReminderBinding.inflate(LayoutInflater.from(this))

        val templates = listOf(
            "Nhắc thanh toán hóa đơn" to "invoice",
            "Nhắc hợp đồng sắp hết hạn" to "contract_expiring"
        )
        val templateLabels = templates.map { it.first }
        val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, templateLabels)
        dialogBinding.actvTemplateType.setAdapter(arrayAdapter)
        dialogBinding.actvTemplateType.setText(templateLabels[0], false)

        var selectedTemplate = templates[0].second
        var selectedResourceId: String? = null
        var currentResourceIds: List<String> = emptyList()

        fun populateResourceOptions() {
            if (selectedTemplate == "invoice") {
                val invoices = viewModel.reminderInvoicesState.value
                currentResourceIds = invoices.map { it.id }
                val labels = invoices.map { inv ->
                    val code = inv.code.ifBlank { "HĐ-${inv.id.take(6)}" }
                    "$code • Phòng ${inv.roomName} (${inv.period})"
                }
                dialogBinding.tilResourceId.hint = "Chọn Hóa đơn *"
                dialogBinding.actvResourcePicker.setAdapter(
                    ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, labels)
                )
                if (labels.isNotEmpty()) {
                    dialogBinding.actvResourcePicker.setText(labels[0], false)
                    selectedResourceId = currentResourceIds[0]
                } else {
                    dialogBinding.actvResourcePicker.setText("", false)
                    selectedResourceId = null
                }
            } else {
                val contracts = viewModel.reminderContractsState.value
                currentResourceIds = contracts.map { it.id }
                val labels = contracts.map { c ->
                    val code = c.contractCode.ifBlank { "HĐ-${c.id.take(6)}" }
                    "$code • Phòng ${c.roomName} (${c.tenantName ?: "Khách thuê"})"
                }
                dialogBinding.tilResourceId.hint = "Chọn Hợp đồng *"
                dialogBinding.actvResourcePicker.setAdapter(
                    ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, labels)
                )
                if (labels.isNotEmpty()) {
                    dialogBinding.actvResourcePicker.setText(labels[0], false)
                    selectedResourceId = currentResourceIds[0]
                } else {
                    dialogBinding.actvResourcePicker.setText("", false)
                    selectedResourceId = null
                }
            }
        }

        populateResourceOptions()

        dialogBinding.actvTemplateType.setOnItemClickListener { _, _, position, _ ->
            selectedTemplate = templates.getOrNull(position)?.second ?: "invoice"
            populateResourceOptions()
        }

        dialogBinding.actvResourcePicker.setOnItemClickListener { _, _, position, _ ->
            selectedResourceId = currentResourceIds.getOrNull(position)
        }

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Gửi lời nhắc") { _, _ ->
                val resourceId = selectedResourceId
                    ?: dialogBinding.actvResourcePicker.text?.toString()?.trim().orEmpty()
                if (resourceId.isBlank()) {
                    Toast.makeText(this, "Vui lòng chọn Hóa đơn hoặc Hợp đồng cần nhắc", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewModel.sendBotReminder(conversationId!!, selectedTemplate, resourceId)
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.currentUserId.collect { userId ->
                        adapter.setCurrentUserId(userId)
                    }
                }

                launch {
                    viewModel.conversationDetailState.collect { detail ->
                        if (detail != null) {
                            bindHeaderAvatar(detail, conversationTitle ?: "Cuộc trò chuyện")
                        }
                    }
                }

                launch {
                    viewModel.messagesState.collect { state ->
                        binding.progressBar.visibility = View.GONE
                        when (state) {
                            is MessageListUiState.Success -> {
                                adapter.submitList(state.messages) {
                                    if (state.messages.isNotEmpty()) {
                                        binding.rvMessages.scrollToPosition(state.messages.size - 1)
                                    }
                                }
                            }
                            is MessageListUiState.Error -> {
                                Toast.makeText(this@LandlordChatDetailActivity, state.message, Toast.LENGTH_SHORT).show()
                            }
                            MessageListUiState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                            }
                        }
                    }
                }

                launch {
                    viewModel.sendState.collect { state ->
                        when (state) {
                            is SendMessageUiState.Error -> {
                                Toast.makeText(this@LandlordChatDetailActivity, state.message, Toast.LENGTH_SHORT).show()
                                viewModel.resetSendState()
                            }
                            SendMessageUiState.Sent -> {
                                viewModel.resetSendState()
                            }
                            else -> {}
                        }
                    }
                }

                launch {
                    viewModel.botReminderState.collect { state ->
                        when (state) {
                            is BotReminderUiState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                            }
                            is BotReminderUiState.Success -> {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(this@LandlordChatDetailActivity, state.message, Toast.LENGTH_SHORT).show()
                                viewModel.resetBotReminderState()
                            }
                            is BotReminderUiState.Error -> {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(this@LandlordChatDetailActivity, state.message, Toast.LENGTH_LONG).show()
                                viewModel.resetBotReminderState()
                            }
                            BotReminderUiState.Idle -> {}
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_CONVERSATION_ID = "extra_conversation_id"
        const val EXTRA_CONVERSATION_TITLE = "extra_conversation_title"
        const val EXTRA_CONVERSATION_KIND = "extra_conversation_kind"
    }
}
