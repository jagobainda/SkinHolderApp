package dev.jagoba.skinholder.logic

import dev.jagoba.skinholder.dataservice.repository.UserRepository
import dev.jagoba.skinholder.models.auth.LoginResponse
import javax.inject.Inject

class UserLogic @Inject constructor(private val repository: UserRepository) {
    suspend fun getCurrentUser(): LoginResponse? {
        return repository.getLoginPlaceholder()
    }
}
