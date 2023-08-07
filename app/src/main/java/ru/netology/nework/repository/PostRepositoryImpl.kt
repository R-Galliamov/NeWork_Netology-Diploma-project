package ru.netology.nework.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import ru.netology.nework.dao.PostDao
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.User
import ru.netology.nework.entity.PostEntity
import ru.netology.nework.entity.toDto
import ru.netology.nework.entity.toEntity
import ru.netology.nework.error.ApiError
import ru.netology.nework.error.NetworkError
import ru.netology.nework.error.UnknownError
import ru.netology.nework.service.api.ApiService
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val postDao: PostDao
) : PostRepository {
    override val data: Flow<List<Post>> =
        postDao.getAll().map(List<PostEntity>::toDto).flowOn(Dispatchers.Default)

    override suspend fun getAll() {
        val response = apiService.getAllPosts()
        if (!response.isSuccessful) {
            throw ApiError(response.code(), response.message())
        }
        val body = response.body() ?: throw ApiError(response.code(), response.message())
        postDao.upsertPost(body.toEntity())
    }

    override suspend fun onLike(post: Post) {
        val likedByMe = !post.likedByMe
        try {
            val response =
                if (likedByMe) apiService.likeById(post.id) else apiService.dislikeById(post.id)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            postDao.upsertPost(PostEntity.fromDto(post.copy(likedByMe = !post.likedByMe)))
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: ApiError) {
            throw e
        } catch (e: Exception) {
            throw UnknownError
        }
    }
}