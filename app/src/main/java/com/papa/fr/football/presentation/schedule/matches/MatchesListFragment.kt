package com.papa.fr.football.presentation.schedule.matches

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
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
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import java.util.Locale

class MatchesListFragment : Fragment() {

    private var _binding: FragmentMatchesListBinding? = null
    private val binding: FragmentMatchesListBinding
        get() = requireNotNull(_binding)

    private val scheduleViewModel: ScheduleViewModel by activityViewModel(
        extrasProducer = { defaultViewModelCreationExtras }
    )
    private val matchesAdapter = MatchesAdapter()
    private var lastErrorMessage: String? = null
    lateinit var matchesType: MatchesTabType

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        matchesType = MatchesTabType.fromStorageKey(
            requireArguments().getString(ARG_TYPE)
        )
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
        val matches = matchesFor(state)

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

        val placeholderText = placeholderTextFor(state)

        val showPlaceholder = matches.isEmpty()
        binding.tvPlaceholder.isVisible = showPlaceholder
        if (showPlaceholder) {
            binding.tvPlaceholder.text = placeholderText
        }
        binding.rvMatches.isVisible = matches.isNotEmpty()

        val futureErrorMessage = resolveFutureError(state)
        if (futureErrorMessage != null && futureErrorMessage != lastErrorMessage) {
            lastErrorMessage = futureErrorMessage
            Snackbar.make(binding.root, futureErrorMessage, Snackbar.LENGTH_LONG).show()
        } else if (futureErrorMessage == null) {
            lastErrorMessage = null
        }
    }

    companion object {
        private const val ARG_TYPE = "arg_type"

        fun newInstance(type: MatchesTabType): MatchesListFragment {
            return MatchesListFragment().apply {
                arguments = bundleOf(ARG_TYPE to type.storageKey)
            }
        }
    }
}

sealed interface MatchesTabType {
    @get:StringRes
    val titleRes: Int
    val storageKey: String

    object Future : MatchesTabType {
        override val titleRes: Int = R.string.matches_tab_future
        override val storageKey: String = "future"
    }

    object Live : MatchesTabType {
        override val titleRes: Int = R.string.matches_tab_live
        override val storageKey: String = "live"
    }

    object Past : MatchesTabType {
        override val titleRes: Int = R.string.matches_tab_past
        override val storageKey: String = "past"
    }

    companion object {
        val all: List<MatchesTabType> = listOf(Future, Live, Past)

        fun fromStorageKey(key: String?): MatchesTabType {
            return all.firstOrNull { it.storageKey == key } ?: Future
        }
    }
}

private fun MatchesListFragment.matchesFor(state: ScheduleUiState): List<MatchUiModel> = when (matchesType) {
    MatchesTabType.Future -> state.futureMatches
    MatchesTabType.Live, MatchesTabType.Past -> emptyList()
}

private fun MatchesListFragment.placeholderTextFor(state: ScheduleUiState): String = when (matchesType) {
    MatchesTabType.Future -> when {
        state.isMatchesLoading -> getString(R.string.matches_placeholder_loading)
        !state.matchesErrorMessage.isNullOrBlank() -> state.matchesErrorMessage
        else -> getString(R.string.matches_placeholder_empty)
    }

    MatchesTabType.Live, MatchesTabType.Past -> getString(
        R.string.matches_placeholder_format,
        matchesType.storageKey.lowercase(Locale.getDefault())
    )
}

private fun MatchesListFragment.resolveFutureError(state: ScheduleUiState): String? {
    if (matchesType != MatchesTabType.Future) {
        return null
    }
    return state.matchesErrorMessage
        ?.ifBlank { getString(R.string.matches_placeholder_empty) }
        ?.takeIf { it.isNotBlank() }
}
