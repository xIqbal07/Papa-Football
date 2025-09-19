package com.papa.fr.football.presentation.schedule.matches

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.papa.fr.football.R
import com.papa.fr.football.databinding.FragmentMatchesListBinding
import com.papa.fr.football.presentation.schedule.ScheduleUiState
import com.papa.fr.football.presentation.schedule.ScheduleViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.util.Locale

class MatchesListFragment : Fragment() {

    private var _binding: FragmentMatchesListBinding? = null
    private val binding: FragmentMatchesListBinding
        get() = requireNotNull(_binding)

    private val scheduleViewModel: ScheduleViewModel by sharedViewModel()
    private val matchesAdapter = MatchesAdapter()
    private var lastErrorMessage: String? = null
    private lateinit var matchesType: MatchesTabType

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        matchesType = requireArguments().getString(ARG_TYPE)
            ?.let { runCatching { MatchesTabType.valueOf(it) }.getOrNull() }
            ?: MatchesTabType.FUTURE
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMatchesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvMatches.adapter = matchesAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                scheduleViewModel.uiState.collect { state ->
                    updateMatches(state)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvMatches.adapter = null
        _binding = null
    }

    private fun updateMatches(state: ScheduleUiState) {
        val matches = when (matchesType) {
            MatchesTabType.FUTURE -> state.futureMatches
            MatchesTabType.LIVE, MatchesTabType.PAST -> emptyList()
        }

        val shouldPreserveScroll = matchesAdapter.currentList.isNotEmpty() &&
                matchesAdapter.currentList.map { it.id } == matches.map { it.id }

        val savedState: Parcelable? = if (shouldPreserveScroll) {
            binding.rvMatches.layoutManager?.onSaveInstanceState()
        } else {
            null
        }

        matchesAdapter.submitMatches(matches) {
            if (savedState != null) {
                binding.rvMatches.layoutManager?.onRestoreInstanceState(savedState)
            }
        }

        val placeholderText = when {
            matchesType == MatchesTabType.FUTURE && state.isMatchesLoading ->
                getString(R.string.matches_placeholder_loading)

            matchesType == MatchesTabType.FUTURE && !state.matchesErrorMessage.isNullOrBlank() ->
                state.matchesErrorMessage

            matchesType == MatchesTabType.FUTURE -> getString(R.string.matches_placeholder_empty)

            else -> getString(
                R.string.matches_placeholder_format,
                matchesType.name.lowercase(Locale.getDefault())
            )
        }

        val showPlaceholder = matches.isEmpty()
        binding.tvPlaceholder.isVisible = showPlaceholder
        if (showPlaceholder) {
            binding.tvPlaceholder.text = placeholderText
        }
        binding.rvMatches.isVisible = matches.isNotEmpty()

        if (matchesType == MatchesTabType.FUTURE) {
            val errorMessage =
                state.matchesErrorMessage?.ifBlank { getString(R.string.matches_placeholder_empty) }
            if (!errorMessage.isNullOrBlank() && errorMessage != lastErrorMessage) {
                lastErrorMessage = errorMessage
                Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()
            } else if (errorMessage.isNullOrBlank()) {
                lastErrorMessage = null
            }
        }
    }

    companion object {
        private const val ARG_TYPE = "arg_type"

        fun newInstance(type: MatchesTabType): MatchesListFragment {
            return MatchesListFragment().apply {
                arguments = bundleOf(ARG_TYPE to type.name)
            }
        }
    }
}

enum class MatchesTabType {
    FUTURE,
    LIVE,
    PAST
}
