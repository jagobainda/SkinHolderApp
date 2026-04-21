package dev.jagoba.skinholder.views.useritems.add

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jagoba.skinholder.R
import dev.jagoba.skinholder.databinding.ItemAvailableItemBinding
import dev.jagoba.skinholder.models.items.Item

class AvailableItemAdapter(
    private val onSelect: (Item) -> Unit
) : ListAdapter<Item, AvailableItemAdapter.ViewHolder>(DiffCallback) {

    // Selection state lives only in the adapter, not in the list model.
    var selectedItemId: Int? = null
        private set

    /** Update the selected row. Only the two affected rows are notified. */
    fun select(itemId: Int?) {
        if (selectedItemId == itemId) return
        val old = selectedItemId
        selectedItemId = itemId
        if (old != null) {
            val idx = currentList.indexOfFirst { it.itemId == old }
            if (idx >= 0) notifyItemChanged(idx)
        }
        if (itemId != null) {
            val idx = currentList.indexOfFirst { it.itemId == itemId }
            if (idx >= 0) notifyItemChanged(idx)
        }
    }

    /**
     * Re-apply selection highlight whenever a new list is dispatched by submitList.
     * Needed because select() may be called while AsyncListDiffer is still computing,
     * so currentList is stale and notifyItemChanged targets the wrong (or missing) index.
     */
    override fun onCurrentListChanged(previousList: List<Item>, currentList: List<Item>) {
        super.onCurrentListChanged(previousList, currentList)
        val selId = selectedItemId ?: return
        val idx = currentList.indexOfFirst { it.itemId == selId }
        if (idx >= 0) notifyItemChanged(idx)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAvailableItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemAvailableItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item) {
            binding.textItemNombre.text = item.nombre
            val ctx = itemView.context
            val isSelected = item.itemId != 0 && item.itemId == selectedItemId
            if (isSelected) {
                binding.rowRoot.setBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.surface_container_highest)
                )
                binding.textItemNombre.setTextColor(
                    ContextCompat.getColor(ctx, R.color.primary)
                )
            } else {
                binding.rowRoot.setBackgroundColor(Color.TRANSPARENT)
                binding.textItemNombre.setTextColor(
                    ContextCompat.getColor(ctx, R.color.on_surface)
                )
            }
            binding.rowRoot.setOnClickListener { onSelect(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item) =
            oldItem.itemId == newItem.itemId && oldItem.itemId != 0

        override fun areContentsTheSame(oldItem: Item, newItem: Item) = oldItem == newItem
    }
}
