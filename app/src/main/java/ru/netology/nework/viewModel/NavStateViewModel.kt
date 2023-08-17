package ru.netology.nework.viewModel

import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ru.netology.nework.ui.events.EventsFragment
import ru.netology.nework.ui.posts.FeedFragment
import ru.netology.nework.ui.users.UsersFragment
import javax.inject.Inject

@HiltViewModel
class NavStateViewModel @Inject constructor() : ViewModel() {

    enum class NavState {
        FeedFragment,
        EventsFragment,
        UsersFragment,
    }

    val navState = MutableLiveData<NavState>()

    init {
        navState.value = NavState.FeedFragment
    }

    fun getFragment(): Fragment {
        return when (navState.value) {
            null -> FeedFragment()
            NavState.FeedFragment -> FeedFragment()
            NavState.EventsFragment -> EventsFragment()
            NavState.UsersFragment -> UsersFragment()
        }
    }
}