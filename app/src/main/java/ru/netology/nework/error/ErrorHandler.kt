package ru.netology.nework.error

import android.content.Context
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.netology.nework.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorHandler @Inject constructor(val context: Context) {
    fun getErrorDescriptor(status: Int): String {
        return when (status) {
            400 -> context.getString(R.string.error_desc_400)
            401 -> context.getString(R.string.error_desc_401)
            403 -> context.getString(R.string.error_desc_403)
            404 -> context.getString(R.string.error_desc_404)
            500 -> context.getString(R.string.error_desc_500)
            ErrorStatus.NETWORK_ERROR -> context.getString(R.string.check_connection)
            else -> context.getString(R.string.error_desk_unknown)
        }
    }
}
