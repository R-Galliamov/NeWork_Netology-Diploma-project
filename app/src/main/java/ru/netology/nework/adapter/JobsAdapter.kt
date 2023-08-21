package ru.netology.nework.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import ru.netology.nework.R
import ru.netology.nework.databinding.JobItemBinding
import ru.netology.nework.dto.Job

class JobsAdapter :
    ListAdapter<Job, JobsAdapter.JobsViewHolder>(JobsDiffCallback()) {

    interface OnInteractionListener {
        fun onItem(job: Job)
    }

    inner class JobsViewHolder(private val binding: JobItemBinding) :
        ViewHolder(binding.root) {
        fun bind(job: Job) {
            binding.apply {
                val textBuilder = StringBuilder()
                textBuilder.append(
                    itemView.context.getString(
                        R.string.job_preview,
                        job.position,
                        job.name
                    )
                )
                textBuilder.append(" ")
                textBuilder.append(itemView.context.getString(R.string.from_date, job.start))
                if (!job.link.isNullOrBlank()) {
                    textBuilder.append(itemView.context.getString(R.string.to_date, job.finish))
                }
                textBuilder.append(".")

                text.text = textBuilder
                if (!job.link.isNullOrBlank()) {
                    link.text = job.link
                }

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