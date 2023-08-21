package ru.netology.nework.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.netology.nework.converters.DbListConverters
import ru.netology.nework.converters.DbMapConverters
import ru.netology.nework.dao.EventDao
import ru.netology.nework.dao.PostDao
import ru.netology.nework.dao.UserDao
import ru.netology.nework.entity.EventEntity
import ru.netology.nework.entity.PostEntity
import ru.netology.nework.entity.UserEntity

@Database(
    entities = [PostEntity::class, UserEntity::class, EventEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DbListConverters::class, DbMapConverters::class)
abstract class AppDb : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun userDao(): UserDao
    abstract fun eventDao(): EventDao
}