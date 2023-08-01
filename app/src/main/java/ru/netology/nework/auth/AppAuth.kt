package ru.netology.nework.auth

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.netology.nework.model.Token
import ru.netology.nework.service.api.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppAuth @Inject constructor(@ApplicationContext private val context: Context) {
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val idKey = "id"
    private val tokenKey = "token"

    private val _tokenStateFlow: MutableStateFlow<Token>

    init {
        val id = prefs.getInt(idKey, 0)
        val token = prefs.getString(tokenKey, null)

        if (id == 0 || token == null) {
            _tokenStateFlow = MutableStateFlow(Token())
            with(prefs.edit()) {
                clear()
                apply()
            }
        } else {
            _tokenStateFlow = MutableStateFlow(Token(id, token))
        }
    }

    val tokenStateFlow: StateFlow<Token> = _tokenStateFlow.asStateFlow()

    @InstallIn(SingletonComponent::class)
    @EntryPoint
    interface AppAuthEntryPoint {
        fun apiService(): ApiService
    }

    @Synchronized
    fun setAuth(token: Token) {
        _tokenStateFlow.value = token
        with (prefs.edit()) {
            putInt(idKey, token.id)
            putString(tokenKey, token.token)
            apply()
        }
        sendPushToken()
    }

    @Synchronized
    fun removeAuth(){
        _tokenStateFlow.value = Token()
        with (prefs.edit()) {
            clear()
            apply()
        }
        sendPushToken()
    }

    fun sendPushToken(token: String? = null) {
      //TODO not yet implemented
    }
}