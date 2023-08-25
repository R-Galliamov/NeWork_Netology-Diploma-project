package ru.netology.nework.ui.events

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
import android.util.Log
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
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.netology.nework.R
import ru.netology.nework.adapter.UserAdapter
import ru.netology.nework.converters.DateTimeConverter
import ru.netology.nework.databinding.FragmentEditEventBinding
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.User
import ru.netology.nework.model.requestModel.EventRequest
import ru.netology.nework.player.AudioLifecycleObserver
import ru.netology.nework.player.VideoLifecycleObserver
import ru.netology.nework.util.AndroidUtils
import ru.netology.nework.view.loadCircleCropAvatar
import ru.netology.nework.view.loadImageAttachment
import ru.netology.nework.viewModel.AuthViewModel
import ru.netology.nework.viewModel.EditEventViewModel
import ru.netology.nework.viewModel.UsersViewModel
import java.io.File
import java.time.LocalDateTime
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class EditEventFragment : Fragment(), DatePickerDialog.OnDateSetListener,
    TimePickerDialog.OnTimeSetListener {

    private var _binding: FragmentEditEventBinding? = null
    private val binding: FragmentEditEventBinding
        get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()
    private val editEventViewModel: EditEventViewModel by activityViewModels()
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
                    editEventViewModel.setPhoto(it.data?.data.toString())
                }
            }
        }

    private val getVideo =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { videoUri ->
                editEventViewModel.setVideo(videoUri.toString())
            }
        }

    private val getAudio =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { audioUri ->
                editEventViewModel.setAudio(audioUri.toString())
            }
        }

    @Inject
    lateinit var audioObserver: AudioLifecycleObserver

    @Inject
    lateinit var videoObserver: VideoLifecycleObserver

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEditEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycle.addObserver(audioObserver)
        lifecycle.addObserver(videoObserver)
        setupUserAdapter()
        val user = authViewModel.authenticatedUser.value
        user?.let { setupUserData(user) }
        binding.usersContainer.visibility = View.GONE
        usersViewModel.users.observe(viewLifecycleOwner) {
            usersAdapter.submitList(usersViewModel.users.value)
        }
        var editDataSet = false
        editEventViewModel.eventRequest.observe(viewLifecycleOwner) { event ->
            Log.d("App log", event.toString())
            setupAttachment(event.attachment)
            setupSpeakers(event)
            setupEventTypeButton(event)
            setupDateTimeButton(event)
            if (!editDataSet && event.id != 0) {
                Log.d("App log", event.toString())
                setupEditData(event)
                editDataSet = true
            }
            binding.sendData.setOnClickListener {
                AndroidUtils.hideKeyboard(requireView())
                if (checkData(event)) {
                    sendData()
                }
            }
        }

        editEventViewModel.postCreated.observe(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        binding.apply {
            editContent.requestFocus()
            cancelButton.setOnClickListener {
                findNavController().navigateUp()
            }

            overlay.setOnClickListener {
                usersContainer.visibility = View.GONE
            }
        }
    }

    private fun checkData(event: EventRequest): Boolean {
        return checkContent() && checkDate(event) && checkCoords()
    }

    private fun setupEditData(event: EventRequest) {
        with(binding) {
            editContent.setText(event.content)
            link.setText(event.link)
            editContent.setSelection(event.content.length)
        }
    }

    private fun setupEventTypeButton(event: EventRequest) {
        when (event.type) {
            Event.Type.OFFLINE -> {
                binding.eventTypeButton.apply {
                    setBackgroundResource(R.drawable.button_gray_outlined)
                    text = requireContext().getText(R.string.offline)
                }
            }

            Event.Type.ONLINE -> {
                binding.eventTypeButton.apply {
                    setBackgroundResource(R.drawable.button_green_outlined)
                    text = requireContext().getText(R.string.online)
                }
            }
        }
        binding.eventTypeButton.setOnClickListener {
            editEventViewModel.switchEventType()
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
                    editEventViewModel.removeAttachment()
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

    private fun addSpeaker(user: User) {
        editEventViewModel.addSpeakerUser(user)
    }

    private fun setupUserAdapter() {
        usersAdapter = UserAdapter(object : UserAdapter.OnInteractionListener {
            override fun onItem(user: User) {
                addSpeaker(user)
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
        editEventViewModel.setContent(content)
    }

    private fun saveLink() {
        val link = binding.link.text.toString()
        if (link.isBlank()) {
            return
        }
        editEventViewModel.setLink(link)
    }

    private fun setupSpeakers(event: EventRequest) {
        with(binding) {
            val spannableStringBuilder = SpannableStringBuilder()
            event.speakerIds.forEachIndexed { index, userId ->
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

                val userPreview = event.speakerUsers.first { it.id == userId }
                spannableStringBuilder.append(
                    userPreview.name,
                    onUserClickableSpan,
                    SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                if (index < event.speakerIds.size - 1) {
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
                    AndroidUtils.hideKeyboard(requireView())
                    binding.usersContainer.visibility = View.VISIBLE
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                }
            }

            spannableStringBuilder.append(
                getString(R.string.add_speaker),
                addClickableSpan,
                SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            speakers.movementMethod = LinkMovementMethod.getInstance()
            speakers.text = spannableStringBuilder
        }
    }

    private fun setupDateTimeButton(event: EventRequest) {
        with(binding.datetimeButton) {
            setOnClickListener {
                showDatePickerDialog()
            }
            if (event.localDateTime == null) {
                text = requireContext().getText(R.string.set_time)
                setTextColor(requireContext().getColor(R.color.blue_800))
            } else {
                val dateTime =
                    DateTimeConverter.datetimeToUiDateTime(event.localDateTime.toString())
                text = dateTime
                setTextColor(requireContext().getColor(R.color.black))
            }
        }
    }

    private fun onUser(userId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val user = usersViewModel.getUserById(userId)
            usersViewModel.setCurrentUser(user)
            findNavController().navigate(R.id.action_editEventFragment_to_userProfileFragment)
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

    private fun checkCoords(): Boolean {
        val coords = binding.coords.text
        if (coords.isNotBlank()) {
            val pattern = """^-?\d+\.\d+,\s?-?\d+\.\d+$""".toRegex()
            if (!pattern.matches(coords)) {
                binding.error.visibility = View.VISIBLE
                binding.error.text = getString(R.string.invalid_coordinates_format)
                return false
            }
        }
        return true
    }

    private fun checkDate(event: EventRequest): Boolean {
        val date = event.localDateTime
        if (date == null) {
            binding.error.visibility = View.VISIBLE
            binding.error.text = getString(R.string.please_choose_event_date)
            return false
        }
        val now = LocalDateTime.now()
        if (date.isBefore(now)) {
            binding.error.visibility = View.VISIBLE
            binding.error.text = getString(R.string.please_choose_a_future_date)
            return false
        }
        return true
    }

    private fun showDatePickerDialog() {
        val now = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog.newInstance(
            this@EditEventFragment,
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show(childFragmentManager, "DatePickerDialog")
    }

    private fun saveCoords() {
        val coords = binding.coords.text.toString()
        if (coords.isBlank()) {
            return
        }
        editEventViewModel.setCoords(coords)
    }

    private fun sendData() {
        val content = binding.editContent.text.toString()
        saveContent(content)
        saveLink()
        saveCoords()
        editEventViewModel.saveEvent()
    }

    override fun onDestroy() {
        super.onDestroy()
        editEventViewModel.clear()
        _binding = null
    }

    override fun onDateSet(
        view: DatePickerDialog?, year: Int, monthOfYear: Int, dayOfMonth: Int
    ) {
        val now = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog.newInstance(
            this@EditEventFragment,
            now.get(Calendar.HOUR_OF_DAY),
            now.get(Calendar.MINUTE),
            now.get(Calendar.SECOND),
            true
        )

        val date = LocalDateTime.of(year, monthOfYear + 1, dayOfMonth, 0, 0)
        editEventViewModel.setLocalDateTime(date)
        timePickerDialog.show(childFragmentManager, "TimePickerDialog")
    }

    override fun onTimeSet(view: TimePickerDialog?, hourOfDay: Int, minute: Int, second: Int) {
        val date = editEventViewModel.eventRequest.value?.localDateTime
        val datetime = date?.withHour(hourOfDay)?.withMinute(minute)
        datetime?.let { editEventViewModel.setLocalDateTime(it) }
    }
}