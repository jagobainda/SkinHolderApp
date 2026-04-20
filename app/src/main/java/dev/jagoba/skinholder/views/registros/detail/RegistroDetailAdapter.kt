package dev.jagoba.skinholder.views.registros.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jagoba.skinholder.databinding.ItemRegistroDetailBinding
import java.util.Locale

class RegistroDetailAdapter : ListAdapter<ItemDetalleUiModel, RegistroDetailAdapter.ViewHolder>(
    ItemDetalleDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRegistroDetailBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemRegistroDetailBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(model: ItemDetalleUiModel) {
            binding.textItemName.text = model.itemName
            binding.textCantidad.text = model.cantidad.toString()
            binding.textPrecioSteam.text = formatPrice(model.precioSteam)
            binding.textPrecioGamerpay.text = formatPrice(model.precioGamerPay)
            binding.textPrecioCsfloat.text = formatPrice(model.precioCsFloat)
        }

        private fun formatPrice(price: Double): String {
            return String.format(Locale.getDefault(), "%.2f €", price)
        }
    }

    class ItemDetalleDiffCallback : DiffUtil.ItemCallback<ItemDetalleUiModel>() {
        override fun areItemsTheSame(oldItem: ItemDetalleUiModel, newItem: ItemDetalleUiModel): Boolean {
            return oldItem.userItemId == newItem.userItemId
        }

        override fun areContentsTheSame(oldItem: ItemDetalleUiModel, newItem: ItemDetalleUiModel): Boolean {
            return oldItem == newItem
        }
    }
}
