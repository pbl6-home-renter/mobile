package com.rentify.app.landlord.ui.operations

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.rentify.app.core.network.dto.room.RoomMyListResponseDto
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.showError
import com.rentify.app.core.ui.extension.showSuccess
import com.rentify.app.landlord.R
import com.rentify.app.landlord.databinding.ActivityLandlordIssuesBinding
import kotlinx.coroutines.launch

class LandlordIssuesActivity : BaseActivity<ActivityLandlordIssuesBinding>(
    ActivityLandlordIssuesBinding::inflate
) {
    private val issueViewModel: IssueViewModel by viewModels {
        IssueViewModelFactory(this)
    }

    private lateinit var adapter: LandlordIssueAdapter
    private var currentStatusFilter: String? = null
    private var roomsList: List<RoomMyListResponseDto> = emptyList()

    private val categoryOptions = listOf(
        "Nước" to "PLUMBING",
        "Điện" to "ELECTRICAL",
        "Thiết bị" to "APPLIANCE",
        "Kết cấu" to "STRUCTURAL",
        "An ninh" to "SECURITY",
        "Khác" to "OTHER"
    )

    private val urgencyOptions = listOf(
        "Thấp" to "LOW",
        "Bình thường" to "MEDIUM",
        "Cao" to "HIGH",
        "Khẩn cấp" to "EMERGENCY"
    )

    override fun initView() {
        binding.rootLayout.applySystemBarsPadding(top = true, bottom = true)

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnReportIssue.setOnClickListener {
            showCreateIssueBottomSheet()
        }

        adapter = LandlordIssueAdapter { issue ->
            val intent = Intent(this, LandlordIssueDetailActivity::class.java).apply {
                putExtra(LandlordIssueDetailActivity.EXTRA_ISSUE_ID, issue.id)
            }
            startActivity(intent)
        }

        binding.rvIssues.layoutManager = LinearLayoutManager(this)
        binding.rvIssues.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            loadIssues()
        }

        setupFilterChips()
        issueViewModel.loadLandlordRooms()
        loadIssues()
    }

    private fun setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: R.id.chipAll
            currentStatusFilter = when (checkedId) {
                R.id.chipOpen -> "open"
                R.id.chipInProgress -> "in_progress"
                R.id.chipResolved -> "resolved"
                R.id.chipCancelled -> "cancelled"
                else -> null
            }
            loadIssues()
        }
    }

    private fun loadIssues() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyStateView.visibility = View.GONE
        issueViewModel.loadIssues(status = currentStatusFilter)
    }

    private fun showCreateIssueBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_create_issue, null)
        dialog.setContentView(sheetView)

        val tilRoom = sheetView.findViewById<TextInputLayout>(R.id.tilIssueRoom)
        val actvRoom = sheetView.findViewById<AutoCompleteTextView>(R.id.actvIssueRoom)
        val tilTitle = sheetView.findViewById<TextInputLayout>(R.id.tilIssueTitle)
        val etTitle = sheetView.findViewById<TextInputEditText>(R.id.etIssueTitle)
        val actvCategory = sheetView.findViewById<AutoCompleteTextView>(R.id.actvIssueCategory)
        val actvUrgency = sheetView.findViewById<AutoCompleteTextView>(R.id.actvIssueUrgency)
        val etDescription = sheetView.findViewById<TextInputEditText>(R.id.etIssueDescription)
        val btnCancel = sheetView.findViewById<MaterialButton>(R.id.btnCancelCreateIssue)
        val btnSubmit = sheetView.findViewById<MaterialButton>(R.id.btnSubmitCreateIssue)

        var selectedRoomId: String? = roomsList.firstOrNull()?.id
        val roomLabels = roomsList.map { r ->
            val tenantSuffix = r.currentTenantName?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""
            "${r.name}$tenantSuffix"
        }
        actvRoom.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, roomLabels))
        if (roomLabels.isNotEmpty()) {
            actvRoom.setText(roomLabels[0], false)
        }
        actvRoom.setOnItemClickListener { _, _, position, _ ->
            selectedRoomId = roomsList.getOrNull(position)?.id
        }

        var selectedCategory = "OTHER"
        actvCategory.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categoryOptions.map { it.first }))
        actvCategory.setText(categoryOptions.last().first, false)
        actvCategory.setOnItemClickListener { _, _, position, _ ->
            selectedCategory = categoryOptions.getOrNull(position)?.second ?: "OTHER"
        }

        var selectedUrgency = "MEDIUM"
        actvUrgency.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, urgencyOptions.map { it.first }))
        actvUrgency.setText(urgencyOptions[1].first, false)
        actvUrgency.setOnItemClickListener { _, _, position, _ ->
            selectedUrgency = urgencyOptions.getOrNull(position)?.second ?: "MEDIUM"
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSubmit.setOnClickListener {
            val roomId = selectedRoomId
            if (roomId.isNullOrBlank()) {
                tilRoom.error = "Vui lòng chọn phòng"
                return@setOnClickListener
            } else {
                tilRoom.error = null
            }

            val title = etTitle.text?.toString()?.trim().orEmpty()
            if (title.isEmpty()) {
                tilTitle.error = "Vui lòng nhập tiêu đề sự cố"
                return@setOnClickListener
            } else {
                tilTitle.error = null
            }

            val desc = etDescription.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }

            issueViewModel.createIssue(
                roomId = roomId,
                title = title,
                category = selectedCategory,
                urgency = selectedUrgency,
                description = desc
            )
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    issueViewModel.roomsState.collect { rooms ->
                        roomsList = rooms
                    }
                }

                launch {
                    issueViewModel.issueListState.collect { state ->
                        binding.swipeRefresh.isRefreshing = false
                        binding.progressBar.visibility = View.GONE

                        when (state) {
                            is IssueListUiState.Success -> {
                                if (state.issues.isEmpty()) {
                                    binding.emptyStateView.visibility = View.VISIBLE
                                    binding.rvIssues.visibility = View.GONE
                                } else {
                                    binding.emptyStateView.visibility = View.GONE
                                    binding.rvIssues.visibility = View.VISIBLE
                                    adapter.submitList(state.issues)
                                }
                            }
                            is IssueListUiState.Error -> {
                                binding.emptyStateView.visibility = View.VISIBLE
                                binding.rvIssues.visibility = View.GONE
                                binding.tvEmptyMessage.text = state.message
                                showError(state.message)
                            }
                            IssueListUiState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                            }
                        }
                    }
                }

                launch {
                    issueViewModel.actionState.collect { state ->
                        when (state) {
                            is IssueActionUiState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                            }
                            is IssueActionUiState.Success -> {
                                binding.progressBar.visibility = View.GONE
                                showSuccess(state.message)
                                issueViewModel.resetActionState()
                            }
                            is IssueActionUiState.Error -> {
                                binding.progressBar.visibility = View.GONE
                                showError(state.message)
                                issueViewModel.resetActionState()
                            }
                            IssueActionUiState.Idle -> {}
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadIssues()
    }
}
