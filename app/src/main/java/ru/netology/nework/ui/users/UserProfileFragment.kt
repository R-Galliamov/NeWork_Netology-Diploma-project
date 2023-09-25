package ru.netology.nework.ui.users

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.adapter.JobsAdapter
import ru.netology.nework.adapter.JobsPreviewAdapter
import ru.netology.nework.adapter.UserWallAdapter
import ru.netology.nework.converters.DateTimeConverter
import ru.netology.nework.databinding.FragmentUserProfileBinding
import ru.netology.nework.dto.Job
import ru.netology.nework.player.AudioLifecycleObserver
import ru.netology.nework.player.VideoLifecycleObserver
import ru.netology.nework.ui.events.UserEventsFragment
import ru.netology.nework.ui.posts.UserPostsFragment
import ru.netology.nework.util.AndroidUtils
import ru.netology.nework.view.loadCircleCropAvatar
import ru.netology.nework.viewModel.AuthViewModel
import ru.netology.nework.viewModel.EventsViewModel
import ru.netology.nework.viewModel.FeedViewModel
import ru.netology.nework.viewModel.UsersViewModel
import java.text.SimpleDateFormat
import javax.inject.Inject

@AndroidEntryPoint
class UserProfileFragment : Fragment() {

    @Inject
    lateinit var androidUtils: AndroidUtils

    private var _binding: FragmentUserProfileBinding? = null
    private val binding: FragmentUserProfileBinding
        get() = _binding!!
    private val usersViewModel: UsersViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()
    private val feedViewModel: FeedViewModel by activityViewModels()
    private val eventViewModel: EventsViewModel by activityViewModels()

    @Inject
    lateinit var audioObserver: AudioLifecycleObserver

    @Inject
    lateinit var videoObserver: VideoLifecycleObserver

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        val fragments = listOf(
            UserPostsFragment(),
            UserEventsFragment(),
        )
        val tabTitles = listOf(getString(R.string.posts), getString(R.string.events))
        val adapter = UserWallAdapter(requireActivity(), fragments)
        binding.userWallViewPager.adapter = adapter

        val tabLayout = binding.tabMode
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab?) {
                val position = tab?.position ?: 0
                binding.addContent.setOnClickListener {
                    when (position) {
                        0 -> findNavController().navigate(R.id.action_userProfileFragment_to_editPostFragment)
                        1 -> findNavController().navigate(R.id.action_userProfileFragment_to_editEventFragment)
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })

        TabLayoutMediator(binding.tabMode, binding.userWallViewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycle.addObserver(audioObserver)
        lifecycle.addObserver(videoObserver)

        usersViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                feedViewModel.updateUserPosts(user.id)
                eventViewModel.updateUserEvents(user.id)

                val isProfileOwner = authViewModel.authenticatedUser.value?.id == user.id

                val jobsPreviewAdapter =
                    JobsPreviewAdapter(object : JobsPreviewAdapter.OnInteractionListener {
                        override fun onClick() {
                            binding.jobContainer.visibility = View.VISIBLE
                        }
                    })

                val jobsAdapter = JobsAdapter(object : JobsAdapter.OnInteractionListener {
                    override fun onLink(job: Job) {
                        if (URLUtil.isValidUrl(job.link)) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(job.link))
                            startActivity(intent)
                        } else {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.invalid_link),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onDelete(job: Job) {
                        usersViewModel.deleteJob(job)
                    }
                }, isProfileOwner)

                binding.recyclerViewJobsPreview.adapter = jobsPreviewAdapter
                binding.recyclerViewJobs.adapter = jobsAdapter

                binding.apply {
                    if (user.avatar.isNullOrBlank()) {
                        avatar.setImageResource(R.drawable.user_icon)
                    } else {
                        avatar.loadCircleCropAvatar(user.avatar)
                    }
                    userName.text = user.name
                    login.text = "@${user.login}"

                    jobContainer.visibility = View.GONE

                    jobContainer.visibility = View.GONE

                    jobsPreviewAdapter.submitList(user.jobs)
                    jobsAdapter.submitList(user.jobs)

                    if (isProfileOwner) {
                        addJobButton.visibility = View.VISIBLE
                        addJobButton.setOnClickListener {
                            jobContainer.visibility = View.VISIBLE
                        }
                        signOutButton.visibility = View.VISIBLE
                        signOutButton.setOnClickListener {
                            authViewModel.signOutUser()
                            findNavController().navigate(R.id.action_userProfileFragment_to_holderFragment)
                        }
                        addJobContainer.visibility = View.VISIBLE

                        setDateChangedListener(from)
                        setDateChangedListener(to)
                        sendData.setOnClickListener {
                            if (companyName.text.isNullOrBlank() || position.text.isNullOrBlank() || from.text.isNullOrBlank()) {
                                error.visibility = View.VISIBLE
                                error.text = getString(R.string.job_request_error)
                                return@setOnClickListener
                            }
                            if (!validateDate(from.text.toString())) {
                                error.visibility = View.VISIBLE
                                error.text = getString(R.string.invalid_date)
                                return@setOnClickListener
                            }

                            if (!to.text.isNullOrBlank()) {
                                if (!validateDate(to.text.toString())) {
                                    error.visibility = View.VISIBLE
                                    error.text = getString(R.string.invalid_date)
                                    return@setOnClickListener
                                }
                            }
                            if (!link.text.isNullOrBlank()) {
                                if (!URLUtil.isValidUrl(link.text.toString())) {
                                    error.visibility = View.VISIBLE
                                    error.text = getString(R.string.invalid_link)
                                    return@setOnClickListener
                                }
                            }
                            sendJobData()
                            clearAddJobFields()
                            androidUtils.hideKeyboard(requireView())
                        }

                        addContent.visibility = View.VISIBLE

                    } else {
                        addJobButton.visibility = View.GONE
                        signOutButton.visibility = View.GONE
                        addJobContainer.visibility = View.GONE
                        addContent.visibility = View.GONE
                    }

                    backButton.setOnClickListener {
                        findNavController().navigateUp()
                    }

                    overlay.setOnClickListener {
                        binding.jobContainer.visibility = View.GONE
                        androidUtils.hideKeyboard(requireView())
                    }
                }
            }
        }
    }

    private fun setDateChangedListener(dateEditText: EditText) {
        val originalText = "01/01/1990"
        dateEditText.hint = originalText

        dateEditText.addTextChangedListener(object : TextWatcher {
            var previousLength = 0
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s != null) {
                    val newText = s.toString().replace("[^0-9/]".toRegex(), "")
                    if (newText.length == 2) {
                        if (previousLength < s.length) {
                            val updatedText = newText.substring(0, 2) + "/" + newText.substring(2)
                            dateEditText.setText(updatedText)
                            dateEditText.setSelection(updatedText.length)
                        }
                    }
                    if (newText.length > 2 && newText[2] != '/') {
                        val updatedText = newText.substring(0, 2) + "/" + newText.substring(2)
                        dateEditText.setText(updatedText)
                        dateEditText.setSelection(updatedText.length)
                        return
                    }
                    if (newText.length == 5) {
                        if (previousLength < s.length) {
                            val updatedText = newText.substring(0, 5) + "/" + newText.substring(5)
                            dateEditText.setText(updatedText)
                            dateEditText.setSelection(updatedText.length)
                        }
                    }
                    if (newText.length > 5 && newText[5] != '/') {
                        val updatedText = newText.substring(0, 5) + "/" + newText.substring(5)
                        dateEditText.setText(updatedText)
                        dateEditText.setSelection(updatedText.length)
                    }
                    previousLength = s.length
                }
            }

            override fun afterTextChanged(p0: Editable?) {

            }
        })
    }

    private fun validateDate(dateStr: String): Boolean {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy")
        dateFormat.isLenient = false
        return try {
            dateFormat.parse(dateStr)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getJobData(): Job {
        with(binding) {
            val companyName = companyName.text.toString()
            val position = position.text.toString()
            val start = DateTimeConverter.uiDateToApiFormat(from.text.toString())
            val finish = to.text.takeIf { !it.isNullOrBlank() }
                ?.let { DateTimeConverter.uiDateToApiFormat(it.toString()) }
            val link = to.text.toString().takeIf { it.isNullOrBlank().not() }
            return Job(
                name = companyName,
                position = position,
                start = start,
                finish = finish,
                link = link
            )
        }
    }

    private fun clearAddJobFields() {
        with(binding) {
            companyName.text.clear()
            position.text.clear()
            from.text.clear()
            to.text.clear()
            link.text.clear()
        }
    }

    private fun sendJobData() {
        val job = getJobData()
        usersViewModel.saveJob(job)
    }

    override fun onDestroy() {
        super.onDestroy()
        resetUser()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun resetUser() {
        usersViewModel.usersNavList.removeLast()
        val previousUser = usersViewModel.usersNavList.lastOrNull()
        if (previousUser != null) {
            usersViewModel.setCurrentUser(previousUser)
        }
    }
}