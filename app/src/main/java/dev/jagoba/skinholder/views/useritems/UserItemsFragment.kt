package dev.jagoba.skinholder.views.useritems

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.jagoba.skinholder.R
import dev.jagoba.skinholder.databinding.FragmentUserItemsBinding
import dev.jagoba.skinholder.views.useritems.add.AddUserItemBottomSheet
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserItemsFragment : Fragment(), UserItemActions {

    private var _binding: FragmentUserItemsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UserItemsViewModel by viewModels()
    private lateinit var adapter: UserItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserItemsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearch()
        setupSwipeRefresh()
        setupFab()
        setupAddItemResultListener()
        observeState()
    }

    private fun setupRecyclerView() {
        adapter = UserItemAdapter(this)
        binding.recyclerUserItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@UserItemsFragment.adapter
        }
    }

    private fun setupSearch() {
        binding.editSearch.doAfterTextChanged { text ->
            viewModel.searchQuery.value = text?.toString().orEmpty()
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.primary)
        binding.swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.surface_container)
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    private fun setupFab() {
        binding.fabAddItem.setOnClickListener {
            val ownedIds = viewModel.ownedItemIds()
            AddUserItemBottomSheet.newInstance(ownedIds)
                .show(parentFragmentManager, AddUserItemBottomSheet.TAG)
        }
    }

    private fun setupAddItemResultListener() {
        parentFragmentManager.setFragmentResultListener(
            AddUserItemBottomSheet.RESULT_KEY,
            viewLifecycleOwner
        ) { _, _ ->
            Snackbar.make(
                binding.root,
                getString(R.string.add_item_success),
                Snackbar.LENGTH_SHORT
            ).show()
            viewModel.refresh()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.filteredItems.collect { state ->
                    binding.swipeRefresh.isRefreshing = false
                    when (state) {
                        is UserItemsUiState.Loading -> {
                            binding.skeletonLoading.isVisible = true
                            binding.recyclerUserItems.isVisible = false
                            binding.layoutEmpty.isVisible = false
                        }

                        is UserItemsUiState.Success -> {
                            binding.skeletonLoading.isVisible = false
                            binding.recyclerUserItems.isVisible = true
                            binding.layoutEmpty.isVisible = false
                            adapter.submitList(state.items)
                        }

                        is UserItemsUiState.Empty -> {
                            binding.skeletonLoading.isVisible = false
                            binding.recyclerUserItems.isVisible = false
                            binding.layoutEmpty.isVisible = true
                        }

                        is UserItemsUiState.Error -> {
                            binding.skeletonLoading.isVisible = false
                            binding.recyclerUserItems.isVisible = false
                            binding.layoutEmpty.isVisible = false
                            Snackbar.make(
                                binding.root,
                                state.message,
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }

    // UserItemActions implementation
    override fun onIncrement(userItemId: Long) {
        viewModel.incrementCantidad(userItemId)
    }

    override fun onDecrement(userItemId: Long) {
        viewModel.decrementCantidad(userItemId)
    }

    override fun onSave(userItemId: Long) {
        viewModel.saveItem(userItemId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
