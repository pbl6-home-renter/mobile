package com.rentify.app.landlord.ui.property

import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayout
import com.rentify.app.core.network.dto.building.BuildingMyListResponseDto
import com.rentify.app.core.network.dto.room.RoomMyListResponseDto
import com.rentify.app.core.ui.base.BaseFragment
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.showError
import com.rentify.app.core.ui.extension.showSuccess
import com.rentify.app.landlord.R
import com.rentify.app.landlord.databinding.FragmentPropertiesBinding
import com.rentify.app.landlord.ui.notification.LandlordNotificationsActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PropertiesFragment : BaseFragment<FragmentPropertiesBinding>(FragmentPropertiesBinding::inflate) {

    private val buildingViewModel: BuildingViewModel by viewModels {
        BuildingViewModelFactory(requireContext())
    }

    private val roomViewModel: RoomViewModel by viewModels {
        RoomViewModelFactory(requireContext())
    }

    private lateinit var buildingAdapter: BuildingAdapter
    private lateinit var roomAdapter: LandlordRoomAdapter
    private var currentTabIndex = 0

    // Building filter state
    private var buildingSearchQuery: String? = null
    private var buildingStatusFilter: String? = null
    private var searchDebounceJob: Job? = null

    // Room filter state
    private var filterBuildingsList: List<BuildingMyListResponseDto> = emptyList()
    private var selectedRoomBuildingId: String? = null
    private var selectedRoomStatusFilter: String? = null
    private var selectedRoomFloorFilter: Int? = null
    private var allLoadedRooms: List<RoomMyListResponseDto> = emptyList()

    private val createBuildingLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            reloadBuildings()
            roomViewModel.loadApprovedBuildings()
        }
    }

    private val createRoomLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            reloadRooms()
            reloadBuildings()
        }
    }

    private val buildingDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            reloadBuildings()
            reloadRooms()
        }
    }

    private val roomDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            reloadRooms()
            reloadBuildings()
        }
    }

    override fun initView() {
        binding.root.applySystemBarsPadding(top = true, bottom = false)

        setupRecyclerView()
        setupTabs()
        setupBuildingFilters()
        setupRoomFilters()
        setupListeners()

        reloadBuildings()
        roomViewModel.loadApprovedBuildings()
    }

    private fun setupRecyclerView() {
        buildingAdapter = BuildingAdapter { building ->
            val intent = BuildingDetailActivity.newIntent(requireContext(), building.id)
            buildingDetailLauncher.launch(intent)
        }

        roomAdapter = LandlordRoomAdapter(
            onRoomClick = { room ->
                val intent = LandlordRoomDetailActivity.newIntent(requireContext(), room.id)
                roomDetailLauncher.launch(intent)
            },
            onMarkReadyClick = { room ->
                roomViewModel.markRoomReady(room.id)
            }
        )

        binding.rvPropertyItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = buildingAdapter
        }
    }

    private fun setupTabs() {
        binding.tabLayoutProperty.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTabIndex = tab?.position ?: 0
                handleTabChanged(currentTabIndex)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun handleTabChanged(position: Int) {
        if (position == 0) {
            binding.layoutBuildingFilters.visibility = View.VISIBLE
            binding.layoutRoomFilters.visibility = View.GONE
            binding.rvPropertyItems.adapter = buildingAdapter
            reloadBuildings()
        } else {
            binding.layoutBuildingFilters.visibility = View.GONE
            binding.layoutRoomFilters.visibility = View.VISIBLE
            binding.rvPropertyItems.adapter = roomAdapter
            roomViewModel.loadApprovedBuildings()
            reloadRooms()
        }
    }

    private fun setupBuildingFilters() {
        binding.etSearchBuilding.doAfterTextChanged { editable ->
            val query = editable?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            if (query == buildingSearchQuery) return@doAfterTextChanged
            buildingSearchQuery = query
            searchDebounceJob?.cancel()
            searchDebounceJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(300)
                if (currentTabIndex == 0) {
                    reloadBuildings()
                }
            }
        }

        binding.chipGroupBuildingStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            buildingStatusFilter = when {
                checkedIds.contains(R.id.chipBuildingApproved) -> "approved"
                checkedIds.contains(R.id.chipBuildingPending) -> "pending"
                checkedIds.contains(R.id.chipBuildingRejected) -> "rejected"
                else -> null
            }
            reloadBuildings()
        }
    }

    private fun setupRoomFilters() {
        binding.actvFilterRoomBuilding.setOnItemClickListener { _, _, position, _ ->
            selectedRoomBuildingId = if (position == 0) {
                null
            } else {
                filterBuildingsList.getOrNull(position - 1)?.id
            }
            reloadRooms()
        }

        binding.chipGroupRoomStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedRoomStatusFilter = when {
                checkedIds.contains(R.id.chipRoomStatusAvailable) -> "available"
                checkedIds.contains(R.id.chipRoomStatusOccupied) -> "occupied"
                checkedIds.contains(R.id.chipRoomStatusMaintenance) -> "maintenance"
                else -> null
            }
            reloadRooms()
        }
    }

    private fun reloadBuildings() {
        buildingViewModel.loadBuildings(
            search = buildingSearchQuery,
            status = buildingStatusFilter
        )
    }

    private fun reloadRooms() {
        roomViewModel.loadRooms(
            buildingId = selectedRoomBuildingId,
            status = selectedRoomStatusFilter
        )
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            if (currentTabIndex == 0) {
                reloadBuildings()
            } else {
                reloadRooms()
            }
        }

        binding.btnCreateNew.setOnClickListener {
            if (currentTabIndex == 0) {
                val intent = Intent(requireContext(), CreateBuildingActivity::class.java)
                createBuildingLauncher.launch(intent)
            } else {
                val intent = CreateRoomActivity.newIntent(requireContext(), selectedRoomBuildingId)
                createRoomLauncher.launch(intent)
            }
        }

        binding.btnNotifications.setOnClickListener {
            startActivity(LandlordNotificationsActivity.newIntent(requireContext()))
        }
    }

    private fun rebuildRoomFloorChips(rooms: List<RoomMyListResponseDto>) {
        val floors = rooms.mapNotNull { it.floorNumber }.filter { it > 0 }.distinct().sorted()
        if (floors.isEmpty()) {
            binding.scrollRoomFloorFilter.visibility = View.GONE
            selectedRoomFloorFilter = null
            return
        }
        if (selectedRoomFloorFilter != null && !floors.contains(selectedRoomFloorFilter)) {
            selectedRoomFloorFilter = null
        }

        binding.scrollRoomFloorFilter.visibility = View.VISIBLE
        binding.chipGroupRoomFloor.setOnCheckedStateChangeListener(null)
        binding.chipGroupRoomFloor.removeAllViews()

        val ctx = requireContext()
        val allChip = Chip(ctx).apply {
            id = View.generateViewId()
            text = "Tất cả tầng"
            isCheckable = true
            isChecked = (selectedRoomFloorFilter == null)
            textSize = 12f
        }
        binding.chipGroupRoomFloor.addView(allChip)

        val floorIdMap = mutableMapOf<Int, Int>()
        for (floor in floors) {
            val chip = Chip(ctx).apply {
                id = View.generateViewId()
                text = "Tầng $floor"
                isCheckable = true
                isChecked = (selectedRoomFloorFilter == floor)
                textSize = 12f
            }
            floorIdMap[chip.id] = floor
            binding.chipGroupRoomFloor.addView(chip)
        }

        binding.chipGroupRoomFloor.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            selectedRoomFloorFilter = if (checkedId != null) floorIdMap[checkedId] else null
            applyRoomFloorFilter()
        }
    }

    private fun applyRoomFloorFilter() {
        val filtered = if (selectedRoomFloorFilter == null) {
            allLoadedRooms
        } else {
            allLoadedRooms.filter { it.floorNumber == selectedRoomFloorFilter }
        }
        roomAdapter.submitList(filtered)
        if (filtered.isEmpty()) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.tvEmptyTitle.text = "Chưa có phòng trọ nào"
            binding.tvEmptySubtitle.text = "Nhấn '+ Thêm mới' để tạo phòng cho tòa nhà"
        } else {
            binding.layoutEmpty.visibility = View.GONE
        }
    }

    override fun observeData() {
        // Observe Approved Buildings for Room Dropdown Filter
        roomViewModel.approvedBuildings.collectWhenStarted { buildings ->
            filterBuildingsList = buildings
            val items = mutableListOf("Tất cả tòa nhà")
            items.addAll(buildings.map { it.name })
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                items
            )
            binding.actvFilterRoomBuilding.setAdapter(adapter)
            val currentName = selectedRoomBuildingId?.let { id ->
                buildings.firstOrNull { it.id == id }?.name
            } ?: "Tất cả tòa nhà"
            binding.actvFilterRoomBuilding.setText(currentName, false)
        }

        // Observe Buildings
        buildingViewModel.buildingListState.collectWhenStarted { state ->
            if (currentTabIndex != 0) return@collectWhenStarted
            binding.swipeRefresh.isRefreshing = false
            when (state) {
                is BuildingListUiState.Loading -> {
                    if (!binding.swipeRefresh.isRefreshing) {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                }
                is BuildingListUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    buildingAdapter.submitList(state.buildings)
                    if (state.buildings.isEmpty()) {
                        binding.layoutEmpty.visibility = View.VISIBLE
                        binding.tvEmptyTitle.text = "Chưa có tòa nhà nào"
                        binding.tvEmptySubtitle.text = "Nhấn '+ Thêm mới' để gửi yêu cầu phê duyệt tòa nhà"
                    } else {
                        binding.layoutEmpty.visibility = View.GONE
                    }
                }
                is BuildingListUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    if (buildingAdapter.itemCount == 0) {
                        binding.layoutEmpty.visibility = View.VISIBLE
                        binding.tvEmptyTitle.text = "Không thể tải danh sách"
                        binding.tvEmptySubtitle.text = state.message
                    }
                    showError(state.message)
                }
            }
        }

        // Observe Rooms
        roomViewModel.roomListState.collectWhenStarted { state ->
            if (currentTabIndex != 1) return@collectWhenStarted
            binding.swipeRefresh.isRefreshing = false
            when (state) {
                is RoomListUiState.Loading -> {
                    if (!binding.swipeRefresh.isRefreshing) {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                }
                is RoomListUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    allLoadedRooms = state.rooms
                    rebuildRoomFloorChips(state.rooms)
                    applyRoomFloorFilter()
                }
                is RoomListUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    if (roomAdapter.itemCount == 0) {
                        binding.layoutEmpty.visibility = View.VISIBLE
                        binding.tvEmptyTitle.text = "Không thể tải danh sách"
                        binding.tvEmptySubtitle.text = state.message
                    }
                    showError(state.message)
                }
            }
        }

        roomViewModel.actionState.collectWhenStarted { state ->
            when (state) {
                is RoomActionUiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is RoomActionUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    showSuccess(state.message)
                    roomViewModel.resetActionState()
                }
                is RoomActionUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    showError(state.message)
                    roomViewModel.resetActionState()
                }
                is RoomActionUiState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    companion object {
        fun newInstance(): PropertiesFragment = PropertiesFragment()
    }
}
