package com.rentify.app.tenant.ui.issue.create

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.core.ui.extension.toast
import com.rentify.app.tenant.R
import com.rentify.app.tenant.databinding.ActivityCreateIssueBinding
import com.rentify.app.tenant.ui.issue.detail.IssueDetailActivity
import kotlinx.coroutines.launch

class CreateIssueActivity : BaseActivity<ActivityCreateIssueBinding>(ActivityCreateIssueBinding::inflate) {

    private val roomId: String by lazy {
        intent.getStringExtra(EXTRA_ROOM_ID).orEmpty()
    }

    private val roomName: String by lazy {
        intent.getStringExtra(EXTRA_ROOM_NAME).orEmpty()
    }

    private val viewModel: CreateIssueViewModel by viewModels {
        CreateIssueViewModelFactory(this, roomId)
    }

    override fun initView() {
        binding.root.applySystemBarsPadding(top = true, bottom = true)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.tvRoomName.text = if (roomName.isNotEmpty()) roomName else "Phòng của bạn"

        setupCategoryChips()
        setupUrgencyChips()
        setupListeners()
    }

    private fun setupCategoryChips() {
        binding.chipGroupCategory.setOnCheckedStateChangeListener { _, checkedIds ->
            val category = when (checkedIds.firstOrNull()) {
                R.id.chipPlumbing -> "PLUMBING"
                R.id.chipElectrical -> "ELECTRICAL"
                R.id.chipAppliance -> "APPLIANCE"
                R.id.chipStructural -> "STRUCTURAL"
                R.id.chipSecurity -> "SECURITY"
                else -> "OTHER"
            }
            viewModel.setCategory(category)
        }
    }

    private fun setupUrgencyChips() {
        binding.chipGroupUrgency.setOnCheckedStateChangeListener { _, checkedIds ->
            val urgency = when (checkedIds.firstOrNull()) {
                R.id.chipUrgencyLow -> "LOW"
                R.id.chipUrgencyMedium -> "MEDIUM"
                R.id.chipUrgencyHigh -> "HIGH"
                R.id.chipUrgencyEmergency -> "EMERGENCY"
                else -> "MEDIUM"
            }
            viewModel.setUrgency(urgency)
        }
    }

    private fun setupListeners() {
        binding.btnSubmitIssue.setOnSingleClickListener {
            binding.tilTitle.error = null
            val title = binding.etTitle.text?.toString().orEmpty()
            val description = binding.etDescription.text?.toString().orEmpty()
            viewModel.submitIssue(title, description)
        }
    }

    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.btnSubmitIssue.isEnabled = !isLoading
                        binding.btnSubmitIssue.text = if (isLoading) "Đang gửi báo cáo..." else "Gửi báo cáo sự cố"
                    }
                }

                launch {
                    viewModel.uiEvent.collect { event ->
                        when (event) {
                            is CreateIssueUiEvent.ValidationError -> {
                                binding.tilTitle.error = event.message
                                toast(event.message)
                            }
                            is CreateIssueUiEvent.ShowError -> {
                                toast(event.message)
                            }
                            is CreateIssueUiEvent.Success -> {
                                toast(event.message)
                                setResult(Activity.RESULT_OK)
                                if (event.issueId.isNotEmpty()) {
                                    IssueDetailActivity.start(this@CreateIssueActivity, event.issueId)
                                }
                                finish()
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_ROOM_ID = "EXTRA_ROOM_ID"
        private const val EXTRA_ROOM_NAME = "EXTRA_ROOM_NAME"

        fun start(context: Context, roomId: String, roomName: String) {
            val intent = Intent(context, CreateIssueActivity::class.java).apply {
                putExtra(EXTRA_ROOM_ID, roomId)
                putExtra(EXTRA_ROOM_NAME, roomName)
            }
            context.startActivity(intent)
        }
    }
}

