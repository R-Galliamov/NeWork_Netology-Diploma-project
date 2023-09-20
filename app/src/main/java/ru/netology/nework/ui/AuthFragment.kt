package ru.netology.nework.ui

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toFile
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.github.dhaval2404.imagepicker.ImagePicker
import com.github.dhaval2404.imagepicker.constant.ImageProvider
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.netology.nework.R
import ru.netology.nework.databinding.FragmentAuthBinding
import ru.netology.nework.error.ErrorHandler
import ru.netology.nework.model.requestModel.AuthenticationRequest
import ru.netology.nework.model.requestModel.RegistrationRequest
import ru.netology.nework.util.AndroidUtils
import ru.netology.nework.view.loadCircleCropAvatar
import ru.netology.nework.viewModel.AuthViewModel
import javax.inject.Inject

@AndroidEntryPoint
class AuthFragment : Fragment() {

    @Inject
    lateinit var androidUtils: AndroidUtils
    @Inject
    lateinit var errorHandler: ErrorHandler

    private var _binding: FragmentAuthBinding? = null
    private val binding: FragmentAuthBinding
        get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels()
    private val pickPhotoLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            when (it.resultCode) {
                ImagePicker.RESULT_ERROR -> {
                    Snackbar.make(
                        binding.root, ImagePicker.getError(it.data), Snackbar.LENGTH_LONG
                    ).show()
                }

                Activity.RESULT_OK -> authViewModel.changePhoto(it.data?.data)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.doneIcon.visibility = View.GONE

        authViewModel.authProcess.observe(viewLifecycleOwner) { state ->
            if (state == AuthViewModel.AuthProcess.AUTHENTICATION) {
                binding.apply {
                    processText.text = getString(R.string.authentication)
                    avatar.visibility = View.GONE
                    userName.visibility = View.GONE
                    sendDataButton.text = getString(R.string.sign_in)
                    toggleButton.text = getString(R.string.sign_up)
                }
            } else if (state == AuthViewModel.AuthProcess.REGISTRATION) {
                binding.apply {
                    processText.text = getString(R.string.registration)
                    avatar.visibility = View.VISIBLE
                    userName.visibility = View.VISIBLE
                    sendDataButton.text = getString(R.string.sign_up)
                    toggleButton.text = getString(R.string.sign_in)
                }
            }
            binding.sendDataButton.setOnClickListener {
                androidUtils.hideKeyboard(requireView())
                if (binding.login.text.isNotBlank() || binding.password.text.isNotBlank() || (authViewModel.authProcess.value == AuthViewModel.AuthProcess.REGISTRATION && binding.userName.text.isNotBlank())) {
                    binding.error.visibility = View.GONE
                    val login = binding.login.text.toString()
                    val password = binding.password.text.toString()
                    if (authViewModel.authProcess.value == AuthViewModel.AuthProcess.AUTHENTICATION) {
                        authViewModel.signInUser(AuthenticationRequest(login, password))
                    } else if (authViewModel.authProcess.value == AuthViewModel.AuthProcess.REGISTRATION) {
                        val name = binding.userName.text.toString()
                        val avatarUri = authViewModel.photo.value?.uri
                        authViewModel.signUpUser(
                            RegistrationRequest(
                                login, password, name, avatarUri?.toFile()
                            )
                        )
                    }
                } else {
                    binding.error.text = getString(R.string.fields_shouldn_t_be_empty)
                    binding.error.visibility = View.VISIBLE
                }
            }

            binding.avatar.setOnClickListener {
                ImagePicker.with(this).crop().compress(2048).provider(ImageProvider.GALLERY)
                    .galleryMimeTypes(
                        arrayOf(
                            "image/png",
                            "image/jpeg",
                        )
                    ).createIntent(pickPhotoLauncher::launch)
            }

            authViewModel.photo.observe(viewLifecycleOwner) {
                binding.avatar.loadCircleCropAvatar(it.uri.toString())
            }

            authViewModel.loadState.observe(viewLifecycleOwner) { state ->
                binding.apply {
                    authDataContainer.isVisible = !state.loading
                    cancelButton.isVisible = !state.loading
                    progressBar.isVisible = state.loading
                }

                if (state.errorState) {
                    val errorDescription = errorHandler.getErrorDescriptor(state.errorStatus)
                    Toast.makeText(
                        requireContext(), errorDescription, Toast.LENGTH_SHORT
                    ).show()
                }
            }

            authViewModel.authData.observe(viewLifecycleOwner) { authState ->
                val isLoading = authViewModel.loadState.value!!.loading
                val authenticated = authViewModel.authenticated
                if (authenticated && !isLoading) {
                    binding.apply {
                        authDataContainer.isVisible = false
                        cancelButton.isVisible = false
                        progressBar.isVisible = false
                        binding.doneIcon.visibility = View.VISIBLE
                        viewLifecycleOwner.lifecycleScope.launch {
                            delay(500)
                            findNavController().navigateUp()
                        }
                    }
                }
            }

            binding.toggleButton.setOnClickListener {
                binding.error.visibility = View.GONE
                authViewModel.switchProcess()
            }

            binding.cancelButton.setOnClickListener {
                findNavController().navigateUp()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}