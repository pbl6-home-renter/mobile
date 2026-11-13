package com.rentify.app.tenant.ui.room.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rentify.app.core.ui.R as CoreUiR
import com.rentify.app.tenant.databinding.ItemRoomImageSliderBinding
import com.rentify.app.tenant.ui.explore.RoomForYouAdapter

class RoomImageSliderAdapter(
    private val onImageClick: ((position: Int, url: String) -> Unit)? = null
) : RecyclerView.Adapter<RoomImageSliderAdapter.ImageViewHolder>() {

    private val images = mutableListOf<String>()

    fun submitList(list: List<String>) {
        images.clear()
        val validList = list.filter { it.isNotBlank() }
        if (validList.isEmpty()) {
            images.add(RoomForYouAdapter.resolveImageUrl(null, 0))
            images.add(RoomForYouAdapter.resolveImageUrl(null, 1))
            images.add(RoomForYouAdapter.resolveImageUrl(null, 2))
        } else if (validList.size == 1) {
            images.add(RoomForYouAdapter.resolveImageUrl(validList[0], 0))
            images.add(RoomForYouAdapter.resolveImageUrl(null, 1))
            images.add(RoomForYouAdapter.resolveImageUrl(null, 2))
        } else {
            images.addAll(validList)
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemRoomImageSliderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position], position)
    }

    override fun getItemCount(): Int = images.size

    inner class ImageViewHolder(
        private val binding: ItemRoomImageSliderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(url: String, position: Int) {
            val resolvedUrl = RoomForYouAdapter.resolveImageUrl(url, position)
            binding.ivRoomImage.load(resolvedUrl) {
                crossfade(true)
                placeholder(CoreUiR.drawable.ic_image_placeholder)
                error(CoreUiR.drawable.ic_image_placeholder)
            }
            binding.root.setOnClickListener {
                onImageClick?.invoke(position, resolvedUrl)
            }
        }
    }
}

