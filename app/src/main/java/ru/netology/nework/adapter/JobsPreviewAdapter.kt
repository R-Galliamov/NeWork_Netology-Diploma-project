package ru.netology.nework.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import ru.netology.nework.R
import ru.netology.nework.databinding.JobItemPreviewBinding
import ru.netology.nework.dto.Job

class JobsPreviewAdapter(private val onInteractionListener: OnInteractionListener) :
    ListAdapter<Job, JobsPreviewAdapter.JobsPreviewViewHolder>(JobsDiffCallback()) {

    interface OnInteractionListener {
        fun onClick()
    }

    inner class JobsPreviewViewHolder(private val binding: JobItemPreviewBinding) :
        ViewHolder(binding.root) {
        fun bind(job: Job) {
            binding.apply {
                position.text =
                    itemView.context.getString(R.string.job_preview, job.position, job.name)
            }
            itemView.setOnClickListener {
                onInteractionListener.onClick()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobsPreviewViewHolder {
        val binding =
            JobItemPreviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return JobsPreviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: JobsPreviewViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }
}
