package dev.jagoba.skinholder.views.useritems.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dev.jagoba.skinholder.databinding.BottomSheetAddUserItemBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddUserItemBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddUserItemBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddUserItemViewModel by viewModels()
    private lateinit var adapter: AvailableItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddUserItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ownedIds = arguments?.getIntArray(ARG_OWNED_IDS)?.toList().orEmpty()
        viewModel.setOwnedItemIds(ownedIds)

        setupRecycler()
        setupInputs()
        setupButton()
        observeState()
        observeEvents()
    }

    private fun setupRecycler() {
        adapter = AvailableItemAdapter(viewModel::onSelectItem)
        binding.recyclerAvailableItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AddUserItemBottomSheet.adapter
        }
    }

    private fun setupInputs() {
        binding.editSearchItems.doAfterTextChanged { text ->
            viewModel.searchQuery.value = text?.toString().orEmpty()
        }
        binding.editQuantity.doAfterTextChanged { text ->
            val raw = text?.toString().orEmpty()
            val digits = raw.filter { it.isDigit() }.take(9)
            if (digits != raw) {
                binding.editQuantity.setText(digits)
                binding.editQuantity.setSelection(digits.length)
                return@doAfterTextChanged
            }
            viewModel.quantityText.value = digits
        }
    }

    private fun setupButton() {
        binding.btnAddItem.setOnClickListener { viewModel.submit() }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.filteredItems.collectLatest { items ->
                        adapter.submitList(items)
                        binding.textEmptyItems.isVisible = items.isEmpty()
                    }
                }
                launch {
                    viewModel.selectedItemId.collectLatest { id ->
                        adapter.select(id)
                    }
                }
                launch {
                    viewModel.isSaving.collectLatest { saving ->
                        binding.progressAdd.isVisible = saving
                        binding.btnAddItem.isEnabled = !saving
                    }
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collectLatest { event ->
                    val ctx = context ?: return@collectLatest
                    when (event) {
                        is AddUserItemEvent.ValidationError -> {
                            Toast.makeText(
                                ctx,
                                getString(event.messageRes),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        AddUserItemEvent.SaveError -> {
                            Toast.makeText(
                                ctx,
                                getString(dev.jagoba.skinholder.R.string.add_item_error_add),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        AddUserItemEvent.Saved -> {
                            if (isAdded) {
                                parentFragmentManager.setFragmentResult(RESULT_KEY, Bundle.EMPTY)
                            }
                            dismissAllowingStateLoss()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val RESULT_KEY = "add_user_item_result"
        private const val ARG_OWNED_IDS = "owned_ids"
        const val TAG = "AddUserItemBottomSheet"

        fun newInstance(ownedIds: List<Int>): AddUserItemBottomSheet =
            AddUserItemBottomSheet().apply {
                arguments = bundleOf(ARG_OWNED_IDS to ownedIds.toIntArray())
            }
    }
}
