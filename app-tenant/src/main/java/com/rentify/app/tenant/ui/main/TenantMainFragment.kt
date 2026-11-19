package com.rentify.app.tenant.ui.main

import android.content.Context
import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.rentify.app.core.data.local.datastore.TokenDataStore
import com.rentify.app.core.ui.base.BaseFragment
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.R as CoreR
import com.rentify.app.core.ui.view.RentifyFloatingBottomBar
import com.rentify.app.tenant.R
import com.rentify.app.tenant.databinding.FragmentTenantMainBinding
import com.rentify.app.tenant.ui.chat.ChatFragment
import com.rentify.app.tenant.ui.dialog.LoginFeature
import com.rentify.app.tenant.ui.dialog.showLoginRequiredSheet
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
        fun onTenantLoginRequested(targetTabId: Int = R.id.nav_explore)
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
        val initialTabId = arguments?.getInt(ARG_INITIAL_TAB_ID, R.id.nav_explore) ?: R.id.nav_explore
        binding.bottomNav.setTabs(
            listOf(
                RentifyFloatingBottomBar.TabItem(R.id.nav_explore, R.string.nav_tenant_explore, CoreR.drawable.ic_nav_explore),
                RentifyFloatingBottomBar.TabItem(R.id.nav_chat, R.string.nav_tenant_chat, CoreR.drawable.ic_nav_chat),
                RentifyFloatingBottomBar.TabItem(R.id.nav_my_room, R.string.nav_tenant_my_room, CoreR.drawable.ic_nav_my_room),
                RentifyFloatingBottomBar.TabItem(R.id.nav_matching, R.string.nav_tenant_matching, CoreR.drawable.ic_nav_matching),
                RentifyFloatingBottomBar.TabItem(R.id.nav_profile, R.string.nav_tenant_profile, CoreR.drawable.ic_nav_profile)
            )
        )
        switchTab(exploreFragment, TAG_EXPLORE)
        binding.bottomNav.setSelectedTab(R.id.nav_explore, animate = false)

        if (initialTabId != R.id.nav_explore) {
            arguments?.putInt(ARG_INITIAL_TAB_ID, R.id.nav_explore)
            navigateToProtectedTab(initialTabId, animate = false)
        }

        binding.bottomNav.setOnTabSelectedListener { tabId ->
            when (tabId) {
                R.id.nav_explore -> {
                    switchTab(exploreFragment, TAG_EXPLORE)
                    true
                }
                R.id.nav_chat,
                R.id.nav_my_room,
                R.id.nav_matching,
                R.id.nav_profile -> {
                    navigateToProtectedTab(tabId, animate = true)
                    false
                }
                else -> false
            }
        }
    }

    private fun navigateToProtectedTab(tabId: Int, animate: Boolean = true) {
        val feature = when (tabId) {
            R.id.nav_chat -> LoginFeature.MESSAGES
            R.id.nav_my_room -> LoginFeature.MY_ROOM
            R.id.nav_matching -> LoginFeature.ROOMMATE
            R.id.nav_profile -> LoginFeature.PROFILE
            else -> return
        }

        lifecycleScope.launch {
            val token = tokenDataStore.accessToken.first()
            if (token.isNullOrBlank()) {
                showLoginRequiredSheet(feature = feature) {
                    listener?.onTenantLoginRequested(targetTabId = tabId)
                }
            } else {
                when (tabId) {
                    R.id.nav_chat -> switchTab(chatFragment, TAG_CHAT)
                    R.id.nav_my_room -> switchTab(myRoomFragment, TAG_MY_ROOM)
                    R.id.nav_matching -> switchTab(matchingFragment, TAG_MATCHING)
                    R.id.nav_profile -> switchTab(profileFragment, TAG_PROFILE)
                }
                binding.bottomNav.setSelectedTab(tabId, animate = animate)
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
        listener?.onTenantLogoutRequested()
    }

    override fun onNavigateToLoginRequested() {
        listener?.onTenantLoginRequested(R.id.nav_explore)
    }

    override fun onNavigateToLoginForTabRequested(targetTabId: Int) {
        listener?.onTenantLoginRequested(targetTabId)
    }

    override fun onNavigateToExploreRequested() {
        switchTab(exploreFragment, TAG_EXPLORE)
        binding.bottomNav.setSelectedTab(R.id.nav_explore, animate = true)
    }

    override fun onNavigateToChatRequested() {
        navigateToProtectedTab(R.id.nav_chat, animate = true)
    }

    override fun onNavigateToMatchingRequested() {
        navigateToProtectedTab(R.id.nav_matching, animate = true)
    }

    override fun onNavigateToMyRoomRequested() {
        navigateToProtectedTab(R.id.nav_my_room, animate = true)
    }

    companion object {
        private const val TAG_EXPLORE = "tag_explore"
        private const val TAG_CHAT = "tag_chat"
        private const val TAG_MY_ROOM = "tag_my_room"
        private const val TAG_MATCHING = "tag_matching"
        private const val TAG_PROFILE = "tag_profile"
        private const val ARG_INITIAL_TAB_ID = "arg_initial_tab_id"

        fun newInstance(initialTabId: Int = R.id.nav_explore): TenantMainFragment {
            return TenantMainFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_INITIAL_TAB_ID, initialTabId)
                }
            }
        }
    }
}
