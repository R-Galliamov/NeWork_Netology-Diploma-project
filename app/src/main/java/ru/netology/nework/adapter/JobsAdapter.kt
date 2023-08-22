package ru.netology.nework.adapter

import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import ru.netology.nework.R
import ru.netology.nework.converters.DateTimeConverter
import ru.netology.nework.databinding.JobItemBinding
import ru.netology.nework.dto.Job

class JobsAdapter(
    private val onInteractionListener: OnInteractionListener,
    private val isProfileOwner: Boolean
) : ListAdapter<Job, JobsAdapter.JobsViewHolder>(JobsDiffCallback()) {

    interface OnInteractionListener {
        fun onLink(job: Job)
        fun onDelete(job: Job)
    }

    inner class JobsViewHolder(private val binding: JobItemBinding) : ViewHolder(binding.root) {
        fun bind(job: Job) {
            binding.apply {
                Log.d("Jobs Adapter", job.toString())
                val textBuilder = StringBuilder()
                textBuilder.append(
                    itemView.context.getString(
                        R.string.job_preview, job.position, job.name
                    )
                )
                textBuilder.append(" ")
                val startDate = DateTimeConverter.datetimeToUiDate(job.start)
                textBuilder.append(itemView.context.getString(R.string.from_date, startDate))
                if (!job.finish.isNullOrBlank()) {
                    textBuilder.append(" ")
                    val finishDate = DateTimeConverter.datetimeToUiDate(job.finish)
                    textBuilder.append(itemView.context.getString(R.string.to_date, finishDate))
                }
                textBuilder.append(". ")
                text.text = textBuilder
                var spannableString = SpannableString(textBuilder)

                if (job.link != null) {
                    val linkText = itemView.context.getString(R.string.go_to)
                    textBuilder.append(linkText)
                    spannableString = SpannableString(textBuilder)
                    val clickableSpan = object : ClickableSpan() {
                        override fun onClick(p0: View) {
                            onInteractionListener.onLink(job)
                        }
                    }

                    val startIndex = textBuilder.indexOf(linkText)
                    if (startIndex != -1) {
                        spannableString.setSpan(
                            clickableSpan,
                            startIndex,
                            startIndex + linkText.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                    text.movementMethod = LinkMovementMethod.getInstance()
                }
                text.text = spannableString

                if (isProfileOwner) {
                    deleteButton.visibility = View.VISIBLE
                    deleteButton.setOnClickListener {
                        onInteractionListener.onDelete(job)
                    }
                } else {
                    deleteButton.visibility = View.GONE
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobsViewHolder {
        val binding = JobItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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