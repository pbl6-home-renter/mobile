package com.rentify.app.tenant.ui.chat.group

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.api.ChatApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.core.ui.extension.showError
import com.rentify.app.core.ui.extension.showMessage
import com.rentify.app.core.ui.extension.showSuccess
import com.rentify.app.tenant.data.repository.TenantChatRepositoryImpl
import com.rentify.app.tenant.databinding.DialogCreateGroupChatBinding
import com.rentify.app.tenant.ui.chat.detail.ChatDetailActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class CreateGroupChatDialogFragment : DialogFragment() {

    private var _binding: DialogCreateGroupChatBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCreateGroupChatBinding.inflate(LayoutInflater.from(requireContext()))

        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnCreate.setOnClickListener {
            val title = binding.edtGroupName.text.toString().trim()
            val rawMembers = binding.edtMemberIds.text.toString().trim()

            if (title.isBlank()) {
                showMessage("Vui lòng nhập tên nhóm")
                return@setOnClickListener
            }

            val memberIds = rawMembers.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (memberIds.isEmpty()) {
                showMessage("Vui lòng nhập ít nhất một User ID thành viên")
                return@setOnClickListener
            }

            performCreateGroup(title, memberIds)
        }

        return dialog
    }

    private fun performCreateGroup(title: String, memberIds: List<String>) {
        binding.btnCreate.isEnabled = false
        lifecycleScope.launch {
            val tokenDataStore = TokenDataStore(requireContext().applicationContext)
            val currentUserId = tokenDataStore.userId.first() ?: ""
            val allMembers = (memberIds + currentUserId).distinct()

            val tokenProvider: () -> String? = {
                runBlocking { tokenDataStore.accessToken.first() }
            }
            val chatApiService = NetworkClient.createService<ChatApiService>(tokenProvider = tokenProvider)
            val chatRepository = TenantChatRepositoryImpl(chatApiService)

            val result = chatRepository.createGroupConversation(title = title, memberUserIds = allMembers)
            binding.btnCreate.isEnabled = true

            when (result) {
                is NetworkResult.Success -> {
                    showSuccess("Tạo nhóm thành công!")
                    val conversation = result.data.data
                    val conversationId = conversation?.id
                    if (!conversationId.isNullOrBlank()) {
                        val intent = ChatDetailActivity.createIntent(
                            context = requireContext(),
                            conversationId = conversationId,
                            recipientName = title,
                            isGroup = true
                        )
                        startActivity(intent)
                    }
                    dismiss()
                }
                is NetworkResult.ApiError -> {
                    showError(result.error?.message ?: "Lỗi khi tạo nhóm")
                }
                is NetworkResult.NetworkError -> {
                    showError("Lỗi kết nối mạng")
                }
                is NetworkResult.UnknownError -> {
                    showError("Không thể tạo nhóm trò chuyện")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): CreateGroupChatDialogFragment {
            return CreateGroupChatDialogFragment()
        }
    }
}

