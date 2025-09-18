package com.papa.fr.football.matches

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.papa.fr.football.R
import com.papa.fr.football.databinding.FragmentMatchesListBinding

class MatchesListFragment : Fragment() {

    private var _binding: FragmentMatchesListBinding? = null
    private val binding: FragmentMatchesListBinding
        get() = requireNotNull(_binding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMatchesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tabLabel = requireArguments().getString(ARG_LABEL).orEmpty()
        binding.tvPlaceholder.text = getString(R.string.matches_placeholder_format, tabLabel)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_LABEL = "arg_label"

        fun newInstance(label: String): MatchesListFragment {
            return MatchesListFragment().apply {
                arguments = bundleOf(ARG_LABEL to label)
            }
        }
    }
}
