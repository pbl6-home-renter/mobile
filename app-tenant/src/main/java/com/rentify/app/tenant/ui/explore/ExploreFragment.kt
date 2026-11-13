package com.rentify.app.tenant.ui.explore

import android.content.Context
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.rentify.app.tenant.ui.dialog.showRequireLoginDialog
import com.rentify.app.tenant.ui.room.detail.RoomDetailActivity
import com.rentify.app.tenant.ui.search.RoomFilter
import com.rentify.app.tenant.ui.search.SearchResultActivity

class ExploreFragment : BaseFragment<FragmentExploreBinding>(FragmentExploreBinding::inflate) {

    interface ExploreListener {
        fun onNavigateToLoginRequested()
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = parentFragment as? ExploreListener ?: context as? ExploreListener
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun initView() {
        setupStatusBarInsets()
        setupRecyclerViews()
        setupListeners()
    }

    private fun setupStatusBarInsets() {
        val initialPaddingTop = binding.layoutHeaderContent.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            binding.layoutHeaderContent.setPadding(
                binding.layoutHeaderContent.paddingLeft,
                initialPaddingTop + statusBarTop,
                binding.layoutHeaderContent.paddingRight,
                binding.layoutHeaderContent.paddingBottom
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
                binding.tvUserGreeting.text = "Xin chào, ${user.name}"
                binding.tvUserAvatarLetter.text = extractInitials(user.name)
            } else {
                binding.tvUserGreeting.text = "Xin chào!"
                binding.tvUserAvatarLetter.text = "RT"
            }
        }

        viewModel.hasRoomVisible.collectWhenStarted { hasRoom ->
            binding.containerMyRoom.showIf(hasRoom)
        }

        viewModel.myRoomHub.collectWhenStarted { hub ->
            if (hub != null) {
                val roomCard = binding.layoutMyRoom
                roomCard.tvMyRoomTitle.text = "${hub.room.name} · ${hub.room.buildingName}"
                roomCard.tvMyRoomAddress.text = hub.room.address
                roomCard.tvMyRoomLandlord.text = "Chủ trọ: ${hub.landlord.name}"

                // TODO: Xóa bỏ workaround resolveImageUrl khi kết nối Backend production trả về URL ảnh thật
                val photo = RoomForYouAdapter.resolveImageUrl(hub.room.photoUrls.firstOrNull(), 0)
                roomCard.ivMyRoomThumbnail.load(photo) {
                    crossfade(true)
                    placeholder(CoreUiR.drawable.ic_image_placeholder)
                    error(CoreUiR.drawable.ic_image_placeholder)
                }

                hub.currentInvoice?.let { inv ->
                    roomCard.tvInvoicePeriod.text = "Hóa đơn ${inv.period}"
                    roomCard.tvInvoiceAmount.text = inv.totalAmount.toVnd()
                    roomCard.tvInvoiceStatusNote.text = "Chưa đóng · hạn ${inv.dueDate}"
                }

                roomCard.tvContractEndDate.text = "Đến ${hub.contract.endDate}"
                roomCard.tvContractStatusTag.text = hub.contract.status
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
                        binding.layoutEmptyState.root.visibility = View.GONE
                        binding.layoutErrorState.root.visibility = View.GONE
                    }
                }
                is UiState.Success<List<RoomPublicListResponseDto>> -> {
                    roomAdapter.submitList(state.data)
                    binding.layoutEmptyState.root.showIf(state.data.isEmpty())
                    binding.layoutErrorState.root.visibility = View.GONE
                }
                is UiState.Error -> {
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

        viewModel.isLoadingMore.collectWhenStarted { isLoadingMore ->
            binding.pbLoadingMore.showIf(isLoadingMore)
        }
    }

    private fun setupRecyclerViews() {
        roomAdapter = RoomForYouAdapter(
            onRoomClick = { room ->
                RoomDetailActivity.start(requireContext(), room.id, isFavorited = room.isFavorited)
            },
            onFavoriteClick = { room ->
                if (viewModel.isLoggedIn.value) {
                    viewModel.toggleFavorite(room)
                } else {
                    showRequireLoginDialog("Vui lòng đăng nhập để lưu phòng trọ yêu thích.") {
                        listener?.onNavigateToLoginRequested()
                    }
                }
            }
        )
        binding.rvRooms.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = roomAdapter
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
        binding.btnAvatar.setOnLongClickListener(openTweaksAction)
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
                showRequireLoginDialog("Vui lòng đăng nhập để xem danh sách phòng trọ đã lưu.") {
                    listener?.onNavigateToLoginRequested()
                }
            }
        }

        binding.btnUtilMatchFriends.setOnClickListener {
            if (viewModel.isLoggedIn.value) {
                listener?.onNavigateToMatchingRequested()
            } else {
                showRequireLoginDialog("Vui lòng đăng nhập để tạo hồ sơ và kết nối bạn ở ghép.") {
                    listener?.onNavigateToLoginRequested()
                }
            }
        }

        binding.btnUtilInvoices.setOnClickListener {
            if (viewModel.isLoggedIn.value) {
                toast("Mở hóa đơn tháng")
            } else {
                showRequireLoginDialog("Vui lòng đăng nhập để xem hợp đồng và hóa đơn thuê phòng.") {
                    listener?.onNavigateToLoginRequested()
                }
            }
        }

        binding.btnViewAllAreas.setOnClickListener {
            SearchResultActivity.start(
                requireContext(),
                RoomFilter(province = "Thành phố Đà Nẵng")
            )
        }

        val roomCard = binding.layoutMyRoom
        roomCard.btnRoomDetails.setOnClickListener {
            listener?.onNavigateToMyRoomRequested()
        }
        roomCard.btnReportIssue.setOnClickListener {
            if (viewModel.isLoggedIn.value) {
                toast("Mở biểu mẫu báo sự cố")
            } else {
                showRequireLoginDialog("Vui lòng đăng nhập để gửi báo cáo sự cố phòng trọ.") {
                    listener?.onNavigateToLoginRequested()
                }
            }
        }
        roomCard.btnContactLandlord.setOnClickListener {
            if (viewModel.isLoggedIn.value) {
                listener?.onNavigateToChatRequested()
            } else {
                showRequireLoginDialog("Vui lòng đăng nhập để trò chuyện với chủ nhà trọ.") {
                    listener?.onNavigateToLoginRequested()
                }
            }
        }
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

    private fun extractInitials(name: String): String {
        val words = name.trim().split("\\s+".toRegex())
        return when {
            words.size >= 2 -> "${words[0].take(1)}${words.last().take(1)}".uppercase()
            words.isNotEmpty() && words[0].isNotEmpty() -> words[0].take(2).uppercase()
            else -> "RT"
        }
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
