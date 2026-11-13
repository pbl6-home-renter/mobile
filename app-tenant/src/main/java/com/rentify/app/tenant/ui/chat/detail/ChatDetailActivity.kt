package com.rentify.app.tenant.ui.chat.detail

import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.core.ui.extension.toast
import com.rentify.app.tenant.databinding.ActivityChatDetailBinding
import com.rentify.app.tenant.ui.chat.group.GroupInfoBottomSheetDialog
import kotlinx.coroutines.launch

class ChatDetailActivity : BaseActivity<ActivityChatDetailBinding>(ActivityChatDetailBinding::inflate) {

    private val conversationId: String by lazy {
        intent.getStringExtra(EXTRA_CONVERSATION_ID) ?: ""
    }

    private val conversationTitle: String? by lazy {
        intent.getStringExtra(EXTRA_CONVERSATION_TITLE)
    }

    private val isGroupChat: Boolean by lazy {
        intent.getBooleanExtra(EXTRA_IS_GROUP, false)
    }

    private val viewModel: ChatDetailViewModel by viewModels {
        ChatDetailViewModelFactory(this, conversationId)
    }

    private var messageAdapter: MessageAdapter? = null

    private val pickMediaLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            viewModel.sendImageMessage(uri, this)
            binding.rvMessages.scrollToPosition(0)
        }
    }

    override fun initView() {
        binding.root.applySystemBarsPadding(top = true, bottom = true)

        if (conversationId.isBlank()) {
            toast("Không tìm thấy thông tin cuộc trò chuyện")
            finish()
            return
        }

        setupHeader()
        setupRecyclerView()
        setupInputBar()
    }

    override fun onStart() {
        super.onStart()
        viewModel.startPolling()
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopPolling()
    }

    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.currentUserId.collect { userId ->
                        if (messageAdapter?.currentUserId != userId) {
                            messageAdapter?.currentUserId = userId
                            messageAdapter?.notifyDataSetChanged()
                        }
                    }
                }

                launch {
                    viewModel.messages.collect { messageList ->
                        messageAdapter?.submitList(messageList)
                    }
                }

                launch {
                    viewModel.isLoadingInitial.collect { isLoading ->
                        binding.pbInitialLoading.isVisible = isLoading
                    }
                }

                launch {
                    viewModel.isLoadingMore.collect { isLoadingMore ->
                        binding.pbLoadMore.isVisible = isLoadingMore
                    }
                }

                launch {
                    viewModel.replyTarget.collect { target ->
                        if (target != null) {
                            binding.layoutReplyBar.isVisible = true
                            val sender = if (target.senderUserId != null && target.senderUserId == viewModel.currentUserId.value) {
                                "chính mình"
                            } else {
                                target.senderName.ifBlank { "tin nhắn" }
                            }
                            binding.tvReplySenderName.text = "Trả lời $sender"
                            binding.tvReplyPreview.text = when (target.type.lowercase()) {
                                "image" -> "[Hình ảnh]"
                                else -> target.content ?: ""
                            }
                        } else {
                            binding.layoutReplyBar.isVisible = false
                        }
                    }
                }

                launch {
                    viewModel.errorEvents.collect { errorMessage ->
                        toast(errorMessage)
                    }
                }
            }
        }
    }

    private fun setupHeader() {
        val displayTitle = if (!conversationTitle.isNullOrBlank()) {
            conversationTitle
        } else if (isGroupChat) {
            "Nhóm trao đổi"
        } else {
            "Trò chuyện trực tiếp"
        }

        binding.txtHeaderTitle.text = displayTitle

        com.rentify.app.tenant.ui.chat.util.ChatAvatarHelper.bindAvatar(
            imgAvatar = binding.imgHeaderAvatar,
            txtInitial = binding.txtHeaderAvatar,
            imgGroupIcon = binding.imgHeaderGroupIcon,
            conversationId = conversationId,
            displayName = displayTitle,
            avatarMediaId = null,
            isGroup = isGroupChat
        )

        binding.txtHeaderSubtitle.text = if (isGroupChat) "Nhóm trao đổi cư dân" else "Trò chuyện trực tiếp"
        binding.btnGroupInfo.isVisible = isGroupChat

        binding.btnGroupInfo.setOnClickListener {
            val dialog = GroupInfoBottomSheetDialog.newInstance(conversationId)
            dialog.show(supportFragmentManager, "GroupInfoBottomSheet")
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        val layoutMgr = LinearLayoutManager(this).apply {
            reverseLayout = true
            stackFromEnd = true
        }

        binding.rvMessages.layoutManager = layoutMgr

        val adapter = MessageAdapter(
            currentUserId = viewModel.currentUserId.value,
            isGroupChat = isGroupChat,
            onImageClick = { imageUrl ->
                PhotoViewerDialogFragment.newInstance(imageUrl).show(supportFragmentManager, "PhotoViewer")
            },
            onMessageLongClick = { msg ->
                viewModel.setReplyTarget(msg)
            }
        )
        messageAdapter = adapter
        binding.rvMessages.adapter = adapter

        binding.rvMessages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val lastVisibleItem = layoutMgr.findLastVisibleItemPosition()
                val totalItemCount = layoutMgr.itemCount
                if (lastVisibleItem >= totalItemCount - 3 && totalItemCount > 0) {
                    viewModel.loadMore()
                }
            }
        })
    }

    private fun setupInputBar() {
        binding.btnCancelReply.setOnSingleClickListener {
            viewModel.clearReplyTarget()
        }

        binding.btnAttachImage.setOnClickListener {
            pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnSend.setOnSingleClickListener {
            val text = binding.edtMessage.text.toString().trim()
            if (text.isNotBlank()) {
                viewModel.sendMessage(text)
                binding.edtMessage.text?.clear()
                binding.rvMessages.scrollToPosition(0)
            }
        }
    }

    companion object {
        const val EXTRA_CONVERSATION_ID = "extra_conversation_id"
        const val EXTRA_CONVERSATION_TITLE = "extra_conversation_title"
        const val EXTRA_IS_GROUP = "extra_is_group"

        fun createIntent(
            context: android.content.Context,
            conversationId: String,
            recipientName: String? = null,
            recipientAvatarUrl: String? = null,
            isGroup: Boolean = false
        ): android.content.Intent {
            return android.content.Intent(context, ChatDetailActivity::class.java).apply {
                putExtra(EXTRA_CONVERSATION_ID, conversationId)
                putExtra(EXTRA_CONVERSATION_TITLE, recipientName)
                putExtra(EXTRA_IS_GROUP, isGroup)
            }
        }
    }
}
