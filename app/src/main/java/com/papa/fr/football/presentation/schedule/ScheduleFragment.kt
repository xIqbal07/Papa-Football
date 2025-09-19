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
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Calendar
import java.util.Locale

class ScheduleFragment : Fragment() {

    private var _binding: FragmentScheduleBinding? = null
    private val binding get() = _binding!!

    private val scheduleViewModel: ScheduleViewModel by viewModel()

    private var lastSeasonIdsByLeague: Map<Int, List<Int>> = emptyMap()
    private var lastErrorMessage: String? = null
    private var lastSelectedLeagueId: Int? = null

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
            scheduleViewModel.onLeagueSelected(league.id)
            updateSeasonDropdown(league.id)
        }

        binding.ddSeason.setOnChangedListener { season ->
            scheduleViewModel.onSeasonSelected(season.id)
        }

        binding.ddSeason.setPlaceholder(defaultSeasonLabel())
        binding.ddLeague.setPlaceholder(scheduleViewModel.defaultLeagueLabel())

        binding.btnSchedule.setOnClickListener {
            scheduleViewModel.refreshSchedule()
        }

        if (scheduleViewModel.uiState.value.seasonsByLeague.isEmpty()) {
            scheduleViewModel.loadAllLeagueSeasons()
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
                scheduleViewModel.uiState.collect { state ->
                    val seasonIdsByLeague = state.seasonsByLeague.mapValues { entry ->
                        entry.value.map { it.id }
                    }
                    if (seasonIdsByLeague != lastSeasonIdsByLeague) {
                        lastSeasonIdsByLeague = seasonIdsByLeague
                        updateSeasonDropdown(state.selectedLeagueId)
                    }

                    val selectedLeagueId = state.selectedLeagueId
                    if (selectedLeagueId != null && selectedLeagueId != lastSelectedLeagueId) {
                        lastSelectedLeagueId = selectedLeagueId
                        val leagueItem = scheduleViewModel.leagueItems.value
                            .firstOrNull { it.id == selectedLeagueId }
                        if (leagueItem != null) {
                            binding.ddLeague.setSelected(leagueItem)
                            updateSeasonDropdown(selectedLeagueId)
                        }
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
                scheduleViewModel.leagueItems.collect { leagues ->
                    if (leagues.isNotEmpty()) {
                        binding.ddLeague.setData(leagues)
                        val selectedLeagueId = scheduleViewModel.uiState.value.selectedLeagueId
                        when {
                            selectedLeagueId != null -> {
                                leagues.firstOrNull { it.id == selectedLeagueId }?.let {
                                    binding.ddLeague.setSelected(it)
                                }
                            }

                            binding.ddLeague.getSelected() == null -> {
                                val firstLeague = leagues.first()
                                binding.ddLeague.setSelected(firstLeague)
                                scheduleViewModel.onLeagueSelected(firstLeague.id)
                                updateSeasonDropdown(firstLeague.id)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateSeasonDropdown(leagueId: Int?) {
        val seasons = leagueId?.let { scheduleViewModel.seasonsForLeague(it) }.orEmpty()
        if (seasons.isEmpty()) {
            binding.ddSeason.setData(emptyList())
            binding.ddSeason.setPlaceholder(defaultSeasonLabel())
            return
        }

        val seasonItems = seasons.map { season ->
            LeagueItem(
                id = season.id,
                name = season.year?.takeIf { it.isNotBlank() } ?: season.name
            )
        }
        binding.ddSeason.setData(seasonItems)

        val selectedSeasonId = scheduleViewModel.uiState.value.selectedSeasonId
        val selectedItem = seasonItems.firstOrNull { it.id == selectedSeasonId }

        if (selectedItem != null) {
            binding.ddSeason.setSelected(selectedItem)
        } else {
            val firstItem = seasonItems.firstOrNull()
            if (firstItem != null) {
                binding.ddSeason.setSelected(firstItem)
                scheduleViewModel.onSeasonSelected(firstItem.id)
            }
        }
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
