package com.rentify.app.tenant.ui.chat.group

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rentify.app.core.network.dto.chat.ConversationMemberDto
import com.rentify.app.core.ui.extension.toast
import com.rentify.app.tenant.databinding.BottomSheetGroupInfoBinding
import com.rentify.app.tenant.databinding.DialogAddGroupMemberBinding
import kotlinx.coroutines.launch

class GroupInfoBottomSheetDialog : BottomSheetDialogFragment() {

    private var _binding: BottomSheetGroupInfoBinding? = null
    private val binding get() = _binding!!

    private val conversationId: String by lazy {
        arguments?.getString(ARG_CONVERSATION_ID) ?: ""
    }

    private val viewModel: GroupInfoViewModel by viewModels {
        GroupInfoViewModelFactory(requireContext(), conversationId)
    }

    private var memberAdapter: GroupMemberAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetGroupInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeData()
    }

    private fun setupUI() {
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.rvGroupMembers.layoutManager = LinearLayoutManager(requireContext())
        val adapter = GroupMemberAdapter(
            currentUserId = viewModel.currentUserId.value,
            isCurrentUserAdmin = viewModel.isCurrentUserAdmin.value,
            onMemberActionClick = { member ->
                showMemberActionDialog(member)
            }
        )
        memberAdapter = adapter
        binding.rvGroupMembers.adapter = adapter

        binding.btnEditGroupName.setOnClickListener {
            showEditGroupNameDialog()
        }

        binding.btnAddMember.setOnClickListener {
            showAddMemberDialog()
        }

        binding.btnLeaveGroup.setOnClickListener {
            showLeaveGroupConfirmDialog()
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.conversationDetail.collect { detail ->
                        if (detail != null) {
                            val name = detail.title ?: "Nhóm trao đổi"
                            binding.txtGroupName.text = name
                            val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "G"
                            binding.txtGroupAvatar.text = initial
                        }
                    }
                }

                launch {
                    viewModel.members.collect { memberList ->
                        binding.txtMemberCount.text = "${memberList.size} thành viên"
                        memberAdapter?.submitList(memberList)
                    }
                }

                launch {
                    viewModel.isCurrentUserAdmin.collect { isAdmin ->
                        memberAdapter?.isCurrentUserAdmin = isAdmin
                        memberAdapter?.notifyDataSetChanged()
                        binding.btnAddMember.isVisible = isAdmin
                        binding.btnEditGroupName.isVisible = isAdmin
                    }
                }

                launch {
                    viewModel.currentUserId.collect { userId ->
                        memberAdapter?.currentUserId = userId
                        memberAdapter?.notifyDataSetChanged()
                    }
                }

                launch {
                    viewModel.isLoading.collect { loading ->
                        binding.pbMembersLoading.isVisible = loading
                    }
                }

                launch {
                    viewModel.toastEvents.collect { msg ->
                        toast(msg)
                    }
                }

                launch {
                    viewModel.leaveSuccessEvent.collect {
                        dismiss()
                        activity?.finish()
                    }
                }
            }
        }
    }

    private fun showEditGroupNameDialog() {
        val currentTitle = binding.txtGroupName.text.toString()
        val input = EditText(requireContext()).apply {
            setText(currentTitle)
            setSelection(length())
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Đổi tên nhóm")
            .setView(input)
            .setPositiveButton("Lưu") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotBlank()) {
                    viewModel.updateGroupName(newName)
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showAddMemberDialog() {
        val dialogBinding = DialogAddGroupMemberBinding.inflate(LayoutInflater.from(requireContext()))
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnAdd.setOnClickListener {
            val text = dialogBinding.edtAddMemberIds.text.toString().trim()
            if (text.isNotBlank()) {
                val ids = text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (ids.isNotEmpty()) {
                    viewModel.addMembers(ids)
                    dialog.dismiss()
                } else {
                    toast("Vui lòng nhập ít nhất 1 User ID hợp lệ")
                }
            } else {
                toast("Vui lòng nhập User ID")
            }
        }

        dialog.show()
    }

    private fun showMemberActionDialog(member: ConversationMemberDto) {
        val userId = member.userId ?: return
        val isAdmin = member.role.equals("admin", ignoreCase = true)
        val roleActionTitle = if (isAdmin) "Chuyển thành Thành viên" else "Bổ nhiệm làm Trưởng nhóm"
        val actions = arrayOf(roleActionTitle, "Xóa khỏi nhóm")

        AlertDialog.Builder(requireContext())
            .setTitle(member.displayName.ifBlank { "Tùy chọn thành viên" })
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> {
                        val newRole = if (isAdmin) "member" else "admin"
                        viewModel.updateMemberRole(userId, newRole)
                    }
                    1 -> {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Xác nhận xóa")
                            .setMessage("Bạn có chắc chắn muốn xóa thành viên này khỏi nhóm?")
                            .setPositiveButton("Xóa") { _, _ ->
                                viewModel.removeMember(userId)
                            }
                            .setNegativeButton("Hủy", null)
                            .show()
                    }
                }
            }
            .show()
    }

    private fun showLeaveGroupConfirmDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Rời khỏi nhóm")
            .setMessage("Bạn có chắc chắn muốn rời khỏi nhóm này không? Bạn sẽ không thể nhận tin nhắn mới trừ khi được mời lại.")
            .setPositiveButton("Rời nhóm") { _, _ ->
                viewModel.leaveGroup()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CONVERSATION_ID = "arg_conversation_id"

        fun newInstance(conversationId: String): GroupInfoBottomSheetDialog {
            return GroupInfoBottomSheetDialog().apply {
                arguments = android.os.Bundle().apply {
                    putString(ARG_CONVERSATION_ID, conversationId)
                }
            }
        }
    }
}
