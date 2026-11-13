package com.rentify.app.tenant.ui.main

import android.content.Context
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.ui.base.BaseFragment
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.tenant.R
import com.rentify.app.tenant.databinding.FragmentTenantMainBinding
import com.rentify.app.tenant.ui.chat.ChatFragment
import com.rentify.app.tenant.ui.dialog.showRequireLoginDialog
import com.rentify.app.tenant.ui.explore.ExploreFragment
import com.rentify.app.tenant.ui.matching.MatchingFragment
import com.rentify.app.tenant.ui.myroom.MyRoomFragment
import com.rentify.app.tenant.ui.profile.ProfileFragment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TenantMainFragment : BaseFragment<FragmentTenantMainBinding>(FragmentTenantMainBinding::inflate),
    ProfileFragment.ProfileListener,
    ExploreFragment.ExploreListener,
    ChatFragment.ChatListener,
    MyRoomFragment.MyRoomListener,
    MatchingFragment.MatchingListener {

    interface TenantMainListener {
        fun onTenantLogoutRequested()
        fun onTenantLoginRequested()
    }

    private var listener: TenantMainListener? = null

    private var activeFragment: Fragment? = null

    private val tokenDataStore by lazy { TokenDataStore(requireContext().applicationContext) }

    private val exploreFragment by lazy { ExploreFragment.newInstance() }
    private val chatFragment by lazy { ChatFragment.newInstance() }
    private val myRoomFragment by lazy { MyRoomFragment.newInstance() }
    private val matchingFragment by lazy { MatchingFragment.newInstance() }
    private val profileFragment by lazy { ProfileFragment.newInstance() }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? TenantMainListener
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun initView() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav) { _, insets -> insets }
        binding.root.applySystemBarsPadding(top = false, bottom = true)
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.isItemActiveIndicatorEnabled = false

        switchTab(exploreFragment, TAG_EXPLORE)

        binding.bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_explore -> {
                    switchTab(exploreFragment, TAG_EXPLORE)
                    true
                }
                R.id.nav_chat -> {
                    switchTab(chatFragment, TAG_CHAT)
                    true
                }
                R.id.nav_my_room -> {
                    switchTab(myRoomFragment, TAG_MY_ROOM)
                    true
                }
                R.id.nav_matching -> {
                    switchTab(matchingFragment, TAG_MATCHING)
                    true
                }
                R.id.nav_profile -> {
                    lifecycleScope.launch {
                        val token = tokenDataStore.accessToken.first()
                        if (token.isNullOrBlank()) {
                            showRequireLoginDialog("Vui lòng đăng nhập để xem thông tin và quản lý tài khoản cá nhân.") {
                                listener?.onTenantLoginRequested()
                            }
                        } else {
                            switchTab(profileFragment, TAG_PROFILE)
                            binding.bottomNav.menu.findItem(R.id.nav_profile).isChecked = true
                        }
                    }
                    false
                }
                else -> false
            }
        }
    }

    private fun switchTab(targetFragment: Fragment, tag: String) {
        if (activeFragment === targetFragment) return

        val transaction = childFragmentManager.beginTransaction()

        activeFragment?.let {
            transaction.hide(it)
        }

        val existing = childFragmentManager.findFragmentByTag(tag)
        if (existing == null) {
            transaction.add(R.id.childNavContainer, targetFragment, tag)
        } else {
            transaction.show(existing)
        }

        transaction.commit()
        activeFragment = targetFragment
    }

    override fun onLogoutRequested() {
        switchTab(exploreFragment, TAG_EXPLORE)
        binding.bottomNav.selectedItemId = R.id.nav_explore
        listener?.onTenantLogoutRequested()
    }

    override fun onNavigateToLoginRequested() {
        listener?.onTenantLoginRequested()
    }

    override fun onNavigateToExploreRequested() {
        switchTab(exploreFragment, TAG_EXPLORE)
        binding.bottomNav.selectedItemId = R.id.nav_explore
    }

    override fun onNavigateToChatRequested() {
        binding.bottomNav.selectedItemId = R.id.nav_chat
    }

    override fun onNavigateToMatchingRequested() {
        binding.bottomNav.selectedItemId = R.id.nav_matching
    }

    override fun onNavigateToMyRoomRequested() {
        binding.bottomNav.selectedItemId = R.id.nav_my_room
    }

    companion object {
        private const val TAG_EXPLORE = "tag_explore"
        private const val TAG_CHAT = "tag_chat"
        private const val TAG_MY_ROOM = "tag_my_room"
        private const val TAG_MATCHING = "tag_matching"
        private const val TAG_PROFILE = "tag_profile"

        fun newInstance(): TenantMainFragment = TenantMainFragment()
    }
}
