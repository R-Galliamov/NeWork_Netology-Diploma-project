package ru.netology.nework.repository

import kotlinx.coroutines.flow.Flow
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Media
import ru.netology.nework.dto.Post
import ru.netology.nework.model.requestModel.PostRequest

interface PostRepository {
    val data: Flow<List<Post>>
    suspend fun getAll()
    suspend fun onLike(post: Post): Post
    suspend fun savePost(postRequest: PostRequest): Post

    suspend fun isDbEmpty(): Boolean

    suspend fun upload(attachment: Attachment): Media
}