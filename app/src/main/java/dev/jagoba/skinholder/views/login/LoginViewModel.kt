package dev.jagoba.skinholder.views.login

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jagoba.skinholder.core.AuthSessionManager
import dev.jagoba.skinholder.core.BaseViewModel
import dev.jagoba.skinholder.dataservice.repository.AuthRepository
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
    private val authSessionManager: AuthSessionManager
) : BaseViewModel<LoginUiState>(LoginUiState.Idle) {

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Debes introducir usuario y contraseña.")
            return
        }

        _uiState.value = LoginUiState.Loading

        viewModelScope.launch {
            val result = authRepository.login(username.trim(), password.trim())
            result.fold(
                onSuccess = { response ->
                    authSessionManager.saveSession(
                        response.token,
                        response.userName,
                        response.userId
                    )
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
