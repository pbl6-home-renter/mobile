package com.rentify.app.tenant.ui.chat

import android.content.Context
import android.content.Intent
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.rentify.app.core.network.dto.chat.ConversationListResponseDto
import com.rentify.app.core.ui.base.BaseFragment
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.toast
import com.rentify.app.core.ui.state.UiState
import com.rentify.app.tenant.databinding.FragmentChatBinding
import com.rentify.app.tenant.ui.chat.detail.ChatDetailActivity
import com.rentify.app.tenant.ui.chat.group.CreateGroupChatDialogFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ChatFragment : BaseFragment<FragmentChatBinding>(FragmentChatBinding::inflate) {

    interface ChatListener {
        fun onNavigateToLoginRequested()
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (isActive) {
                    delay(12000)
                    if (viewModel.isLoggedIn.value) {
                        viewModel.checkAuthAndLoad(isRefresh = true)
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
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.checkAuthAndLoad(isRefresh = true)
        }
    }

    private fun setupGuestView() {
        binding.btnLogin.setOnClickListener {
            listener?.onNavigateToLoginRequested()
        }
    }

    private fun setupCreateGroupButton() {
        binding.btnCreateGroup.setOnClickListener {
            if (viewModel.isLoggedIn.value) {
                CreateGroupChatDialogFragment.newInstance().show(childFragmentManager, "CreateGroupChatDialog")
            } else {
                toast("Vui lòng đăng nhập để tạo nhóm trò chuyện")
            }
        }
    }

    private fun handleConversationsState(state: UiState<List<ConversationListResponseDto>>) {
        when (state) {
            is UiState.Loading -> {
                binding.progressBar.isVisible = true
                binding.layoutEmptyState.isVisible = false
            }
            is UiState.Success -> {
                binding.progressBar.isVisible = false
                val items = state.data
                conversationAdapter.submitList(items)
                binding.layoutEmptyState.isVisible = items.isEmpty()
            }
            is UiState.Error -> {
                binding.progressBar.isVisible = false
                toast(state.message)
            }
            is UiState.Idle -> {
                binding.progressBar.isVisible = false
                binding.layoutEmptyState.isVisible = false
            }
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

    companion object {
        fun newInstance(): ChatFragment = ChatFragment()
    }
}
