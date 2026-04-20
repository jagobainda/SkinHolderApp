package dev.jagoba.skinholder.views.registros

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jagoba.skinholder.core.BaseViewModel
import dev.jagoba.skinholder.dataservice.repository.ItemPrecioRepository
import dev.jagoba.skinholder.dataservice.repository.RegistroRepository
import dev.jagoba.skinholder.models.registros.Registro
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

enum class SortField { FECHA, STEAM, GAMERPAY, CSFLOAT }

data class SortOption(val field: SortField = SortField.FECHA, val ascending: Boolean = false)

sealed class RegistrosUiState {
    data object Loading : RegistrosUiState()
    data object Loaded : RegistrosUiState()
    data class Error(val message: String) : RegistrosUiState()
    data object Empty : RegistrosUiState()
}

sealed class RegistrosEvent {
    data class Deleted(val message: String) : RegistrosEvent()
    data class DeleteError(val message: String) : RegistrosEvent()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RegistrosViewModel @Inject constructor(
    private val registroRepository: RegistroRepository,
    private val itemPrecioRepository: ItemPrecioRepository
) : BaseViewModel<RegistrosUiState>(RegistrosUiState.Loading) {

    private val _allRegistros = MutableStateFlow<List<Registro>>(emptyList())

    private val _dateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val dateRange: StateFlow<Pair<Long, Long>?> = _dateRange

    private val _sortOption = MutableStateFlow(SortOption())
    val sortOption: StateFlow<SortOption> = _sortOption

    private val _events = MutableSharedFlow<RegistrosEvent>()
    val events = _events.asSharedFlow()

    private val dateParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    private val filteredSorted: Flow<List<Registro>> = combine(
        _allRegistros, _dateRange, _sortOption
    ) { registros, range, sort ->
        var result = registros

        if (range != null) {
            result = result.filter { registro ->
                val time = parseDate(registro.fechaHora)
                time != null && time >= range.first && time <= range.second
            }
        }

        result = when (sort.field) {
            SortField.FECHA -> if (sort.ascending) result.sortedBy { parseDate(it.fechaHora) ?: 0L }
            else result.sortedByDescending { parseDate(it.fechaHora) ?: 0L }

            SortField.STEAM -> if (sort.ascending) result.sortedBy { it.totalSteam }
            else result.sortedByDescending { it.totalSteam }

            SortField.GAMERPAY -> if (sort.ascending) result.sortedBy { it.totalGamerPay }
            else result.sortedByDescending { it.totalGamerPay }

            SortField.CSFLOAT -> if (sort.ascending) result.sortedBy { it.totalCsFloat }
            else result.sortedByDescending { it.totalCsFloat }
        }

        result
    }

    val pagingData: Flow<PagingData<Registro>> = filteredSorted.flatMapLatest { list ->
        Pager(PagingConfig(pageSize = 20, enablePlaceholders = false)) {
            ListPagingSource(list)
        }.flow
    }.cachedIn(viewModelScope)

    init {
        loadRegistros()
    }

    fun loadRegistros() {
        _uiState.value = RegistrosUiState.Loading
        viewModelScope.launch {
            registroRepository.getRegistros().fold(
                onSuccess = { registros ->
                    _allRegistros.value = registros
                    _uiState.value = if (registros.isEmpty()) RegistrosUiState.Empty
                    else RegistrosUiState.Loaded
                },
                onFailure = { error ->
                    _uiState.value = RegistrosUiState.Error(
                        error.message ?: "Error inesperado"
                    )
                }
            )
        }
    }

    fun refresh() {
        loadRegistros()
    }

    fun setDateRange(startMillis: Long, endMillis: Long) {
        _dateRange.value = Pair(startMillis, endMillis)
    }

    fun clearDateRange() {
        _dateRange.value = null
    }

    fun setSortOption(field: SortField) {
        val current = _sortOption.value
        _sortOption.value = if (current.field == field) {
            current.copy(ascending = !current.ascending)
        } else {
            SortOption(field = field, ascending = false)
        }
    }

    fun deleteRegistro(registroId: Long) {
        viewModelScope.launch {
            itemPrecioRepository.deleteItemPrecios(registroId).fold(
                onSuccess = {
                    registroRepository.deleteRegistro(registroId).fold(
                        onSuccess = {
                            _allRegistros.value = _allRegistros.value.filter { it.registroId != registroId }
                            if (_allRegistros.value.isEmpty()) {
                                _uiState.value = RegistrosUiState.Empty
                            }
                            _events.emit(RegistrosEvent.Deleted("Registro eliminado"))
                        },
                        onFailure = { e ->
                            _events.emit(RegistrosEvent.DeleteError(e.message ?: "Error al eliminar"))
                        }
                    )
                },
                onFailure = { e ->
                    _events.emit(RegistrosEvent.DeleteError(e.message ?: "Error al eliminar"))
                }
            )
        }
    }

    private fun parseDate(dateStr: String): Long? {
        return try {
            dateParser.parse(dateStr)?.time
        } catch (_: Exception) {
            null
        }
    }
}

class ListPagingSource<T : Any>(
    private val items: List<T>
) : PagingSource<Int, T>() {

    override fun getRefreshKey(state: PagingState<Int, T>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        val page = params.key ?: 0
        val pageSize = params.loadSize
        val fromIndex = page * pageSize
        val toIndex = minOf(fromIndex + pageSize, items.size)

        return if (fromIndex >= items.size) {
            LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)
        } else {
            LoadResult.Page(
                data = items.subList(fromIndex, toIndex),
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (toIndex >= items.size) null else page + 1
            )
        }
    }
}
