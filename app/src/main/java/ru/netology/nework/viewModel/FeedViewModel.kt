package ru.netology.nework.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.Post
import ru.netology.nework.error.ApiError
import ru.netology.nework.model.LoadingStateModel
import ru.netology.nework.repository.PostRepository
import java.lang.Exception
import java.text.DecimalFormat
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val postRepository: PostRepository, private val appAuth: AppAuth
) : ViewModel() {

    val posts: LiveData<List<Post>> = postRepository.data.map { posts ->
        posts.map { post ->
            val likeOwnerUsers =
                post.users.filterKeys { it.toIntOrNull() in post.likeOwnerIds }.values.toList()
            val mentionUsers =
                post.users.filterKeys { it.toIntOrNull() in post.mentionIds }.values.toList()
            post.copy(
                ownedByMe = appAuth.authStateFlow.value.id == post.authorId,
                coords = post.coords?.let { getValidCoords(it) },
                mentionUsers = mentionUsers.map { it.copy(name = it.name.replace(' ', '\u00A0')) },
                likeOwnerUsers = likeOwnerUsers
            )
        }

    }.asLiveData()

    private val _dataState = MutableLiveData<LoadingStateModel>()
    val dataState: LiveData<LoadingStateModel>
        get() = _dataState

    private var _currentPost: MutableLiveData<Post> = MutableLiveData(null)
    val currentPost: LiveData<Post>
        get() = _currentPost

    private var _userPosts: MutableLiveData<List<Post>> = MutableLiveData(null)
    val userPosts: LiveData<List<Post>>
        get() = _userPosts

    init {
        loadPosts()
    }

    fun setCurrentPost(post: Post) {
        _currentPost.value = post
    }

    fun updateUserPosts(userId: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            val userPosts = postRepository.getUserPosts(userId).map { post ->
                post.copy(
                    ownedByMe = appAuth.authStateFlow.value.id == post.authorId,
                    coords = post.coords?.let { getValidCoords(it) },
                )
            }
            withContext(Dispatchers.Main) {
                _userPosts.value = userPosts
            }
        }
    }

    fun updateCurrentPost(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val post = postRepository.getPost(id).let { dbPost ->
                dbPost.copy(
                    ownedByMe = appAuth.authStateFlow.value.id == dbPost.authorId,
                    coords = dbPost.coords?.let { getValidCoords(it) },
                )
            }
            withContext(Dispatchers.Main) {
                setCurrentPost(post)
            }
        }
    }

    fun loadPosts() {
        viewModelScope.launch(Dispatchers.IO) {
            var newState = if (postRepository.isDbEmpty()) {
                LoadingStateModel(loading = true)
            } else {
                LoadingStateModel(refreshing = true)
            }
            withContext(Dispatchers.Main) {
                _dataState.value = newState
            }
            newState = try {
                postRepository.getAll()
                LoadingStateModel()
            } catch (e: ApiError) {
                LoadingStateModel(errorState = true, errorObject = e)
            } catch (e: Exception) {
                LoadingStateModel(errorState = true)
            }
            withContext(Dispatchers.Main) {
                _dataState.value = newState
            }
        }
    }

    fun onLike(post: Post) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val post = postRepository.onLike(post)
                withContext(Dispatchers.Main) {
                    setCurrentPost(post)
                    updateUserPosts(post)
                }
            } catch (e: ApiError) {
                withContext(Dispatchers.Main) {
                    _dataState.value = LoadingStateModel(errorState = true, errorObject = e)
                }
            } catch (e: Exception) {
                LoadingStateModel(errorState = true)
            }
        }
    }

    fun deletePost(post: Post) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                postRepository.deletePost(post.id)

                withContext(Dispatchers.Main) {
                    updateUserPosts(post)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateUserPosts(post: Post) {
        val currentPosts = userPosts.value?.toMutableList()
        currentPosts?.indexOfFirst { it.id == post.id }?.takeIf { it != -1 }?.let { index ->
            currentPosts[index] = post
            currentPosts.let {
                _userPosts.value = it
            }
        }
    }

    fun resetState() {
        _dataState.value = LoadingStateModel()
    }

    private fun getValidCoords(coords: Coordinates): Coordinates {
        val decimalFormat = DecimalFormat("0.000000")
        val lat = decimalFormat.format(coords.lat.toDouble())
        val long = decimalFormat.format(coords.long.toDouble())
        return Coordinates(lat, long)
    }
}