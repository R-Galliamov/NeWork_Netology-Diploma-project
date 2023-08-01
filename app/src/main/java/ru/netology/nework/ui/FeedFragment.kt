package ru.netology.nework.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.databinding.FragmentFeedBinding
import ru.netology.nework.viewModel.PostViewModel

@AndroidEntryPoint
class FeedFragment : Fragment() {

    private var binding: FragmentFeedBinding? = null
    private val viewModel: PostViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.loadPosts()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}