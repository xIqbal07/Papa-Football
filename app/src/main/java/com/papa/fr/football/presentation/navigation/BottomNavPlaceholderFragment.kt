package com.papa.fr.football.presentation.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.papa.fr.football.R
import com.papa.fr.football.databinding.FragmentPlaceholderBinding

private const val ARG_TITLE = "arg_title"

class BottomNavPlaceholderFragment : Fragment() {

    private var _binding: FragmentPlaceholderBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaceholderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val title = requireArguments().getString(ARG_TITLE).orEmpty()
        val message = getString(R.string.bottom_nav_placeholder_message, title)
        binding.tvPlaceholder.text = message
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(title: String): BottomNavPlaceholderFragment {
            return BottomNavPlaceholderFragment().apply {
                arguments = bundleOf(ARG_TITLE to title)
            }
        }
    }
}
