package ru.netology.nework.viewModel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.dto.AuthState
import ru.netology.nework.dto.User
import ru.netology.nework.error.ApiError
import ru.netology.nework.repository.UserRepository
import ru.netology.nework.model.requestModel.AuthenticationRequest
import ru.netology.nework.model.PhotoModel
import ru.netology.nework.model.requestModel.RegistrationRequest
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val appAuth: AppAuth, private val repository: UserRepository
) : ViewModel() {
    val authData: LiveData<AuthState> = appAuth.authStateFlow.asLiveData(Dispatchers.Default)
    val authenticated: Boolean
        get() = appAuth.authStateFlow.value.id != 0

    private val _authProcess: MutableLiveData<AuthProcess> =
        MutableLiveData(AuthProcess.AUTHENTICATION)

    private val _authenticatedUser: MutableLiveData<User?> = MutableLiveData(null)
    val authenticatedUser: LiveData<User?>
        get() = _authenticatedUser

    private val _isLoading: MutableLiveData<Boolean> = MutableLiveData(false)
    val isLoading: LiveData<Boolean>
        get() = _isLoading

    val authProcess: MutableLiveData<AuthProcess>
        get() = _authProcess

    val photo = MutableLiveData(PhotoModel())

    enum class AuthProcess {
        AUTHENTICATION, REGISTRATION
    }

    init {
        if (authenticated) {
            setAuthenticatedUser(authData.value!!.id)
        }
    }

    fun switchProcess() {
        _authProcess.value =
            if (authProcess.value == AuthProcess.AUTHENTICATION) AuthProcess.REGISTRATION else AuthProcess.AUTHENTICATION
    }

    fun signUpUser(regRequest: RegistrationRequest) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val authState = repository.signUpUser(regRequest)
                appAuth.setAuth(authState)
                _isLoading.value = false
                setAuthenticatedUser(authState.id)
                Log.d("App log", appAuth.authStateFlow.value.toString())
            } catch (e: ApiError) {
                _isLoading.value = false
            }
        }
    }

    fun signInUser(authRequest: AuthenticationRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            val authState = repository.signInUser(authRequest)
            appAuth.setAuth(authState)
            _isLoading.value = false
            setAuthenticatedUser(authState.id)
            Log.d("App log", appAuth.authStateFlow.value.toString())
        }
    }

    fun signOutUser() {
        appAuth.removeAuth()
        _authenticatedUser.value = null
        Log.d("App log", appAuth.authStateFlow.value.toString())
    }

    private fun setAuthenticatedUser(userId: Int) {
        viewModelScope.launch {
            val user = repository.getUserById(userId)
            _authenticatedUser.value = user
        }
    }

    fun changePhoto(uri: Uri?) {
        photo.value = PhotoModel(uri)
    }

    fun removePhoto() {
        photo.value = PhotoModel()
    }
}