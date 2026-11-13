package com.rentify.app.landlord.ui.chat

import android.content.Intent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.rentify.app.core.ui.base.BaseFragment
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.landlord.R
import com.rentify.app.landlord.databinding.FragmentLandlordChatBinding
import kotlinx.coroutines.launch

class LandlordChatFragment : BaseFragment<FragmentLandlordChatBinding>(FragmentLandlordChatBinding::inflate) {

    private val chatViewModel: LandlordChatViewModel by viewModels {
        LandlordChatViewModelFactory(requireContext())
    }

    private lateinit var adapter: LandlordConversationAdapter
    private var currentKindFilter: String? = null
    private var currentSearchQuery: String? = null

    override fun initView() {
        binding.root.applySystemBarsPadding(top = false, bottom = false)

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

        setupSearch()
        setupFilterChips()
        loadConversations()
    }

    private fun setupSearch() {
        binding.etSearch.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                currentSearchQuery = v.text?.toString()?.trim()
                loadConversations()
                true
            } else {
                false
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
            q = if (!currentSearchQuery.isNullOrBlank()) currentSearchQuery else null
        )
    }

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
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

    override fun onResume() {
        super.onResume()
        loadConversations()
    }

    companion object {
        fun newInstance(): LandlordChatFragment = LandlordChatFragment()
    }
}
