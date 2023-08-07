package ru.netology.nework.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.User
import ru.netology.nework.error.ApiError
import ru.netology.nework.repository.PostRepository
import ru.netology.nework.repository.UserRepository
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val postRepository: PostRepository
) :
    ViewModel() {

    val posts: LiveData<List<Post>> = postRepository.data.map { posts ->
        posts.map { post ->
            val mentionUsers =
                post.users.filterKeys { it.toIntOrNull() in post.mentionIds }.values.toList()
            post.copy(
                mentionUsers = mentionUsers
            )
        }

    }.asLiveData()

    private var _errorLiveData: MutableLiveData<ApiError> = MutableLiveData()
    val errorLiveData: MutableLiveData<ApiError>
        get() = _errorLiveData

    init {
        loadPosts()
    }

    fun loadPosts() {
        viewModelScope.launch {
            postRepository.getAll()
        }
    }

    fun onLike(post: Post) {
        viewModelScope.launch {
            try {
                postRepository.onLike(post)
            } catch (e: ApiError) {
                _errorLiveData.value = ApiError(e.status, e.code)
            }
        }
    }
}