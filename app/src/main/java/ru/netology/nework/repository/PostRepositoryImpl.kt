package ru.netology.nework.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import ru.netology.nework.dao.PostDao
import ru.netology.nework.dto.Post
import ru.netology.nework.entity.PostEntity
import ru.netology.nework.entity.toDto
import ru.netology.nework.entity.toEntity
import ru.netology.nework.error.ApiError
import ru.netology.nework.error.NetworkError
import ru.netology.nework.error.UnknownError
import ru.netology.nework.model.requestModel.PostRequest
import ru.netology.nework.service.api.ApiService
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val postDao: PostDao,
) : PostRepository {

    override val data: Flow<List<Post>> =
        postDao.getAll().map(List<PostEntity>::toDto).flowOn(Dispatchers.Default)

    override suspend fun getAll() {
        try {
            val response = apiService.getAllPosts()
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            postDao.upsertPost(body.toEntity())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: ApiError) {
            throw e
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun savePost(postRequest: PostRequest): Post {
        try {
            val response = apiService.savePost(postRequest)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val post = response.body() ?: throw ApiError(response.code(), response.message())
            postDao.upsertPost(PostEntity.fromDto(post))
            return post
        } catch (e: IOException) {
            Log.d("Error", e.message.toString())
            throw NetworkError
        } catch (e: ApiError) {
            Log.d("Error", e.message.toString())
            throw e
        } catch (e: Exception) {
            Log.d("Error", e.message.toString())
            throw UnknownError
        }
    }

    override suspend fun onLike(post: Post): Post {
        try {
            val response =
                if (!post.likedByMe) apiService.likePostById(post.id) else apiService.dislikePostById(
                    post.id
                )
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val post = response.body() ?: throw ApiError(response.code(), response.message())
            postDao.upsertPost(PostEntity.fromDto(post))
            return post
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: ApiError) {
            throw e
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun isDbEmpty() = postDao.getRowCount() == 0
}