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

private const val DEFAULT_UNIQUE_TOURNAMENT_ID = 8

class ScheduleFragment : Fragment() {

    private var _binding: FragmentScheduleBinding? = null
    private val binding get() = _binding!!

    private val seasonsViewModel: SeasonsViewModel by viewModel()

    private var lastSeasonIds: List<Int> = emptyList()
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

        binding.ddSeason.setPlaceholder(defaultSeasonLabel())

        binding.btnSchedule.setOnClickListener {
            seasonsViewModel.loadSeasons(DEFAULT_UNIQUE_TOURNAMENT_ID)
        }

        if (seasonsViewModel.uiState.value.seasons.isEmpty()) {
            seasonsViewModel.loadSeasons(DEFAULT_UNIQUE_TOURNAMENT_ID)
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
                    binding.ddSeason.isEnabled = !state.isLoading
                    binding.btnSchedule.isEnabled = !state.isLoading

                    val seasonIds = state.seasons.map { it.id }
                    if (seasonIds != lastSeasonIds && state.seasons.isNotEmpty()) {
                        lastSeasonIds = seasonIds
                        val items = state.seasons.map { season ->
                            LeagueItem(
                                id = season.id.toString(),
                                name = season.name,
                                iconRes = R.drawable.ic_nav_schedule
                            )
                        }
                        binding.ddSeason.setData(items)
                    }

                    val errorMessage = state.errorMessage?.ifBlank { getString(R.string.season_load_error) }
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
}
