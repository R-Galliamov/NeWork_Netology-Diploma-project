package ru.netology.nework.dao

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.netology.nework.db.AppDb

@InstallIn(SingletonComponent::class)
@Module
object DaoModules {
    @Provides
    fun providePostDao(db: AppDb): PostDao = db.postDao()

    @Provides
    fun provideUserDao(db: AppDb): UserDao = db.userDao()

    @Provides
    fun provideEventDao(db: AppDb): EventDao = db.eventDao()
}