package com.rentify.app.tenant.ui.matching.detail

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rentify.app.core.network.dto.matching.RoommateProfileDetailResponseDto
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.core.ui.extension.toVnd
import com.rentify.app.core.ui.extension.toast
import com.rentify.app.core.ui.state.UiState
import com.rentify.app.core.ui.R as CoreR
import com.rentify.app.tenant.databinding.ActivityRoommateDetailBinding
import com.rentify.app.tenant.ui.matching.profile.EditRoommateProfileActivity
import kotlinx.coroutines.launch
import java.time.Year

class RoommateDetailActivity : BaseActivity<ActivityRoommateDetailBinding>(ActivityRoommateDetailBinding::inflate) {

    private val profileId: String by lazy {
        intent.getStringExtra(EXTRA_PROFILE_ID).orEmpty()
    }

    private val viewModel: RoommateDetailViewModel by viewModels {
        RoommateDetailViewModelFactory(this, profileId)
    }

    override fun initView() {
        binding.root.applySystemBarsPadding(top = true, bottom = true)

        if (profileId.isBlank()) {
            toast("Không tìm thấy thông tin hồ sơ")
            finish()
            return
        }

        binding.toolbarDetail.setNavigationOnClickListener { finish() }

        binding.btnSendMatchRequest.setOnSingleClickListener {
            viewModel.sendMatchRequest()
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is UiState.Loading -> {
                                binding.pbDetailLoading.isVisible = true
                                binding.scrollContent.isVisible = false
                            }
                            is UiState.Success -> {
                                binding.pbDetailLoading.isVisible = false
                                binding.scrollContent.isVisible = true
                                bindDetail(state.data)
                            }
                            is UiState.Error -> {
                                binding.pbDetailLoading.isVisible = false
                                toast(state.message)
                                finish()
                            }
                            is UiState.Idle -> Unit
                        }
                    }
                }

                launch {
                    viewModel.uiEvent.collect { event ->
                        when (event) {
                            is RoommateDetailUiEvent.ShowToast -> toast(event.message)
                            is RoommateDetailUiEvent.RequestSentSuccess -> {
                                binding.btnSendMatchRequest.isEnabled = false
                                binding.btnSendMatchRequest.text = "Đã gửi lời mời ghép đôi"
                            }
                            is RoommateDetailUiEvent.RequireCreateProfile -> {
                                MaterialAlertDialogBuilder(this@RoommateDetailActivity)
                                    .setTitle("Chưa có hồ sơ bạn cùng phòng")
                                    .setMessage("Bạn cần tạo hồ sơ bạn cùng phòng để đối phương có thể xem thông tin của bạn trước khi kết nối.")
                                    .setPositiveButton("Tạo hồ sơ ngay") { _, _ ->
                                        startActivity(EditRoommateProfileActivity.createIntent(this@RoommateDetailActivity))
                                    }
                                    .setNegativeButton("Để sau", null)
                                    .show()
                            }
                            is RoommateDetailUiEvent.RequireProfileVisibility -> {
                                MaterialAlertDialogBuilder(this@RoommateDetailActivity)
                                    .setTitle("Hồ sơ đang tạm ẩn")
                                    .setMessage(event.reason)
                                    .setPositiveButton("Mở cài đặt hồ sơ") { _, _ ->
                                        startActivity(EditRoommateProfileActivity.createIntent(this@RoommateDetailActivity))
                                    }
                                    .setNegativeButton("Để sau", null)
                                    .show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun bindDetail(detail: RoommateProfileDetailResponseDto) {
        if (!detail.avatarUrl.isNullOrBlank()) {
            binding.ivDetailAvatar.load(detail.avatarUrl) {
                crossfade(true)
                placeholder(CoreR.drawable.ic_user_circle)
                error(CoreR.drawable.ic_user_circle)
            }
        } else {
            binding.ivDetailAvatar.setImageResource(CoreR.drawable.ic_user_circle)
        }

        binding.tvDetailName.text = detail.tenantName
        val ageStr = detail.birthYear?.let { "${Year.now().value - it} tuổi" } ?: ""
        val genderStr = when (detail.gender.lowercase()) {
            "male" -> "Nam"
            "female" -> "Nữ"
            else -> "Khác"
        }
        val occupationStr = detail.occupation ?: "Chưa rõ nghề nghiệp"
        binding.tvDetailInfoSub.text = "$genderStr · $ageStr · $occupationStr"

        // AI Match
        if (detail.aiMatchScore != null) {
            binding.cardAiAnalysis.isVisible = true
            binding.tvDetailMatchScore.text = "${detail.aiMatchScore}% Tương thích"
            binding.tvDetailAiAdvice.text = detail.aiAdvice ?: "Hệ thống AI nhận thấy hai bạn có nhiều đặc điểm lối sống tương đồng."
        } else {
            binding.cardAiAnalysis.isVisible = false
        }

        // Budget & Area
        val minBudgetStr = detail.budgetMin?.toVnd() ?: "0 đ"
        val maxBudgetStr = detail.budgetMax?.toVnd() ?: "Thương lượng"
        binding.tvDetailBudget.text = "$minBudgetStr - $maxBudgetStr"

        val district = detail.preferredDistrict.orEmpty()
        val area = detail.preferredArea.orEmpty()
        binding.tvDetailArea.text = when {
            district.isNotBlank() && area.isNotBlank() -> "$district · $area"
            district.isNotBlank() -> district
            area.isNotBlank() -> area
            else -> "Chưa xác định"
        }

        // Habits
        val habits = detail.lifestyleDetails
        val textPrimary = ContextCompat.getColor(this, CoreR.color.rentify_text_primary)
        val textNeutral = ContextCompat.getColor(this, CoreR.color.rentify_neutral)

        binding.tvHabitClean.text = if (habits?.cleanAndTidy == true) "Rất ngăn nắp" else "Bình thường"
        binding.tvHabitClean.setTextColor(if (habits?.cleanAndTidy == true) textPrimary else textNeutral)

        binding.tvHabitSleep.text = when {
            habits?.earlyBird == true -> "Dậy sớm (Early Bird)"
            habits?.nightOwl == true -> "Thức khuya (Night Owl)"
            else -> "Linh hoạt"
        }

        binding.tvHabitCooking.text = if (habits?.cooking == true) "Thường xuyên" else "Ít nấu"
        binding.tvHabitCooking.setTextColor(if (habits?.cooking == true) textPrimary else textNeutral)

        binding.tvHabitPet.text = if (habits?.petFriendly == true) "Thích nuôi thú cưng" else "Không nuôi"
        binding.tvHabitSmoking.text = if (habits?.smoking == true) "Có hút thuốc" else "Không hút thuốc"
        binding.tvHabitSmoking.setTextColor(if (habits?.smoking == true) ContextCompat.getColor(this, CoreR.color.rentify_error) else ContextCompat.getColor(this, CoreR.color.rentify_success))

        binding.tvHabitGuests.text = if (habits?.guestsAllowed == true) "Thoải mái" else "Hạn chế"

        // Bio
        binding.tvDetailBio.text = detail.bio ?: "Chưa có lời giới thiệu bản thân."
    }

    companion object {
        const val EXTRA_PROFILE_ID = "extra_profile_id"

        fun createIntent(context: Context, profileId: String): Intent {
            return Intent(context, RoommateDetailActivity::class.java).apply {
                putExtra(EXTRA_PROFILE_ID, profileId)
            }
        }
    }
}

