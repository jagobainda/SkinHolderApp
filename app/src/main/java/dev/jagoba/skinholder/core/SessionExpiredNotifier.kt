package dev.jagoba.skinholder.core

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionExpiredNotifier @Inject constructor() {

    private val _sessionExpired = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    private val hasNotified = AtomicBoolean(false)

    fun notifySessionExpired() {
        if (hasNotified.compareAndSet(false, true)) {
            _sessionExpired.tryEmit(Unit)
        }
    }

    fun reset() {
        hasNotified.set(false)
        _sessionExpired.resetReplayCache()
    }
}
