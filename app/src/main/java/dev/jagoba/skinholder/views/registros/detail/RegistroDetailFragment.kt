package dev.jagoba.skinholder.views.registros.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.jagoba.skinholder.R
import dev.jagoba.skinholder.databinding.FragmentRegistroDetailBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class RegistroDetailFragment : Fragment() {

    private var _binding: FragmentRegistroDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RegistroDetailViewModel by viewModels()
    private lateinit var adapter: RegistroDetailAdapter

    private val sortFields = DetailSortField.entries.toTypedArray()
    private val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private var pendingScrollToTop = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegistroDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSortControls()
        observeState()
    }

    private fun setupRecyclerView() {
        adapter = RegistroDetailAdapter()
        binding.recyclerDetailItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@RegistroDetailFragment.adapter
        }
    }

    private fun setupSortControls() {
        val sortLabels = arrayOf(
            getString(R.string.registro_detail_sort_name),
            getString(R.string.registro_detail_sort_cantidad),
            getString(R.string.registro_detail_sort_steam),
            getString(R.string.registro_detail_sort_gamerpay),
            getString(R.string.registro_detail_sort_csfloat)
        )

        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item,
            sortLabels
        ).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item)
        }
        binding.spinnerDetailSort.adapter = spinnerAdapter

        binding.spinnerDetailSort.setSelection(0, false)
        binding.spinnerDetailSort.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long
                ) {
                    pendingScrollToTop = true
                    viewModel.setSortOption(sortFields[position])
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }

        binding.btnDetailSort.setOnClickListener {
            val currentPos = binding.spinnerDetailSort.selectedItemPosition
            pendingScrollToTop = true
            viewModel.setSortOption(sortFields[currentPos])
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sortedState.collect { state ->
                    when (state) {
                        is RegistroDetailUiState.Loading -> {
                            binding.progressDetailLoading.isVisible = true
                            binding.cardSummary.isVisible = false
                            binding.layoutSort.isVisible = false
                            binding.recyclerDetailItems.isVisible = false
                            binding.layoutDetailEmpty.isVisible = false
                        }

                        is RegistroDetailUiState.Success -> {
                            binding.progressDetailLoading.isVisible = false
                            binding.cardSummary.isVisible = true
                            binding.layoutSort.isVisible = true

                            // Populate header
                            binding.textDetailFecha.text = formatDate(state.registro.fechaHora)
                            binding.textDetailTotalSteam.text = formatPrice(state.registro.totalSteam)
                            binding.textDetailTotalGamerpay.text = formatPrice(state.registro.totalGamerPay)
                            binding.textDetailTotalCsfloat.text = formatPrice(state.registro.totalCsFloat)

                            if (state.items.isEmpty()) {
                                binding.recyclerDetailItems.isVisible = false
                                binding.layoutDetailEmpty.isVisible = true
                            } else {
                                binding.recyclerDetailItems.isVisible = true
                                binding.layoutDetailEmpty.isVisible = false
                                val shouldScroll = pendingScrollToTop
                                pendingScrollToTop = false
                                adapter.submitList(state.items) {
                                    if (shouldScroll) {
                                        binding.recyclerDetailItems.scrollToPosition(0)
                                    }
                                }
                            }
                        }

                        is RegistroDetailUiState.Error -> {
                            binding.progressDetailLoading.isVisible = false
                            binding.cardSummary.isVisible = false
                            binding.layoutSort.isVisible = false
                            binding.recyclerDetailItems.isVisible = false
                            binding.layoutDetailEmpty.isVisible = false
                            Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                        }
                    }
                }
            }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
