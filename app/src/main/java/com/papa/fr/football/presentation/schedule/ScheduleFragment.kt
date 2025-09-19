package com.papa.fr.football.presentation.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.papa.fr.football.R
import com.papa.fr.football.common.dropdown.LeagueItem
import com.papa.fr.football.common.matches.MatchesTabLayoutView
import com.papa.fr.football.databinding.FragmentScheduleBinding
import com.papa.fr.football.matches.MatchesListFragment
import com.papa.fr.football.matches.MatchesTabType
import com.papa.fr.football.presentation.seasons.SeasonsViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Calendar
import java.util.Locale

class ScheduleFragment : Fragment() {

    private var _binding: FragmentScheduleBinding? = null
    private val binding get() = _binding!!

    private val seasonsViewModel: SeasonsViewModel by viewModel()

    private var lastSeasonIdsByLeague: Map<Int, List<Int>> = emptyMap()
    private var lastErrorMessage: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMatchesTabs()
        observeSeasons()
        observeLeagues()

        binding.ddLeague.setOnChangedListener { league ->
            updateSeasonDropdown(league.id)
        }

        binding.ddSeason.setPlaceholder(defaultSeasonLabel())
        binding.ddLeague.setPlaceholder(seasonsViewModel.defaultLeagueLabel())

        binding.btnSchedule.setOnClickListener {
            val selectedLeagueId = binding.ddLeague.getSelected()?.id
            if (selectedLeagueId != null) {
                seasonsViewModel.loadSeasonsForLeague(selectedLeagueId)
            } else {
                seasonsViewModel.loadAllLeagueSeasons()
            }
        }

        if (seasonsViewModel.uiState.value.seasonsByLeague.isEmpty()) {
            seasonsViewModel.loadAllLeagueSeasons()
        }
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

    private fun observeSeasons() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                seasonsViewModel.uiState.collect { state ->
                    val seasonIdsByLeague = state.seasonsByLeague.mapValues { entry ->
                        entry.value.map { it.id }
                    }
                    if (seasonIdsByLeague != lastSeasonIdsByLeague) {
                        lastSeasonIdsByLeague = seasonIdsByLeague
                        updateSeasonDropdown(binding.ddLeague.getSelected()?.id)
                    }

                    val errorMessage =
                        state.errorMessage?.ifBlank { getString(R.string.season_load_error) }
                    if (errorMessage != null && errorMessage != lastErrorMessage) {
                        lastErrorMessage = errorMessage
                        Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()
                    } else if (errorMessage == null) {
                        lastErrorMessage = null
                    }
                }
            }
        }
    }

    private fun observeLeagues() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                seasonsViewModel.leagueItems.collect { leagues ->
                    if (leagues.isNotEmpty()) {
                        binding.ddLeague.setData(leagues)
                        if (binding.ddLeague.getSelected() == null) {
                            binding.ddLeague.setSelected(leagues.first())
                            updateSeasonDropdown(leagues.first().id)
                        }
                    }
                }
            }
        }
    }

    private fun updateSeasonDropdown(leagueId: Int?) {
        val seasons = leagueId?.let { seasonsViewModel.seasonsForLeague(it) }.orEmpty()
        if (seasons.isEmpty()) {
            binding.ddSeason.setData(emptyList())
            binding.ddSeason.setPlaceholder(defaultSeasonLabel())
            return
        }

        val seasonItems = seasons.map { season ->
            LeagueItem(
                id = season.id,
                name = season.year.orEmpty()
            )
        }
        binding.ddSeason.setData(seasonItems)
    }

    private fun defaultSeasonLabel(): String {
        val calendar = Calendar.getInstance()
        val startYear = calendar.get(Calendar.YEAR) % 100
        val endYear = (startYear + 1) % 100
        return String.format(Locale.getDefault(), "%02d/%02d", startYear, endYear)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ScheduleFragment"
    }
}
