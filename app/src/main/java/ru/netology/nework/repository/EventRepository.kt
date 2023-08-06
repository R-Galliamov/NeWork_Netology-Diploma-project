package ru.netology.nework.repository

import kotlinx.coroutines.flow.Flow
import ru.netology.nework.dto.Event

interface EventRepository {
    val data: Flow<List<Event>>

    suspend fun getAll()
}