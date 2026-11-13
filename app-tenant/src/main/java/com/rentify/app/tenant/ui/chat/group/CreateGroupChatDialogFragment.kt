package com.rentify.app.tenant.ui.chat.group

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.api.ChatApiService
import com.rentify.app.core.network.client.NetworkClient
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
                Toast.makeText(requireContext(), "Vui lòng nhập tên nhóm", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val memberIds = rawMembers.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (memberIds.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập ít nhất một User ID thành viên", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(requireContext(), "Tạo nhóm thành công!", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(requireContext(), result.error?.message ?: "Lỗi khi tạo nhóm", Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.NetworkError -> {
                    Toast.makeText(requireContext(), "Lỗi kết nối mạng", Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.UnknownError -> {
                    Toast.makeText(requireContext(), "Không thể tạo nhóm trò chuyện", Toast.LENGTH_SHORT).show()
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

