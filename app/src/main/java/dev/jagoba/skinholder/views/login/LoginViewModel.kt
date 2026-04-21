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

/** One-shot data given to the fragment on first render to pre-fill the form. */
data class SavedCredentials(
    val username: String,
    val password: String,
    val rememberMe: Boolean
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val authSessionManager: AuthSessionManager,
    private val sessionExpiredNotifier: SessionExpiredNotifier
) : BaseViewModel<LoginUiState>(LoginUiState.Idle) {

    private val _savePassword = MutableStateFlow(false)
    val savePassword: StateFlow<Boolean> = _savePassword.asStateFlow()

    init {
        // If the user previously chose "Recordarme", restore the checkbox state
        // so the fragment reflects it. Actual text pre-fill is done via
        // [getSavedCredentials], which the fragment reads once on view create.
        if (!authSessionManager.getSavedPassword().isNullOrBlank()) {
            _savePassword.value = true
        }
    }

    /**
     * Returns the credentials the fragment should pre-fill on first render, or
     * `null` if there is nothing saved.
     */
    fun getSavedCredentials(): SavedCredentials? {
        val username = authSessionManager.getUsername()
        val password = authSessionManager.getSavedPassword()
        return when {
            !username.isNullOrBlank() && !password.isNullOrBlank() ->
                SavedCredentials(username, password, rememberMe = true)
            !username.isNullOrBlank() ->
                SavedCredentials(username, password = "", rememberMe = false)
            else -> null
        }
    }

    fun setSavePassword(value: Boolean) {
        _savePassword.value = value
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
