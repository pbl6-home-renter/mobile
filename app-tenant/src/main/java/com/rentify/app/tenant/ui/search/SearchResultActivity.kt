package com.rentify.app.tenant.ui.search

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.PopupWindow
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rentify.app.core.network.dto.room.RoomPublicListResponseDto
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.toViFormatted
import com.rentify.app.core.ui.extension.toast
import com.rentify.app.tenant.R
import com.rentify.app.tenant.databinding.ActivitySearchResultBinding
import com.rentify.app.tenant.databinding.LayoutPopupRoomSortBinding
import com.rentify.app.tenant.ui.dialog.LoginFeature
import com.rentify.app.tenant.ui.dialog.showLoginRequiredSheet
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

    private val recentSearchManager by lazy { RecentSearchManager(this) }
    private var pendingFavoriteRoom: RoomPublicListResponseDto? = null

    private val adapter by lazy {
        SearchResultAdapter(
            onRoomClick = { room ->
                val fullAddress = listOf(room.address, room.ward, room.province)
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString(", ")
                RoomDetailActivity.start(
                    context = this,
                    roomId = room.id,
                    isFavorited = room.isFavorited,
                    fallbackAddress = fullAddress
                )
            },
            onFavoriteClick = { room ->
                if (viewModel.uiState.value.isLoggedIn) {
                    viewModel.toggleFavorite(room)
                } else {
                    pendingFavoriteRoom = room
                    showLoginRequiredSheet(LoginFeature.PROFILE) {
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
        setupRecentAndPopularSection()
        observeViewModel()

        if (initialFilter.keyword.isNullOrBlank() && !initialFilter.isFiltered()) {
            showInitialState()
        } else {
            hideInitialState()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkAuth()
    }

    private fun setupTopSearchBar() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        val initialKeyword = initialFilter.keyword.orEmpty()
        binding.etSearchKeyword.setText(initialKeyword)
        binding.btnClearKeyword.isVisible = initialKeyword.isNotBlank()

        binding.etSearchKeyword.doAfterTextChanged { text ->
            val kw = text?.toString().orEmpty()
            binding.btnClearKeyword.isVisible = kw.isNotBlank()
            if (kw.isBlank() && !viewModel.uiState.value.filter.isFiltered()) {
                showInitialState()
            }
        }

        binding.btnClearKeyword.setOnClickListener {
            binding.etSearchKeyword.setText("")
            if (!viewModel.uiState.value.filter.isFiltered()) {
                showInitialState()
            }
            viewModel.updateKeyword("")
        }

        binding.etSearchKeyword.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = v.text.toString().trim()
                if (query.isNotBlank()) {
                    recentSearchManager.saveSearch(query)
                    renderRecentSearches()
                }
                hideInitialState()
                viewModel.updateKeyword(query)
                true
            } else {
                false
            }
        }

        binding.etSearchKeyword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && binding.etSearchKeyword.text.isNullOrBlank() && !viewModel.uiState.value.filter.isFiltered()) {
                showInitialState()
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rvSearchResults.layoutManager = LinearLayoutManager(this)
        binding.rvSearchResults.adapter = adapter

        binding.rvSearchResults.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val canScrollUp = recyclerView.canScrollVertically(-1)
                binding.layoutHeaderShadow.isVisible = canScrollUp

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

        binding.layoutErrorState.btnRetry.setOnClickListener {
            viewModel.loadInitial()
        }

        binding.btnSort.setOnClickListener {
            showSortDropdown()
        }
    }

    private fun setupQuickFilterChips() {
        binding.btnTopFilter.setOnClickListener {
            openFilterBottomSheet()
        }

        binding.chipPrice.setOnClickListener {
            openFilterBottomSheet()
        }

        binding.chipGender.setOnClickListener {
            openFilterBottomSheet()
        }

        binding.chipArea.setOnClickListener {
            openFilterBottomSheet()
        }
    }

    private fun openFilterBottomSheet() {
        val dialog = FilterBottomSheetDialogFragment.newInstance(viewModel.uiState.value.filter)
        dialog.onFilterApplied = { newFilter ->
            hideInitialState()
            viewModel.applyFilter(newFilter)
        }
        dialog.show(supportFragmentManager, FilterBottomSheetDialogFragment.TAG)
    }

    private fun showSortDropdown() {
        val currentFilter = viewModel.uiState.value.filter
        val popupBinding = LayoutPopupRoomSortBinding.inflate(layoutInflater)

        val popupWindow = PopupWindow(
            popupBinding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = dpToPx(12f).toFloat()
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        val primaryBlue = ContextCompat.getColor(this, com.rentify.app.core.ui.R.color.rentify_header_blue)
        val textDefault = Color.parseColor("#0F172A")
        val boldTypeface = try {
            ResourcesCompat.getFont(this, com.rentify.app.core.ui.R.font.plus_jakarta_sans)
        } catch (_: Exception) {
            null
        }

        val isNewest = currentFilter.sort == "createdAt"
        val isPriceAsc = currentFilter.sort == "rentPrice" && currentFilter.order == "asc"
        val isPriceDesc = currentFilter.sort == "rentPrice" && currentFilter.order == "desc"

        // 1. Mới nhất
        popupBinding.ivCheckNewest.isVisible = isNewest
        popupBinding.tvTitleNewest.setTextColor(if (isNewest) primaryBlue else textDefault)
        if (isNewest && boldTypeface != null) {
            popupBinding.tvTitleNewest.setTypeface(boldTypeface, Typeface.BOLD)
        }

        // 2. Giá: Thấp đến cao
        popupBinding.ivCheckPriceAsc.isVisible = isPriceAsc
        popupBinding.tvTitlePriceAsc.setTextColor(if (isPriceAsc) primaryBlue else textDefault)
        if (isPriceAsc && boldTypeface != null) {
            popupBinding.tvTitlePriceAsc.setTypeface(boldTypeface, Typeface.BOLD)
        }

        // 3. Giá: Cao đến thấp
        popupBinding.ivCheckPriceDesc.isVisible = isPriceDesc
        popupBinding.tvTitlePriceDesc.setTextColor(if (isPriceDesc) primaryBlue else textDefault)
        if (isPriceDesc && boldTypeface != null) {
            popupBinding.tvTitlePriceDesc.setTypeface(boldTypeface, Typeface.BOLD)
        }

        popupBinding.itemSortNewest.setOnClickListener {
            popupWindow.dismiss()
            if (!isNewest) {
                viewModel.applyFilter(currentFilter.copy(sort = "createdAt", order = "desc"))
            }
        }

        popupBinding.itemSortPriceAsc.setOnClickListener {
            popupWindow.dismiss()
            if (!isPriceAsc) {
                viewModel.applyFilter(currentFilter.copy(sort = "rentPrice", order = "asc"))
            }
        }

        popupBinding.itemSortPriceDesc.setOnClickListener {
            popupWindow.dismiss()
            if (!isPriceDesc) {
                viewModel.applyFilter(currentFilter.copy(sort = "rentPrice", order = "desc"))
            }
        }

        popupBinding.root.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupWidth = popupBinding.root.measuredWidth
        val btnSortWidth = binding.btnSort.width
        val xOffset = btnSortWidth - popupWidth
        val yOffset = dpToPx(4f)

        popupWindow.showAsDropDown(binding.btnSort, xOffset, yOffset)
    }

    private fun setupRecentAndPopularSection() {
        renderRecentSearches()
        binding.btnClearRecentSearches.setOnClickListener {
            recentSearchManager.clearAll()
            renderRecentSearches()
        }

        setupPopularAreas()
    }

    private fun renderRecentSearches() {
        val recents = recentSearchManager.getRecentSearches()
        binding.layoutRecentSection.isVisible = recents.isNotEmpty()
        binding.layoutRecentSearchesContainer.removeAllViews()

        recents.forEach { keyword ->
            val itemView = layoutInflater.inflate(R.layout.item_recent_search, binding.layoutRecentSearchesContainer, false)
            val tvKeyword = itemView.findViewById<TextView>(R.id.tvRecentKeyword)
            val btnRemove = itemView.findViewById<View>(R.id.btnRemoveRecent)
            tvKeyword.text = keyword
            itemView.setOnClickListener {
                binding.etSearchKeyword.setText(keyword)
                binding.etSearchKeyword.setSelection(keyword.length)
                recentSearchManager.saveSearch(keyword)
                hideInitialState()
                viewModel.updateKeyword(keyword)
            }
            btnRemove.setOnClickListener {
                recentSearchManager.removeSearch(keyword)
                renderRecentSearches()
            }
            binding.layoutRecentSearchesContainer.addView(itemView)
        }
    }

    private fun setupPopularAreas() {
        val popularAreas = listOf("Hòa Khánh", "Hải Châu", "Ngũ Hành Sơn", "Sơn Trà", "Thanh Khê", "Cẩm Lệ")
        binding.chipGroupPopularAreas.removeAllViews()

        val typeface = try {
            ResourcesCompat.getFont(this, com.rentify.app.core.ui.R.font.plus_jakarta_sans)
        } catch (_: Exception) {
            android.graphics.Typeface.DEFAULT
        }

        popularAreas.forEach { areaName ->
            val chip = TextView(this).apply {
                text = areaName
                setBackgroundResource(R.drawable.bg_chip_pill_default)
                setTextColor(Color.parseColor("#14161B"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTypeface(typeface)
                gravity = Gravity.CENTER
                val h = dpToPx(36f)
                val padH = dpToPx(14f)
                setPadding(padH, 0, padH, 0)
                layoutParams = ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, h).apply {
                    marginEnd = dpToPx(8f)
                    bottomMargin = dpToPx(8f)
                }
                isClickable = true
                isFocusable = true
                contentDescription = "Chọn khu vực $areaName"
                setOnClickListener {
                    binding.etSearchKeyword.setText(areaName)
                    recentSearchManager.saveSearch(areaName)
                    hideInitialState()
                    val current = viewModel.uiState.value.filter
                    viewModel.applyFilter(
                        current.copy(
                            keyword = areaName,
                            ward = null,
                            province = "Thành phố Đà Nẵng"
                        )
                    )
                }
            }
            binding.chipGroupPopularAreas.addView(chip)
        }
    }

    private fun showInitialState() {
        renderRecentSearches()
        binding.layoutInitialState.isVisible = true
        binding.layoutSummaryRow.isVisible = false
        binding.swipeRefreshLayout.isVisible = false
        binding.pbLoading.isVisible = false
        binding.layoutEmptyState.isVisible = false
        binding.layoutErrorState.root.isVisible = false
    }

    private fun hideInitialState() {
        binding.layoutInitialState.isVisible = false
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Tự động lưu phòng sau khi đăng nhập thành công
                    if (state.isLoggedIn && pendingFavoriteRoom != null) {
                        val roomToSave = pendingFavoriteRoom!!
                        pendingFavoriteRoom = null
                        viewModel.toggleFavorite(roomToSave)
                    }

                    binding.swipeRefreshLayout.isRefreshing = state.isRefreshing
                    binding.pbLoadingMore.isVisible = state.isLoadingMore

                    if (binding.layoutInitialState.isVisible) {
                        return@collect
                    }

                    adapter.submitList(state.rooms)

                    val isInitialLoading = state.isLoading && !state.isRefreshing && state.rooms.isEmpty()
                    val hasError = !state.isLoading && state.errorMessage != null && state.rooms.isEmpty()
                    val isEmpty = !state.isLoading && state.errorMessage == null && state.rooms.isEmpty()
                    val hasData = state.rooms.isNotEmpty()

                    // Trạng thái Loading
                    binding.pbLoading.isVisible = isInitialLoading

                    // Trạng thái Error
                    binding.layoutErrorState.root.isVisible = hasError

                    // Trạng thái Empty
                    binding.layoutEmptyState.isVisible = isEmpty
                    if (isEmpty) {
                        binding.btnClearAllFilters.text = if (state.filter.isFiltered()) "Xóa bộ lọc" else "Xem tất cả phòng"
                    }

                    // Hiển thị danh sách và tóm tắt
                    binding.swipeRefreshLayout.isVisible = hasData
                    binding.layoutSummaryRow.isVisible = hasData

                    binding.tvResultCount.text = "${state.totalItems.toViFormatted()} phòng phù hợp"
                    binding.tvSortText.text = when {
                        state.filter.sort == "createdAt" -> "Mới nhất"
                        state.filter.sort == "rentPrice" && state.filter.order == "desc" -> "Giá: Cao đến thấp"
                        else -> "Giá: Thấp đến cao"
                    }

                    updateFilterChips(state.filter)
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

    private fun updateFilterChips(filter: RoomFilter) {
        val activeFilters = filter.activeFilterCount()

        // 1. Cập nhật nút Lọc trên Top Bar
        if (activeFilters > 0) {
            binding.btnTopFilter.setBackgroundResource(R.drawable.bg_btn_filter_top_active)
            binding.ivTopFilterIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#2563EB"))
            binding.tvTopFilterBadge.isVisible = true
            binding.tvTopFilterBadge.text = activeFilters.toString()
        } else {
            binding.btnTopFilter.setBackgroundResource(R.drawable.bg_btn_filter_top_default)
            binding.ivTopFilterIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#1F2937"))
            binding.tvTopFilterBadge.isVisible = false
        }

        // 2. Ẩn/Hiện toàn bộ hàng chip: Chỉ hiện khi có ít nhất 1 bộ lọc được áp dụng
        binding.scrollChipBar.isVisible = activeFilters > 0

        // 3. Chip Giá (Chỉ hiện khi có lọc giá, không có icon mũi tên)
        val minP = filter.minPrice
        val maxP = filter.maxPrice
        val hasPrice = minP != null || maxP != null
        binding.chipPrice.isVisible = hasPrice
        if (hasPrice) {
            binding.tvChipPriceText.text = when {
                minP != null && maxP != null -> {
                    val minM = minP / 1_000_000
                    val maxM = maxP / 1_000_000
                    if (minM == 0L) "Dưới $maxM triệu" else "$minM–$maxM triệu"
                }
                minP != null -> "Trên ${minP / 1_000_000} triệu"
                maxP != null -> "Dưới ${maxP / 1_000_000} triệu"
                else -> "Giá"
            }
        }

        // 4. Chip Giới tính (Chỉ hiện khi có lọc giới tính, không có icon mũi tên)
        val hasGender = filter.genderPolicy != "any"
        binding.chipGender.isVisible = hasGender
        if (hasGender) {
            binding.tvChipGenderText.text = if (filter.genderPolicy == "male") "Nam" else "Nữ"
        }

        // 5. Chip Khu vực (Chỉ hiện khi có lọc khu vực, không có icon mũi tên)
        val areaText = filter.ward ?: filter.province
        val hasArea = !areaText.isNullOrBlank()
        binding.chipArea.isVisible = hasArea
        if (hasArea) {
            binding.tvChipAreaText.text = areaText
        }
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density + 0.5f).toInt()
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
