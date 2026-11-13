package com.rentify.app.tenant.ui.myroom

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.google.android.material.tabs.TabLayout
import com.rentify.app.core.network.dto.contract.ContractMyListResponseDto
import com.rentify.app.core.network.dto.room.MyRoomHubResponseDto
import com.rentify.app.core.ui.base.BaseFragment
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.core.ui.extension.toDisplayDate
import com.rentify.app.core.ui.extension.toast
import com.rentify.app.core.ui.extension.toVnd
import com.rentify.app.core.ui.R as CoreR
import com.rentify.app.tenant.databinding.FragmentMyRoomBinding
import com.rentify.app.tenant.ui.chat.detail.ChatDetailActivity
import com.rentify.app.tenant.ui.contract.detail.ContractDetailActivity
import com.rentify.app.tenant.ui.contract.list.MyContractsAdapter
import com.rentify.app.tenant.ui.invoice.detail.InvoiceDetailActivity
import com.rentify.app.tenant.ui.invoice.list.MyInvoicesActivity
import com.rentify.app.tenant.ui.issue.create.CreateIssueActivity
import com.rentify.app.tenant.ui.issue.list.MyIssuesActivity
import kotlinx.coroutines.launch

class MyRoomFragment : BaseFragment<FragmentMyRoomBinding>(FragmentMyRoomBinding::inflate) {

    interface MyRoomListener {
        fun onNavigateToLoginRequested()
        fun onNavigateToExploreRequested()
    }

    private var listener: MyRoomListener? = null

    private val viewModel: MyRoomViewModel by viewModels {
        MyRoomViewModelFactory(requireContext())
    }

    private lateinit var roommateAdapter: RoommateAdapter
    private lateinit var contractsAdapter: MyContractsAdapter
    private var currentRoomId: String? = null
    private var currentRoomName: String? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = parentFragment as? MyRoomListener ?: context as? MyRoomListener
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun initView() {
        binding.root.applySystemBarsPadding(top = true, bottom = false)

        setupSwipeRefresh()
        setupRecyclerViews()
        setupTabs()
        setupListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadRoomHub()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(
            CoreR.color.rentify_primary_gradient_start,
            CoreR.color.rentify_primary_gradient_end
        )
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadRoomHub(isPullToRefresh = true)
        }
    }

    private fun setupRecyclerViews() {
        // Roommates in active room
        roommateAdapter = RoommateAdapter(
            onChatClick = { roommate ->
                viewModel.contactRoommate(roommate)
            }
        )
        binding.rvRoommates.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = roommateAdapter
            isNestedScrollingEnabled = false
        }

        // Multi contracts list
        contractsAdapter = MyContractsAdapter(
            onContractClick = { contract ->
                ContractDetailActivity.start(requireContext(), contract.id)
            }
        )
        binding.rvContractsList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = contractsAdapter
        }
    }

    private fun setupTabs() {
        binding.tabLayoutContracts.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val position = tab?.position ?: 0
                viewModel.selectTab(position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupListeners() {
        binding.btnGuestLogin.setOnSingleClickListener {
            listener?.onNavigateToLoginRequested()
        }

        binding.btnExploreRooms.setOnSingleClickListener {
            listener?.onNavigateToExploreRequested()
        }

        binding.btnRetry.setOnSingleClickListener {
            viewModel.loadRoomHub()
        }

        binding.btnChatLandlord.setOnSingleClickListener {
            viewModel.contactLandlord()
        }

        binding.btnViewAllContracts.setOnSingleClickListener {
            viewModel.toggleViewAllContracts()
        }

        binding.btnViewAllInvoices.setOnSingleClickListener {
            MyInvoicesActivity.start(requireContext())
        }

        binding.btnViewInvoiceHistorySettled.setOnSingleClickListener {
            MyInvoicesActivity.start(requireContext())
        }

        binding.btnBackToDashboard.setOnSingleClickListener {
            viewModel.toggleViewAllContracts()
        }

        binding.cardIssues.setOnSingleClickListener {
            currentRoomId?.let { roomId ->
                MyIssuesActivity.start(requireContext(), roomId, currentRoomName)
            } ?: MyIssuesActivity.start(requireContext())
        }

        binding.btnReportIssue.setOnSingleClickListener {
            val roomId = currentRoomId
            if (!roomId.isNullOrBlank()) {
                CreateIssueActivity.start(requireContext(), roomId, currentRoomName.orEmpty())
            } else {
                toast("Không tìm thấy thông tin phòng hiện tại")
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        binding.swipeRefreshLayout.isRefreshing = false
                        when (state) {
                            is MyRoomUiState.Loading -> {
                                binding.pbLoading.isVisible = true
                                binding.layoutGuest.isVisible = false
                                binding.layoutNoRoom.isVisible = false
                                binding.layoutError.isVisible = false
                                binding.layoutActiveRoom.isVisible = false
                                binding.layoutMultiRooms.isVisible = false
                            }
                            is MyRoomUiState.Guest -> {
                                binding.pbLoading.isVisible = false
                                binding.layoutGuest.isVisible = true
                                binding.layoutNoRoom.isVisible = false
                                binding.layoutError.isVisible = false
                                binding.layoutActiveRoom.isVisible = false
                                binding.layoutMultiRooms.isVisible = false
                            }
                            is MyRoomUiState.NoRoom -> {
                                binding.pbLoading.isVisible = false
                                binding.layoutGuest.isVisible = false
                                binding.layoutNoRoom.isVisible = true
                                binding.layoutError.isVisible = false
                                binding.layoutActiveRoom.isVisible = false
                                binding.layoutMultiRooms.isVisible = false
                            }
                            is MyRoomUiState.Error -> {
                                binding.pbLoading.isVisible = false
                                binding.layoutGuest.isVisible = false
                                binding.layoutNoRoom.isVisible = false
                                binding.layoutError.isVisible = true
                                binding.layoutActiveRoom.isVisible = false
                                binding.layoutMultiRooms.isVisible = false
                                binding.tvErrorMessage.text = state.message
                            }
                            is MyRoomUiState.SingleRoom -> {
                                binding.pbLoading.isVisible = false
                                binding.layoutGuest.isVisible = false
                                binding.layoutNoRoom.isVisible = false
                                binding.layoutError.isVisible = false
                                binding.layoutMultiRooms.isVisible = false
                                binding.layoutActiveRoom.isVisible = true
                                binding.btnViewAllContracts.isVisible = true
                                bindActiveRoomData(state.hub)
                            }
                            is MyRoomUiState.MultiRooms -> {
                                binding.pbLoading.isVisible = false
                                binding.layoutGuest.isVisible = false
                                binding.layoutNoRoom.isVisible = false
                                binding.layoutError.isVisible = false
                                binding.layoutActiveRoom.isVisible = false
                                binding.layoutMultiRooms.isVisible = true
                                bindMultiRoomsData(state)
                            }
                        }
                    }
                }

                launch {
                    viewModel.uiEvent.collect { event ->
                        when (event) {
                            is MyRoomUiEvent.OpenChat -> {
                                val intent = ChatDetailActivity.createIntent(
                                    context = requireContext(),
                                    conversationId = event.conversationId,
                                    recipientName = event.recipientName,
                                    recipientAvatarUrl = event.recipientAvatarUrl
                                )
                                startActivity(intent)
                            }
                            is MyRoomUiEvent.ShowToast -> {
                                toast(event.message)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun bindMultiRoomsData(state: MyRoomUiState.MultiRooms) {
        binding.btnBackToDashboard.isVisible = state.currentSingleHub != null && state.activeContracts.size == 1

        val currentTab = binding.tabLayoutContracts.selectedTabPosition
        if (currentTab != state.selectedTab && state.selectedTab in 0..1) {
            binding.tabLayoutContracts.getTabAt(state.selectedTab)?.select()
        }

        val displayList: List<ContractMyListResponseDto> = if (state.selectedTab == 0) {
            state.activeContracts
        } else {
            state.endedContracts
        }

        if (displayList.isEmpty()) {
            binding.rvContractsList.isVisible = false
            binding.layoutEmptyContracts.isVisible = true
            if (state.selectedTab == 0) {
                binding.tvEmptyContractsTitle.text = "Không có hợp đồng đang hiệu lực"
                binding.tvEmptyContractsDesc.text = "Hiện tại bạn không có hợp đồng thuê nào đang có hiệu lực."
            } else {
                binding.tvEmptyContractsTitle.text = "Chưa có hợp đồng đã kết thúc"
                binding.tvEmptyContractsDesc.text = "Lịch sử các hợp đồng đã hết hạn hoặc kết thúc sẽ hiển thị tại đây."
            }
        } else {
            binding.layoutEmptyContracts.isVisible = false
            binding.rvContractsList.isVisible = true
            contractsAdapter.submitList(displayList)
        }
    }

    private fun bindActiveRoomData(hub: MyRoomHubResponseDto) {
        val room = hub.room
        val landlord = hub.landlord
        val contract = hub.contract
        val invoice = hub.currentInvoice

        currentRoomId = room.id
        currentRoomName = room.name

        // 1. Room info
        val resolvedCoverUrl = resolveRoomCoverUrl(room.photoUrls.firstOrNull(), room.id)
        binding.ivRoomCover.load(resolvedCoverUrl) {
            crossfade(true)
            placeholder(CoreR.drawable.ic_image_outline)
            error(CoreR.drawable.ic_image_outline)
        }
        binding.tvRoomTitle.text = "${room.name} · ${room.buildingName}"
        binding.tvRoomAddress.text = room.address
        val parsedArea = room.area?.trim()?.toDoubleOrNull()
        val displayArea = if (parsedArea != null) {
            if (parsedArea % 1.0 == 0.0) "${parsedArea.toInt()} m²" else "$parsedArea m²"
        } else {
            "25 m²"
        }
        binding.tvRoomArea.text = displayArea
        binding.tvRoomPrice.text = "${room.rentPrice.toVnd()}/tháng"

        // 2. Landlord info
        val resolvedLandlordAvatar = resolveLandlordAvatarUrl(landlord.avatarUrl, landlord.name)
        binding.ivLandlordAvatar.load(resolvedLandlordAvatar) {
            crossfade(true)
            placeholder(CoreR.drawable.ic_user_circle)
            error(CoreR.drawable.ic_user_circle)
        }
        binding.tvLandlordName.text = landlord.name
        binding.tvLandlordPhone.text = "Chủ nhà · ${landlord.phone}"

        binding.btnCallLandlord.setOnSingleClickListener {
            callPhoneNumber(landlord.phone)
        }

        // 3. Current Invoice
        val isSettled = invoice == null || invoice.invoiceStatus.equals("paid", ignoreCase = true) || invoice.remainingAmount <= 0
        binding.cardCurrentInvoice.setOnSingleClickListener {
            if (invoice != null && !isSettled) {
                InvoiceDetailActivity.start(requireContext(), invoice.id)
            } else {
                MyInvoicesActivity.start(requireContext())
            }
        }

        if (isSettled) {
            binding.layoutInvoicePending.isVisible = false
            binding.layoutInvoiceSettled.isVisible = true
            binding.layoutInvoiceHeaderActions.isVisible = false
        } else {
            binding.layoutInvoicePending.isVisible = true
            binding.layoutInvoiceSettled.isVisible = false
            binding.layoutInvoiceHeaderActions.isVisible = true
            binding.tvInvoicePeriod.text = "Kỳ ${invoice.period}"
            binding.tvInvoiceDueDate.text = "Hạn: ${invoice.dueDate.toDisplayDate()}"
            binding.tvInvoiceAmount.text = invoice.remainingAmount.toVnd()

            val statusText = when (invoice.invoiceStatus.lowercase()) {
                "overdue" -> "Quá hạn"
                "partially_paid" -> "Thanh toán 1 phần"
                else -> "Chờ thanh toán"
            }
            binding.tvInvoiceStatusBadge.text = statusText
            val statusColor = if (invoice.invoiceStatus.equals("overdue", ignoreCase = true)) {
                CoreR.color.rentify_error
            } else {
                CoreR.color.rentify_sub_card_invoice_text
            }
            binding.tvInvoiceStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), statusColor))

            binding.btnPayVietQr.setOnSingleClickListener {
                val dialog = VietQrBottomSheetDialog.newInstance(
                    invoiceCode = invoice.code,
                    period = invoice.period,
                    amount = invoice.remainingAmount,
                    qrUrl = invoice.vietQrPaymentUrl
                )
                dialog.show(childFragmentManager, "VietQrBottomSheet")
            }
        }

        // 4. Contract info
        val contractCode = contract.contractCode.trim()
        binding.tvContractCode.text = when {
            contractCode.startsWith("HD", ignoreCase = true) -> contractCode
            contractCode.isNotEmpty() -> "HĐ #$contractCode"
            else -> "HĐ thuê phòng"
        }
        val startDateStr = contract.startDate.toDisplayDate()
        val endDateStr = contract.endDate.toDisplayDate()
        binding.tvContractDuration.text = "$startDateStr - $endDateStr"

        binding.cardContract.setOnSingleClickListener {
            ContractDetailActivity.start(requireContext(), contract.id)
        }

        binding.btnViewContract.isVisible = true
        binding.btnViewContract.setOnSingleClickListener {
            if (!contract.signedContractUrl.isNullOrBlank()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(contract.signedContractUrl))
                startActivity(intent)
            } else {
                ContractDetailActivity.start(requireContext(), contract.id)
            }
        }

        // 5. Issues info
        binding.tvActiveIssuesCount.text = "Hiện có ${hub.activeIssuesCount} yêu cầu đang xử lý"

        // 6. Roommates
        val roommates = hub.roommates
        binding.tvRoommatesCountBadge.text = "${roommates.size} người"
        if (roommates.isEmpty()) {
            binding.tvNoRoommates.isVisible = true
            binding.rvRoommates.isVisible = false
        } else {
            binding.tvNoRoommates.isVisible = false
            binding.rvRoommates.isVisible = true
            roommateAdapter.submitList(roommates)
        }
    }

    private fun callPhoneNumber(phone: String?) {
        if (!phone.isNullOrBlank()) {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            startActivity(intent)
        } else {
            toast("Không có số điện thoại liên hệ")
        }
    }

    companion object {
        fun newInstance(): MyRoomFragment = MyRoomFragment()

        private val MOCK_ROOM_PHOTOS = listOf(
            "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1484154218962-a197022b5858?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1554995207-c18c203602cb?w=600&auto=format&fit=crop&q=80"
        )

        private val MOCK_LANDLORD_AVATARS = listOf(
            "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=160&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=160&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=160&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=160&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=160&auto=format&fit=crop&q=80"
        )

        fun resolveRoomCoverUrl(url: String?, seedId: String): String {
            if (!url.isNullOrBlank() && (url.contains("unsplash.com") || url.contains("cloudinary.com") || url.contains("res.cloudinary.com"))) {
                return url
            }
            val index = kotlin.math.abs(seedId.hashCode()) % MOCK_ROOM_PHOTOS.size
            return MOCK_ROOM_PHOTOS[index]
        }

        fun resolveLandlordAvatarUrl(url: String?, name: String): String {
            if (!url.isNullOrBlank() && (url.contains("unsplash.com") || url.contains("cloudinary.com") || url.contains("res.cloudinary.com"))) {
                return url
            }
            val index = kotlin.math.abs(name.hashCode()) % MOCK_LANDLORD_AVATARS.size
            return MOCK_LANDLORD_AVATARS[index]
        }
    }
}
