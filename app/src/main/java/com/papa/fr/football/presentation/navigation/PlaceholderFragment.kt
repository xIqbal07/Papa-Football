package com.papa.fr.football.presentation.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.papa.fr.football.databinding.FragmentPlaceholderBinding

class PlaceholderFragment : Fragment() {

    private var _binding: FragmentPlaceholderBinding? = null
    private val binding: FragmentPlaceholderBinding
        get() = requireNotNull(_binding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPlaceholderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val message = requireArguments().getString(ARG_MESSAGE).orEmpty()
        binding.tvPlaceholder.text = message
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_MESSAGE = "arg_message"

        fun newInstance(message: String): PlaceholderFragment {
            return PlaceholderFragment().apply {
                arguments = bundleOf(ARG_MESSAGE to message)
            }
        }

        fun tagFor(itemId: Int): String = "placeholder_$itemId"
    }
}
