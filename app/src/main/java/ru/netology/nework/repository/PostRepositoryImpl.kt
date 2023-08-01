package ru.netology.nework.repository

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import ru.netology.nework.model.ApiError
import ru.netology.nework.model.Post
import ru.netology.nework.service.api.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepositoryImpl @Inject constructor(private val apiService: ApiService) : PostRepository {
    override val data: Flow<List<Post>> = emptyFlow()
    val liveData: MutableLiveData<List<Post>> = MutableLiveData(emptyList())

    override suspend fun getAll() {
        try {
            val response = apiService.getAllPosts()
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            liveData.postValue(body)
        } catch (e: Exception) {

        }
    }
}