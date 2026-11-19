package com.rentify.app.landlord.ui.notification

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.rentify.app.core.network.dto.notification.NotificationListResponseDto
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.landlord.R
import com.rentify.app.landlord.databinding.ActivityLandlordNotificationsBinding
import com.rentify.app.landlord.databinding.BottomSheetNotificationSettingsBinding
import com.rentify.app.landlord.ui.billing.LandlordInvoicesActivity
import com.rentify.app.landlord.ui.contract.LandlordContractsActivity
import com.rentify.app.landlord.ui.operations.LandlordIssuesActivity

class LandlordNotificationsActivity : BaseActivity<ActivityLandlordNotificationsBinding>(
    ActivityLandlordNotificationsBinding::inflate
) {

    private val viewModel: NotificationViewModel by viewModels {
        NotificationViewModelFactory(this)
    }

    private lateinit var adapter: LandlordNotificationAdapter
    private var unreadOnlyFilter: Boolean? = null

    override fun initView() {
        binding.root.applySystemBarsPadding(top = true, bottom = true)

        adapter = LandlordNotificationAdapter { item ->
            handleNotificationClicked(item)
        }
        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        binding.rvNotifications.adapter = adapter

        binding.btnBack.setOnSingleClickListener {
            finish()
        }

        binding.btnMarkAllRead.setOnSingleClickListener {
            viewModel.markAllRead()
        }

        binding.btnNotificationSettings.setOnSingleClickListener {
            showSettingsBottomSheet()
        }

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            unreadOnlyFilter = if (checkedIds.contains(R.id.chipFilterUnread)) true else null
            viewModel.loadNotifications(unreadOnlyFilter)
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadNotifications(unreadOnlyFilter)
        }

        viewModel.loadNotifications(null)
        viewModel.loadPreferences()
    }

    private fun handleNotificationClicked(item: NotificationListResponseDto) {
        if (item.readAt.isNullOrBlank()) {
            viewModel.markRead(item.id)
        }
        when (item.type.lowercase()) {
            "invoice" -> {
                startActivity(Intent(this, LandlordInvoicesActivity::class.java))
            }
            "issue" -> {
                startActivity(Intent(this, LandlordIssuesActivity::class.java))
            }
            "system" -> {
                if (item.title.contains("CONTRACT", ignoreCase = true) ||
                    item.deepLink?.contains("contract", ignoreCase = true) == true
                ) {
                    startActivity(Intent(this, LandlordContractsActivity::class.java))
                }
            }
        }
    }

    private fun showSettingsBottomSheet() {
        val sheetBinding = BottomSheetNotificationSettingsBinding.inflate(LayoutInflater.from(this))
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(sheetBinding.root)

        val currentPrefs = viewModel.preferences.value
        val chatPref = currentPrefs.firstOrNull { it.type.equals("chat", ignoreCase = true) }?.pushEnabled ?: true
        val invoicePref = currentPrefs.firstOrNull { it.type.equals("invoice", ignoreCase = true) }?.pushEnabled ?: true
        val issuePref = currentPrefs.firstOrNull { it.type.equals("issue", ignoreCase = true) }?.pushEnabled ?: true

        sheetBinding.switchPrefChat.isChecked = chatPref
        sheetBinding.switchPrefInvoice.isChecked = invoicePref
        sheetBinding.switchPrefIssue.isChecked = issuePref

        sheetBinding.btnSavePreferences.setOnSingleClickListener {
            viewModel.updatePreferences(
                chatEnabled = sheetBinding.switchPrefChat.isChecked,
                invoiceEnabled = sheetBinding.switchPrefInvoice.isChecked,
                issueEnabled = sheetBinding.switchPrefIssue.isChecked
            )
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun observeData() {
        viewModel.listState.collectWhenStarted { state ->
            binding.swipeRefresh.isRefreshing = false
            when (state) {
                is NotificationListUiState.Loading -> {
                    if (!binding.swipeRefresh.isRefreshing) {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                }
                is NotificationListUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    adapter.submitList(state.items)
                    binding.layoutEmpty.visibility = if (state.items.isEmpty()) View.VISIBLE else View.GONE
                    if (state.items.isEmpty()) {
                        binding.tvEmptyTitle.text = if (unreadOnlyFilter == true) {
                            "Không có thông báo chưa đọc"
                        } else {
                            "Chưa có thông báo nào"
                        }
                    }
                }
                is NotificationListUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    if (adapter.itemCount == 0) {
                        binding.layoutEmpty.visibility = View.VISIBLE
                        binding.tvEmptyTitle.text = "Không tải được thông báo"
                        binding.tvEmptySubtitle.text = state.message
                    }
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.actionState.collectWhenStarted { state ->
            when (state) {
                is NotificationActionState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is NotificationActionState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    viewModel.resetActionState()
                }
                is NotificationActionState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    viewModel.resetActionState()
                }
                is NotificationActionState.Idle -> {}
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, LandlordNotificationsActivity::class.java)
        }
    }
}
