package ru.netology.nework.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import ru.netology.nework.databinding.UserItemBinding
import ru.netology.nework.dto.User
import ru.netology.nework.view.loadCircleCropAvatar

class UserAdapter(private val onInteractionListener: OnInteractionListener) :
    ListAdapter<User, UserAdapter.UserViewHolder>(UserDiffCallback()) {

    interface OnInteractionListener {
        fun onItem(user: User)
    }

    inner class UserViewHolder(private val binding: UserItemBinding) : ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.apply {
                avatar.loadCircleCropAvatar(user.avatar.toString())
                author.text = user.name
                login.text = "@${user.login}"
            }
            itemView.setOnClickListener {
                onInteractionListener.onItem(getItem(adapterPosition))
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