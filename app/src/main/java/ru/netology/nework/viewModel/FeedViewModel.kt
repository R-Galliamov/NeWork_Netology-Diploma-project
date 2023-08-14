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
import ru.netology.nework.error.ApiError
import ru.netology.nework.repository.PostRepository
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
                mentionUsers = mentionUsers.map { it.copy(name = it.name.replace(' ', '\u00A0')) },
                likeOwnerUsers = likeOwnerUsers
            )
        }

    }.asLiveData()

    fun getUserPosts(userId: Int) = posts.value?.filter { post -> post.authorId == userId }

    private var _errorLiveData: MutableLiveData<ApiError> = MutableLiveData()
    val errorLiveData: MutableLiveData<ApiError>
        get() = _errorLiveData

    init {
        loadPosts()
    }

    private fun loadPosts() {
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