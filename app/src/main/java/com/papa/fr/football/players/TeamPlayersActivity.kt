package com.papa.fr.football.players

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.papa.fr.football.data.TeamsRepository
import com.papa.fr.football.databinding.ActivityTeamPlayersBinding

class TeamPlayersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTeamPlayersBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityTeamPlayersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val leagueId = intent.getStringExtra(EXTRA_LEAGUE_ID)
        val teamId = intent.getStringExtra(EXTRA_TEAM_ID)
        val team = if (leagueId != null && teamId != null) {
            TeamsRepository.getTeam(leagueId, teamId)
        } else {
            null
        }

        if (team == null) {
            finish()
            return
        }

        binding.toolbar.title = team.name
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val adapter = PlayersAdapter()
        binding.rvPlayers.layoutManager = LinearLayoutManager(this)
        binding.rvPlayers.adapter = adapter
        adapter.submitList(team.players)

        binding.rvPlayers.isVisible = team.players.isNotEmpty()
        binding.tvEmpty.isVisible = team.players.isEmpty()
    }

    companion object {
        private const val EXTRA_LEAGUE_ID = "extra_league_id"
        private const val EXTRA_TEAM_ID = "extra_team_id"

        fun createIntent(context: Context, leagueId: String, teamId: String): Intent {
            return Intent(context, TeamPlayersActivity::class.java).apply {
                putExtra(EXTRA_LEAGUE_ID, leagueId)
                putExtra(EXTRA_TEAM_ID, teamId)
            }
        }
    }
}
