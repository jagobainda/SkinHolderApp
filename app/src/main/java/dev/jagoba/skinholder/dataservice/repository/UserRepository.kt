package dev.jagoba.skinholder.dataservice.repository

import dev.jagoba.skinholder.models.auth.LoginResponse
import javax.inject.Inject

interface UserRepository {
    suspend fun getLoginPlaceholder(): LoginResponse?
}

class UserRepositoryImpl @Inject constructor() : UserRepository {
    override suspend fun getLoginPlaceholder(): LoginResponse? {
        return LoginResponse("asdhnfjksladhbfgjklahsdjkflhasjkdljklasdhfjkldashjkglbhdfjaklgdfbhjklagdsl", "jagoba", 1)
    }
}