package ru.netology.nework.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import okhttp3.internal.checkOffsetAndCount
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentHolderBinding
import ru.netology.nework.view.loadCircleCropAvatar
import ru.netology.nework.viewModel.AuthViewModel
import ru.netology.nework.viewModel.UsersViewModel

class HolderFragment : Fragment() {
    private var _binding: FragmentHolderBinding? = null
    private val binding: FragmentHolderBinding
        get() = _binding!!

    private lateinit var badge: BadgeDrawable
    private val authViewModel: AuthViewModel by activityViewModels()
    private val usersViewModel: UsersViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHolderBinding.inflate(inflater, container, false)
        return binding.root
    }

    @ExperimentalBadgeUtils
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.create_content -> {
                    val menuItemView = binding.toolbar.findViewById<View>(R.id.create_content)
                    showCreateContentPopupMenu(menuItemView)
                    true
                }

                else -> false
            }
        }

        binding.navbar.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.feed -> {
                    childFragmentManager.beginTransaction()
                        .replace(R.id.place_holder, FeedFragment()).commit()
                    true
                }

                R.id.events -> {
                    childFragmentManager.beginTransaction()
                        .replace(R.id.place_holder, EventFragment()).commit()
                    true
                }

                R.id.users -> {
                    childFragmentManager.beginTransaction()
                        .replace(R.id.place_holder, UsersFragment()).commit()
                    true
                }

                else -> false
            }
        }

        authViewModel.authenticatedUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.avatar.loadCircleCropAvatar(user.avatar.toString())
                binding.avatar.setOnClickListener {
                    usersViewModel.setCurrentUser(user)
                    findNavController().navigate(R.id.action_holderFragment_to_userProfileFragment)
                }
            } else {
                binding.avatar.setOnClickListener {
                    findNavController().navigate(R.id.action_holderFragment_to_authFragment)
                }
            }
        }

        badge = BadgeDrawable.create(requireContext())
        badge.number = 10
        BadgeUtils.attachBadgeDrawable(badge, binding.toolbar, R.id.notification_badge)

        //TODO handle the returning from fullscreen fragments
        childFragmentManager.beginTransaction().replace(R.id.place_holder, FeedFragment()).commit()
    }

    private fun showCreateContentPopupMenu(view: View) {
        val popUpMenu = PopupMenu(requireContext(), view)
        popUpMenu.inflate(R.menu.add_menu)
        popUpMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.add_post -> {
                    findNavController().navigate(R.id.action_holderFragment_to_newPostFragment)
                    true
                }

                R.id.add_event -> {
                    true
                }

                else -> false
            }
        }
        popUpMenu.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}