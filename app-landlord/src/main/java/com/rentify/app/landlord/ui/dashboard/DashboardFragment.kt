package com.rentify.app.landlord.ui.dashboard

import android.content.Context
import android.content.Intent
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rentify.app.core.network.dto.invoice.InvoiceListResponseDto
import com.rentify.app.core.network.dto.issue.IssueReportListResponseDto
import com.rentify.app.core.ui.R as CoreUiR
import com.rentify.app.core.ui.base.BaseFragment
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.core.ui.extension.toast
import com.rentify.app.landlord.databinding.FragmentDashboardBinding
import com.rentify.app.landlord.ui.billing.LandlordInvoiceDetailActivity
import com.rentify.app.landlord.ui.billing.LandlordInvoicesActivity
import com.rentify.app.landlord.ui.billing.LandlordMeterReadingsActivity
import com.rentify.app.landlord.ui.contract.LandlordContractsActivity
import com.rentify.app.landlord.ui.notification.LandlordNotificationsActivity
import com.rentify.app.landlord.ui.operations.LandlordIssueDetailActivity
import com.rentify.app.landlord.ui.operations.LandlordIssuesActivity
import com.rentify.app.landlord.ui.property.CreateBuildingActivity
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

class DashboardFragment : BaseFragment<FragmentDashboardBinding>(FragmentDashboardBinding::inflate) {

    interface DashboardListener {
        fun onNavigateToProperties()
        fun onNavigateToOperations(tabIndex: Int = 0)
    }

    private var listener: DashboardListener? = null

    private val viewModel: DashboardViewModel by viewModels {
        DashboardViewModelFactory(requireContext())
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = parentFragment as? DashboardListener ?: context as? DashboardListener
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun initView() {
        binding.layoutHeader.applySystemBarsPadding(top = true, bottom = false)

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadDashboard()
        }

        binding.btnNotifications.setOnSingleClickListener {
            startActivity(LandlordNotificationsActivity.newIntent(requireContext()))
        }

        binding.btnQuickAddBuilding.setOnSingleClickListener {
            startActivity(Intent(requireContext(), CreateBuildingActivity::class.java))
        }

        binding.btnQuickAddRoom.setOnSingleClickListener {
            startActivity(Intent(requireContext(), LandlordContractsActivity::class.java))
        }

        binding.btnQuickMeters.setOnSingleClickListener {
            startActivity(Intent(requireContext(), LandlordMeterReadingsActivity::class.java))
        }

        binding.btnQuickIssues.setOnSingleClickListener {
            startActivity(Intent(requireContext(), LandlordInvoicesActivity::class.java))
        }

        binding.btnViewAllIssues.setOnSingleClickListener {
            startActivity(Intent(requireContext(), LandlordIssuesActivity::class.java))
        }

        binding.btnViewAllInvoices.setOnSingleClickListener {
            startActivity(Intent(requireContext(), LandlordInvoicesActivity::class.java))
        }
    }

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is DashboardUiState.Loading -> {
                            binding.swipeRefresh.isRefreshing = true
                        }
                        is DashboardUiState.Success -> {
                            binding.swipeRefresh.isRefreshing = false
                            val metrics = state.metrics
                            binding.tvOccupancyRate.text = metrics.occupancyRate.ifBlank { "0%" }
                            binding.tvTotalBuildings.text = metrics.totalBuildings.toString()
                            binding.tvTotalRooms.text = metrics.totalRooms.toString()
                            binding.tvTotalTenants.text = metrics.activeContracts.toString()

                            if (metrics.pendingApprovalBuildings > 0) {
                                binding.tvPendingBuildingsSubtitle.text = "${metrics.pendingApprovalBuildings} tòa chờ duyệt"
                            } else {
                                binding.tvPendingBuildingsSubtitle.text = "Tất cả đã duyệt"
                            }

                            renderRecentIssues(metrics.recentIssues)
                            renderRecentInvoices(metrics.recentInvoices)
                        }
                        is DashboardUiState.Error -> {
                            binding.swipeRefresh.isRefreshing = false
                            toast(state.message)
                        }
                    }
                }
            }
        }
    }

    private fun renderRecentIssues(issues: List<IssueReportListResponseDto>) {
        binding.layoutRecentIssuesList.removeAllViews()
        if (issues.isEmpty()) {
            binding.tvEmptyRecentIssues.visibility = View.VISIBLE
            binding.layoutRecentIssuesList.visibility = View.GONE
            return
        }

        binding.tvEmptyRecentIssues.visibility = View.GONE
        binding.layoutRecentIssuesList.visibility = View.VISIBLE
        val ctx = requireContext()

        issues.forEachIndexed { index, issue ->
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dpToPx(8), 0, dpToPx(8))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    val intent = Intent(ctx, LandlordIssueDetailActivity::class.java).apply {
                        putExtra(LandlordIssueDetailActivity.EXTRA_ISSUE_ID, issue.id)
                    }
                    startActivity(intent)
                }
            }

            val textContainer = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val titleTv = TextView(ctx).apply {
                text = issue.title.ifBlank { "Sự cố phòng ${issue.roomName}" }
                textSize = 14f
                setTextColor(ContextCompat.getColor(ctx, CoreUiR.color.rentify_text_primary))
                paint.isFakeBoldText = true
            }

            val roomLabel = buildString {
                if (issue.roomName.isNotBlank()) append("Phòng ${issue.roomName}")
                if (issue.buildingName.isNotBlank()) {
                    if (isNotEmpty()) append(" · ")
                    append(issue.buildingName)
                }
            }.ifBlank { "Cần kiểm tra" }

            val subTv = TextView(ctx).apply {
                text = roomLabel
                textSize = 12f
                setTextColor(ContextCompat.getColor(ctx, CoreUiR.color.rentify_text_secondary))
            }

            textContainer.addView(titleTv)
            textContainer.addView(subTv)

            val statusLabel = when (issue.status.lowercase()) {
                "in_progress" -> "Đang xử lý"
                else -> "Mới tiếp nhận"
            }
            val statusColor = when (issue.status.lowercase()) {
                "in_progress" -> CoreUiR.color.rentify_primary
                else -> CoreUiR.color.rentify_warning
            }

            val statusTv = TextView(ctx).apply {
                text = statusLabel
                textSize = 11f
                setTextColor(ContextCompat.getColor(ctx, statusColor))
                paint.isFakeBoldText = true
            }

            row.addView(textContainer)
            row.addView(statusTv)
            binding.layoutRecentIssuesList.addView(row)

            if (index < issues.size - 1) {
                val divider = View(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1))
                    setBackgroundColor(ContextCompat.getColor(ctx, CoreUiR.color.rentify_divider))
                }
                binding.layoutRecentIssuesList.addView(divider)
            }
        }
    }

    private fun renderRecentInvoices(invoices: List<InvoiceListResponseDto>) {
        binding.layoutRecentInvoicesList.removeAllViews()
        if (invoices.isEmpty()) {
            binding.tvEmptyRecentInvoices.visibility = View.VISIBLE
            binding.layoutRecentInvoicesList.visibility = View.GONE
            return
        }

        binding.tvEmptyRecentInvoices.visibility = View.GONE
        binding.layoutRecentInvoicesList.visibility = View.VISIBLE
        val ctx = requireContext()
        val nf = NumberFormat.getNumberInstance(Locale("vi", "VN"))

        invoices.forEachIndexed { index, invoice ->
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dpToPx(8), 0, dpToPx(8))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    val intent = Intent(ctx, LandlordInvoiceDetailActivity::class.java).apply {
                        putExtra(LandlordInvoiceDetailActivity.EXTRA_INVOICE_ID, invoice.id)
                    }
                    startActivity(intent)
                }
            }

            val textContainer = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val titleTv = TextView(ctx).apply {
                text = "${invoice.code.ifBlank { "Hóa đơn" }} · Phòng ${invoice.roomName.ifBlank { "--" }}"
                textSize = 14f
                setTextColor(ContextCompat.getColor(ctx, CoreUiR.color.rentify_text_primary))
                paint.isFakeBoldText = true
            }

            val statusText = when (invoice.invoiceStatus.lowercase()) {
                "paid" -> "Đã thanh toán"
                "issued" -> "Đã phát hành"
                "overdue" -> "Quá hạn"
                "void", "cancelled" -> "Đã hủy"
                else -> "Chờ phát hành"
            }

            val subTv = TextView(ctx).apply {
                text = "Kỳ ${invoice.period} · $statusText"
                textSize = 12f
                setTextColor(ContextCompat.getColor(ctx, CoreUiR.color.rentify_text_secondary))
            }

            textContainer.addView(titleTv)
            textContainer.addView(subTv)

            val amountTv = TextView(ctx).apply {
                text = "${nf.format(invoice.totalAmountLong)} đ"
                textSize = 13f
                setTextColor(ContextCompat.getColor(ctx, CoreUiR.color.rentify_primary))
                paint.isFakeBoldText = true
            }

            row.addView(textContainer)
            row.addView(amountTv)
            binding.layoutRecentInvoicesList.addView(row)

            if (index < invoices.size - 1) {
                val divider = View(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1))
                    setBackgroundColor(ContextCompat.getColor(ctx, CoreUiR.color.rentify_divider))
                }
                binding.layoutRecentInvoicesList.addView(divider)
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    companion object {
        fun newInstance(): DashboardFragment = DashboardFragment()
    }
}
