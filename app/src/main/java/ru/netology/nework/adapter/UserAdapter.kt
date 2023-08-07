package ru.netology.nework.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import ru.netology.nework.R
import ru.netology.nework.USER_AVATARS
import ru.netology.nework.converters.DateTimeConverter
import ru.netology.nework.databinding.PostItemBinding
import ru.netology.nework.databinding.UserItemBinding
import ru.netology.nework.dto.User
import ru.netology.nework.view.loadCircleCropAvatar
import kotlin.random.Random

class UserAdapter(private val onInteractionListener: OnInteractionListener) :
    ListAdapter<User, UserAdapter.UserViewHolder>(UserDiffCallback()) {

    interface OnInteractionListener {
        fun onLike(user: User)
    }

    inner class UserViewHolder(private val binding: UserItemBinding) : ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.apply {
                if (user.avatar.isNullOrBlank()) {
                    val index = Random.nextInt(USER_AVATARS.size)
                    avatar.setImageResource(USER_AVATARS[index])
                } else {
                    avatar.loadCircleCropAvatar(user.avatar.toString())
                }
                author.text = user.name
                login.text = "@${user.login}"
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding =
            UserItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }
}

class UserDiffCallback : DiffUtil.ItemCallback<User>() {
    override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
        return oldItem == newItem
    }
}