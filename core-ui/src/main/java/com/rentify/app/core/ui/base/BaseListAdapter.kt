package com.rentify.app.core.ui.base

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.rentify.app.core.ui.extension.setOnSingleClickListener

class BaseViewHolder<VB : ViewBinding>(val binding: VB) : RecyclerView.ViewHolder(binding.root)

abstract class BaseListAdapter<T : Any, VB : ViewBinding>(
    private val inflate: (LayoutInflater, ViewGroup, Boolean) -> VB,
    diffCallback: DiffUtil.ItemCallback<T>
) : ListAdapter<T, BaseViewHolder<VB>>(diffCallback) {

    var onItemClick: ((T) -> Unit)? = null

    abstract fun bind(binding: VB, item: T, position: Int)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<VB> {
        val binding = inflate(LayoutInflater.from(parent.context), parent, false)
        return BaseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<VB>, position: Int) {
        val safePosition = holder.bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }
            ?: holder.absoluteAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }
            ?: position
        if (safePosition in 0 until itemCount) {
            val item = getItem(safePosition)
            bind(holder.binding, item, safePosition)
            holder.itemView.setOnSingleClickListener {
                onItemClick?.invoke(getItem(holder.bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: safePosition))
            }
        }
    }

    companion object {
        fun <T : Any> simpleDiff(
            areItemsTheSame: (T, T) -> Boolean
        ): DiffUtil.ItemCallback<T> {
            return object : DiffUtil.ItemCallback<T>() {
                override fun areItemsTheSame(oldItem: T, newItem: T): Boolean =
                    areItemsTheSame(oldItem, newItem)

                override fun areContentsTheSame(oldItem: T, newItem: T): Boolean =
                    oldItem == newItem
            }
        }
    }
}

fun <T : Any> simpleDiff(
    areItemsTheSame: (T, T) -> Boolean
): DiffUtil.ItemCallback<T> = BaseListAdapter.simpleDiff(areItemsTheSame)
