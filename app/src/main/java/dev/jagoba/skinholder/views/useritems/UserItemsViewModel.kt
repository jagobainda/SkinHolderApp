package dev.jagoba.skinholder.views.useritems

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jagoba.skinholder.core.BaseViewModel
import dev.jagoba.skinholder.core.SessionExpiredException
import dev.jagoba.skinholder.dataservice.repository.UserItemRepository
import dev.jagoba.skinholder.models.items.UserItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserItemUiModel(
    val userItem: UserItem,
    val editedCantidad: Int = userItem.cantidad,
    val isSaving: Boolean = false,
    val saveResult: SaveResult? = null
)

enum class SaveResult { SUCCESS, ERROR }

sealed class UserItemsUiState {
    data object Loading : UserItemsUiState()
    data class Success(val items: List<UserItemUiModel>) : UserItemsUiState()
    data class Error(val message: String) : UserItemsUiState()
    data object Empty : UserItemsUiState()
}

@HiltViewModel
class UserItemsViewModel @Inject constructor(
    private val userItemRepository: UserItemRepository
) : BaseViewModel<UserItemsUiState>(UserItemsUiState.Loading) {

    val searchQuery = MutableStateFlow("")

    val filteredItems: StateFlow<UserItemsUiState> = combine(_uiState, searchQuery) { state, query ->
        when (state) {
            is UserItemsUiState.Success -> {
                val filtered = if (query.isBlank()) {
                    state.items
                } else {
                    state.items.filter {
                        it.userItem.itemName.contains(query, ignoreCase = true)
                    }
                }
                if (filtered.isEmpty()) UserItemsUiState.Empty else UserItemsUiState.Success(filtered)
            }
            else -> state
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserItemsUiState.Loading)

    init {
        loadItems()
    }

    fun loadItems() {
        _uiState.value = UserItemsUiState.Loading
        viewModelScope.launch(exceptionHandler) {
            val result = userItemRepository.getUserItems()
            result.fold(
                onSuccess = { items ->
                    if (items.isEmpty()) {
                        _uiState.value = UserItemsUiState.Empty
                    } else {
                        val sorted = items.sortedBy { it.itemName.lowercase() }
                        _uiState.value = UserItemsUiState.Success(
                            sorted.map { UserItemUiModel(it) }
                        )
                    }
                },
                onFailure = { error ->
                    if (error !is SessionExpiredException) {
                        _uiState.value = UserItemsUiState.Error(
                            error.message ?: "Error inesperado"
                        )
                    }
                }
            )
        }
    }

    fun refresh() {
        loadItems()
    }

    fun incrementCantidad(userItemId: Long) {
        updateItems { items ->
            items.map {
                if (it.userItem.userItemId == userItemId) {
                    it.copy(editedCantidad = it.editedCantidad + 1)
                } else it
            }
        }
    }

    fun decrementCantidad(userItemId: Long) {
        updateItems { items ->
            items.map {
                if (it.userItem.userItemId == userItemId && it.editedCantidad > 0) {
                    it.copy(editedCantidad = it.editedCantidad - 1)
                } else it
            }
        }
    }

    fun saveItem(userItemId: Long) {
        val currentState = _uiState.value
        if (currentState !is UserItemsUiState.Success) return

        val item = currentState.items.find { it.userItem.userItemId == userItemId } ?: return

        updateItems { items ->
            items.map {
                if (it.userItem.userItemId == userItemId) it.copy(isSaving = true, saveResult = null)
                else it
            }
        }

        viewModelScope.launch(exceptionHandler) {
            val updatedUserItem = item.userItem.copy(cantidad = item.editedCantidad)
            val result = userItemRepository.updateUserItem(updatedUserItem)

            result.fold(
                onSuccess = {
                    updateItems { items ->
                        items.map {
                            if (it.userItem.userItemId == userItemId) {
                                it.copy(
                                    userItem = updatedUserItem,
                                    isSaving = false,
                                    saveResult = SaveResult.SUCCESS
                                )
                            } else it
                        }
                    }
                },
                onFailure = {
                    updateItems { items ->
                        items.map {
                            if (it.userItem.userItemId == userItemId) {
                                it.copy(isSaving = false, saveResult = SaveResult.ERROR)
                            } else it
                        }
                    }
                }
            )

            // Clear saveResult after a delay for transient feedback
            kotlinx.coroutines.delay(1500)
            updateItems { items ->
                items.map {
                    if (it.userItem.userItemId == userItemId) it.copy(saveResult = null)
                    else it
                }
            }
        }
    }

    private fun updateItems(transform: (List<UserItemUiModel>) -> List<UserItemUiModel>) {
        val currentState = _uiState.value
        if (currentState is UserItemsUiState.Success) {
            _uiState.value = UserItemsUiState.Success(transform(currentState.items))
        }
    }
}
