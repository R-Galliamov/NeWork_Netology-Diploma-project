package ru.netology.nework.dao

import ru.netology.nework.entity.EventEntity
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM EventEntity ORDER BY id DESC")
    fun getAll(): Flow<List<EventEntity>>

    @Upsert
    suspend fun upsertEvent(event: EventEntity)

    @Upsert
    suspend fun upsertEvent(events: List<EventEntity>)

    @Query("SELECT COUNT(*) FROM EventEntity")
    fun getRowCount(): Int

    @Query("DELETE FROM EventEntity WHERE id = :id")
    suspend fun deleteEvent(id: Int)

    @Query("SELECT * FROM EventEntity WHERE authorId = :userId")
    fun getUserEvents(userId: Int): List<EventEntity>

    @Query("SELECT * FROM EventEntity WHERE id = :id")
    suspend fun getEvent(id: Int): EventEntity
}