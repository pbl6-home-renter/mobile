package com.rentify.app.tenant.ui.favoriteroom

import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rentify.app.core.network.dto.room.FavoriteRoomListResponseDto
import com.rentify.app.core.ui.base.BaseActivity
import com.rentify.app.core.ui.extension.applySystemBarsPadding
import com.rentify.app.core.ui.extension.toast
import com.rentify.app.tenant.databinding.ActivityFavoriteRoomsBinding
import com.rentify.app.tenant.ui.room.detail.RoomDetailActivity
import kotlinx.coroutines.launch

class FavoriteRoomsActivity : BaseActivity<ActivityFavoriteRoomsBinding>(ActivityFavoriteRoomsBinding::inflate) {

    private val viewModel: FavoriteRoomsViewModel by viewModels {
        FavoriteRoomsViewModelFactory(this)
    }

    private val adapter by lazy {
        FavoriteRoomsAdapter(
            onRoomClick = { room ->
                RoomDetailActivity.start(this, room.roomId, isFavorited = true)
            },
            onRemoveFavoriteClick = { room ->
                showRemoveFavoriteConfirmDialog(room)
            }
        )
    }

    override fun initView() {
        binding.root.applySystemBarsPadding(top = true, bottom = true)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.rvFavoriteRooms.layoutManager = LinearLayoutManager(this)
        binding.rvFavoriteRooms.adapter = adapter

        binding.rvFavoriteRooms.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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

        binding.btnExploreNow.setOnClickListener {
            finish()
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.swipeRefreshLayout.isRefreshing = state.isRefreshing
                    binding.progressBar.isVisible = state.isLoading && !state.isRefreshing

                    adapter.submitList(state.items)

                    val isEmpty = !state.isLoading && state.items.isEmpty()
                    binding.layoutEmptyState.isVisible = isEmpty
                    binding.rvFavoriteRooms.isVisible = !isEmpty

                    binding.toolbar.subtitle = if (state.totalItems > 0) {
                        "${state.totalItems} phòng đã lưu"
                    } else {
                        null
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is FavoriteRoomsEvent.ShowToast -> toast(event.message)
                        is FavoriteRoomsEvent.ShowError -> toast(event.message)
                        is FavoriteRoomsEvent.RoomRemoved -> {}
                    }
                }
            }
        }
    }

    private fun showRemoveFavoriteConfirmDialog(room: FavoriteRoomListResponseDto) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Bỏ lưu phòng trọ")
            .setMessage("Bạn có chắc chắn muốn bỏ lưu phòng \"${room.name}\" khỏi danh sách yêu thích không?")
            .setPositiveButton("Bỏ lưu") { _, _ ->
                viewModel.removeFavorite(room)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}
