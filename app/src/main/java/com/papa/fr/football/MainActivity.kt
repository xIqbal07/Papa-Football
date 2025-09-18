package com.papa.fr.football

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.papa.fr.football.common.matches.MatchesTabLayoutView
import com.papa.fr.football.data.TeamsRepository
import com.papa.fr.football.databinding.ActivityMainBinding
import com.papa.fr.football.matches.MatchesListFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupDropdowns()

        binding.matchesTabs.setupWith(
            fragmentActivity = this,
            tabs = listOf(
                MatchesTabLayoutView.TabItem(getString(R.string.matches_tab_future)) {
                    MatchesListFragment.newInstance(getString(R.string.matches_tab_future))
                },
                MatchesTabLayoutView.TabItem(getString(R.string.matches_tab_live)) {
                    MatchesListFragment.newInstance(getString(R.string.matches_tab_live))
                },
                MatchesTabLayoutView.TabItem(getString(R.string.matches_tab_past)) {
                    MatchesListFragment.newInstance(getString(R.string.matches_tab_past))
                }
            )
        )
    }

    private fun setupDropdowns() {
        binding.ddSeason.setData(TeamsRepository.getSeasons())

        val leagues = TeamsRepository.getLeagues()
        binding.ddLeague.setData(leagues)
        binding.ddLeague.setOnChangedListener { league ->
            viewModel.selectLeague(league.id)
        }

        viewModel.selectedLeagueId.observe(this) { leagueId ->
            val selected = leagues.firstOrNull { it.id == leagueId }
            binding.ddLeague.setSelected(selected)
        }

        binding.ddLeague.getSelected()?.let { viewModel.selectLeague(it.id) }
    }
}