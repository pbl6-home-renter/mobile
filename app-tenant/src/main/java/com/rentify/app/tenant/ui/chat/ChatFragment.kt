package com.rentify.app.tenant.ui.chat

import android.content.Context
import android.content.Intent
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.rentify.app.core.network.dto.chat.ConversationListResponseDto
import com.rentify.app.core.ui.R as CoreUiR
import com.rentify.app.core.ui.base.BaseFragment
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.showMessage
import com.rentify.app.core.ui.state.UiState
import com.rentify.app.tenant.R
import com.rentify.app.tenant.databinding.FragmentChatBinding
import com.rentify.app.tenant.ui.chat.detail.ChatDetailActivity
import com.rentify.app.tenant.ui.chat.group.CreateGroupChatDialogFragment
import com.rentify.app.tenant.ui.dialog.LoginFeature
import com.rentify.app.tenant.ui.dialog.showLoginRequiredSheet
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.Locale

class ChatFragment : BaseFragment<FragmentChatBinding>(FragmentChatBinding::inflate) {

    interface ChatListener {
        fun onNavigateToLoginRequested()
    }

    private enum class ConversationFilter {
        ALL,
        UNREAD,
        LANDLORD,
        GROUP
    }

    private var listener: ChatListener? = null

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModelFactory(requireContext())
    }

    private val conversationAdapter by lazy {
        ConversationAdapter { conversation ->
            openChatDetail(conversation)
        }
    }

    private var allConversations: List<ConversationListResponseDto> = emptyList()
    private var selectedFilter: ConversationFilter = ConversationFilter.ALL
    private var searchQuery: String = ""
    private var searchDebounceJob: Job? = null

    private val skeletonPulseAnimation by lazy {
        AlphaAnimation(0.45f, 1.0f).apply {
            duration = 750L
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? ChatListener
            ?: (parentFragment as? ChatListener)
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun initView() {
        binding.root.applySystemBarsPadding(top = true, bottom = false)
        setupRecyclerView()
        setupSwipeRefresh()
        setupSearchBox()
        setupFilterChips()
        setupErrorState()
        setupGuestView()
        setupCreateGroupButton()
    }

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoggedIn.collect { loggedIn ->
                        binding.layoutGuest.isVisible = !loggedIn
                        binding.swipeRefreshLayout.isVisible = loggedIn
                        binding.btnCreateGroup.isVisible = loggedIn
                        binding.layoutSearchContainer.isVisible = loggedIn
                        binding.scrollFilterChips.isVisible = loggedIn
                        if (!loggedIn) {
                            stopSkeletonLoading()
                            binding.layoutEmptyState.root.isVisible = false
                            binding.layoutErrorState.root.isVisible = false
                        }
                    }
                }

                launch {
                    viewModel.currentUserId.collect { userId ->
                        conversationAdapter.currentUserId = userId
                    }
                }

                launch {
                    viewModel.isRefreshing.collect { isRefreshing ->
                        binding.swipeRefreshLayout.isRefreshing = isRefreshing
                    }
                }

                launch {
                    viewModel.conversationsState.collect { state ->
                        handleConversationsState(state)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkAuthAndLoad(isRefresh = false)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            viewModel.checkAuthAndLoad(isRefresh = false)
        }
    }

    private fun setupRecyclerView() {
        binding.rvConversations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = conversationAdapter
            if (itemDecorationCount == 0) {
                addItemDecoration(ConversationDividerItemDecoration(requireContext()))
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.checkAuthAndLoad(isRefresh = true)
        }
    }

    private fun setupSearchBox() {
        binding.edtSearch.doAfterTextChanged { editable ->
            val raw = editable?.toString()?.trim().orEmpty()
            searchDebounceJob?.cancel()
            searchDebounceJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(300L)
                if (searchQuery != raw) {
                    searchQuery = raw
                    if (viewModel.conversationsState.value is UiState.Success) {
                        applyFiltersAndRender()
                    }
                }
            }
        }
    }

    private fun setupFilterChips() {
        binding.chipFilterAll.setOnClickListener {
            selectFilter(ConversationFilter.ALL)
        }
        binding.chipFilterUnread.setOnClickListener {
            selectFilter(ConversationFilter.UNREAD)
        }
        binding.chipFilterLandlord.setOnClickListener {
            selectFilter(ConversationFilter.LANDLORD)
        }
        binding.chipFilterGroup.setOnClickListener {
            selectFilter(ConversationFilter.GROUP)
        }
        updateFilterChipsAppearance()
    }

    private fun selectFilter(filter: ConversationFilter) {
        if (selectedFilter == filter) return
        selectedFilter = filter
        updateFilterChipsAppearance()
        if (viewModel.conversationsState.value is UiState.Success) {
            applyFiltersAndRender()
        }
    }

    private fun updateFilterChipsAppearance() {
        val context = requireContext()
        val fontRegular = ResourcesCompat.getFont(context, CoreUiR.font.plus_jakarta_sans)
        val fontMedium = ResourcesCompat.getFont(context, CoreUiR.font.plus_jakarta_sans_medium)
        val colorSelectedText = ContextCompat.getColor(context, CoreUiR.color.rentify_text)
        val colorNormalText = ContextCompat.getColor(context, CoreUiR.color.rentify_text_secondary)

        fun styleChip(chip: TextView, isSelected: Boolean) {
            chip.setBackgroundResource(
                if (isSelected) R.drawable.bg_chat_filter_chip_selected
                else R.drawable.bg_chat_filter_chip_normal
            )
            chip.setTextColor(if (isSelected) colorSelectedText else colorNormalText)
            chip.typeface = if (isSelected) fontMedium else fontRegular
        }

        styleChip(binding.chipFilterAll, selectedFilter == ConversationFilter.ALL)
        styleChip(binding.chipFilterUnread, selectedFilter == ConversationFilter.UNREAD)
        styleChip(binding.chipFilterLandlord, selectedFilter == ConversationFilter.LANDLORD)
        styleChip(binding.chipFilterGroup, selectedFilter == ConversationFilter.GROUP)
    }

    private fun updateUnreadChipLabel(conversations: List<ConversationListResponseDto>) {
        val unreadConvCount = conversations.count { it.unreadCount > 0 }
        binding.chipFilterUnread.text = if (unreadConvCount > 0) {
            "Chưa đọc $unreadConvCount"
        } else {
            "Chưa đọc"
        }
    }

    private fun setupErrorState() {
        binding.layoutErrorState.btnRetry.setOnClickListener {
            viewModel.checkAuthAndLoad(isRefresh = false)
        }
    }

    private fun setupGuestView() {
        binding.btnLogin.setOnClickListener {
            showLoginRequiredSheet(LoginFeature.MESSAGES) {
                listener?.onNavigateToLoginRequested()
            }
        }
    }

    private fun setupCreateGroupButton() {
        binding.btnCreateGroup.setOnClickListener {
            if (viewModel.isLoggedIn.value) {
                CreateGroupChatDialogFragment.newInstance().show(childFragmentManager, "CreateGroupChatDialog")
            } else {
                showMessage("Vui lòng đăng nhập để tạo nhóm trò chuyện")
            }
        }
    }

    private fun handleConversationsState(state: UiState<List<ConversationListResponseDto>>) {
        when (state) {
            is UiState.Loading -> {
                startSkeletonLoading()
                binding.rvConversations.isVisible = false
                binding.layoutEmptyState.root.isVisible = false
                binding.layoutErrorState.root.isVisible = false
            }
            is UiState.Success -> {
                stopSkeletonLoading()
                binding.layoutErrorState.root.isVisible = false
                allConversations = state.data
                updateUnreadChipLabel(allConversations)
                applyFiltersAndRender()
            }
            is UiState.Error -> {
                stopSkeletonLoading()
                binding.rvConversations.isVisible = false
                binding.layoutEmptyState.root.isVisible = false
                binding.layoutErrorState.root.isVisible = true
                binding.layoutErrorState.tvErrorMessage.text = state.message
            }
            is UiState.Idle -> {
                stopSkeletonLoading()
                binding.rvConversations.isVisible = false
                binding.layoutEmptyState.root.isVisible = false
                binding.layoutErrorState.root.isVisible = false
            }
        }
    }

    private fun applyFiltersAndRender() {
        val normalizedQuery = normalizeForSearch(searchQuery)
        val hasActiveFilterOrQuery = selectedFilter != ConversationFilter.ALL || normalizedQuery.isNotEmpty()

        val filtered = allConversations.filter { conv ->
            val matchesChip = when (selectedFilter) {
                ConversationFilter.ALL -> true
                ConversationFilter.UNREAD -> conv.unreadCount > 0
                ConversationFilter.GROUP -> conv.kind.equals("group", ignoreCase = true)
                ConversationFilter.LANDLORD -> false
            }
            if (!matchesChip) return@filter false

            if (normalizedQuery.isEmpty()) return@filter true

            val nameCandidate = conv.displayName ?: conv.title ?: ""
            val lastMsgCandidate = conv.lastMessage?.contentPreview ?: ""
            normalizeForSearch(nameCandidate).contains(normalizedQuery) ||
                normalizeForSearch(lastMsgCandidate).contains(normalizedQuery)
        }

        conversationAdapter.submitList(filtered)

        if (filtered.isEmpty()) {
            binding.rvConversations.isVisible = false
            binding.layoutEmptyState.root.isVisible = true
            if (allConversations.isEmpty() && !hasActiveFilterOrQuery) {
                binding.layoutEmptyState.ivEmptyIcon.setImageResource(CoreUiR.drawable.ic_nav_chat)
                binding.layoutEmptyState.tvEmptyTitle.text = "Chưa có tin nhắn nào"
                binding.layoutEmptyState.tvEmptyMessage.isVisible = false
            } else {
                binding.layoutEmptyState.ivEmptyIcon.setImageResource(CoreUiR.drawable.ic_search)
                binding.layoutEmptyState.tvEmptyTitle.text = "Không tìm thấy hội thoại phù hợp"
                binding.layoutEmptyState.tvEmptyMessage.isVisible = false
            }
        } else {
            binding.rvConversations.isVisible = true
            binding.layoutEmptyState.root.isVisible = false
        }
    }

    private fun normalizeForSearch(input: String): String {
        if (input.isBlank()) return ""
        val decomposed = Normalizer.normalize(input.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        return decomposed
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace('đ', 'd')
    }

    private fun startSkeletonLoading() {
        if (!binding.layoutSkeletonLoading.isVisible) {
            binding.layoutSkeletonLoading.isVisible = true
            binding.layoutSkeletonLoading.startAnimation(skeletonPulseAnimation)
        }
    }

    private fun stopSkeletonLoading() {
        if (binding.layoutSkeletonLoading.isVisible) {
            binding.layoutSkeletonLoading.clearAnimation()
            binding.layoutSkeletonLoading.isVisible = false
        }
    }

    private fun openChatDetail(conversation: ConversationListResponseDto) {
        val intent = Intent(requireContext(), ChatDetailActivity::class.java).apply {
            putExtra(ChatDetailActivity.EXTRA_CONVERSATION_ID, conversation.id)
            putExtra(ChatDetailActivity.EXTRA_CONVERSATION_TITLE, conversation.displayName ?: conversation.title)
            putExtra(ChatDetailActivity.EXTRA_IS_GROUP, conversation.kind.equals("group", ignoreCase = true))
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        searchDebounceJob?.cancel()
        binding.layoutSkeletonLoading.clearAnimation()
        super.onDestroyView()
    }

    companion object {
        fun newInstance(): ChatFragment = ChatFragment()
    }
}
