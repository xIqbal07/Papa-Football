package com.papa.fr.football.presentation.schedule

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.papa.fr.football.R
import com.papa.fr.football.common.dropdown.LeagueItem
import com.papa.fr.football.common.matches.MatchesTabLayoutView
import com.papa.fr.football.databinding.FragmentScheduleBinding
import com.papa.fr.football.domain.model.Season
import com.papa.fr.football.matches.MatchesListFragment
import com.papa.fr.football.matches.MatchesTabType
import com.papa.fr.football.presentation.seasons.SeasonsViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class ScheduleFragment : Fragment(R.layout.fragment_schedule) {

    private var _binding: FragmentScheduleBinding? = null
    private val binding get() = _binding!!

    private val seasonsViewModel: SeasonsViewModel by viewModel()

    private var displayedSeasons: List<Season> = emptyList()
    private var lastErrorMessage: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentScheduleBinding.bind(view)

        setupMatchesTabs()
        observeSeasons()
        setupInteractions()

        if (seasonsViewModel.uiState.value.seasons.isEmpty() && !seasonsViewModel.uiState.value.isLoading) {
            seasonsViewModel.loadSeasons(DEFAULT_UNIQUE_TOURNAMENT_ID)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        displayedSeasons = emptyList()
        lastErrorMessage = null
    }

    private fun setupMatchesTabs() {
        binding.matchesTabs.setupWith(
            fragmentActivity = requireActivity(),
            tabs = listOf(
                MatchesTabLayoutView.TabItem(getString(R.string.matches_tab_future)) {
                    MatchesListFragment.newInstance(MatchesTabType.FUTURE)
                },
                MatchesTabLayoutView.TabItem(getString(R.string.matches_tab_live)) {
                    MatchesListFragment.newInstance(MatchesTabType.LIVE)
                },
                MatchesTabLayoutView.TabItem(getString(R.string.matches_tab_past)) {
                    MatchesListFragment.newInstance(MatchesTabType.PAST)
                }
            )
        )
    }

    private fun setupInteractions() {
        binding.btnSchedule.setOnClickListener {
            if (!seasonsViewModel.uiState.value.isLoading) {
                seasonsViewModel.loadSeasons(DEFAULT_UNIQUE_TOURNAMENT_ID)
            }
        }
    }

    private fun observeSeasons() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                seasonsViewModel.uiState.collect { state ->
                    handleLoadingState(state.isLoading)
                    renderSeasons(state.seasons)
                    renderError(state.errorMessage)
                }
            }
        }
    }

    private fun handleLoadingState(isLoading: Boolean) {
        binding.btnSchedule.isEnabled = !isLoading
        binding.ddSeason.isEnabled = !isLoading
    }

    private fun renderSeasons(seasons: List<Season>) {
        if (displayedSeasons == seasons) return
        displayedSeasons = seasons

        if (seasons.isEmpty()) {
            binding.ddSeason.setData(emptyList())
            binding.ddSeason.setSelected(null)
            return
        }

        val items = seasons.map { season ->
            LeagueItem(
                id = season.id.toString(),
                name = season.name,
                iconRes = R.drawable.ic_nav_schedule
            )
        }
        binding.ddSeason.setData(items)
    }

    private fun renderError(errorMessage: String?) {
        if (errorMessage == null) {
            lastErrorMessage = null
            return
        }

        val message = errorMessage.takeUnless { it.isBlank() }
            ?: getString(R.string.seasons_generic_error)

        if (message == lastErrorMessage) return
        lastErrorMessage = message
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val DEFAULT_UNIQUE_TOURNAMENT_ID = 8
    }
}
