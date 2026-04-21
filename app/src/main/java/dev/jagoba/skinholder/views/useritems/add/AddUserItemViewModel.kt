package dev.jagoba.skinholder.views.useritems.add

import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jagoba.skinholder.R
import dev.jagoba.skinholder.core.AuthSessionManager
import dev.jagoba.skinholder.core.BaseViewModel
import dev.jagoba.skinholder.core.SessionExpiredException
import dev.jagoba.skinholder.dataservice.repository.ItemsRepository
import dev.jagoba.skinholder.dataservice.repository.UserItemRepository
import dev.jagoba.skinholder.models.items.Item
import dev.jagoba.skinholder.models.items.UserItem
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AddUserItemUiState {
    data object Loading : AddUserItemUiState()
    data class Ready(val items: List<Item>) : AddUserItemUiState()
    data class Error(val message: String) : AddUserItemUiState()
}

sealed class AddUserItemEvent {
    data class ValidationError(@StringRes val messageRes: Int) : AddUserItemEvent()
    data object Saved : AddUserItemEvent()
    data object SaveError : AddUserItemEvent()
}

@HiltViewModel
class AddUserItemViewModel @Inject constructor(
    private val itemsRepository: ItemsRepository,
    private val userItemRepository: UserItemRepository,
    private val authSessionManager: AuthSessionManager
) : BaseViewModel<AddUserItemUiState>(AddUserItemUiState.Loading) {

    val searchQuery = MutableStateFlow("")
    val selectedItemId = MutableStateFlow<Int?>(null)
    val quantityText = MutableStateFlow("")
    val isSaving = MutableStateFlow(false)

    private val ownedItemIds = MutableStateFlow<Set<Int>>(emptySet())

    private val _events = MutableSharedFlow<AddUserItemEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<AddUserItemEvent> = _events.asSharedFlow()

    val filteredItems: StateFlow<List<Item>> = combine(
        _uiState, searchQuery, ownedItemIds
    ) { state, query, owned ->
        val items = (state as? AddUserItemUiState.Ready)?.items.orEmpty()
        val available = items.filter { it.itemId !in owned }
        if (query.isBlank()) available
        else available.filter { it.nombre.orEmpty().contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadItems()
    }

    fun setOwnedItemIds(ids: Collection<Int>) {
        ownedItemIds.value = ids.toSet()
    }

    fun loadItems() {
        _uiState.value = AddUserItemUiState.Loading
        viewModelScope.launch(exceptionHandler) {
            val result = itemsRepository.getItems()
            result.fold(
                onSuccess = { items ->
                    val sorted = items.sortedBy { it.nombre.orEmpty().lowercase() }
                    _uiState.value = AddUserItemUiState.Ready(sorted)
                },
                onFailure = { error ->
                    if (error !is SessionExpiredException) {
                        _uiState.value = AddUserItemUiState.Error(
                            error.message ?: "Error inesperado"
                        )
                    }
                }
            )
        }
    }

    fun onSelectItem(item: Item) {
        selectedItemId.value = item.itemId
    }

    fun submit() {
        if (isSaving.value) return

        val state = _uiState.value
        val items = (state as? AddUserItemUiState.Ready)?.items.orEmpty()

        if (items.isEmpty()) {
            _events.tryEmit(AddUserItemEvent.ValidationError(R.string.add_item_error_no_items))
            return
        }

        val selectedId = selectedItemId.value
        val selected = items.firstOrNull { it.itemId == selectedId }
        if (selected == null) {
            _events.tryEmit(AddUserItemEvent.ValidationError(R.string.add_item_error_select))
            return
        }

        if (selected.itemId in ownedItemIds.value) {
            _events.tryEmit(AddUserItemEvent.ValidationError(R.string.add_item_error_already_owned))
            return
        }

        val cantidad = quantityText.value.trim().toIntOrNull()
        if (cantidad == null || cantidad <= 0) {
            _events.tryEmit(AddUserItemEvent.ValidationError(R.string.add_item_error_invalid_qty))
            return
        }

        val newUserItem = UserItem(
            userItemId = 0,
            cantidad = cantidad,
            itemId = selected.itemId,
            userId = authSessionManager.getUserId(),
            itemName = selected.nombre.orEmpty(),
            steamHashName = selected.hashNameSteam.orEmpty(),
            gamerPayName = selected.gamerPayNombre.orEmpty(),
            csFloatMarketHashName = ""
        )

        isSaving.value = true
        viewModelScope.launch(exceptionHandler) {
            val result = try {
                userItemRepository.addUserItem(newUserItem)
            } catch (e: SessionExpiredException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
            isSaving.value = false
            result.fold(
                onSuccess = { _events.tryEmit(AddUserItemEvent.Saved) },
                onFailure = { error ->
                    if (error !is SessionExpiredException) {
                        _events.tryEmit(AddUserItemEvent.SaveError)
                    }
                }
            )
        }
    }
}
