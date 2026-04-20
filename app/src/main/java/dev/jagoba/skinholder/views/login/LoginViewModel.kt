package dev.jagoba.skinholder.views.login

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jagoba.skinholder.core.AuthSessionManager
import dev.jagoba.skinholder.core.BaseViewModel
import dev.jagoba.skinholder.core.SessionExpiredNotifier
import dev.jagoba.skinholder.dataservice.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LoginUiState {
    data object Idle : LoginUiState()
    data object Loading : LoginUiState()
    data class Success(val username: String) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val authSessionManager: AuthSessionManager,
    private val sessionExpiredNotifier: SessionExpiredNotifier
) : BaseViewModel<LoginUiState>(LoginUiState.Idle) {

    private val _savePassword = MutableStateFlow(false)
    val savePassword: StateFlow<Boolean> = _savePassword.asStateFlow()

    init {
        loadSavedCredentials()
    }

    private fun loadSavedCredentials() {
        val username = authSessionManager.getUsername()
        val password = authSessionManager.getSavedPassword()

        if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
            _uiState.value = LoginUiState.Idle
            // Set values but don't auto-login - let user click login
            // In a real scenario with CredentialManager, this would auto-login
        }
    }

    fun toggleSavePassword() {
        _savePassword.value = !_savePassword.value
    }

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Debes introducir usuario y contraseña.")
            return
        }

        _uiState.value = LoginUiState.Loading

        viewModelScope.launch(exceptionHandler) {
            val result = authRepository.login(username.trim(), password.trim())
            result.fold(
                onSuccess = { response ->
                    if (_savePassword.value) {
                        authSessionManager.saveSessionWithPassword(
                            response.token,
                            response.userName,
                            response.userId,
                            password.trim()
                        )
                    } else {
                        authSessionManager.saveSession(
                            response.token,
                            response.userName,
                            response.userId
                        )
                        authSessionManager.clearSavedPassword()
                    }
                    sessionExpiredNotifier.reset()
                    authSessionManager.getOrInitAppStartTime()
                    _uiState.value = LoginUiState.Success(response.userName)
                },
                onFailure = { error ->
                    _uiState.value = LoginUiState.Error(
                        error.message ?: "Error inesperado durante el login"
                    )
                }
            )
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}
