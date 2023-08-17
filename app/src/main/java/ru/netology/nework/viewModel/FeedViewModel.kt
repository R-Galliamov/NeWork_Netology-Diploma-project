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
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.Post
import ru.netology.nework.error.ApiError
import ru.netology.nework.model.LoadingStateModel
import ru.netology.nework.repository.PostRepository
import java.text.DecimalFormat
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val postRepository: PostRepository
) :
    ViewModel() {

    val posts: LiveData<List<Post>> = postRepository.data.map { posts ->
        posts.map { post ->
            val likeOwnerUsers =
                post.users.filterKeys { it.toIntOrNull() in post.likeOwnerIds }.values.toList()
            val mentionUsers =
                post.users.filterKeys { it.toIntOrNull() in post.mentionIds }.values.toList()
            post.copy(
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

    fun setCurrentPost(post: Post) {
        _currentPost.value = post
    }

    fun loadUserPosts(userId: Int) {
        _userPosts.value = posts.value?.filter { post -> post.authorId == userId }
        _dataState.value = LoadingStateModel()
    }

    init {
        loadPosts()
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

                    val currentPosts = userPosts.value?.toMutableList()
                    currentPosts?.indexOfFirst { it.id == post.id }?.takeIf { it != -1 }
                        ?.let { index ->
                            currentPosts[index] = post
                            _userPosts.value = currentPosts!!
                        }
                }
            } catch (e: ApiError) {
                withContext(Dispatchers.Main) {
                    _dataState.value = LoadingStateModel(errorState = true, errorObject = e)
                }
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