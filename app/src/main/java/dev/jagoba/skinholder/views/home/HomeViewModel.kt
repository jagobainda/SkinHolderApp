package dev.jagoba.skinholder.views.home

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jagoba.skinholder.core.BaseViewModel
import dev.jagoba.skinholder.logic.UserLogic
import dev.jagoba.skinholder.models.auth.LoginResponse
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val user: LoginResponse? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(private val userLogic: UserLogic) : BaseViewModel<HomeUiState>(HomeUiState()) {

    fun loadUser() {
        viewModelScope.launch {
            val user = userLogic.getCurrentUser()
            _uiState.value = _uiState.value.copy(user = user)
        }
    }
    
}
