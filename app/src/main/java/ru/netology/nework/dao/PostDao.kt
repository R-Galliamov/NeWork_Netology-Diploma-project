package ru.netology.nework.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.netology.nework.entity.PostEntity


@Dao
interface PostDao {
    @Query("SELECT * FROM PostEntity ORDER BY id DESC")
    fun getAll(): Flow<List<PostEntity>>

    @Upsert
    suspend fun upsertPost(post: PostEntity)

    @Upsert
    suspend fun upsertPost(posts: List<PostEntity>)

    @Query("DELETE FROM PostEntity WHERE id = :id")
    suspend fun deletePost(id: Int)

    @Query("SELECT COUNT(*) FROM PostEntity")
    fun getRowCount(): Int

    @Query("SELECT * FROM PostEntity WHERE authorId = :userId ORDER BY id DESC")
    fun getUserPosts(userId: Int): List<PostEntity>

    @Query("SELECT * FROM PostEntity WHERE id = :id")
    suspend fun getPost(id: Int): PostEntity
}