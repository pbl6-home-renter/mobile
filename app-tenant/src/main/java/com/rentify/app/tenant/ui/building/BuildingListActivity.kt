package com.rentify.app.tenant.ui.building

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.setOnSingleClickListener
import com.rentify.app.core.ui.state.UiState
import com.rentify.app.tenant.databinding.ActivityBuildingListBinding
import kotlinx.coroutines.launch

class BuildingListActivity : BaseActivity<ActivityBuildingListBinding>(ActivityBuildingListBinding::inflate) {

    private val viewModel: BuildingListViewModel by viewModels {
        BuildingListViewModelFactory(this)
    }

    private val adapter by lazy {
        BuildingCardAdapter(fullWidth = true) { building ->
            BuildingDetailActivity.start(this, building.id)
        }
    }

    override fun initView() {
        binding.root.applySystemBarsPadding(top = true, bottom = true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.rvBuildings.layoutManager = LinearLayoutManager(this)
        binding.rvBuildings.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { viewModel.load(isRefresh = true) }
        binding.btnRetry.setOnSingleClickListener { viewModel.load() }
    }

    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state is UiState.Loading
                    binding.swipeRefresh.isVisible = state is UiState.Success && state.data.isNotEmpty()
                    binding.layoutEmpty.isVisible = state is UiState.Success && state.data.isEmpty()
                    binding.layoutError.isVisible = state is UiState.Error
                    if (state !is UiState.Loading) binding.swipeRefresh.isRefreshing = false
                    when (state) {
                        is UiState.Success -> {
                            adapter.submitList(state.data)
                            binding.toolbar.subtitle = if (state.data.isNotEmpty()) "${state.data.size} tòa nhà" else null
                        }
                        is UiState.Error -> binding.tvError.text = state.message
                        else -> Unit
                    }
                }
            }
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, BuildingListActivity::class.java))
        }
    }
}
