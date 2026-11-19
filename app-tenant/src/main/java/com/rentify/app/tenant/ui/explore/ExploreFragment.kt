package com.rentify.app.tenant.ui.explore

import android.animation.ValueAnimator
import android.content.Context
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.viewModels
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.rentify.app.core.network.dto.room.RoomPublicListResponseDto
import com.rentify.app.core.ui.R as CoreUiR
import com.rentify.app.core.ui.base.BaseFragment
import com.rentify.app.core.ui.extension.showIf
import com.rentify.app.core.ui.extension.toast
import com.rentify.app.core.ui.extension.toVnd
import com.rentify.app.core.ui.state.UiState
import com.rentify.app.tenant.R
import com.rentify.app.tenant.databinding.FragmentExploreBinding
import com.rentify.app.tenant.ui.building.BuildingCardAdapter
import com.rentify.app.tenant.ui.building.BuildingDetailActivity
import com.rentify.app.tenant.ui.building.BuildingListActivity
import com.rentify.app.tenant.ui.dialog.LoginFeature
import com.rentify.app.tenant.ui.dialog.showLoginRequiredSheet
import com.rentify.app.tenant.ui.invoice.detail.InvoiceDetailActivity
import com.rentify.app.tenant.ui.issue.create.CreateIssueActivity
import com.rentify.app.tenant.ui.room.detail.RoomDetailActivity
import com.rentify.app.tenant.ui.search.RoomFilter
import com.rentify.app.tenant.ui.search.SearchResultActivity

class ExploreFragment : BaseFragment<FragmentExploreBinding>(FragmentExploreBinding::inflate) {

    interface ExploreListener {
        fun onNavigateToLoginRequested()
        fun onNavigateToLoginForTabRequested(targetTabId: Int)
        fun onNavigateToChatRequested()
        fun onNavigateToMatchingRequested()
        fun onNavigateToMyRoomRequested()
    }

    private var listener: ExploreListener? = null

    private val viewModel: ExploreViewModel by viewModels {
        ExploreViewModelFactory(requireContext())
    }

    private lateinit var roomAdapter: RoomForYouAdapter
    private lateinit var popularAreaAdapter: PopularAreaAdapter
    private lateinit var buildingAdapter: BuildingCardAdapter
    private lateinit var myRoomCardsAdapter: MyRoomCardsAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = parentFragment as? ExploreListener ?: context as? ExploreListener
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun initView() {
        updateStatusBarIcons()
        setupStatusBarInsets()
        setupRecyclerViews()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        updateStatusBarIcons()
    }

    private fun updateStatusBarIcons() {
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        }
    }

    private fun setupStatusBarInsets() {
        val initialPaddingTop = binding.layoutHeaderContent.paddingTop
        val initialPaddingStart = binding.layoutHeaderContent.paddingStart
        val initialPaddingEnd = binding.layoutHeaderContent.paddingEnd
        val initialPaddingBottom = binding.layoutHeaderContent.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            binding.layoutHeaderContent.setPaddingRelative(
                initialPaddingStart,
                initialPaddingTop + statusBarTop,
                initialPaddingEnd,
                initialPaddingBottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    override fun observeData() {
        viewModel.isLoggedIn.collectWhenStarted { loggedIn ->
            binding.layoutGuestHeader.showIf(!loggedIn)
            binding.layoutUserHeader.showIf(loggedIn)
        }

        viewModel.currentUser.collectWhenStarted { user ->
            if (user != null) {
                binding.tvUserName.text = extractCallingName(user.name)
            } else {
                binding.tvUserName.text = "bạn"
            }
        }

        viewModel.hasRoomVisible.collectWhenStarted { hasRoom ->
            binding.containerMyRoom.showIf(hasRoom)
        }

        viewModel.myRoomsList.collectWhenStarted { roomsList ->
            if (roomsList.isEmpty()) {
                binding.containerMyRoom.visibility = View.GONE
            } else if (roomsList.size == 1) {
                binding.containerMyRoom.visibility = View.VISIBLE
                binding.layoutMyRoom.containerSingleRoom.visibility = View.VISIBLE
                binding.layoutMyRoom.containerMultiRooms.visibility = View.GONE
                binding.layoutMyRoom.containerIndicator.visibility = View.GONE

                MyRoomCardBinder.bind(
                    binding = binding.layoutMyRoom.layoutSingleCard,
                    hub = roomsList[0],
                    onCardClick = { listener?.onNavigateToMyRoomRequested() },
                    onPayInvoice = { _, invoice ->
                        InvoiceDetailActivity.start(requireContext(), invoice.id)
                    },
                    onContactLandlord = {
                        if (viewModel.isLoggedIn.value) {
                            listener?.onNavigateToChatRequested()
                        } else {
                            showLoginRequiredSheet(LoginFeature.MESSAGES) {
                                listener?.onNavigateToLoginForTabRequested(R.id.nav_chat)
                            }
                        }
                    },
                    onReportIssue = { hub ->
                        if (viewModel.isLoggedIn.value) {
                            CreateIssueActivity.start(requireContext(), hub.room.id, hub.room.name)
                        } else {
                            showLoginRequiredSheet(LoginFeature.MY_ROOM) {
                                listener?.onNavigateToLoginForTabRequested(R.id.nav_my_room)
                            }
                        }
                    }
                )
            } else {
                binding.containerMyRoom.visibility = View.VISIBLE
                binding.layoutMyRoom.containerSingleRoom.visibility = View.GONE
                binding.layoutMyRoom.containerMultiRooms.visibility = View.VISIBLE
                binding.layoutMyRoom.containerIndicator.visibility = View.VISIBLE

                myRoomCardsAdapter.submitList(roomsList) {
                    val targetPage = viewModel.savedRoomPageIndex.coerceIn(0, roomsList.size - 1)
                    binding.layoutMyRoom.pagerMyRooms.setCurrentItem(targetPage, false)
                    updateRoomDots(targetPage, roomsList.size)
                }
            }
        }

        viewModel.hasUnpaidInvoice.collectWhenStarted { hasUnpaid ->
            binding.viewInvoiceBadge.showIf(hasUnpaid)
        }

        viewModel.roomsState.collectWhenStarted { state ->
            binding.swipeRefreshLayout.isRefreshing = false

            when (state) {
                is UiState.Loading -> {
                    if (roomAdapter.currentList.isEmpty()) {
                        binding.pbLoading.visibility = View.VISIBLE
                        binding.layoutEmptyState.root.visibility = View.GONE
                        binding.layoutErrorState.root.visibility = View.GONE
                    }
                }
                is UiState.Success<List<RoomPublicListResponseDto>> -> {
                    binding.pbLoading.visibility = View.GONE
                    roomAdapter.submitList(state.data)
                    binding.layoutEmptyState.root.showIf(state.data.isEmpty())
                    binding.layoutErrorState.root.visibility = View.GONE
                }
                is UiState.Error -> {
                    binding.pbLoading.visibility = View.GONE
                    if (roomAdapter.currentList.isEmpty()) {
                        binding.layoutErrorState.root.visibility = View.VISIBLE
                        binding.layoutEmptyState.root.visibility = View.GONE
                    } else {
                        toast(state.message)
                    }
                }
                else -> Unit
            }
        }

        viewModel.favoriteError.collectWhenStarted { message ->
            toast(message)
        }

        viewModel.buildings.collectWhenStarted { buildings ->
            buildingAdapter.submitList(buildings)
            binding.layoutBuildings.showIf(buildings.isNotEmpty())
        }

        viewModel.isLoadingMore.collectWhenStarted { isLoadingMore ->
            binding.pbLoadingMore.showIf(isLoadingMore)
        }
    }

    private fun setupRecyclerViews() {
        roomAdapter = RoomForYouAdapter(
            onRoomClick = { room ->
                val fullAddress = listOf(room.address, room.ward, room.province)
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString(", ")
                RoomDetailActivity.start(
                    context = requireContext(),
                    roomId = room.id,
                    isFavorited = room.isFavorited,
                    fallbackAddress = fullAddress
                )
            },
            onFavoriteClick = { room ->
                if (viewModel.isLoggedIn.value) {
                    viewModel.toggleFavorite(room)
                } else {
                    showLoginRequiredSheet(LoginFeature.PROFILE) {
                        listener?.onNavigateToLoginRequested()
                    }
                }
            }
        )
        binding.rvRooms.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = roomAdapter
        }

        buildingAdapter = BuildingCardAdapter { building ->
            BuildingDetailActivity.start(requireContext(), building.id)
        }
        binding.rvBuildings.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = buildingAdapter
        }

        myRoomCardsAdapter = MyRoomCardsAdapter(
            onCardClick = { _ ->
                listener?.onNavigateToMyRoomRequested()
            },
            onPayInvoice = { _, invoice ->
                InvoiceDetailActivity.start(requireContext(), invoice.id)
            },
            onContactLandlord = { _ ->
                if (viewModel.isLoggedIn.value) {
                    listener?.onNavigateToChatRequested()
                } else {
                    showLoginRequiredSheet(LoginFeature.MESSAGES) {
                        listener?.onNavigateToLoginForTabRequested(R.id.nav_chat)
                    }
                }
            },
            onReportIssue = { hub ->
                if (viewModel.isLoggedIn.value) {
                    CreateIssueActivity.start(requireContext(), hub.room.id, hub.room.name)
                } else {
                    showLoginRequiredSheet(LoginFeature.MY_ROOM) {
                        listener?.onNavigateToLoginForTabRequested(R.id.nav_my_room)
                    }
                }
            }
        )

        binding.layoutMyRoom.pagerMyRooms.apply {
            adapter = myRoomCardsAdapter
            offscreenPageLimit = 1
            clipToPadding = false
            clipChildren = false
            setPageTransformer(MarginPageTransformer(dpToPx(12f)))
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    viewModel.savedRoomPageIndex = position
                    updateRoomDots(position, myRoomCardsAdapter.itemCount)
                }
            })
        }

        popularAreaAdapter = PopularAreaAdapter { area ->
            val filter = RoomFilter(
                keyword = area.name,
                province = if (area.province.contains("Đà Nẵng")) "Thành phố Đà Nẵng" else area.province,
                ward = if (area.type == "Phường") area.name else null
            )
            SearchResultActivity.start(requireContext(), filter)
        }
        binding.rvPopularAreas.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = popularAreaAdapter
        }
        popularAreaAdapter.submitList(getSamplePopularAreas())
    }

    private fun setupListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadRooms(isRefresh = true)
            viewModel.checkAuthAndLoadData()
        }

        binding.layoutErrorState.btnRetry.setOnClickListener {
            viewModel.loadRooms(isRefresh = true)
            viewModel.checkAuthAndLoadData()
        }

        binding.nestedScrollView.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
                if (scrollY >= (v.getChildAt(0).measuredHeight - v.measuredHeight - 400)) {
                    viewModel.loadNextPage()
                }
            }
        )

        binding.btnGuestLogin.setOnClickListener {
            listener?.onNavigateToLoginRequested()
        }

        val openTweaksAction = View.OnLongClickListener {
            openDevTweaksDialog()
            true
        }
        binding.viewHeaderContainer.setOnLongClickListener(openTweaksAction)

        binding.btnNotification.setOnClickListener {
            toast("Chưa có thông báo mới")
        }

        binding.btnExploreFilter.setOnClickListener {
            val dialog = com.rentify.app.tenant.ui.search.FilterBottomSheetDialogFragment.newInstance(
                com.rentify.app.tenant.ui.search.RoomFilter.default()
            )
            dialog.onFilterApplied = { filter ->
                com.rentify.app.tenant.ui.search.SearchResultActivity.start(requireContext(), filter)
            }
            dialog.show(childFragmentManager, com.rentify.app.tenant.ui.search.FilterBottomSheetDialogFragment.TAG)
        }

        binding.layoutSearchBar.setOnClickListener {
            com.rentify.app.tenant.ui.search.SearchResultActivity.start(requireContext())
        }

        binding.btnUtilNearMe.setOnClickListener {
            toast("Tìm phòng trọ gần vị trí của bạn...")
        }

        binding.btnUtilSavedRooms.setOnClickListener {
            if (viewModel.isLoggedIn.value) {
                val intent = android.content.Intent(requireContext(), com.rentify.app.tenant.ui.favoriteroom.FavoriteRoomsActivity::class.java)
                startActivity(intent)
            } else {
                showLoginRequiredSheet(LoginFeature.PROFILE) {
                    listener?.onNavigateToLoginForTabRequested(R.id.nav_profile)
                }
            }
        }

        binding.btnUtilMatchFriends.setOnClickListener {
            if (viewModel.isLoggedIn.value) {
                listener?.onNavigateToMatchingRequested()
            } else {
                showLoginRequiredSheet(LoginFeature.ROOMMATE) {
                    listener?.onNavigateToLoginForTabRequested(R.id.nav_matching)
                }
            }
        }

        binding.btnUtilInvoices.setOnClickListener {
            if (viewModel.isLoggedIn.value) {
                toast("Mở hóa đơn tháng")
            } else {
                showLoginRequiredSheet(LoginFeature.MY_ROOM) {
                    listener?.onNavigateToLoginForTabRequested(R.id.nav_my_room)
                }
            }
        }

        binding.btnViewAllBuildings.setOnClickListener {
            BuildingListActivity.start(requireContext())
        }

        binding.btnViewAllAreas.setOnClickListener {
            SearchResultActivity.start(
                requireContext(),
                RoomFilter(province = "Thành phố Đà Nẵng")
            )
        }
    }

    private fun updateRoomDots(selectedIndex: Int, totalCount: Int) {
        if (totalCount <= 1) {
            binding.layoutMyRoom.containerIndicator.visibility = View.GONE
            return
        }

        binding.layoutMyRoom.containerIndicator.visibility = View.VISIBLE
        binding.layoutMyRoom.pagerMyRooms.contentDescription = "Phòng ${selectedIndex + 1} trên $totalCount"

        if (totalCount > 5) {
            binding.layoutMyRoom.layoutDotsIndicator.visibility = View.GONE
            binding.layoutMyRoom.tvRoomPageCounter.visibility = View.VISIBLE
            binding.layoutMyRoom.tvRoomPageCounter.text = "${selectedIndex + 1}/$totalCount"
        } else {
            binding.layoutMyRoom.layoutDotsIndicator.visibility = View.VISIBLE
            binding.layoutMyRoom.tvRoomPageCounter.visibility = View.GONE

            val dotsContainer = binding.layoutMyRoom.layoutDotsIndicator
            if (dotsContainer.childCount != totalCount) {
                dotsContainer.removeAllViews()
                for (i in 0 until totalCount) {
                    val dot = View(requireContext()).apply {
                        val isCurrent = i == selectedIndex
                        background = ContextCompat.getDrawable(
                            requireContext(),
                            if (isCurrent) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive
                        )
                        val w = if (isCurrent) dpToPx(18f) else dpToPx(6f)
                        val h = dpToPx(6f)
                        val lp = LinearLayout.LayoutParams(w, h).apply {
                            marginEnd = if (i < totalCount - 1) dpToPx(6f) else 0
                        }
                        layoutParams = lp
                    }
                    dotsContainer.addView(dot)
                }
            } else {
                for (i in 0 until totalCount) {
                    val dot = dotsContainer.getChildAt(i)
                    val isCurrent = i == selectedIndex
                    val targetWidth = if (isCurrent) dpToPx(18f) else dpToPx(6f)
                    val targetBg = if (isCurrent) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive

                    dot.background = ContextCompat.getDrawable(requireContext(), targetBg)

                    if (dot.layoutParams.width != targetWidth) {
                        val anim = ValueAnimator.ofInt(dot.layoutParams.width, targetWidth).apply {
                            duration = 200L
                            interpolator = FastOutSlowInInterpolator()
                            addUpdateListener { animator ->
                                dot.layoutParams.width = animator.animatedValue as Int
                                dot.requestLayout()
                            }
                        }
                        anim.start()
                    }
                }
            }
        }
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density + 0.5f).toInt()
    }

    private fun openDevTweaksDialog() {
        DevTweaksDialogFragment.newInstance(
            currentTweak = viewModel.devTweakHasRoom.value,
            onTweakChanged = { isChecked ->
                viewModel.setDevTweakHasRoom(isChecked)
                toast("Đã cập nhật tweak hasRoom = $isChecked")
            }
        ).show(childFragmentManager, DevTweaksDialogFragment.TAG)
    }

    private fun extractCallingName(fullName: String?): String {
        if (fullName.isNullOrBlank()) return "bạn"
        val words = fullName.trim().split("\\s+".toRegex())
        return words.lastOrNull()?.takeIf { it.isNotBlank() } ?: "bạn"
    }

    private fun getSamplePopularAreas(): List<PopularAreaItem> {
        return listOf(
            PopularAreaItem("1", "Hòa Khánh", "Phường", "Đà Nẵng"),
            PopularAreaItem("2", "Hải Châu", "Quận", "Đà Nẵng"),
            PopularAreaItem("3", "Ngũ Hành Sơn", "Quận", "Đà Nẵng"),
            PopularAreaItem("4", "Thanh Khê", "Quận", "Đà Nẵng"),
            PopularAreaItem("5", "Liên Chiểu", "Quận", "Đà Nẵng"),
            PopularAreaItem("6", "Sơn Trà", "Quận", "Đà Nẵng")
        )
    }

    companion object {
        fun newInstance(): ExploreFragment = ExploreFragment()
    }
}
