package dev.jagoba.skinholder.views.home

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jagoba.skinholder.core.BaseViewModel
import dev.jagoba.skinholder.dataservice.repository.DashboardRepository
import dev.jagoba.skinholder.models.dashboard.DashboardStats
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DashboardUiState {
    data object Loading : DashboardUiState()
    data class Success(val stats: DashboardStats) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository
) : BaseViewModel<DashboardUiState>(DashboardUiState.Loading) {

    private var refreshJob: Job? = null

    init {
        startAutoRefresh()
    }

    /**
     * Loads the dashboard once. Errors keep the previous successful state visible
     * (so periodic failures don't blank the UI), only surfacing an Error state when
     * we have nothing else to show.
     */
    fun loadDashboard() {
        viewModelScope.launch(exceptionHandler) {
            if (_uiState.value !is DashboardUiState.Success) {
                _uiState.value = DashboardUiState.Loading
            }
            try {
                val stats = dashboardRepository.getDashboardStats()
                _uiState.value = DashboardUiState.Success(stats)
            } catch (e: Exception) {
                if (_uiState.value !is DashboardUiState.Success) {
                    _uiState.value = DashboardUiState.Error(
                        e.message ?: "Error cargando el dashboard"
                    )
                }
            }
        }
    }

    /**
     * Starts a coroutine that refreshes the dashboard every [REFRESH_INTERVAL_MS]
     * milliseconds, equivalent to React Query's `refetchInterval: 30_000` in the web.
     * The job is cancelled in [onCleared].
     */
    private fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch(exceptionHandler) {
            while (isActive) {
                try {
                    val stats = dashboardRepository.getDashboardStats()
                    _uiState.value = DashboardUiState.Success(stats)
                } catch (e: Exception) {
                    if (_uiState.value !is DashboardUiState.Success) {
                        _uiState.value = DashboardUiState.Error(
                            e.message ?: "Error cargando el dashboard"
                        )
                    }
                }
                delay(REFRESH_INTERVAL_MS)
            }
        }
    }

    override fun onCleared() {
        refreshJob?.cancel()
        refreshJob = null
        super.onCleared()
    }

    companion object {
        private const val REFRESH_INTERVAL_MS = 30_000L
    }
}
