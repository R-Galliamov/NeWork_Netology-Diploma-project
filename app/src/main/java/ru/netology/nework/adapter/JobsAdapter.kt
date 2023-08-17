package ru.netology.nework.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import ru.netology.nework.databinding.JobItemBinding
import ru.netology.nework.databinding.UserItemBinding
import ru.netology.nework.dto.Job
import ru.netology.nework.dto.User
import ru.netology.nework.view.loadCircleCropAvatar

class JobsAdapter(private val onInteractionListener: OnInteractionListener) :
    ListAdapter<Job, JobsAdapter.JobsViewHolder>(JobsDiffCallback()) {

    interface OnInteractionListener {
        fun onItem(job: Job)
    }

    inner class JobsViewHolder(private val binding: JobItemBinding) : ViewHolder(binding.root) {
        fun bind(job: Job) {
            binding.apply {
                text.text = job.name
            }
            itemView.setOnClickListener {
                onInteractionListener.onItem(getItem(adapterPosition))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobsViewHolder {
        val binding =
            JobItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return JobsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: JobsViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }
}

class JobsDiffCallback : DiffUtil.ItemCallback<Job>() {
    override fun areItemsTheSame(oldItem: Job, newItem: Job): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Job, newItem: Job): Boolean {
        return oldItem == newItem
    }
}