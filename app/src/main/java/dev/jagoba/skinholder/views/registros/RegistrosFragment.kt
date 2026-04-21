package dev.jagoba.skinholder.views.registros

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.jagoba.skinholder.R
import dev.jagoba.skinholder.databinding.FragmentRegistrosBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class RegistrosFragment : Fragment(), RegistroActions {

    private var _binding: FragmentRegistrosBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RegistrosViewModel by viewModels()
    private lateinit var adapter: RegistrosAdapter

    private val sortFields = SortField.entries.toTypedArray()
    private val chipDateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegistrosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupDateFilter()
        setupSortControls()
        setupSwipeRefresh()
        setupConsultar()
        observeState()
        observeConsultaState()
        observeEvents()
    }

    private fun setupRecyclerView() {
        adapter = RegistrosAdapter(this)
        binding.recyclerRegistros.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@RegistrosFragment.adapter
        }

        adapter.addLoadStateListener { loadState ->
            val isListEmpty = loadState.refresh is LoadState.NotLoading && adapter.itemCount == 0
            val isLoading = loadState.refresh is LoadState.Loading

            binding.progressLoading.isVisible = isLoading
            binding.recyclerRegistros.isVisible = !isLoading && !isListEmpty
            binding.layoutEmpty.isVisible = isListEmpty && !isLoading
                    && viewModel.uiState.value !is RegistrosUiState.Error
        }
    }

    private fun setupDateFilter() {
        binding.chipDateRange.setOnClickListener {
            val picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText(getString(R.string.registros_filter_date))
                .build()

            picker.addOnPositiveButtonClickListener { selection ->
                viewModel.setDateRange(selection.first, selection.second + 86_399_999L) // end of day
                updateDateChipText(selection.first, selection.second)
                binding.chipDateRange.isCloseIconVisible = true
            }

            picker.show(parentFragmentManager, "date_range_picker")
        }

        binding.chipDateRange.setOnCloseIconClickListener {
            viewModel.clearDateRange()
            binding.chipDateRange.text = getString(R.string.registros_filter_date)
            binding.chipDateRange.isCloseIconVisible = false
        }
    }

    private fun updateDateChipText(startMillis: Long, endMillis: Long) {
        val start = chipDateFormat.format(Date(startMillis))
        val end = chipDateFormat.format(Date(endMillis))
        binding.chipDateRange.text = getString(R.string.registros_date_range_format, start, end)
    }

    private fun setupSortControls() {
        val sortLabels = arrayOf(
            getString(R.string.registros_sort_fecha),
            getString(R.string.registros_sort_steam),
            getString(R.string.registros_sort_gamerpay),
            getString(R.string.registros_sort_csfloat)
        )

        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item,
            sortLabels
        ).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item)
        }
        binding.spinnerSort.adapter = spinnerAdapter

        binding.spinnerSort.setSelection(0, false)
        binding.spinnerSort.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long
                ) {
                    viewModel.setSortOption(sortFields[position])
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }

        binding.btnSort.setOnClickListener {
            val currentPos = binding.spinnerSort.selectedItemPosition
            viewModel.setSortOption(sortFields[currentPos])
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.primary)
        binding.swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.surface_container)
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    private fun setupConsultar() {
        binding.fabConsultar.setOnClickListener {
            viewModel.consultarPrecios()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        binding.swipeRefresh.isRefreshing = false
                        when (state) {
                            is RegistrosUiState.Loading -> {
                                binding.progressLoading.isVisible = true
                                binding.recyclerRegistros.isVisible = false
                                binding.layoutEmpty.isVisible = false
                            }
                            is RegistrosUiState.Loaded -> {
                                binding.progressLoading.isVisible = false
                                binding.recyclerRegistros.isVisible = true
                                binding.layoutEmpty.isVisible = false
                            }
                            is RegistrosUiState.Empty -> {
                                binding.progressLoading.isVisible = false
                                binding.recyclerRegistros.isVisible = false
                                binding.layoutEmpty.isVisible = true
                            }
                            is RegistrosUiState.Error -> {
                                binding.progressLoading.isVisible = false
                                binding.recyclerRegistros.isVisible = false
                                binding.layoutEmpty.isVisible = false
                                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                            }
                        }
                    }
                }

                launch {
                    viewModel.pagingData.collectLatest { pagingData ->
                        adapter.submitData(pagingData)
                    }
                }
            }
        }
    }

    private fun observeConsultaState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.consultaState.collect { state ->
                    when (state) {
                        is ConsultaState.Loading -> {
                            binding.fabConsultar.isEnabled = false
                            binding.layoutConsultaProgress.isVisible = true
                            if (state.total > 0) {
                                binding.progressConsulta.isIndeterminate = false
                                binding.progressConsulta.max = state.total
                                binding.progressConsulta.setProgressCompat(state.progreso, true)
                                binding.textConsultaProgress.text = getString(
                                    R.string.registros_consultando_progreso,
                                    state.progreso,
                                    state.total
                                )
                            } else {
                                binding.progressConsulta.isIndeterminate = true
                                binding.textConsultaProgress.text =
                                    getString(R.string.registros_consultando_inicio)
                            }
                        }
                        is ConsultaState.Idle,
                        is ConsultaState.Success,
                        is ConsultaState.Error -> {
                            binding.fabConsultar.isEnabled = true
                            binding.layoutConsultaProgress.isVisible = false
                        }
                    }
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is RegistrosEvent.Deleted ->
                            Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
                        is RegistrosEvent.DeleteError ->
                            Snackbar.make(binding.root, event.message, Snackbar.LENGTH_LONG).show()
                        is RegistrosEvent.ConsultaSuccess ->
                            showColoredSnackbar(
                                getString(R.string.registros_consulta_success),
                                R.color.status_success
                            )
                        is RegistrosEvent.ConsultaError ->
                            showColoredSnackbar(event.message, R.color.status_error)
                    }
                }
            }
        }
    }

    private fun showColoredSnackbar(message: String, backgroundColorRes: Int) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAnchorView(binding.fabConsultar)
            .setBackgroundTint(ContextCompat.getColor(requireContext(), backgroundColorRes))
            .setTextColor(ContextCompat.getColor(requireContext(), R.color.on_error))
            .show()
    }

    // RegistroActions
    override fun onViewDetail(registroId: Long) {
        findNavController().navigate(
            R.id.action_registros_to_detail,
            bundleOf("registroId" to registroId)
        )
    }

    override fun onDelete(registroId: Long) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.registros_delete_confirm_title))
            .setMessage(getString(R.string.registros_delete_confirm_message))
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .setPositiveButton(getString(R.string.dialog_delete)) { _, _ ->
                viewModel.deleteRegistro(registroId)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
