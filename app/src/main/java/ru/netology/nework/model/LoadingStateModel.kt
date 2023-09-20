package ru.netology.nework.model

data class LoadingStateModel(
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val errorState: Boolean = false,
    val errorStatus: Int = -1,
)