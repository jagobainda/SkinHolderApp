package dev.jagoba.skinholder.views.registros

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jagoba.skinholder.core.AuthSessionManager
import dev.jagoba.skinholder.core.BaseViewModel
import dev.jagoba.skinholder.core.SessionExpiredException
import dev.jagoba.skinholder.dataservice.repository.ExternalRepository
import dev.jagoba.skinholder.dataservice.repository.ItemPrecioRepository
import dev.jagoba.skinholder.dataservice.repository.RegistroRepository
import dev.jagoba.skinholder.dataservice.repository.UserItemRepository
import dev.jagoba.skinholder.models.items.ItemPrecio
import dev.jagoba.skinholder.models.registros.Registro
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
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

sealed class ConsultaState {
    data object Idle : ConsultaState()
    data class Loading(val progreso: Int, val total: Int) : ConsultaState()
    data object Success : ConsultaState()
    data class Error(val message: String) : ConsultaState()
}

data class ConsultaProgreso(
    val totalItems: Int = 0,
    val progresoSteam: Int = 0,
    val progresoGamerPay: Int = 0,
    val progresoCSFloat: Int = 0,
    val totalSteam: Double = 0.0,
    val totalGamerPay: Double = 0.0,
    val totalCSFloat: Double = 0.0,
    val itemsNoListadosGamerPay: Int = 0,
    val itemsWarningSteam: Int = 0,
    val itemsErrorSteam: Int = 0
)

sealed class RegistrosEvent {
    data class Deleted(val message: String) : RegistrosEvent()
    data class DeleteError(val message: String) : RegistrosEvent()
    data class ConsultaSuccess(val registroId: Long) : RegistrosEvent()
    data class ConsultaError(val message: String) : RegistrosEvent()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RegistrosViewModel @Inject constructor(
    private val registroRepository: RegistroRepository,
    private val itemPrecioRepository: ItemPrecioRepository,
    private val userItemRepository: UserItemRepository,
    private val externalRepository: ExternalRepository,
    private val authSessionManager: AuthSessionManager
) : BaseViewModel<RegistrosUiState>(RegistrosUiState.Loading) {

    private val _allRegistros = MutableStateFlow<List<Registro>>(emptyList())

    private val _dateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val dateRange: StateFlow<Pair<Long, Long>?> = _dateRange

    private val _sortOption = MutableStateFlow(SortOption())
    val sortOption: StateFlow<SortOption> = _sortOption

    private val _events = MutableSharedFlow<RegistrosEvent>()
    val events = _events.asSharedFlow()

    private val _consultaState = MutableStateFlow<ConsultaState>(ConsultaState.Idle)
    val consultaState: StateFlow<ConsultaState> = _consultaState.asStateFlow()

    private val _consultaProgreso = MutableStateFlow(ConsultaProgreso())
    val consultaProgreso: StateFlow<ConsultaProgreso> = _consultaProgreso.asStateFlow()

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
        viewModelScope.launch(exceptionHandler) {
            registroRepository.getRegistros().fold(
                onSuccess = { registros ->
                    _allRegistros.value = registros
                    _uiState.value = if (registros.isEmpty()) RegistrosUiState.Empty
                    else RegistrosUiState.Loaded
                },
                onFailure = { error ->
                    if (error !is SessionExpiredException) {
                        _uiState.value = RegistrosUiState.Error(
                            error.message ?: "Error inesperado"
                        )
                    }
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
        viewModelScope.launch(exceptionHandler) {
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
                            if (e !is SessionExpiredException) {
                                _events.emit(RegistrosEvent.DeleteError(e.message ?: "Error al eliminar"))
                            }
                        }
                    )
                },
                onFailure = { e ->
                    if (e !is SessionExpiredException) {
                        _events.emit(RegistrosEvent.DeleteError(e.message ?: "Error al eliminar"))
                    }
                }
            )
        }
    }

    fun consultarPrecios() {
        if (_consultaState.value is ConsultaState.Loading) return

        _consultaProgreso.value = ConsultaProgreso()
        _consultaState.value = ConsultaState.Loading(progreso = 0, total = 0)

        viewModelScope.launch(exceptionHandler) {
            try {
                val userItems = userItemRepository.getUserItems().getOrElse { error ->
                    if (error is SessionExpiredException) return@launch
                    finishConsultaWithError(error.message ?: "Error obteniendo los items del usuario")
                    return@launch
                }

                val total = userItems.size
                _consultaProgreso.update { it.copy(totalItems = total) }
                _consultaState.value = ConsultaState.Loading(progreso = 0, total = total)

                if (userItems.isEmpty()) {
                    finishConsultaWithError("No tienes items para consultar")
                    return@launch
                }

                val gamerPayPrices = externalRepository.getGamerPayPrices().getOrElse { error ->
                    if (error is SessionExpiredException) return@launch
                    emptyList()
                }

                val csFloatNames = userItems
                    .map { it.csFloatMarketHashName.ifBlank { it.itemName } }
                    .filter { it.isNotBlank() }
                    .distinct()
                val csFloatPrices = if (csFloatNames.isNotEmpty()) {
                    externalRepository.getCSFloatPrices(csFloatNames).getOrElse { error ->
                        if (error is SessionExpiredException) return@launch
                        emptyMap()
                    }
                } else {
                    emptyMap()
                }

                val itemPrecios = mutableListOf<ItemPrecio>()
                var totalSteam = 0.0
                var totalGamerPay = 0.0
                var totalCSFloat = 0.0
                var warningSteam = 0
                var errorSteam = 0
                var noListadosGamerPay = 0

                userItems.forEachIndexed { index, userItem ->
                    val steamHashName = userItem.steamHashName.ifBlank { userItem.itemName }
                    val gamerPayLookup = userItem.gamerPayName.ifBlank { userItem.itemName }
                    val csFloatLookup = userItem.csFloatMarketHashName.ifBlank { userItem.itemName }

                    val steamInfo = externalRepository.getSteamPriceWithPolling(steamHashName)
                    val gamerPayItem = gamerPayPrices.firstOrNull {
                        it.name.trim().equals(gamerPayLookup.trim(), ignoreCase = true)
                    }
                    val csFloatPrice = csFloatPrices[csFloatLookup] ?: 0.0

                    if (steamInfo.isWarning) warningSteam++
                    if (steamInfo.isError) errorSteam++
                    if (gamerPayItem == null) noListadosGamerPay++

                    if (steamInfo.price > 0) totalSteam += steamInfo.price * userItem.cantidad
                    if (gamerPayItem != null) totalGamerPay += gamerPayItem.price * userItem.cantidad
                    if (csFloatPrice > 0) totalCSFloat += csFloatPrice * userItem.cantidad

                    itemPrecios.add(
                        ItemPrecio(
                            precioSteam = maxOf(steamInfo.price, 0.0),
                            precioGamerPay = gamerPayItem?.price ?: 0.0,
                            precioCsFloat = maxOf(csFloatPrice, 0.0),
                            userItemId = userItem.userItemId
                        )
                    )

                    val progreso = index + 1
                    _consultaProgreso.update {
                        it.copy(
                            progresoSteam = progreso,
                            progresoGamerPay = progreso,
                            progresoCSFloat = progreso,
                            totalSteam = totalSteam,
                            totalGamerPay = totalGamerPay,
                            totalCSFloat = totalCSFloat,
                            itemsWarningSteam = warningSteam,
                            itemsErrorSteam = errorSteam,
                            itemsNoListadosGamerPay = noListadosGamerPay
                        )
                    }
                    _consultaState.value = ConsultaState.Loading(progreso = progreso, total = total)
                }

                val registro = Registro(
                    fechaHora = dateParser.format(Date()),
                    totalSteam = totalSteam,
                    totalGamerPay = totalGamerPay,
                    totalCsFloat = totalCSFloat,
                    userId = authSessionManager.getUserId()
                )

                val registroId = registroRepository.createRegistro(registro).getOrElse { error ->
                    if (error is SessionExpiredException) return@launch
                    finishConsultaWithError(error.message ?: "Error creando el registro")
                    return@launch
                }

                if (registroId < 1) {
                    finishConsultaWithError("Error creando el registro")
                    return@launch
                }

                val preciosConRegistro = itemPrecios.map { it.copy(registroId = registroId) }

                itemPrecioRepository.createItemPrecios(preciosConRegistro).getOrElse { error ->
                    if (error is SessionExpiredException) return@launch
                    finishConsultaWithError(error.message ?: "Error guardando los precios de los items")
                    return@launch
                }

                _consultaState.value = ConsultaState.Success
                _events.emit(RegistrosEvent.ConsultaSuccess(registroId))
                loadRegistros()
            } catch (e: Exception) {
                if (e !is SessionExpiredException) {
                    finishConsultaWithError(e.message ?: "Error inesperado al consultar precios")
                }
            }
        }
    }

    private suspend fun finishConsultaWithError(message: String) {
        _consultaState.value = ConsultaState.Error(message)
        _events.emit(RegistrosEvent.ConsultaError(message))
    }

    fun resetConsultaState() {
        _consultaState.value = ConsultaState.Idle
        _consultaProgreso.value = ConsultaProgreso()
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
