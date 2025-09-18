package com.papa.fr.football.presentation.placeholder

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.papa.fr.football.R
import com.papa.fr.football.databinding.FragmentPlaceholderBinding

class PlaceholderFragment : Fragment(R.layout.fragment_placeholder) {

    private var _binding: FragmentPlaceholderBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPlaceholderBinding.bind(view)

        val title = requireArguments().getInt(ARG_TITLE_RES)
        binding.tvPlaceholder.text = getString(R.string.placeholder_screen_format, getString(title))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TITLE_RES = "title_res"

        fun newInstance(@StringRes titleRes: Int): PlaceholderFragment {
            return PlaceholderFragment().apply {
                arguments = bundleOf(ARG_TITLE_RES to titleRes)
            }
        }
    }
}
