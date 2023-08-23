package ru.netology.nework.ui.posts

import android.app.Activity
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.github.dhaval2404.imagepicker.ImagePicker
import com.github.dhaval2404.imagepicker.constant.ImageProvider
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.netology.nework.R
import ru.netology.nework.adapter.UserAdapter
import ru.netology.nework.databinding.FragmentNewPostBinding
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.User
import ru.netology.nework.model.requestModel.PostRequest
import ru.netology.nework.player.AudioLifecycleObserver
import ru.netology.nework.player.VideoLifecycleObserver
import ru.netology.nework.view.loadCircleCropAvatar
import ru.netology.nework.view.loadImageAttachment
import ru.netology.nework.viewModel.AuthViewModel
import ru.netology.nework.viewModel.NewPostViewModel
import ru.netology.nework.viewModel.UsersViewModel
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class NewPostFragment : Fragment() {

    private var _binding: FragmentNewPostBinding? = null
    private val binding: FragmentNewPostBinding
        get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()
    private val newPostViewModel: NewPostViewModel by activityViewModels()
    private val usersViewModel: UsersViewModel by activityViewModels()

    private lateinit var usersAdapter: UserAdapter

    private val pickPhotoLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            when (it.resultCode) {
                ImagePicker.RESULT_ERROR -> {
                    Snackbar.make(
                        binding.root, ImagePicker.getError(it.data), Snackbar.LENGTH_LONG
                    ).show()
                }

                Activity.RESULT_OK -> {
                    newPostViewModel.setPhoto(it.data?.data.toString())
                }
            }
        }

    private val getVideo =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { videoUri ->
                newPostViewModel.setVideo(videoUri.toString())
            }
        }

    private val getAudio =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { audioUri ->
                newPostViewModel.setAudio(audioUri.toString())
            }
        }

    @Inject
    lateinit var audioObserver: AudioLifecycleObserver

    @Inject
    lateinit var videoObserver: VideoLifecycleObserver

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentNewPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycle.addObserver(audioObserver)
        lifecycle.addObserver(videoObserver)
        setupUserAdapter()
        val user = authViewModel.authenticatedUser.value
        if (user != null) {
            setupUserData(user)
        }
        binding.usersContainer.visibility = View.GONE

        usersViewModel.users.observe(viewLifecycleOwner) {
            usersAdapter.submitList(usersViewModel.users.value)
        }

        newPostViewModel.postRequest.observe(viewLifecycleOwner) { post ->
            setupAttachment(post.attachment)
            setupMentions(post)

        }

        binding.apply {
            editContent.requestFocus()
            cancelButton.setOnClickListener {
                findNavController().navigateUp()
            }
            sendData.setOnClickListener {
                if (checkContent()) {
                    sendData()
                    findNavController().navigateUp()
                }
            }
            overlay.setOnClickListener {
                usersContainer.visibility = View.GONE
            }
        }
    }

    private fun setupAttachment(attachment: Attachment?) {
        when (attachment?.type) {
            Attachment.Type.IMAGE -> setupImageAttachment(attachment)
            Attachment.Type.VIDEO -> setupVideoAttachment(attachment)
            Attachment.Type.AUDIO -> setupAudioAttachment(attachment)
            null -> hideAttachment()
        }
        setupAddAttachmentButton(attachment)
    }

    private fun setupImageAttachment(photo: Attachment) {
        binding.imageAttachment.loadImageAttachment(photo.url)
        binding.imageAttachment.visibility = View.VISIBLE
    }

    private fun setupVideoAttachment(video: Attachment) {
        with(binding) {
            videoAttachment.visibility = View.VISIBLE
            thumbnail.visibility = View.VISIBLE
            videoPlayerView.visibility = View.GONE
            thumbnail.loadImageAttachment(video.url)
            videoAttachment.setOnClickListener {
                thumbnail.visibility = View.GONE
                videoPlayerView.visibility = View.VISIBLE
                if (URLUtil.isValidUrl(video.url)) {
                    videoObserver.videoPlayerDelegate(videoPlayerView, video)
                } else {
                    Toast.makeText(
                        requireContext(), getString(R.string.invalid_link), Toast.LENGTH_SHORT
                    ).show()
                }

            }
        }
    }

    private fun setupAudioAttachment(audio: Attachment) {
        binding.audioAttachment.visibility = View.VISIBLE
        binding.playButton.setOnClickListener {
            val audioUri = Uri.parse(audio.url)
            val inputStream = requireActivity().contentResolver.openInputStream(audioUri)
            val localFile = File(context?.cacheDir, "temp_audio.mp3")
            localFile.outputStream().use { outputStream ->
                inputStream?.copyTo(outputStream)
            }
            val preparedAudio = Attachment(localFile.absolutePath, Attachment.Type.AUDIO)
            audioObserver.mediaPlayerDelegate(preparedAudio) {
                updateAudioPlayerUI()
                binding.progressBar.progress = 0
            }
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                while (audioObserver.isPlaying) {
                    val currentPosition = audioObserver.getCurrentPosition()
                    delay(100)
                    withContext(Dispatchers.Main) {
                        val trackDuration = audioObserver.getTracDuration()
                        if (trackDuration != 0) {
                            binding.progressBar.progress = (currentPosition * 100) / trackDuration
                        }
                    }
                }
            }
            updateAudioPlayerUI()
        }
    }

    private fun updateAudioPlayerUI() {
        val imageId = if (audioObserver.isPlaying) R.drawable.pause_icon else R.drawable.play_icon
        binding.playButton.setImageResource(imageId)
    }

    private fun openImagePicker() {
        ImagePicker.with(this).crop().compress(2048).provider(ImageProvider.GALLERY)
            .galleryMimeTypes(
                arrayOf(
                    "image/png",
                    "image/jpeg",
                )
            ).createIntent(pickPhotoLauncher::launch)
    }

    private fun openVideoPicker() {
        getVideo.launch("video/*")
    }

    private fun openAudioPicker() {
        getAudio.launch("audio/*")
    }

    private fun setupAddAttachmentButton(attachment: Attachment?) {
        with(binding) {
            if (attachment != null) {
                manageAttachmentIcon.setImageResource(R.drawable.cross_icon)
                manageAttachmentText.text = getString(R.string.remove_attachment)
                manageAttachmentButton.setOnClickListener {
                    newPostViewModel.removeAttachment()
                }
            } else {
                manageAttachmentIcon.setImageResource(R.drawable.attachment_icon)
                manageAttachmentText.text = getString(R.string.add_attachment)
                manageAttachmentButton.setOnClickListener {
                    showAddAttachmentPopupMenu(manageAttachmentButton)
                }
            }
        }
    }

    private fun showAddAttachmentPopupMenu(view: View) {
        val popUpMenu = PopupMenu(requireContext(), view)
        popUpMenu.inflate(R.menu.add_attachment_menu)
        popUpMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.add_image -> {
                    openImagePicker()
                    true
                }

                R.id.add_video -> {
                    openVideoPicker()
                    true
                }

                R.id.add_audio -> {
                    openAudioPicker()
                    true
                }

                else -> false
            }
        }
        popUpMenu.show()
    }

    private fun hideAttachment() {
        binding.imageAttachment.visibility = View.GONE
        binding.videoAttachment.visibility = View.GONE
        binding.audioAttachment.visibility = View.GONE
    }

    private fun addMentionUser(user: User) {
        newPostViewModel.addMentionUser(user)
    }

    private fun setupUserAdapter() {
        usersAdapter = UserAdapter(object : UserAdapter.OnInteractionListener {
            override fun onItem(user: User) {
                addMentionUser(user)
                binding.usersContainer.visibility = View.GONE
            }
        })
        binding.recyclerViewUsers.adapter = usersAdapter
    }

    private fun setupUserData(user: User) {
        with(binding) {
            avatar.loadCircleCropAvatar(user.avatar.toString())
            userName.text = user.name
        }
    }

    private fun saveContent(content: String) {
        newPostViewModel.setContent(content)
    }

    private fun saveLink() {
        val link = binding.link.text.toString()
        if (link.isBlank()) {
            return
        }
        newPostViewModel.setLink(link)
    }

    private fun setupMentions(post: PostRequest) {
        with(binding) {
            val spannableStringBuilder = SpannableStringBuilder()
            post.mentionIds.forEachIndexed { index, userId ->
                val onUserClickableSpan = object : ClickableSpan() {
                    override fun onClick(view: View) {
                        onUser(userId)
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.isUnderlineText = false
                        ds.color = Color.GRAY
                    }
                }

                val userPreview = post.mentionUsers.first { it.id == userId }
                spannableStringBuilder.append(
                    userPreview.name,
                    onUserClickableSpan,
                    SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                if (index < post.mentionIds.size - 1) {
                    val commaAndSpace = ", "
                    val grayCommaAndSpace = SpannableString(commaAndSpace)
                    grayCommaAndSpace.setSpan(
                        ForegroundColorSpan(Color.GRAY),
                        0,
                        commaAndSpace.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    spannableStringBuilder.append(grayCommaAndSpace)
                } else {
                    spannableStringBuilder.append(" ")
                }
            }

            val addClickableSpan = object : ClickableSpan() {
                override fun onClick(view: View) {
                    binding.usersContainer.visibility = View.VISIBLE
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                }
            }

            spannableStringBuilder.append(
                getString(R.string.mention_user),
                addClickableSpan,
                SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            mention.movementMethod = LinkMovementMethod.getInstance()
            mention.text = spannableStringBuilder
        }
    }

    private fun onUser(userId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val user = usersViewModel.getUserById(userId)
            usersViewModel.setCurrentUser(user)
            findNavController().navigate(R.id.action_newPostFragment_to_userProfileFragment)
        }
    }

    private fun checkContent(): Boolean {
        val content = binding.editContent.text.toString()
        if (content.isBlank()) {
            binding.error.visibility = View.VISIBLE
            binding.error.text = getString(R.string.content_couldn_t_be_empty)
            return false
        }
        return true
    }

    private fun sendData() {
        val content = binding.editContent.text.toString()
        saveContent(content)
        saveLink()
        newPostViewModel.savePost()
    }

    override fun onDestroy() {
        super.onDestroy()
        newPostViewModel.clear()
        _binding = null
    }
}