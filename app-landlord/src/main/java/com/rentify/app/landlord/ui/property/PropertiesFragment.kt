package com.rentify.app.landlord.ui.property

import android.app.Activity
import android.content.Intent
import android.os.Bundle
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

    private lateinit var buildingAdapter: BuildingAdapter
    private var currentTabIndex = 0

    private val createBuildingLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            buildingViewModel.loadBuildings()
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
            // Tab Tòa nhà
            binding.rvPropertyItems.adapter = buildingAdapter
            buildingViewModel.loadBuildings()
        } else {
            // Tab Phòng trọ (sẽ tích hợp LandlordRoomAdapter trong Commit 4)
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.tvEmptyTitle.text = "Chưa có danh sách phòng trọ"
            binding.tvEmptySubtitle.text = "Chọn tòa nhà hoặc nhấn '+ Thêm mới' để tạo phòng"
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            if (currentTabIndex == 0) {
                buildingViewModel.loadBuildings()
            } else {
                binding.swipeRefresh.isRefreshing = false
            }
        }

        binding.btnCreateNew.setOnClickListener {
            if (currentTabIndex == 0) {
                val intent = Intent(requireContext(), CreateBuildingActivity::class.java)
                createBuildingLauncher.launch(intent)
            } else {
                Toast.makeText(requireContext(), "Tính năng tạo phòng đang được chuẩn bị", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun observeData() {
        buildingViewModel.buildingListState.collectWhenStarted { state ->
            binding.swipeRefresh.isRefreshing = false
            when (state) {
                is BuildingListUiState.Loading -> {
                    if (!binding.swipeRefresh.isRefreshing) {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                }
                is BuildingListUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    if (currentTabIndex == 0) {
                        buildingAdapter.submitList(state.buildings)
                        if (state.buildings.isEmpty()) {
                            binding.layoutEmpty.visibility = View.VISIBLE
                            binding.tvEmptyTitle.text = "Chưa có tòa nhà nào"
                            binding.tvEmptySubtitle.text = "Nhấn '+ Thêm mới' để gửi yêu cầu phê duyệt tòa nhà"
                        } else {
                            binding.layoutEmpty.visibility = View.GONE
                        }
                    }
                }
                is BuildingListUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    if (currentTabIndex == 0 && buildingAdapter.itemCount == 0) {
                        binding.layoutEmpty.visibility = View.VISIBLE
                        binding.tvEmptyTitle.text = "Không thể tải danh sách"
                        binding.tvEmptySubtitle.text = state.message
                    }
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        fun newInstance(): PropertiesFragment = PropertiesFragment()
    }
}
