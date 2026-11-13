package com.rentify.app.landlord.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.landlord.databinding.ActivityLandlordChatDetailBinding
import com.rentify.app.landlord.databinding.DialogSendBotReminderBinding
import kotlinx.coroutines.launch

class LandlordChatDetailActivity : BaseActivity<ActivityLandlordChatDetailBinding>(
    ActivityLandlordChatDetailBinding::inflate
) {
    private val viewModel: LandlordChatDetailViewModel by viewModels {
        LandlordChatDetailViewModelFactory(this)
    }

    private var conversationId: String? = null
    private var conversationTitle: String? = null
    private lateinit var adapter: LandlordMessageAdapter

    override fun initView() {
        binding.rootLayout.applySystemBarsPadding(top = true, bottom = true)

        conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        conversationTitle = intent.getStringExtra(EXTRA_CONVERSATION_TITLE)

        if (conversationId.isNullOrBlank()) {
            Toast.makeText(this, "Không tìm thấy cuộc trò chuyện", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.tvConversationTitle.text = conversationTitle ?: "Cuộc trò chuyện"

        binding.btnBack.setOnClickListener {
            finish()
        }

        adapter = LandlordMessageAdapter(currentUserId = null)
        val layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvMessages.layoutManager = layoutManager
        binding.rvMessages.adapter = adapter

        binding.btnSend.setOnClickListener {
            val content = binding.etMessage.text?.toString()?.trim() ?: ""
            if (content.isNotBlank()) {
                binding.etMessage.setText("")
                viewModel.sendMessage(conversationId!!, content)
            }
        }

        binding.btnBotReminder.setOnClickListener {
            showBotReminderDialog()
        }

        viewModel.loadMessages(conversationId!!)
    }

    private fun showBotReminderDialog() {
        val dialogBinding = DialogSendBotReminderBinding.inflate(LayoutInflater.from(this))

        val templates = listOf(
            "Nhắc thanh toán hóa đơn" to "invoice",
            "Nhắc hợp đồng sắp hết hạn" to "contract_expiring",
            "Thông báo chỉ số điện nước" to "ocr_result"
        )
        val templateLabels = templates.map { it.first }
        val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, templateLabels)
        dialogBinding.actvTemplateType.setAdapter(arrayAdapter)
        dialogBinding.actvTemplateType.setText(templateLabels[0], false)

        var selectedTemplate = templates[0].second
        dialogBinding.actvTemplateType.setOnItemClickListener { _, _, position, _ ->
            selectedTemplate = templates.getOrNull(position)?.second ?: "invoice"
        }

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Gửi lời nhắc") { _, _ ->
                val resourceId = dialogBinding.etResourceId.text?.toString()?.trim() ?: ""
                if (resourceId.isBlank()) {
                    Toast.makeText(this, "Vui lòng nhập mã hóa đơn hoặc hợp đồng", Toast.LENGTH_SHORT).show()
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

