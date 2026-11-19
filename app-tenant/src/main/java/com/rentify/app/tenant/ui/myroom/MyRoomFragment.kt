package com.rentify.app.tenant.ui.myroom

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.net.Uri
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
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
import com.rentify.app.core.network.dto.room.RoomHubInvoiceDto
import com.rentify.app.core.ui.base.BaseFragment
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.core.ui.extension.showError
import com.rentify.app.core.ui.extension.showMessage
import com.rentify.app.core.ui.extension.toDisplayDate
import com.rentify.app.core.ui.extension.toVnd
import com.rentify.app.core.ui.R as CoreR
import com.rentify.app.tenant.R
import com.rentify.app.tenant.databinding.FragmentMyRoomBinding
import com.rentify.app.tenant.databinding.TabMyRoomsBinding
import com.rentify.app.tenant.ui.chat.detail.ChatDetailActivity
import com.rentify.app.tenant.ui.contract.ContractUiHelper
import com.rentify.app.tenant.ui.contract.detail.ContractDetailActivity
import com.rentify.app.tenant.ui.dialog.LoginFeature
import com.rentify.app.tenant.ui.dialog.showLoginRequiredSheet
import com.rentify.app.tenant.ui.invoice.detail.InvoiceDetailActivity
import com.rentify.app.tenant.ui.invoice.list.MyInvoicesActivity
import com.rentify.app.tenant.ui.issue.list.MyIssuesActivity
import com.rentify.app.tenant.ui.room.detail.RoomDetailActivity
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
    private lateinit var contractsAdapter: MyRoomsAdapter
    private var currentRoomId: String? = null
    private var currentRoomName: String? = null
    private var currentContractId: String? = null
    private var hasBlueHeader = false

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
        updateStatusBarIcons()
        viewModel.loadRoomHub()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            setLightStatusBarIcons(false)
        } else {
            updateStatusBarIcons()
        }
    }

    private fun updateStatusBarIcons() {
        setLightStatusBarIcons(!hasBlueHeader)
    }

    private fun setLightStatusBarIcons(light: Boolean) {
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = light
        }
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
        contractsAdapter = MyRoomsAdapter(
            onRoomClick = { contract ->
                RoomDetailActivity.start(requireContext(), contract.roomId)
            }
        )
        binding.rvContractsList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = contractsAdapter
        }
    }

    private fun setupTabs() {
        val tabLayout = binding.tabLayoutContracts
        listOf(R.string.my_rooms_tab_active, R.string.my_rooms_tab_ended).forEach { nameRes ->
            val tabBinding = TabMyRoomsBinding.inflate(LayoutInflater.from(requireContext()))
            tabBinding.tvTabName.setText(nameRes)
            tabLayout.addTab(tabLayout.newTab().setCustomView(tabBinding.root))
        }
        updateTabTypeface(tabLayout.getTabAt(0), selected = true)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                updateTabTypeface(tab, selected = true)
                viewModel.selectTab(tab?.position ?: 0)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                updateTabTypeface(tab, selected = false)
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun updateTabTypeface(tab: TabLayout.Tab?, selected: Boolean) {
        val nameView = tab?.customView?.let { TabMyRoomsBinding.bind(it).tvTabName } ?: return
        val fontRes = if (selected) CoreR.font.plus_jakarta_sans_medium else CoreR.font.plus_jakarta_sans
        nameView.typeface = ResourcesCompat.getFont(requireContext(), fontRes) ?: Typeface.DEFAULT
    }

    private fun updateTabCount(position: Int, count: Int) {
        val customView = binding.tabLayoutContracts.getTabAt(position)?.customView ?: return
        TabMyRoomsBinding.bind(customView).tvTabCount.text = count.toString()
    }

    private fun setupListeners() {
        binding.btnGuestLogin.setOnSingleClickListener {
            showLoginRequiredSheet(LoginFeature.MY_ROOM) {
                listener?.onNavigateToLoginRequested()
            }
        }

        binding.btnExploreRooms.setOnSingleClickListener {
            listener?.onNavigateToExploreRequested()
        }

        binding.btnEmptyFindRoom.setOnSingleClickListener {
            listener?.onNavigateToExploreRequested()
        }

        binding.layoutError.btnRetry.setOnSingleClickListener {
            viewModel.loadRoomHub()
        }

        binding.btnChatLandlord.setOnSingleClickListener {
            viewModel.contactLandlord()
        }

        binding.btnViewAllContracts.setOnSingleClickListener {
            viewModel.toggleViewAllContracts()
        }

        binding.btnInvoiceHistory.setOnSingleClickListener {
            MyInvoicesActivity.start(requireContext())
        }

        binding.btnInvoiceHistorySettled.setOnSingleClickListener {
            MyInvoicesActivity.start(requireContext())
        }

        binding.actionInvoices.setOnSingleClickListener {
            MyInvoicesActivity.start(requireContext())
        }

        binding.actionContract.setOnSingleClickListener {
            currentContractId?.let { ContractDetailActivity.start(requireContext(), it) }
        }

        binding.actionIssues.setOnSingleClickListener {
            currentRoomId?.let { roomId ->
                MyIssuesActivity.start(requireContext(), roomId, currentRoomName)
            } ?: MyIssuesActivity.start(requireContext())
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        binding.swipeRefreshLayout.isRefreshing = false
                        val isDashboard = state is MyRoomUiState.SingleRoom
                        val isListState = state is MyRoomUiState.MultiRooms || state is MyRoomUiState.NoRoom
                        binding.swipeRefreshLayout.setBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                when {
                                    isDashboard -> CoreR.color.rentify_primary_light
                                    isListState -> CoreR.color.rentify_surface
                                    else -> CoreR.color.rentify_background
                                }
                            )
                        )
                        hasBlueHeader = isDashboard
                        updateStatusBarIcons()
                        when (state) {
                            is MyRoomUiState.Loading -> {
                                binding.pbLoading.isVisible = true
                                binding.layoutGuest.isVisible = false
                                binding.layoutNoRoom.isVisible = false
                                binding.layoutError.root.isVisible = false
                                binding.layoutActiveRoom.isVisible = false
                                binding.layoutMultiRooms.isVisible = false
                            }
                            is MyRoomUiState.Guest -> {
                                binding.pbLoading.isVisible = false
                                binding.layoutGuest.isVisible = true
                                binding.layoutNoRoom.isVisible = false
                                binding.layoutError.root.isVisible = false
                                binding.layoutActiveRoom.isVisible = false
                                binding.layoutMultiRooms.isVisible = false
                            }
                            is MyRoomUiState.NoRoom -> {
                                binding.pbLoading.isVisible = false
                                binding.layoutGuest.isVisible = false
                                binding.layoutNoRoom.isVisible = true
                                binding.layoutError.root.isVisible = false
                                binding.layoutActiveRoom.isVisible = false
                                binding.layoutMultiRooms.isVisible = false
                            }
                            is MyRoomUiState.Error -> {
                                binding.pbLoading.isVisible = false
                                binding.layoutGuest.isVisible = false
                                binding.layoutNoRoom.isVisible = false
                                binding.layoutError.root.isVisible = true
                                binding.layoutActiveRoom.isVisible = false
                                binding.layoutMultiRooms.isVisible = false
                            }
                            is MyRoomUiState.SingleRoom -> {
                                binding.pbLoading.isVisible = false
                                binding.layoutGuest.isVisible = false
                                binding.layoutNoRoom.isVisible = false
                                binding.layoutError.root.isVisible = false
                                binding.layoutMultiRooms.isVisible = false
                                binding.layoutActiveRoom.isVisible = true
                                binding.btnViewAllContracts.isVisible = true
                                bindActiveRoomData(state.hub)
                            }
                            is MyRoomUiState.MultiRooms -> {
                                binding.pbLoading.isVisible = false
                                binding.layoutGuest.isVisible = false
                                binding.layoutNoRoom.isVisible = false
                                binding.layoutError.root.isVisible = false
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
                                showMessage(event.message)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun bindMultiRoomsData(state: MyRoomUiState.MultiRooms) {
        val currentTab = binding.tabLayoutContracts.selectedTabPosition
        if (currentTab != state.selectedTab && state.selectedTab in 0..1) {
            binding.tabLayoutContracts.getTabAt(state.selectedTab)?.select()
        }

        updateTabCount(0, state.activeContracts.size)
        updateTabCount(1, state.endedContracts.size)

        val bothEmpty = state.activeContracts.isEmpty() && state.endedContracts.isEmpty()
        binding.tabLayoutContracts.isVisible = !bothEmpty
        binding.dividerTabs.isVisible = !bothEmpty

        val showActive = bothEmpty || state.selectedTab == 0
        val displayList: List<ContractMyListResponseDto> = if (showActive) {
            state.activeContracts
        } else {
            state.endedContracts
        }

        if (displayList.isEmpty()) {
            binding.rvContractsList.isVisible = false
            binding.layoutEmptyContracts.isVisible = true
            binding.tvEmptyContractsTitle.setText(
                if (showActive) R.string.my_rooms_empty_active_title else R.string.my_rooms_empty_ended_title
            )
            binding.tvEmptyContractsDesc.setText(
                if (showActive) R.string.my_rooms_empty_active_desc else R.string.my_rooms_empty_ended_desc
            )
            binding.btnEmptyFindRoom.isVisible = showActive
        } else {
            binding.layoutEmptyContracts.isVisible = false
            binding.rvContractsList.isVisible = true
            contractsAdapter.submitList(displayList.map { toCardItem(it, isEnded = !showActive) })
        }
    }

    private fun toCardItem(contract: ContractMyListResponseDto, isEnded: Boolean): MyRoomCardItem {
        val footer = if (isEnded) {
            MyRoomFooter.Ended(ContractUiHelper.formatDate(contract.endDate).drop(3))
        } else {
            null
        }
        return MyRoomCardItem(contract, footer)
    }

    private fun bindActiveRoomData(hub: MyRoomHubResponseDto) {
        val room = hub.room
        val landlord = hub.landlord
        val invoice = hub.currentInvoice

        currentRoomId = room.id
        currentRoomName = room.name
        currentContractId = hub.contract.id

        binding.ivRoomCover.load(resolveRoomCoverUrl(room.photoUrls.firstOrNull(), room.id)) {
            crossfade(true)
        }
        binding.tvRoomTitle.text = room.name
        binding.tvRoomBuilding.text = room.buildingName

        bindInvoiceCard(invoice)

        val issuesCount = hub.activeIssuesCount
        binding.tvIssuesBadge.isVisible = issuesCount > 0
        binding.tvIssuesBadge.text = issuesCount.toString()

        binding.layoutLandlordRow.isVisible = landlord != null
        if (landlord != null) {
            binding.landlordAvatarView.bindPersonal(landlord.id, landlord.name, landlord.avatarUrl)
            binding.tvLandlordName.text = landlord.name
            binding.btnCallLandlord.setOnSingleClickListener {
                callPhoneNumber(landlord.phone)
            }
        }

        roommateAdapter.submitList(hub.roommates)
        binding.tvPeopleTitle.isVisible = landlord != null || hub.roommates.isNotEmpty()
        binding.rvRoommates.isVisible = hub.roommates.isNotEmpty()
    }

    private fun bindInvoiceCard(invoice: RoomHubInvoiceDto?) {
        val month = invoice?.period?.substringAfter('-', "")?.take(2)?.toIntOrNull()
        val isSettled = invoice == null || invoice.invoiceStatus.equals("paid", ignoreCase = true) || invoice.remainingAmount <= 0

        binding.cardCurrentInvoice.setOnSingleClickListener {
            if (invoice != null && !isSettled) {
                InvoiceDetailActivity.start(requireContext(), invoice.id)
            } else {
                MyInvoicesActivity.start(requireContext())
            }
        }

        binding.layoutInvoicePending.isVisible = !isSettled
        binding.layoutInvoiceSettled.isVisible = isSettled

        if (invoice == null || isSettled) {
            val settledIcon = if (invoice == null) CoreR.drawable.ic_receipt else CoreR.drawable.ic_check_circle
            val settledColor = if (invoice == null) CoreR.color.rentify_text_muted else CoreR.color.rentify_teal
            val color = ContextCompat.getColor(requireContext(), settledColor)
            binding.ivInvoiceSettledIcon.setImageResource(settledIcon)
            binding.ivInvoiceSettledIcon.imageTintList = ColorStateList.valueOf(color)
            binding.tvInvoiceSettledText.setTextColor(color)
            binding.tvInvoiceSettledText.text = if (invoice == null || month == null) {
                getString(R.string.my_room_invoice_none)
            } else {
                getString(R.string.my_room_invoice_paid, month)
            }
            return
        }

        val isOverdue = invoice.invoiceStatus.equals("overdue", ignoreCase = true)
        binding.tvInvoicePeriod.text = if (month != null) {
            getString(R.string.my_room_invoice_title, month)
        } else {
            invoice.period
        }
        binding.tvInvoiceDueDate.text = getString(R.string.my_room_invoice_due, invoice.dueDate.toDisplayDate().take(5))
        binding.tvInvoiceDueDate.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (isOverdue) CoreR.color.rentify_error else CoreR.color.rentify_warning_text
            )
        )
        binding.tvInvoiceAmount.text = invoice.remainingAmount.toVnd()
        binding.btnPayVietQr.setOnSingleClickListener {
            VietQrBottomSheetDialog.newInstance(
                invoiceCode = invoice.code,
                period = invoice.period,
                amount = invoice.remainingAmount,
                qrUrl = invoice.vietQrPaymentUrl
            ).show(childFragmentManager, "VietQrBottomSheet")
        }
    }

    private fun callPhoneNumber(phone: String?) {
        if (!phone.isNullOrBlank()) {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            startActivity(intent)
        } else {
            showError("Không có số điện thoại liên hệ")
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

        fun resolveRoomCoverUrl(url: String?, seedId: String): String {
            if (!url.isNullOrBlank() && (url.contains("unsplash.com") || url.contains("cloudinary.com") || url.contains("res.cloudinary.com"))) {
                return url
            }
            val index = kotlin.math.abs(seedId.hashCode()) % MOCK_ROOM_PHOTOS.size
            return MOCK_ROOM_PHOTOS[index]
        }
    }
}
