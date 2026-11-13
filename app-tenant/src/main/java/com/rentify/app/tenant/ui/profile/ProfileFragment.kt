package com.rentify.app.tenant.ui.profile

import android.content.Context
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rentify.app.core.ui.base.BaseFragment
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.toast
import com.rentify.app.tenant.databinding.BottomSheetChangePasswordBinding
import com.rentify.app.tenant.databinding.BottomSheetEditProfileBinding
import com.rentify.app.tenant.databinding.FragmentProfileBinding
import com.rentify.app.tenant.ui.dialog.showRequireLoginDialog
import com.rentify.app.tenant.ui.explore.DevTweaksDialogFragment

class ProfileFragment : BaseFragment<FragmentProfileBinding>(FragmentProfileBinding::inflate) {

    interface ProfileListener {
        fun onLogoutRequested()
        fun onNavigateToLoginRequested()
    }

    private var listener: ProfileListener? = null
    private var editProfileDialog: BottomSheetDialog? = null
    private var changePasswordDialog: BottomSheetDialog? = null

    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(requireContext())
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? ProfileListener
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
        editProfileDialog?.dismiss()
        editProfileDialog = null
        changePasswordDialog?.dismiss()
        changePasswordDialog = null
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && !viewModel.uiState.value.isLoggedIn) {
            showRequireLoginDialog("Vui lòng đăng nhập để xem thông tin và quản lý tài khoản cá nhân.") {
                listener?.onNavigateToLoginRequested()
            }
        }
    }

    override fun initView() {
        binding.root.applySystemBarsPadding(top = true, bottom = false)
        setupClickListeners()
    }

    override fun observeData() {
        viewModel.uiState.collectWhenStarted { state ->
            binding.swipeRefreshLayout.isRefreshing = state.isRefreshing
            binding.progressBar.isVisible = state.isLoading && !state.isRefreshing

            if (state.isLoggedIn) {
                binding.layoutGuestMode.isVisible = false
                binding.layoutLoggedInMode.isVisible = true

                val profile = state.profile
                val displayName = profile?.name?.ifBlank { "Người dùng Rentify" } ?: "Người dùng Rentify"
                binding.tvUserName.text = displayName
                binding.tvUserEmail.text = profile?.email ?: ""
                binding.tvUserPhone.text = if (!profile?.phone.isNullOrBlank()) {
                    profile?.phone
                } else {
                    "Chưa cập nhật"
                }

                binding.tvAvatarLetter.text = (displayName.trim().firstOrNull() ?: 'U').uppercaseChar().toString()

                binding.tvUserRoleBadge.text = if (state.role.equals("landlord", ignoreCase = true)) {
                    "Chủ nhà trọ"
                } else {
                    "Người thuê trọ"
                }
            } else {
                binding.layoutGuestMode.isVisible = false
                binding.layoutLoggedInMode.isVisible = false
            }
        }

        viewModel.events.collectWhenStarted { event ->
            when (event) {
                is ProfileEvent.ShowToast -> {
                    toast(event.message)
                }
                is ProfileEvent.ShowError -> {
                    toast(event.message)
                }
                is ProfileEvent.ProfileUpdated -> {
                    editProfileDialog?.dismiss()
                    editProfileDialog = null
                }
                is ProfileEvent.PasswordChanged -> {
                    changePasswordDialog?.dismiss()
                    changePasswordDialog = null
                }
                is ProfileEvent.LoggedOut -> {
                    listener?.onLogoutRequested()
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadUserProfile(isRefresh = true)
        }

        binding.btnGuestLogin.setOnClickListener {
            listener?.onNavigateToLoginRequested()
        }

        binding.btnGuestDevTweaks.setOnClickListener {
            showDevTweaksDialog()
        }

        binding.btnDevTweaksRow.setOnClickListener {
            showDevTweaksDialog()
        }

        binding.btnGuestTerms.setOnClickListener {
            toast("Điều khoản và chính sách đang được cập nhật")
        }

        binding.btnTermsRow.setOnClickListener {
            toast("Điều khoản và chính sách đang được cập nhật")
        }

        binding.btnFavoriteRooms.setOnClickListener {
            val intent = android.content.Intent(requireContext(), com.rentify.app.tenant.ui.favoriteroom.FavoriteRoomsActivity::class.java)
            startActivity(intent)
        }


        binding.btnNotifications.setOnClickListener {
            toast("Cài đặt thông báo đang được hoàn thiện")
        }

        binding.btnQuickEditProfile.setOnClickListener {
            showEditProfileBottomSheet()
        }

        binding.btnEditProfileRow.setOnClickListener {
            showEditProfileBottomSheet()
        }

        binding.btnChangePasswordRow.setOnClickListener {
            showChangePasswordBottomSheet()
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun showDevTweaksDialog() {
        val currentTweak = viewModel.uiState.value.devTweakHasRoom
        DevTweaksDialogFragment.newInstance(currentTweak) { newValue ->
            viewModel.setDevTweakHasRoom(newValue)
        }.show(childFragmentManager, DevTweaksDialogFragment.TAG)
    }

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Đăng xuất")
            .setMessage("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản không?")
            .setPositiveButton("Đăng xuất") { _, _ ->
                viewModel.logout()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showEditProfileBottomSheet() {
        val currentProfile = viewModel.uiState.value.profile
        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = BottomSheetEditProfileBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        currentProfile?.let {
            dialogBinding.etName.setText(it.name)
            dialogBinding.etPhone.setText(it.phone)
        }

        dialogBinding.btnSaveProfile.setOnClickListener {
            val name = dialogBinding.etName.text?.toString().orEmpty()
            val phone = dialogBinding.etPhone.text?.toString().orEmpty()
            viewModel.updateProfile(name, phone)
        }

        dialogBinding.btnCancelEditProfile.setOnClickListener {
            dialog.dismiss()
        }

        editProfileDialog = dialog
        dialog.show()
    }

    private fun showChangePasswordBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = BottomSheetChangePasswordBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.btnSubmitChangePassword.setOnClickListener {
            val currentPass = dialogBinding.etCurrentPassword.text?.toString().orEmpty()
            val newPass = dialogBinding.etNewPassword.text?.toString().orEmpty()
            val confirmPass = dialogBinding.etConfirmPassword.text?.toString().orEmpty()

            viewModel.changePassword(currentPass, newPass, confirmPass)
        }

        dialogBinding.btnCancelChangePassword.setOnClickListener {
            dialog.dismiss()
        }

        changePasswordDialog = dialog
        dialog.show()
    }

    companion object {
        fun newInstance(): ProfileFragment = ProfileFragment()
    }
}
