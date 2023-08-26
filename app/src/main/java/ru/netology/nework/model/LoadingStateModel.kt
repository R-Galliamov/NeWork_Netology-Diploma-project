package ru.netology.nework.model

import ru.netology.nework.error.ApiError

data class LoadingStateModel(
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val errorState: Boolean = false,
    val errorObject: ApiError = ApiError(),
)