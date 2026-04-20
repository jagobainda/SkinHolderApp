package dev.jagoba.skinholder.views.settings

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jagoba.skinholder.core.AuthSessionManager
import dev.jagoba.skinholder.core.BaseViewModel
import dev.jagoba.skinholder.core.SessionExpiredException
import dev.jagoba.skinholder.dataservice.repository.UserSettingsRepository
import dev.jagoba.skinholder.logic.LoggerService
import dev.jagoba.skinholder.models.enums.ELogType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

sealed class UserSettingsUiState {
    data object Loading : UserSettingsUiState()
    data class Loaded(val username: String, val createdAt: String) : UserSettingsUiState()
    data class Error(val message: String) : UserSettingsUiState()
}

sealed class UserSettingsEvent {
    data class PasswordChanged(val message: String) : UserSettingsEvent()
    data class PasswordError(val message: String) : UserSettingsEvent()
    data object AccountDeleted : UserSettingsEvent()
    data class DeleteError(val message: String) : UserSettingsEvent()
    data object LoggedOut : UserSettingsEvent()
}

@HiltViewModel
class UserSettingsViewModel @Inject constructor(
    private val userSettingsRepository: UserSettingsRepository,
    private val authSessionManager: AuthSessionManager,
    private val loggerService: LoggerService
) : BaseViewModel<UserSettingsUiState>(UserSettingsUiState.Loading) {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _events = MutableSharedFlow<UserSettingsEvent>()
    val events = _events.asSharedFlow()

    private val dateParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    init {
        loadUserInfo()
    }

    fun loadUserInfo() {
        viewModelScope.launch(exceptionHandler) {
            _uiState.value = UserSettingsUiState.Loading
            userSettingsRepository.getUserInfo().fold(
                onSuccess = { info ->
                    val formattedDate = try {
                        val date = dateParser.parse(info.createdAt)
                        if (date != null) dateFormatter.format(date) else "No disponible"
                    } catch (_: Exception) {
                        "No disponible"
                    }
                    _uiState.value = UserSettingsUiState.Loaded(
                        username = info.username.ifBlank {
                            authSessionManager.getUsername() ?: "No disponible"
                        },
                        createdAt = formattedDate
                    )
                },
                onFailure = {
                    _uiState.value = UserSettingsUiState.Loaded(
                        username = authSessionManager.getUsername() ?: "No disponible",
                        createdAt = "No disponible"
                    )
                }
            )
        }
    }

    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        viewModelScope.launch(exceptionHandler) {
            if (currentPassword.isBlank()) {
                _events.emit(UserSettingsEvent.PasswordError("Introduce tu contraseña actual."))
                return@launch
            }
            if (newPassword.isBlank()) {
                _events.emit(UserSettingsEvent.PasswordError("Introduce la nueva contraseña."))
                return@launch
            }
            if (newPassword.length < 6) {
                _events.emit(UserSettingsEvent.PasswordError("La nueva contraseña debe tener al menos 6 caracteres."))
                return@launch
            }
            if (newPassword != confirmPassword) {
                _events.emit(UserSettingsEvent.PasswordError("Las contraseñas no coinciden."))
                return@launch
            }
            if (newPassword == currentPassword) {
                _events.emit(UserSettingsEvent.PasswordError("La nueva contraseña no puede ser igual a la actual."))
                return@launch
            }

            _isLoading.value = true
            userSettingsRepository.changePassword(currentPassword, newPassword).fold(
                onSuccess = { msg ->
                    _events.emit(UserSettingsEvent.PasswordChanged(msg))
                },
                onFailure = { e ->
                    if (e !is SessionExpiredException) {
                        _events.emit(UserSettingsEvent.PasswordError(e.message ?: "Error al cambiar la contraseña."))
                    }
                }
            )
            _isLoading.value = false
        }
    }

    fun deleteAccount(password: String) {
        viewModelScope.launch(exceptionHandler) {
            if (password.isBlank()) {
                _events.emit(UserSettingsEvent.DeleteError("Introduce tu contraseña para confirmar."))
                return@launch
            }

            _isLoading.value = true
            userSettingsRepository.deleteAccount(password).fold(
                onSuccess = {
                    loggerService.sendLog("Cuenta eliminada por el usuario", ELogType.Info)
                    authSessionManager.clearSession()
                    _events.emit(UserSettingsEvent.AccountDeleted)
                },
                onFailure = { e ->
                    if (e !is SessionExpiredException) {
                        _events.emit(UserSettingsEvent.DeleteError(e.message ?: "Error al eliminar la cuenta."))
                    }
                }
            )
            _isLoading.value = false
        }
    }

    fun logout() {
        viewModelScope.launch(exceptionHandler) {
            authSessionManager.clearSession()
            _events.emit(UserSettingsEvent.LoggedOut)
        }
    }
}
