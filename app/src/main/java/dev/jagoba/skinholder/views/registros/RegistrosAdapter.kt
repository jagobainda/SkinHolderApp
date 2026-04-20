package dev.jagoba.skinholder.views.registros

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dev.jagoba.skinholder.databinding.ItemRegistroBinding
import dev.jagoba.skinholder.models.registros.Registro
import java.text.SimpleDateFormat
import java.util.Locale

interface RegistroActions {
    fun onViewDetail(registroId: Long)
    fun onDelete(registroId: Long)
}

class RegistrosAdapter(
    private val actions: RegistroActions
) : PagingDataAdapter<Registro, RegistrosAdapter.ViewHolder>(RegistroDiffCallback()) {

    private val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRegistroBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    inner class ViewHolder(
        private val binding: ItemRegistroBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(registro: Registro) {
            binding.textFecha.text = formatDate(registro.fechaHora)
            binding.textTotalSteam.text = formatPrice(registro.totalSteam)
            binding.textTotalGamerpay.text = formatPrice(registro.totalGamerPay)
            binding.textTotalCsfloat.text = formatPrice(registro.totalCsFloat)

            binding.btnViewDetail.setOnClickListener {
                actions.onViewDetail(registro.registroId)
            }
            binding.btnDelete.setOnClickListener {
                actions.onDelete(registro.registroId)
            }
        }

        private fun formatDate(dateStr: String): String {
            return try {
                val date = inputFormat.parse(dateStr)
                date?.let { displayFormat.format(it) } ?: dateStr
            } catch (_: Exception) {
                dateStr
            }
        }

        private fun formatPrice(price: Double): String {
            return String.format(Locale.getDefault(), "%.2f €", price)
        }
    }

    class RegistroDiffCallback : DiffUtil.ItemCallback<Registro>() {
        override fun areItemsTheSame(oldItem: Registro, newItem: Registro): Boolean {
            return oldItem.registroId == newItem.registroId
        }

        override fun areContentsTheSame(oldItem: Registro, newItem: Registro): Boolean {
            return oldItem == newItem
        }
    }
}
