package dev.jagoba.skinholder.views.useritems

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jagoba.skinholder.databinding.ItemUserItemBinding

interface UserItemActions {
    fun onIncrement(userItemId: Long)
    fun onDecrement(userItemId: Long)
    fun onSave(userItemId: Long)
}

class UserItemAdapter(
    private val actions: UserItemActions
) : ListAdapter<UserItemUiModel, UserItemAdapter.ViewHolder>(UserItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUserItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemUserItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(model: UserItemUiModel) {
            binding.textItemName.text = model.userItem.itemName
            binding.textCantidad.text = model.editedCantidad.toString()

            val buttonsEnabled = !model.isSaving
            binding.btnDecrement.isEnabled = buttonsEnabled
            binding.btnIncrement.isEnabled = buttonsEnabled
            binding.btnSave.isEnabled = buttonsEnabled

            binding.btnDecrement.setOnClickListener {
                actions.onDecrement(model.userItem.userItemId)
            }
            binding.btnIncrement.setOnClickListener {
                actions.onIncrement(model.userItem.userItemId)
            }
            binding.btnSave.setOnClickListener {
                actions.onSave(model.userItem.userItemId)
            }

            // Transient color flash on save result (matches Desktop NombreForeground behavior)
            when (model.saveResult) {
                SaveResult.SUCCESS -> flashNameColor(Color.parseColor("#4CAF50"))
                SaveResult.ERROR -> flashNameColor(Color.parseColor("#F44336"))
                null -> {
                    // Reset to default text color
                    val defaultColor = binding.textItemName.textColors.defaultColor
                    binding.textItemName.setTextColor(defaultColor)
                }
            }
        }

        private fun flashNameColor(color: Int) {
            val defaultColor = binding.textItemName.textColors.defaultColor
            binding.textItemName.setTextColor(color)
            ValueAnimator.ofObject(ArgbEvaluator(), color, defaultColor).apply {
                duration = 1200
                startDelay = 300
                addUpdateListener { animator ->
                    binding.textItemName.setTextColor(animator.animatedValue as Int)
                }
                start()
            }
        }
    }

    class UserItemDiffCallback : DiffUtil.ItemCallback<UserItemUiModel>() {
        override fun areItemsTheSame(oldItem: UserItemUiModel, newItem: UserItemUiModel): Boolean {
            return oldItem.userItem.userItemId == newItem.userItem.userItemId
        }

        override fun areContentsTheSame(oldItem: UserItemUiModel, newItem: UserItemUiModel): Boolean {
            return oldItem == newItem
        }
    }
}
