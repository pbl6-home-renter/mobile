package com.rentify.app.landlord.ui.property

import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.rentify.app.core.ui.base.BaseFragment
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.landlord.databinding.FragmentPropertiesBinding

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

    private val createBuildingLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            buildingViewModel.loadBuildings()
        }
    }

    private val createRoomLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            roomViewModel.loadRooms()
        }
    }

    private val buildingDetailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            buildingViewModel.loadBuildings()
        }
    }

    override fun initView() {
        binding.root.applySystemBarsPadding(top = false, bottom = false)

        setupRecyclerView()
        setupTabs()
        setupListeners()

        buildingViewModel.loadBuildings()
    }

    private fun setupRecyclerView() {
        buildingAdapter = BuildingAdapter { building ->
            val intent = BuildingDetailActivity.newIntent(requireContext(), building.id)
            buildingDetailLauncher.launch(intent)
        }

        roomAdapter = LandlordRoomAdapter(
            onRoomClick = { room ->
                Toast.makeText(requireContext(), "Phòng ${room.name} - Giá: ${room.rentPrice} đ", Toast.LENGTH_SHORT).show()
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
            binding.rvPropertyItems.adapter = buildingAdapter
            buildingViewModel.loadBuildings()
        } else {
            binding.rvPropertyItems.adapter = roomAdapter
            roomViewModel.loadRooms()
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            if (currentTabIndex == 0) {
                buildingViewModel.loadBuildings()
            } else {
                roomViewModel.loadRooms()
            }
        }

        binding.btnCreateNew.setOnClickListener {
            if (currentTabIndex == 0) {
                val intent = Intent(requireContext(), CreateBuildingActivity::class.java)
                createBuildingLauncher.launch(intent)
            } else {
                val intent = Intent(requireContext(), CreateRoomActivity::class.java)
                createRoomLauncher.launch(intent)
            }
        }
    }

    override fun observeData() {
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
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
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
                    roomAdapter.submitList(state.rooms)
                    if (state.rooms.isEmpty()) {
                        binding.layoutEmpty.visibility = View.VISIBLE
                        binding.tvEmptyTitle.text = "Chưa có phòng trọ nào"
                        binding.tvEmptySubtitle.text = "Nhấn '+ Thêm mới' để tạo phòng cho tòa nhà"
                    } else {
                        binding.layoutEmpty.visibility = View.GONE
                    }
                }
                is RoomListUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    if (roomAdapter.itemCount == 0) {
                        binding.layoutEmpty.visibility = View.VISIBLE
                        binding.tvEmptyTitle.text = "Không thể tải danh sách"
                        binding.tvEmptySubtitle.text = state.message
                    }
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    roomViewModel.resetActionState()
                }
                is RoomActionUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
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
