package com.rentify.app.tenant.ui.chat.detail

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.core.ui.extension.showError
import com.rentify.app.core.ui.view.RentifyAvatarView
import com.rentify.app.tenant.R
import com.rentify.app.tenant.databinding.ActivityChatDetailBinding
import com.rentify.app.tenant.ui.chat.group.GroupInfoBottomSheetDialog
import kotlinx.coroutines.launch

class ChatDetailActivity : BaseActivity<ActivityChatDetailBinding>(ActivityChatDetailBinding::inflate),
    GroupInfoBottomSheetDialog.GroupInfoListener {

    private val initialConversationId: String by lazy {
        intent.getStringExtra(EXTRA_CONVERSATION_ID) ?: ""
    }

    private val recipientUserId: String? by lazy {
        intent.getStringExtra(EXTRA_RECIPIENT_USER_ID)
    }

    private val conversationTitle: String? by lazy {
        intent.getStringExtra(EXTRA_CONVERSATION_TITLE)
    }

    private val recipientAvatarUrl: String? by lazy {
        intent.getStringExtra(EXTRA_RECIPIENT_AVATAR_URL)
    }

    private val isGroupChat: Boolean by lazy {
        intent.getBooleanExtra(EXTRA_IS_GROUP, false)
    }

    private val viewModel: ChatDetailViewModel by viewModels {
        ChatDetailViewModelFactory(
            context = this,
            conversationId = initialConversationId,
            recipientUserId = recipientUserId
        )
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

        if (initialConversationId.isBlank() && recipientUserId.isNullOrBlank()) {
            showError("Không tìm thấy thông tin cuộc trò chuyện")
            finish()
            return
        }

        setupHeader()
        setupRecyclerView()
        setupInputBar()
    }

    override fun onStart() {
        super.onStart()
        viewModel.onRoomActive()
    }

    override fun onStop() {
        super.onStop()
        viewModel.onRoomInactive()
    }

    override fun onGroupInfoUpdated() {
        viewModel.refreshConversationDetail()
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
                        updateHeaderUi()
                    }
                }

                launch {
                    viewModel.conversationDetail.collect {
                        updateHeaderUi()
                    }
                }

                launch {
                    viewModel.members.collect { memberList ->
                        messageAdapter?.updateMembers(memberList)
                        updateHeaderUi()
                    }
                }

                launch {
                    viewModel.messages.collect { messageList ->
                        messageAdapter?.submitList(messageList)
                        if (isGroupChat) {
                            updateHeaderUi()
                        }
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
                    viewModel.isOtherTyping.collect {
                        updateHeaderUi()
                    }
                }

                launch {
                    viewModel.errorEvents.collect { errorMessage ->
                        showError(errorMessage)
                    }
                }
            }
        }
    }

    private fun setupHeader() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        if (isGroupChat) {
            binding.ivHeaderChevronRight.isVisible = true
            binding.layoutHeaderCenter.isClickable = true
            binding.layoutHeaderCenter.isFocusable = true
            binding.layoutHeaderCenter.setOnClickListener {
                val convId = viewModel.conversationId
                if (convId.isNotBlank()) {
                    val dialog = GroupInfoBottomSheetDialog.newInstance(
                        conversationId = convId,
                        fallbackTitle = binding.txtHeaderTitle.text?.toString()
                    )
                    dialog.show(supportFragmentManager, "GroupInfoBottomSheet")
                }
            }
        } else {
            // Chat CÁ NHÂN: tên và avatar KHÔNG bấm được, KHÔNG có mũi tên
            binding.ivHeaderChevronRight.isVisible = false
            binding.layoutHeaderCenter.isClickable = false
            binding.layoutHeaderCenter.isFocusable = false
            binding.layoutHeaderCenter.background = null
            binding.layoutHeaderCenter.setOnClickListener(null)
        }

        updateHeaderUi()
    }

    private fun updateHeaderUi() {
        val detail = viewModel.conversationDetail.value
        val members = viewModel.members.value
        val currentUserId = viewModel.currentUserId.value
        val isTyping = viewModel.isOtherTyping.value

        if (isGroupChat) {
            val unifiedGroupTitle = ChatDetailViewModel.resolveUnifiedGroupTitle(detail, conversationTitle)
            binding.txtHeaderTitle.text = unifiedGroupTitle

            val subtitleText = when {
                isTyping -> "Đang nhập..."
                members.isNotEmpty() -> "${members.size} thành viên"
                else -> ""
            }
            binding.txtHeaderSubtitle.isVisible = subtitleText.isNotEmpty()
            binding.txtHeaderSubtitle.text = subtitleText

            val avatarMembers = RentifyAvatarView.selectGroupAvatarMembers(
                currentUserId = currentUserId,
                members = members,
                recentMessages = viewModel.messages.value
            )
            binding.headerAvatarView.bindGroup(
                groupId = viewModel.conversationId.ifBlank { unifiedGroupTitle },
                groupName = unifiedGroupTitle,
                avatarUrl = detail?.avatarUrl,
                otherMembers = avatarMembers
            )
        } else {
            val otherMember = members.firstOrNull { m ->
                m.participantType.equals("user", ignoreCase = true) &&
                    !m.userId.isNullOrBlank() &&
                    (currentUserId.isNullOrBlank() || m.userId != currentUserId)
            }
            val displayTitle = detail?.displayName?.takeIf { it.isNotBlank() }
                ?: otherMember?.displayName?.takeIf { it.isNotBlank() }
                ?: conversationTitle?.takeIf { it.isNotBlank() }
                ?: "Trò chuyện trực tiếp"

            binding.txtHeaderTitle.text = displayTitle

            if (isTyping) {
                binding.txtHeaderSubtitle.isVisible = true
                binding.txtHeaderSubtitle.text = "Đang nhập..."
            } else {
                binding.txtHeaderSubtitle.isVisible = false
            }

            val seedId = otherMember?.userId
                ?: recipientUserId?.takeIf { it.isNotBlank() }
                ?: viewModel.conversationId.takeIf { it.isNotBlank() }
                ?: displayTitle
            val avatarUrl = detail?.avatarUrl
                ?: otherMember?.avatarUrl
                ?: recipientAvatarUrl

            binding.headerAvatarView.bindPersonal(
                userId = seedId,
                displayName = displayTitle,
                avatarUrl = avatarUrl
            )
        }
    }

    private fun setupRecyclerView() {
        val layoutMgr = LinearLayoutManager(this).apply {
            reverseLayout = true
            stackFromEnd = true
        }

        binding.rvMessages.layoutManager = layoutMgr
        binding.rvMessages.itemAnimator = null

        val adapter = MessageAdapter(
            currentUserId = viewModel.currentUserId.value,
            isGroupChat = isGroupChat,
            onImageClick = { imageUrl ->
                PhotoViewerDialogFragment.newInstance(imageUrl).show(supportFragmentManager, "PhotoViewer")
            },
            onMessageLongClick = { msg ->
                viewModel.setReplyTarget(msg)
            },
            mediaUrlResolver = { mediaId -> viewModel.getMediaDownloadUrl(mediaId) },
            scope = lifecycleScope
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

        binding.btnSend.isEnabled = !binding.edtMessage.text.isNullOrBlank()

        binding.edtMessage.doAfterTextChanged { text ->
            val hasText = !text.isNullOrBlank()
            binding.btnSend.isEnabled = hasText
            viewModel.onInputTypingChanged(hasText)

            binding.edtMessage.post {
                val isMultiline = binding.edtMessage.lineCount > 1
                binding.layoutInput.setBackgroundResource(
                    if (isMultiline) R.drawable.bg_chat_input_bar_multiline
                    else R.drawable.bg_chat_input_bar_pill
                )
            }
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
        const val EXTRA_RECIPIENT_USER_ID = "extra_recipient_user_id"
        const val EXTRA_CONVERSATION_TITLE = "extra_conversation_title"
        const val EXTRA_RECIPIENT_AVATAR_URL = "extra_recipient_avatar_url"
        const val EXTRA_IS_GROUP = "extra_is_group"

        fun createIntent(
            context: Context,
            conversationId: String,
            recipientName: String? = null,
            recipientAvatarUrl: String? = null,
            isGroup: Boolean = false,
            recipientUserId: String? = null
        ): Intent {
            return Intent(context, ChatDetailActivity::class.java).apply {
                putExtra(EXTRA_CONVERSATION_ID, conversationId)
                putExtra(EXTRA_RECIPIENT_USER_ID, recipientUserId)
                putExtra(EXTRA_CONVERSATION_TITLE, recipientName)
                putExtra(EXTRA_RECIPIENT_AVATAR_URL, recipientAvatarUrl)
                putExtra(EXTRA_IS_GROUP, isGroup)
            }
        }
    }
}
