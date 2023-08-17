package ru.netology.nework.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.netology.nework.entity.UserEntity

@Dao
interface UserDao {

    @Query("SELECT * FROM UserEntity ORDER BY id DESC")
    fun getAll(): Flow<List<UserEntity>>

    @Upsert
    suspend fun upsertUser(user: UserEntity)

    @Upsert
    suspend fun upsertUsers(users: List<UserEntity>)

    @Query("DELETE FROM UserEntity WHERE id = :id")
    suspend fun deleteUserById(id: Int)

    @Query("SELECT COUNT(*) FROM UserEntity")
    fun getRowCount(): Int
}