package ru.netology.nework.repository

import kotlinx.coroutines.flow.Flow
import ru.netology.nework.dto.AuthState
import ru.netology.nework.dto.User
import ru.netology.nework.model.requestModel.AuthenticationRequest
import ru.netology.nework.model.requestModel.RegistrationRequest

interface UserRepository {
    val data: Flow<List<User>>

    suspend fun getAll()
    suspend fun signInUser(authRequest: AuthenticationRequest): AuthState
    suspend fun signUpUser(
        regRequest: RegistrationRequest
    ): AuthState
    suspend fun getUserById(userId: Int): User

    suspend fun isDbEmpty() : Boolean
}