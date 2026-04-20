package dev.jagoba.skinholder.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class GlobalEvent {
    data class ShowError(val message: String) : GlobalEvent()
    data object SessionExpired : GlobalEvent()
}

@HiltViewModel
class GlobalViewModel @Inject constructor(
    private val authSessionManager: AuthSessionManager,
    private val sessionExpiredNotifier: SessionExpiredNotifier
) : ViewModel() {

    private val _currentUsername = MutableStateFlow(authSessionManager.getUsername())
    val currentUsername: StateFlow<String?> = _currentUsername.asStateFlow()

    private val _userId = MutableStateFlow(authSessionManager.getUserId())
    val userId: StateFlow<Int> = _userId.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(authSessionManager.isLoggedIn())
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _globalEvents = MutableSharedFlow<GlobalEvent>(extraBufferCapacity = 1)
    val globalEvents: SharedFlow<GlobalEvent> = _globalEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            sessionExpiredNotifier.sessionExpired.collect {
                _isAuthenticated.value = false
                _currentUsername.value = null
                _userId.value = 0
                _globalEvents.tryEmit(GlobalEvent.SessionExpired)
            }
        }
    }

    fun refreshSessionState() {
        _currentUsername.value = authSessionManager.getUsername()
        _userId.value = authSessionManager.getUserId()
        _isAuthenticated.value = authSessionManager.isLoggedIn()
    }

    fun emitError(message: String) {
        _globalEvents.tryEmit(GlobalEvent.ShowError(message))
    }
}
