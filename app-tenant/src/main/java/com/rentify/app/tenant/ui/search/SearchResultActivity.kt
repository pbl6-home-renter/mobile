package com.rentify.app.tenant.ui.search

import android.content.Context
import android.content.Intent
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.toast
import com.rentify.app.tenant.databinding.ActivitySearchResultBinding
import com.rentify.app.tenant.ui.dialog.showRequireLoginDialog
import com.rentify.app.tenant.ui.room.detail.RoomDetailActivity
import kotlinx.coroutines.launch

class SearchResultActivity : BaseActivity<ActivitySearchResultBinding>(ActivitySearchResultBinding::inflate) {

    private val initialFilter: RoomFilter by lazy {
        @Suppress("DEPRECATION")
        (intent.getSerializableExtra(EXTRA_FILTER) as? RoomFilter) ?: RoomFilter.default()
    }

    private val viewModel: SearchResultViewModel by viewModels {
        SearchResultViewModelFactory(this, initialFilter)
    }

    private val adapter by lazy {
        SearchResultAdapter(
            onRoomClick = { room ->
                RoomDetailActivity.start(this, room.id, isFavorited = room.isFavorited)
            },
            onFavoriteClick = { room ->
                if (viewModel.uiState.value.isLoggedIn) {
                    viewModel.toggleFavorite(room)
                } else {
                    showRequireLoginDialog("Vui lòng đăng nhập để lưu phòng trọ yêu thích.") {
                        startActivity(com.rentify.app.tenant.MainActivity.createLoginIntent(this@SearchResultActivity))
                    }
                }
            }
        )
    }

    override fun initView() {
        binding.root.applySystemBarsPadding(top = true, bottom = true)

        setupTopSearchBar()
        setupRecyclerView()
        setupQuickFilterChips()
        observeViewModel()
    }

    private fun setupTopSearchBar() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        val initialKeyword = initialFilter.keyword.orEmpty()
        binding.etSearchKeyword.setText(initialKeyword)
        binding.btnClearKeyword.isVisible = initialKeyword.isNotBlank()

        binding.etSearchKeyword.doAfterTextChanged { text ->
            binding.btnClearKeyword.isVisible = !text.isNullOrBlank()
        }

        binding.btnClearKeyword.setOnClickListener {
            binding.etSearchKeyword.setText("")
            viewModel.updateKeyword("")
        }

        binding.etSearchKeyword.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.updateKeyword(v.text.toString())
                true
            } else {
                false
            }
        }

        binding.btnTopFilter.setOnClickListener {
            openFilterBottomSheet()
        }
    }

    private fun setupRecyclerView() {
        binding.rvSearchResults.layoutManager = LinearLayoutManager(this)
        binding.rvSearchResults.adapter = adapter

        binding.rvSearchResults.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 3 && firstVisibleItemPosition >= 0) {
                        viewModel.loadNextPage()
                    }
                }
            }
        })

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }

        binding.btnClearAllFilters.setOnClickListener {
            binding.etSearchKeyword.setText("")
            viewModel.clearAllFilters()
        }
    }

    private fun setupQuickFilterChips() {
        binding.chipFilter.setOnClickListener {
            openFilterBottomSheet()
        }

        binding.chipPrice.setOnClickListener {
            openFilterBottomSheet()
        }

        binding.chipGender.setOnClickListener {
            openFilterBottomSheet()
        }

        binding.chipSort.setOnClickListener {
            val currentFilter = viewModel.uiState.value.filter
            val nextOrder = if (currentFilter.order == "asc") "desc" else "asc"
            viewModel.applyFilter(currentFilter.copy(sort = "rentPrice", order = nextOrder))
        }

        binding.chipClearAll.setOnClickListener {
            binding.etSearchKeyword.setText("")
            viewModel.clearAllFilters()
        }
    }

    private fun openFilterBottomSheet() {
        val dialog = FilterBottomSheetDialogFragment.newInstance(viewModel.uiState.value.filter)
        dialog.onFilterApplied = { newFilter ->
            viewModel.applyFilter(newFilter)
        }
        dialog.show(supportFragmentManager, FilterBottomSheetDialogFragment.TAG)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.swipeRefreshLayout.isRefreshing = state.isRefreshing
                    binding.progressBar.isVisible = state.isLoading && !state.isRefreshing

                    adapter.submitList(state.rooms)

                    val isEmpty = !state.isLoading && state.rooms.isEmpty()
                    binding.layoutEmptyState.isVisible = isEmpty
                    binding.rvSearchResults.isVisible = !isEmpty

                    // Update Badge and Chips
                    val activeFilters = state.filter.activeFilterCount()
                    binding.tvTopFilterBadge.isVisible = activeFilters > 0
                    binding.tvTopFilterBadge.text = activeFilters.toString()

                    binding.chipClearAll.isVisible = state.filter.isFiltered()

                    // Update Price Chip Label
                    val minP = state.filter.minPrice
                    val maxP = state.filter.maxPrice
                    binding.chipPrice.text = when {
                        minP != null && maxP != null -> "${minP / 1_000_000}-${maxP / 1_000_000}tr ▾"
                        minP != null -> ">${minP / 1_000_000}tr ▾"
                        maxP != null -> "<${maxP / 1_000_000}tr ▾"
                        else -> "Chọn giá ▾"
                    }

                    // Update Gender Chip Label
                    binding.chipGender.text = when (state.filter.genderPolicy) {
                        "male" -> "Nam ▾"
                        "female" -> "Nữ ▾"
                        else -> "Giới tính ▾"
                    }

                    // Update Sort Chip Label
                    binding.chipSort.text = if (state.filter.sort == "rentPrice") {
                        if (state.filter.order == "asc") "Giá tăng ↑" else "Giá giảm ↓"
                    } else {
                        "Xếp giá ⇅"
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is SearchResultEvent.ShowToast -> toast(event.message)
                        is SearchResultEvent.ShowError -> toast(event.message)
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_FILTER = "extra_filter"

        fun start(context: Context, filter: RoomFilter = RoomFilter.default()) {
            val intent = Intent(context, SearchResultActivity::class.java).apply {
                putExtra(EXTRA_FILTER, filter)
            }
            context.startActivity(intent)
        }
    }
}
