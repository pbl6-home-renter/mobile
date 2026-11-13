package com.rentify.app.tenant.ui.matching

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.rentify.app.core.ui.base.BaseFragment
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.core.ui.extension.toast
import com.rentify.app.core.ui.state.UiState
import com.rentify.app.core.ui.R as CoreR
import com.rentify.app.tenant.databinding.FragmentMatchingBinding
import com.rentify.app.tenant.ui.chat.detail.ChatDetailActivity
import com.rentify.app.tenant.ui.matching.detail.RoommateDetailActivity
import com.rentify.app.tenant.ui.matching.profile.EditRoommateProfileActivity
import kotlinx.coroutines.launch

class MatchingFragment : BaseFragment<FragmentMatchingBinding>(FragmentMatchingBinding::inflate) {

    interface MatchingListener {
        fun onNavigateToLoginRequested()
    }

    private var listener: MatchingListener? = null

    private val viewModel: MatchingViewModel by viewModels {
        MatchingViewModelFactory(requireContext())
    }

    private lateinit var feedAdapter: RoommateFeedAdapter
    private lateinit var requestsAdapter: MatchRequestAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = parentFragment as? MatchingListener ?: context as? MatchingListener
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun initView() {
        binding.root.applySystemBarsPadding(top = true, bottom = false)

        setupTabs()
        setupFeedRecyclerView()
        setupRequestsRecyclerView()
        setupSearchFilter()
        setupRefreshLayouts()
        setupButtons()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkAuthAndLoad()
    }

    private fun setupTabs() {
        binding.tabLayoutMatching.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        binding.layoutExploreTab.isVisible = true
                        binding.layoutRequestsTab.isVisible = false
                    }
                    1 -> {
                        binding.layoutExploreTab.isVisible = false
                        binding.layoutRequestsTab.isVisible = true
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) = Unit
            override fun onTabReselected(tab: TabLayout.Tab?) = Unit
        })

        binding.chipGroupDirection.setOnCheckedStateChangeListener { _, checkedIds ->
            val isIncoming = checkedIds.contains(binding.chipIncoming.id)
            val direction = if (isIncoming) "incoming" else "outgoing"
            requestsAdapter.setDirection(isIncoming)
            binding.tvEmptyRequestsTitle.text = if (isIncoming) "Chưa có lời mời ghép đôi nào" else "Bạn chưa gửi lời mời ghép đôi nào"
            binding.tvEmptyRequestsDesc.text = if (isIncoming) {
                "Khi có người gửi lời mời ghép đôi tới bạn, danh sách sẽ hiển thị tại đây."
            } else {
                "Các lời mời tìm bạn ở bạn đã gửi sẽ hiển thị trạng thái tại đây."
            }
            viewModel.loadRequests(direction = direction, isRefresh = true)
        }
    }

    private fun setupFeedRecyclerView() {
        feedAdapter = RoommateFeedAdapter(
            onViewDetailClick = { item ->
                val intent = RoommateDetailActivity.createIntent(requireContext(), item.id)
                startActivity(intent)
            },
            onSendRequestClick = { item ->
                viewModel.sendMatchRequest(item.tenantId)
            }
        )

        binding.rvRoommatesFeed.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = feedAdapter
        }
    }

    private fun setupRequestsRecyclerView() {
        requestsAdapter = MatchRequestAdapter(
            isIncoming = true,
            onAcceptClick = { item -> viewModel.acceptRequest(item) },
            onRejectClick = { item -> viewModel.rejectRequest(item) },
            onWithdrawClick = { item -> viewModel.withdrawRequest(item) },
            onChatClick = { item ->
                val user = if (binding.chipIncoming.isChecked) item.requester else item.target
                viewModel.contactUser(user)
            }
        )

        binding.rvMatchRequests.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = requestsAdapter
        }
    }

    private fun setupSearchFilter() {
        binding.edtSearchArea.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.edtSearchArea.text?.toString()?.trim()
                viewModel.loadFeed(isRefresh = true, areaFilter = query.takeIf { !it.isNullOrBlank() })
                true
            } else false
        }

        binding.edtSearchArea.doAfterTextChanged { s ->
            binding.btnClearSearch.isVisible = !s.isNullOrEmpty()
        }

        binding.btnClearSearch.setOnSingleClickListener {
            binding.edtSearchArea.text?.clear()
            viewModel.loadFeed(isRefresh = true, areaFilter = null)
        }
    }

    private fun setupRefreshLayouts() {
        binding.swipeRefreshFeed.setColorSchemeResources(
            CoreR.color.rentify_primary_gradient_start,
            CoreR.color.rentify_primary_gradient_end
        )
        binding.swipeRefreshFeed.setOnRefreshListener {
            val query = binding.edtSearchArea.text?.toString()?.trim()
            viewModel.loadFeed(isRefresh = true, areaFilter = query.takeIf { !it.isNullOrBlank() })
        }

        binding.swipeRefreshRequests.setColorSchemeResources(
            CoreR.color.rentify_primary_gradient_start,
            CoreR.color.rentify_primary_gradient_end
        )
        binding.swipeRefreshRequests.setOnRefreshListener {
            val direction = if (binding.chipIncoming.isChecked) "incoming" else "outgoing"
            viewModel.loadRequests(direction = direction, isRefresh = true)
        }
    }

    private fun setupButtons() {
        binding.btnMyProfile.setOnSingleClickListener {
            startActivity(EditRoommateProfileActivity.createIntent(requireContext()))
        }

        binding.btnGuestLogin.setOnSingleClickListener {
            listener?.onNavigateToLoginRequested()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoggedIn.collect { loggedIn ->
                        if (loggedIn == false) {
                            binding.layoutGuest.isVisible = true
                            binding.layoutExploreTab.isVisible = false
                            binding.layoutRequestsTab.isVisible = false
                            binding.tabLayoutMatching.isVisible = false
                            binding.btnMyProfile.isVisible = false
                        } else if (loggedIn == true) {
                            binding.layoutGuest.isVisible = false
                            binding.tabLayoutMatching.isVisible = true
                            binding.btnMyProfile.isVisible = true
                            if (binding.tabLayoutMatching.selectedTabPosition == 0) {
                                binding.layoutExploreTab.isVisible = true
                                binding.layoutRequestsTab.isVisible = false
                            } else {
                                binding.layoutExploreTab.isVisible = false
                                binding.layoutRequestsTab.isVisible = true
                            }
                        }
                    }
                }

                launch {
                    viewModel.myProfileStatus.collect { status ->
                        when (status) {
                            is MyProfileStatus.Hidden -> {
                                binding.layoutProfileHidden.isVisible = true
                                binding.layoutSearchArea.isVisible = false
                                binding.swipeRefreshFeed.isVisible = false
                                binding.cardProfileBanner.isVisible = false
                                binding.btnEnableProfile.setOnSingleClickListener {
                                    viewModel.quickEnableVisibility()
                                }
                            }
                            is MyProfileStatus.NoProfile -> {
                                binding.layoutProfileHidden.isVisible = false
                                binding.layoutSearchArea.isVisible = true
                                binding.swipeRefreshFeed.isVisible = true
                                binding.cardProfileBanner.isVisible = true
                                binding.cardProfileBanner.setCardBackgroundColor(ContextCompat.getColor(requireContext(), CoreR.color.rentify_primary_container))
                                binding.cardProfileBanner.strokeColor = Color.parseColor("#C7D2FE")
                                binding.ivBannerIcon.setImageResource(CoreR.drawable.ic_ai_sparkle)
                                binding.ivBannerIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), CoreR.color.rentify_primary))
                                binding.ivBannerIcon.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E0E7FF"))
                                binding.tvBannerTitle.text = "Thiết lập hồ sơ bạn cùng phòng"
                                binding.tvBannerTitle.setTextColor(ContextCompat.getColor(requireContext(), CoreR.color.rentify_primary))
                                binding.tvBannerDesc.text = "Tạo hồ sơ lối sống và tiêu chí phòng để AI tính độ hòa hợp và gửi lời mời kết nối."
                                binding.btnBannerAction.text = "Tạo hồ sơ"
                                binding.btnBannerAction.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), CoreR.color.rentify_primary))
                                binding.btnBannerAction.setOnSingleClickListener {
                                    startActivity(EditRoommateProfileActivity.createIntent(requireContext()))
                                }
                            }
                            is MyProfileStatus.Visible -> {
                                binding.layoutProfileHidden.isVisible = false
                                binding.layoutSearchArea.isVisible = true
                                binding.swipeRefreshFeed.isVisible = true
                                binding.cardProfileBanner.isVisible = false
                            }
                            is MyProfileStatus.Loading -> {
                                binding.cardProfileBanner.isVisible = false
                            }
                        }
                    }
                }

                launch {
                    viewModel.feedState.collect { state ->
                        binding.swipeRefreshFeed.isRefreshing = false
                        when (state) {
                            is UiState.Loading -> {
                                binding.pbFeedLoading.isVisible = true
                                binding.layoutEmptyFeed.isVisible = false
                            }
                            is UiState.Success -> {
                                binding.pbFeedLoading.isVisible = false
                                feedAdapter.submitList(state.data)
                                binding.layoutEmptyFeed.isVisible = state.data.isEmpty()
                            }
                            is UiState.Error -> {
                                binding.pbFeedLoading.isVisible = false
                                binding.layoutEmptyFeed.isVisible = feedAdapter.itemCount == 0
                                toast(state.message)
                            }
                            is UiState.Idle -> {
                                binding.pbFeedLoading.isVisible = false
                            }
                        }
                    }
                }

                launch {
                    viewModel.requestsState.collect { state ->
                        binding.swipeRefreshRequests.isRefreshing = false
                        when (state) {
                            is UiState.Loading -> {
                                binding.pbRequestsLoading.isVisible = true
                                binding.layoutEmptyRequests.isVisible = false
                            }
                            is UiState.Success -> {
                                binding.pbRequestsLoading.isVisible = false
                                requestsAdapter.submitList(state.data)
                                binding.layoutEmptyRequests.isVisible = state.data.isEmpty()
                            }
                            is UiState.Error -> {
                                binding.pbRequestsLoading.isVisible = false
                                binding.layoutEmptyRequests.isVisible = requestsAdapter.itemCount == 0
                                toast(state.message)
                            }
                            is UiState.Idle -> {
                                binding.pbRequestsLoading.isVisible = false
                            }
                        }
                    }
                }

                launch {
                    viewModel.uiEvent.collect { event ->
                        when (event) {
                            is MatchingUiEvent.ShowToast -> toast(event.message)
                            is MatchingUiEvent.OpenChat -> {
                                val intent = ChatDetailActivity.createIntent(
                                    context = requireContext(),
                                    conversationId = event.conversationId,
                                    recipientName = event.recipientName,
                                    recipientAvatarUrl = event.recipientAvatarUrl
                                )
                                startActivity(intent)
                            }
                            is MatchingUiEvent.RequestSentSuccess -> {
                                // Feed remains updated
                            }
                            is MatchingUiEvent.RequireCreateProfile -> {
                                MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("Chưa có hồ sơ bạn cùng phòng")
                                    .setMessage("Bạn cần tạo hồ sơ bạn cùng phòng để AI tính điểm tương thích và đối phương có thể xem thông tin của bạn trước khi kết nối.")
                                    .setPositiveButton("Tạo hồ sơ ngay") { _, _ ->
                                        startActivity(EditRoommateProfileActivity.createIntent(requireContext()))
                                    }
                                    .setNegativeButton("Để sau", null)
                                    .show()
                            }
                            is MatchingUiEvent.RequireProfileVisibility -> {
                                MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("Hồ sơ đang tạm ẩn")
                                    .setMessage(event.reason)
                                    .setPositiveButton("Bật lại ngay") { _, _ ->
                                        viewModel.quickEnableVisibility()
                                    }
                                    .setNeutralButton("Chỉnh sửa hồ sơ") { _, _ ->
                                        startActivity(EditRoommateProfileActivity.createIntent(requireContext()))
                                    }
                                    .setNegativeButton("Để sau", null)
                                    .show()
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        fun newInstance(): MatchingFragment = MatchingFragment()
    }
}
