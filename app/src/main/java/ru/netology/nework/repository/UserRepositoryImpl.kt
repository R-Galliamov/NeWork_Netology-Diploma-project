package ru.netology.nework.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import ru.netology.nework.dao.UserDao
import ru.netology.nework.dto.AuthState
import ru.netology.nework.dto.User
import ru.netology.nework.entity.UserEntity
import ru.netology.nework.entity.toDto
import ru.netology.nework.entity.toEntity
import ru.netology.nework.error.ApiError
import ru.netology.nework.model.requestModel.AuthenticationRequest
import ru.netology.nework.model.requestModel.RegistrationRequest
import ru.netology.nework.service.api.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val userDao: UserDao
) :
    UserRepository {
    override val data: Flow<List<User>> =
        userDao.getAll().map(List<UserEntity>::toDto).flowOn(Dispatchers.Default)

    override suspend fun getAll() {
        val response = apiService.getAllUsers()
        if (!response.isSuccessful) {
            throw ApiError(response.code(), response.message())
        }
        val body = response.body() ?: throw ApiError(response.code(), response.message())
        userDao.upsertUsers(body.toEntity())
    }

    override suspend fun signInUser(authRequest: AuthenticationRequest): AuthState {
        val response = apiService.updateAuth(authRequest.login, authRequest.password)
        if (!response.isSuccessful) {
            throw ApiError(response.code(), response.message())
        }
        return response.body() ?: throw ApiError(response.code(), response.message())
    }

    override suspend fun signUpUser(
        regRequest: RegistrationRequest
    ): AuthState {
        val loginRequest = regRequest.login.toRequestBody("text/plain".toMediaType())
        val passwordRequest = regRequest.password.toRequestBody("text/plain".toMediaType())
        val nameRequest = regRequest.name.toRequestBody("text/plain".toMediaType())
        val response = if (regRequest.avatarFile == null) {
            apiService.registerUser(loginRequest, passwordRequest, nameRequest)
        } else {
            val avatarFileRequest =
                MultipartBody.Part.createFormData(
                    "file",
                    regRequest.avatarFile.name,
                    regRequest.avatarFile.asRequestBody()
                )
            apiService.registerUser(
                loginRequest,
                passwordRequest,
                nameRequest,
                avatarFileRequest
            )
        }
        if (!response.isSuccessful) {
            throw ApiError(response.code(), response.message())
        }
        return response.body() ?: throw ApiError(response.code(), response.message())
    }

    override suspend fun getUserById(userId: Int): User {
        val response = apiService.getUserById(userId)
        if (!response.isSuccessful) {
            throw ApiError(response.code(), response.message())
        }
        return response.body() ?: throw ApiError(response.code(), response.message())
    }

    override suspend fun isDbEmpty() = userDao.getRowCount() == 0
}