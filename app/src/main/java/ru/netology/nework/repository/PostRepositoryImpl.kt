package ru.netology.nework.repository

import android.content.ContentResolver
import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import ru.netology.nework.dao.PostDao
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.Media
import ru.netology.nework.dto.Post
import ru.netology.nework.entity.PostEntity
import ru.netology.nework.entity.toDto
import ru.netology.nework.entity.toEntity
import ru.netology.nework.error.ApiError
import ru.netology.nework.error.NetworkError
import ru.netology.nework.error.UnknownError
import ru.netology.nework.model.requestModel.PostRequest
import ru.netology.nework.service.api.ApiService
import ru.netology.nework.util.AndroidUtils
import java.io.IOException
import java.text.DecimalFormat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val postDao: PostDao,
    private val contentResolver: ContentResolver,
) : PostRepository {
    override val data: Flow<List<Post>> =
        postDao.getAll().map(List<PostEntity>::toDto).flowOn(Dispatchers.Default)

    override suspend fun getAll() {
        try {
            val response = apiService.getAllPosts()
            if (!response.isSuccessful) throw ApiError(response.code(), response.message())
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
            var data = postRequest
            if (postRequest.attachment != null) {
                val media = upload(postRequest.attachment)
                data = data.copy(attachment = data.attachment?.copy(url = media.url))
            }
            val response = apiService.savePost(data)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            postDao.upsertPost(PostEntity.fromDto(body))
            return body
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: ApiError) {
            throw e
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun onLike(post: Post): Post {
        try {
            val response =
                if (!post.likedByMe) apiService.likePostById(post.id) else apiService.dislikePostById(
                    post.id
                )
            if (!response.isSuccessful) throw ApiError(response.code(), response.message())
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

    override suspend fun deletePost(postId: Int) {
        try {
            apiService.deletePost(postId)
            postDao.deletePost(postId)
        } catch (e: java.lang.Exception) {
            throw e
        }
    }

    override suspend fun upload(attachment: Attachment): Media {
        try {
            val media = contentResolver.openInputStream(attachment.url.toUri())?.use {
                MultipartBody.Part.createFormData(
                    "file", "file", it.readBytes().toRequestBody()
                )
            }
            requireNotNull(media) {
                "Resource ${attachment.url} not found"
            }
            val response = apiService.upload(media)
            if (!response.isSuccessful) throw ApiError(response.code(), response.message())
            return response.body() ?: throw ApiError(response.code(), response.message())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    override suspend fun getPost(id: Int): Post {
        return postDao.getPost(id).toDto()
    }

    override suspend fun getUserPosts(userId: Int): List<Post> {
        try {
            return postDao.getUserPosts(userId).toDto()
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: Exception) {
            throw UnknownError
        }
    }
}