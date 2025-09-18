package com.papa.fr.football

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.papa.fr.football.common.matches.MatchesTabLayoutView
import com.papa.fr.football.databinding.ActivityMainBinding
import com.papa.fr.football.matches.MatchesListFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

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
}