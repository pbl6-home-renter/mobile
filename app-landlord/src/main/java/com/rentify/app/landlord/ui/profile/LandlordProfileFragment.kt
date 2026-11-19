package com.rentify.app.landlord.ui.profile

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.data.repository.user.UserRepositoryImpl
import com.rentify.app.core.network.adapter.NetworkResult
import com.rentify.app.core.network.api.MediaApiService
import com.rentify.app.core.network.api.UserApiService
import com.rentify.app.core.network.client.NetworkClient
import com.rentify.app.core.network.dto.user.PayoutAccountDto
import com.rentify.app.core.network.dto.user.UserChangePasswordRequestDto
import com.rentify.app.core.network.dto.user.UserCurrentResponseDto
import com.rentify.app.core.ui.base.BaseFragment
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.landlord.data.repository.LandlordMediaRepositoryImpl
import com.rentify.app.landlord.databinding.BottomSheetEditLandlordProfileBinding
import com.rentify.app.landlord.databinding.DialogChangePasswordBinding
import com.rentify.app.landlord.databinding.FragmentLandlordProfileBinding
import com.rentify.app.landlord.ui.notification.LandlordNotificationsActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LandlordProfileFragment : BaseFragment<FragmentLandlordProfileBinding>(FragmentLandlordProfileBinding::inflate) {

    interface LandlordProfileListener {
        fun onLandlordLogoutRequested()
    }

    private var listener: LandlordProfileListener? = null
    private val tokenDataStore by lazy { TokenDataStore(requireContext().applicationContext) }
    private var currentUser: UserCurrentResponseDto? = null

    private val pickAvatarLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            uploadAvatar(uri)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = parentFragment as? LandlordProfileListener ?: context as? LandlordProfileListener
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun initView() {
        binding.root.applySystemBarsPadding(top = true, bottom = false)

        loadProfile()

        binding.btnEditProfileTop.setOnSingleClickListener {
            showEditProfileBottomSheet()
        }

        binding.btnEditProfile.setOnSingleClickListener {
            showEditProfileBottomSheet()
        }

        binding.cardPayoutAccount.setOnSingleClickListener {
            showEditProfileBottomSheet()
        }

        binding.btnChangeAvatar.setOnSingleClickListener {
            pickAvatarLauncher.launch("image/*")
        }

        binding.rowNotifications.setOnSingleClickListener {
            startActivity(LandlordNotificationsActivity.newIntent(requireContext()))
        }

        binding.rowChangePassword.setOnSingleClickListener {
            showChangePasswordDialog()
        }

        binding.rowLogout.setOnSingleClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản Chủ trọ?")
                .setPositiveButton("Đăng xuất") { _, _ ->
                    listener?.onLandlordLogoutRequested()
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            val token = tokenDataStore.accessToken.first()
            if (!token.isNullOrBlank()) {
                val userApi = NetworkClient.createService<UserApiService>(
                    tokenProvider = { token }
                )
                val userRepo = UserRepositoryImpl(userApi)
                when (val result = userRepo.getCurrentUser()) {
                    is NetworkResult.Success -> {
                        val user = result.data.data
                        if (user != null) {
                            currentUser = user
                            bindUserToUi(user)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun bindUserToUi(user: UserCurrentResponseDto) {
        val displayName = user.name.ifBlank { "Chủ nhà Rentify" }
        binding.tvLandlordName.text = displayName
        binding.tvLandlordEmail.text = user.email
        binding.tvLandlordPhone.text = if (user.phone.isNotBlank()) {
            "SĐT: ${user.phone}"
        } else {
            "Chưa cập nhật số điện thoại"
        }

        binding.avatarView.bindPersonal(
            userId = user.id.ifBlank { user.email },
            displayName = displayName,
            avatarUrl = user.avatarUrl
        )

        val payout = user.payoutAccount
        if (payout != null && payout.bankCode.isNotBlank() && payout.accountNumber.isNotBlank()) {
            binding.tvPayoutSummary.text = "${payout.bankCode} · ${payout.accountNumber} (${payout.accountName})"
        } else {
            binding.tvPayoutSummary.text = "Chưa cấu hình tài khoản ngân hàng mặc định."
        }
    }

    private fun uploadAvatar(uri: Uri) {
        val user = currentUser
        lifecycleScope.launch {
            val token = tokenDataStore.accessToken.first()
            if (token.isNullOrBlank()) return@launch
            val userId = user?.id?.takeIf { it.isNotBlank() } ?: tokenDataStore.userId.first().orEmpty()
            if (userId.isBlank()) {
                Toast.makeText(requireContext(), "Không xác định được tài khoản người dùng", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val bytes = try {
                requireContext().contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (_: Exception) {
                null
            }
            if (bytes == null || bytes.isEmpty()) {
                Toast.makeText(requireContext(), "Không thể đọc tệp ảnh", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val mimeType = requireContext().contentResolver.getType(uri) ?: "image/jpeg"

            Toast.makeText(requireContext(), "Đang tải ảnh đại diện...", Toast.LENGTH_SHORT).show()
            val mediaApi = NetworkClient.createService<MediaApiService>(tokenProvider = { token })
            val rawMediaApi = NetworkClient.createService<MediaApiService>(tokenProvider = { null })
            val mediaRepo = LandlordMediaRepositoryImpl(mediaApi, rawMediaApi)

            when (val res = mediaRepo.uploadMedia(
                ownerType = "user",
                ownerId = userId,
                purpose = "avatar",
                fileBytes = bytes,
                fileName = "avatar_${System.currentTimeMillis()}.jpg",
                contentType = mimeType
            )) {
                is NetworkResult.Success -> {
                    Toast.makeText(requireContext(), "Đã cập nhật ảnh đại diện!", Toast.LENGTH_SHORT).show()
                    loadProfile()
                }
                is NetworkResult.ApiError -> {
                    Toast.makeText(
                        requireContext(),
                        res.error?.message ?: "Không thể tải lên ảnh đại diện",
                        Toast.LENGTH_LONG
                    ).show()
                }
                else -> {
                    Toast.makeText(requireContext(), "Lỗi kết nối khi tải ảnh đại diện", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showEditProfileBottomSheet() {
        val sheetBinding = BottomSheetEditLandlordProfileBinding.inflate(LayoutInflater.from(requireContext()))
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(sheetBinding.root)

        val user = currentUser
        sheetBinding.etEditName.setText(user?.name.orEmpty())
        sheetBinding.etEditPhone.setText(user?.phone.orEmpty())
        sheetBinding.etEditEmail.setText(user?.email.orEmpty())
        sheetBinding.etBankCode.setText(user?.payoutAccount?.bankCode.orEmpty())
        sheetBinding.etAccountNumber.setText(user?.payoutAccount?.accountNumber.orEmpty())
        sheetBinding.etAccountName.setText(user?.payoutAccount?.accountName.orEmpty())

        sheetBinding.btnSaveProfile.setOnSingleClickListener {
            val name = sheetBinding.etEditName.text?.toString()?.trim().orEmpty()
            val phone = sheetBinding.etEditPhone.text?.toString()?.trim().orEmpty()
            val bankCode = sheetBinding.etBankCode.text?.toString()?.trim()?.uppercase().orEmpty()
            val accountNumber = sheetBinding.etAccountNumber.text?.toString()?.trim().orEmpty()
            val accountName = sheetBinding.etAccountName.text?.toString()?.trim()?.uppercase().orEmpty()

            if (name.isBlank() || phone.isBlank()) {
                Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ Họ tên và Số điện thoại", Toast.LENGTH_SHORT).show()
                return@setOnSingleClickListener
            }

            val hasAnyBankField = bankCode.isNotBlank() || accountNumber.isNotBlank() || accountName.isNotBlank()
            val payoutDto = if (hasAnyBankField) {
                if (bankCode.isBlank() || accountNumber.isBlank() || accountName.isBlank()) {
                    Toast.makeText(
                        requireContext(),
                        "Vui lòng nhập đủ Mã ngân hàng, Số tài khoản và Tên chủ tài khoản",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnSingleClickListener
                }
                PayoutAccountDto(
                    bankCode = bankCode,
                    accountNumber = accountNumber,
                    accountName = accountName
                )
            } else {
                null
            }

            lifecycleScope.launch {
                val token = tokenDataStore.accessToken.first()
                if (!token.isNullOrBlank()) {
                    val userApi = NetworkClient.createService<UserApiService>(tokenProvider = { token })
                    val userRepo = UserRepositoryImpl(userApi)
                    when (val res = userRepo.updateCurrentUser(
                        name = name,
                        phone = phone,
                        payoutAccount = payoutDto
                    )) {
                        is NetworkResult.Success -> {
                            Toast.makeText(requireContext(), "Cập nhật hồ sơ thành công", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            loadProfile()
                        }
                        is NetworkResult.ApiError -> {
                            Toast.makeText(
                                requireContext(),
                                res.error?.message ?: "Không thể cập nhật hồ sơ",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        else -> {
                            Toast.makeText(requireContext(), "Lỗi kết nối khi cập nhật hồ sơ", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        dialog.show()
    }

    private fun showChangePasswordDialog() {
        val dialogBinding = DialogChangePasswordBinding.inflate(LayoutInflater.from(requireContext()))

        AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton("Đổi mật khẩu") { _, _ ->
                val currentPassword = dialogBinding.etCurrentPassword.text?.toString() ?: ""
                val newPassword = dialogBinding.etNewPassword.text?.toString() ?: ""
                val confirmPassword = dialogBinding.etConfirmPassword.text?.toString() ?: ""

                if (currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
                    Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (newPassword != confirmPassword) {
                    Toast.makeText(requireContext(), "Mật khẩu xác nhận không trùng khớp", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (newPassword.length < 8) {
                    Toast.makeText(requireContext(), "Mật khẩu mới phải có tối thiểu 8 ký tự", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    val token = tokenDataStore.accessToken.first()
                    if (!token.isNullOrBlank()) {
                        val userApi = NetworkClient.createService<UserApiService>(
                            tokenProvider = { token }
                        )
                        val userRepo = UserRepositoryImpl(userApi)
                        when (val res = userRepo.changePassword(
                            UserChangePasswordRequestDto(
                                currentPassword = currentPassword,
                                newPassword = newPassword,
                                confirmPassword = confirmPassword
                            )
                        )) {
                            is NetworkResult.Success -> {
                                Toast.makeText(requireContext(), "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show()
                            }
                            is NetworkResult.ApiError -> {
                                Toast.makeText(requireContext(), res.error?.message ?: "Lỗi đổi mật khẩu", Toast.LENGTH_LONG).show()
                            }
                            else -> {
                                Toast.makeText(requireContext(), "Lỗi kết nối khi đổi mật khẩu", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadProfile()
    }

    companion object {
        fun newInstance(): LandlordProfileFragment = LandlordProfileFragment()
    }
}
