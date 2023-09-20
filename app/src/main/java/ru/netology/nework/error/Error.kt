package ru.netology.nework.error

import java.io.IOException
import java.lang.RuntimeException
import java.sql.SQLException

object ErrorStatus {
    const val NETWORK_ERROR = 1000
    const val DB_ERROR = 2000
    const val UNKNOWN_ERROR = 10000
}

sealed class AppError(var status: Int, var code: String) : RuntimeException() {

    companion object {
        fun from(e: Throwable): AppError = when (e) {
            is AppError -> e
            is SQLException -> DbError
            is IOException -> NetworkError
            else -> UnknownError
        }
    }
}

class ApiError(status: Int = 0, code: String = "") : AppError(status, code)
object NetworkError : AppError(ErrorStatus.NETWORK_ERROR, "error_network")

object DbError : AppError(ErrorStatus.DB_ERROR, "error_db")
object UnknownError : AppError(ErrorStatus.UNKNOWN_ERROR, "error_unknown")