package dev.jagoba.skinholder.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseViewModel<T>(initialState: T) : ViewModel() {
    protected val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<T> = _uiState

    protected val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        // Silently ignore SessionExpiredException - it's handled by GlobalViewModel
        if (exception !is SessionExpiredException) {
            // Log other exceptions if needed in future
            exception.printStackTrace()
        }
    }
}
