package dev.jagoba.skinholder.logic

import android.util.Log
import dev.jagoba.skinholder.core.AuthSessionManager
import dev.jagoba.skinholder.dataservice.repository.LogRepository
import dev.jagoba.skinholder.models.Logger
import dev.jagoba.skinholder.models.enums.ELogPlace
import dev.jagoba.skinholder.models.enums.ELogType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoggerService @Inject constructor(
    private val logRepository: LogRepository,
    private val authSessionManager: AuthSessionManager
) {

    companion object {
        private const val TAG = "SkinHolder"
    }

    suspend fun sendLog(message: String, logType: ELogType) {
        when (logType) {
            ELogType.Info -> Log.i(TAG, message)
            ELogType.Warning -> Log.w(TAG, message)
            ELogType.Error -> Log.e(TAG, message)
        }

        try {
            val logger = Logger(
                logDescription = message,
                logTypeId = logType.value,
                logPlaceId = ELogPlace.Mobile.value,
                userId = authSessionManager.getUserId()
            )
            logRepository.addLog(logger)
        } catch (_: Exception) {
            Log.e(TAG, "Error en el reporte de errores")
        }
    }
}
