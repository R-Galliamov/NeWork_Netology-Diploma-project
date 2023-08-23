package ru.netology.nework.repository

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
class ContentResolverModule {

    @Provides
    fun provideContentResolver(@ApplicationContext context: Context) = context.contentResolver
}