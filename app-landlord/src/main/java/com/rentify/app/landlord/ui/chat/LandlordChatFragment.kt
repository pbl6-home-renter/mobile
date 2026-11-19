package com.rentify.app.landlord.ui.chat

import android.content.Intent
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.rentify.app.core.network.dto.user.UserDirectoryResponseDto
import com.rentify.app.core.ui.R as CoreUiR
import com.rentify.app.core.ui.base.BaseFragment
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.view.RentifyAvatarView
import com.rentify.app.landlord.R
import com.rentify.app.landlord.databinding.FragmentLandlordChatBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class LandlordChatFragment : BaseFragment<FragmentLandlordChatBinding>(FragmentLandlordChatBinding::inflate) {

    private val chatViewModel: LandlordChatViewModel by viewModels {
        LandlordChatViewModelFactory(requireContext())
    }

    private lateinit var adapter: LandlordConversationAdapter
    private var currentKindFilter: String? = null
    private var currentSearchQuery: String? = null
    private var searchDebounceJob: Job? = null
    private var directoryDebounceJob: Job? = null

    private var onDirectoryUsersUpdated: ((List<UserDirectoryResponseDto>) -> Unit)? = null

    override fun initView() {
        binding.root.applySystemBarsPadding(top = true, bottom = false)

        adapter = LandlordConversationAdapter { conversation ->
            val title = conversation.displayName ?: conversation.title ?: "Cuộc trò chuyện"
            val intent = Intent(requireContext(), LandlordChatDetailActivity::class.java).apply {
                putExtra(LandlordChatDetailActivity.EXTRA_CONVERSATION_ID, conversation.id)
                putExtra(LandlordChatDetailActivity.EXTRA_CONVERSATION_TITLE, title)
                putExtra(LandlordChatDetailActivity.EXTRA_CONVERSATION_KIND, conversation.kind)
            }
            startActivity(intent)
        }

        binding.rvConversations.layoutManager = LinearLayoutManager(requireContext())
        binding.rvConversations.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            loadConversations()
        }

        binding.btnNewChat.setOnClickListener {
            showNewChatBottomSheet()
        }

        setupSearch()
        setupFilterChips()
        loadConversations()
    }

    private fun setupSearch() {
        binding.etSearch.doAfterTextChanged { editable ->
            val query = editable?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            if (query == currentSearchQuery) return@doAfterTextChanged
            currentSearchQuery = query
            searchDebounceJob?.cancel()
            searchDebounceJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(300)
                loadConversations()
            }
        }
    }

    private fun setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: R.id.chipAll
            currentKindFilter = when (checkedId) {
                R.id.chipDirect -> "direct"
                R.id.chipGroup -> "group"
                else -> null
            }
            loadConversations()
        }
    }

    private fun loadConversations() {
        binding.progressBar.visibility = View.VISIBLE
        binding.layoutEmptyChat.visibility = View.GONE
        chatViewModel.loadConversations(
            kind = currentKindFilter,
            q = currentSearchQuery
        )
    }

    private fun showNewChatBottomSheet() {
        val ctx = requireContext()
        val dialog = BottomSheetDialog(ctx)
        val sheetView = LayoutInflater.from(ctx).inflate(R.layout.bottom_sheet_new_chat, null)
        dialog.setContentView(sheetView)

        val chipGroupMode = sheetView.findViewById<ChipGroup>(R.id.chipGroupNewChatMode)
        val tilGroupTitle = sheetView.findViewById<TextInputLayout>(R.id.tilNewGroupTitle)
        val etGroupTitle = sheetView.findViewById<TextInputEditText>(R.id.etNewGroupTitle)
        val etSearchUser = sheetView.findViewById<TextInputEditText>(R.id.etSearchDirectoryUser)
        val tvHint = sheetView.findViewById<TextView>(R.id.tvNewChatModeHint)
        val layoutUsers = sheetView.findViewById<LinearLayout>(R.id.layoutDirectoryUsersList)
        val layoutGroupFooter = sheetView.findViewById<LinearLayout>(R.id.layoutGroupFooterActions)
        val btnCancelGroup = sheetView.findViewById<MaterialButton>(R.id.btnCancelNewGroup)
        val btnCreateGroup = sheetView.findViewById<MaterialButton>(R.id.btnCreateNewGroup)

        var isGroupMode = false
        val selectedUserIds = mutableSetOf<String>()
        var currentUsers: List<UserDirectoryResponseDto> = emptyList()

        val density = resources.displayMetrics.density
        val avatarSizePx = (40f * density).roundToInt()
        val padPx = (8f * density).roundToInt()
        val fontRegular = ResourcesCompat.getFont(ctx, CoreUiR.font.plus_jakarta_sans)

        fun renderUserRows() {
            layoutUsers.removeAllViews()
            btnCreateGroup.isEnabled = selectedUserIds.size >= 2
            if (currentUsers.isEmpty()) {
                val emptyTv = TextView(ctx).apply {
                    text = "Không tìm thấy người dùng phù hợp."
                    setTextColor(ContextCompat.getColor(ctx, CoreUiR.color.rentify_text_muted))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    typeface = fontRegular
                    gravity = Gravity.CENTER
                    setPadding(padPx, padPx * 3, padPx, padPx * 3)
                }
                layoutUsers.addView(emptyTv)
                return
            }

            for (user in currentUsers) {
                val row = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(padPx, padPx, padPx, padPx)
                    isClickable = true
                    isFocusable = true
                }

                val avatar = RentifyAvatarView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(avatarSizePx, avatarSizePx)
                    bindPersonal(userId = user.id, displayName = user.name, avatarUrl = user.avatarUrl)
                }

                val nameTv = TextView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginStart = (12f * density).roundToInt()
                    }
                    text = user.name
                    setTextColor(ContextCompat.getColor(ctx, CoreUiR.color.rentify_text_primary))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    typeface = fontRegular
                    paint.isFakeBoldText = true
                }

                row.addView(avatar)
                row.addView(nameTv)

                if (isGroupMode) {
                    val cb = MaterialCheckBox(ctx).apply {
                        isChecked = selectedUserIds.contains(user.id)
                        setOnCheckedChangeListener { _, checked ->
                            if (checked) selectedUserIds.add(user.id) else selectedUserIds.remove(user.id)
                            btnCreateGroup.isEnabled = selectedUserIds.size >= 2
                        }
                    }
                    row.addView(cb)
                    row.setOnClickListener {
                        cb.isChecked = !cb.isChecked
                    }
                } else {
                    row.setOnClickListener {
                        dialog.dismiss()
                        chatViewModel.startDirectConversation(user.id)
                    }
                }

                layoutUsers.addView(row)
            }
        }

        chipGroupMode.setOnCheckedStateChangeListener { _, checkedIds ->
            isGroupMode = checkedIds.contains(R.id.chipModeGroup)
            selectedUserIds.clear()
            tilGroupTitle.visibility = if (isGroupMode) View.VISIBLE else View.GONE
            layoutGroupFooter.visibility = if (isGroupMode) View.VISIBLE else View.GONE
            tvHint.text = if (isGroupMode) {
                "Chọn ít nhất 2 người khác để tạo nhóm"
            } else {
                "Chọn 1 người trong danh sách bên dưới để bắt đầu trò chuyện."
            }
            renderUserRows()
        }

        etSearchUser.doAfterTextChanged { editable ->
            val q = editable?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            directoryDebounceJob?.cancel()
            directoryDebounceJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(300)
                chatViewModel.searchDirectoryUsers(q)
            }
        }

        btnCancelGroup.setOnClickListener {
            dialog.dismiss()
        }

        btnCreateGroup.setOnClickListener {
            if (selectedUserIds.size < 2) {
                Toast.makeText(ctx, "Vui lòng chọn ít nhất 2 người khác để tạo nhóm", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val title = etGroupTitle.text?.toString()?.trim()
            dialog.dismiss()
            chatViewModel.createGroupConversation(selectedUserIds.toList(), title)
        }

        onDirectoryUsersUpdated = { users ->
            currentUsers = users
            renderUserRows()
        }

        dialog.setOnDismissListener {
            onDirectoryUsersUpdated = null
        }

        chatViewModel.searchDirectoryUsers(null)
        dialog.show()
    }

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    chatViewModel.directoryUsersState.collect { users ->
                        onDirectoryUsersUpdated?.invoke(users)
                    }
                }

                launch {
                    chatViewModel.newConversationState.collect { state ->
                        when (state) {
                            is NewConversationActionState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                            }
                            is NewConversationActionState.Created -> {
                                binding.progressBar.visibility = View.GONE
                                chatViewModel.resetNewConversationState()
                                val detail = state.detail
                                val title = detail.displayName ?: detail.title ?: "Cuộc trò chuyện"
                                val intent = Intent(requireContext(), LandlordChatDetailActivity::class.java).apply {
                                    putExtra(LandlordChatDetailActivity.EXTRA_CONVERSATION_ID, detail.id)
                                    putExtra(LandlordChatDetailActivity.EXTRA_CONVERSATION_TITLE, title)
                                    putExtra(LandlordChatDetailActivity.EXTRA_CONVERSATION_KIND, detail.kind)
                                }
                                startActivity(intent)
                            }
                            is NewConversationActionState.Error -> {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                                chatViewModel.resetNewConversationState()
                            }
                            NewConversationActionState.Idle -> {}
                        }
                    }
                }

                launch {
                    chatViewModel.conversationListState.collect { state ->
                        binding.swipeRefresh.isRefreshing = false
                        binding.progressBar.visibility = View.GONE

                        when (state) {
                            is ConversationListUiState.Success -> {
                                if (state.conversations.isEmpty()) {
                                    binding.layoutEmptyChat.visibility = View.VISIBLE
                                    binding.rvConversations.visibility = View.GONE
                                } else {
                                    binding.layoutEmptyChat.visibility = View.GONE
                                    binding.rvConversations.visibility = View.VISIBLE
                                    adapter.submitList(state.conversations)
                                }
                            }
                            is ConversationListUiState.Error -> {
                                binding.layoutEmptyChat.visibility = View.VISIBLE
                                binding.rvConversations.visibility = View.GONE
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            }
                            ConversationListUiState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadConversations()
    }

    companion object {
        fun newInstance(): LandlordChatFragment = LandlordChatFragment()
    }
}
