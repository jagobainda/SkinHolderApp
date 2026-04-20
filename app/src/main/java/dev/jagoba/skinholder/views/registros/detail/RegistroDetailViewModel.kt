package dev.jagoba.skinholder.views.registros.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jagoba.skinholder.core.BaseViewModel
import dev.jagoba.skinholder.core.SessionExpiredException
import dev.jagoba.skinholder.dataservice.repository.ItemPrecioRepository
import dev.jagoba.skinholder.dataservice.repository.RegistroRepository
import dev.jagoba.skinholder.dataservice.repository.UserItemRepository
import dev.jagoba.skinholder.models.registros.Registro
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ItemDetalleUiModel(
    val userItemId: Long,
    val itemName: String,
    val cantidad: Int,
    val precioSteam: Double,
    val precioGamerPay: Double,
    val precioCsFloat: Double
)

enum class DetailSortField { NAME, CANTIDAD, STEAM, GAMERPAY, CSFLOAT }

data class DetailSortOption(
    val field: DetailSortField = DetailSortField.NAME,
    val ascending: Boolean = true
)

sealed class RegistroDetailUiState {
    data object Loading : RegistroDetailUiState()
    data class Success(
        val registro: Registro,
        val items: List<ItemDetalleUiModel>
    ) : RegistroDetailUiState()
    data class Error(val message: String) : RegistroDetailUiState()
}

@HiltViewModel
class RegistroDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val registroRepository: RegistroRepository,
    private val itemPrecioRepository: ItemPrecioRepository,
    private val userItemRepository: UserItemRepository
) : BaseViewModel<RegistroDetailUiState>(RegistroDetailUiState.Loading) {

    private val registroId: Long = savedStateHandle["registroId"] ?: 0L

    private val _sortOption = MutableStateFlow(DetailSortOption())

    val sortedState: StateFlow<RegistroDetailUiState> = combine(
        _uiState, _sortOption
    ) { state, sort ->
        when (state) {
            is RegistroDetailUiState.Success -> {
                val sorted = sortItems(state.items, sort)
                state.copy(items = sorted)
            }
            else -> state
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RegistroDetailUiState.Loading)

    init {
        loadDetail()
    }

    private fun loadDetail() {
        _uiState.value = RegistroDetailUiState.Loading
        viewModelScope.launch(exceptionHandler) {
            val registrosResult = registroRepository.getRegistros()

            if (registrosResult.isFailure) {
                val error = registrosResult.exceptionOrNull()
                if (error !is SessionExpiredException) {
                    _uiState.value = RegistroDetailUiState.Error(
                        error?.message ?: "Error al cargar registros"
                    )
                }
                return@launch
            }

            val registro = registrosResult.getOrNull()?.find { it.registroId == registroId }

            if (registro == null) {
                _uiState.value = RegistroDetailUiState.Error("Registro no encontrado")
                return@launch
            }

            val preciosResult = itemPrecioRepository.getItemPrecios(registroId)
            val userItemsResult = userItemRepository.getUserItems()

            if (preciosResult.isFailure) {
                val error = preciosResult.exceptionOrNull()
                if (error !is SessionExpiredException) {
                    _uiState.value = RegistroDetailUiState.Error(
                        error?.message ?: "Error al cargar precios"
                    )
                }
                return@launch
            }

            if (userItemsResult.isFailure && userItemsResult.exceptionOrNull() is SessionExpiredException) {
                return@launch
            }

            val precios = preciosResult.getOrDefault(emptyList())
            val userItems = userItemsResult.getOrDefault(emptyList())
            val userItemMap = userItems.associateBy { it.userItemId }

            val items = precios.map { precio ->
                val userItem = userItemMap[precio.userItemId]
                ItemDetalleUiModel(
                    userItemId = precio.userItemId,
                    itemName = userItem?.itemName ?: "Item #${precio.userItemId}",
                    cantidad = userItem?.cantidad ?: 1,
                    precioSteam = precio.precioSteam,
                    precioGamerPay = precio.precioGamerPay,
                    precioCsFloat = precio.precioCsFloat
                )
            }

            _uiState.value = RegistroDetailUiState.Success(registro, items)
        }
    }

    fun setSortOption(field: DetailSortField) {
        val current = _sortOption.value
        _sortOption.value = if (current.field == field) {
            current.copy(ascending = !current.ascending)
        } else {
            DetailSortOption(field = field, ascending = true)
        }
    }

    private fun sortItems(items: List<ItemDetalleUiModel>, sort: DetailSortOption): List<ItemDetalleUiModel> {
        return when (sort.field) {
            DetailSortField.NAME -> if (sort.ascending) items.sortedBy { it.itemName.lowercase() }
            else items.sortedByDescending { it.itemName.lowercase() }

            DetailSortField.CANTIDAD -> if (sort.ascending) items.sortedBy { it.cantidad }
            else items.sortedByDescending { it.cantidad }

            DetailSortField.STEAM -> if (sort.ascending) items.sortedBy { it.precioSteam }
            else items.sortedByDescending { it.precioSteam }

            DetailSortField.GAMERPAY -> if (sort.ascending) items.sortedBy { it.precioGamerPay }
            else items.sortedByDescending { it.precioGamerPay }

            DetailSortField.CSFLOAT -> if (sort.ascending) items.sortedBy { it.precioCsFloat }
            else items.sortedByDescending { it.precioCsFloat }
        }
    }
}
