package com.papa.fr.football

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.commit
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.papa.fr.football.databinding.ActivityMainBinding
import com.papa.fr.football.presentation.navigation.PlaceholderFragment
import com.papa.fr.football.presentation.schedule.ScheduleFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var selectedItemId: Int = R.id.menu_schedule

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigation) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, systemBars.bottom)
            insets
        }

        selectedItemId = savedInstanceState?.getInt(KEY_SELECTED_ITEM) ?: R.id.menu_schedule
        setupBottomNav(binding.bottomNavigation)
        if (savedInstanceState == null) {
            navigateTo(selectedItemId)
        }
        binding.bottomNavigation.selectedItemId = selectedItemId
    }

    private fun setupBottomNav(bottomNav: BottomNavigationView) {
        bottomNav.setOnItemSelectedListener { item ->
            if (selectedItemId == item.itemId) {
                return@setOnItemSelectedListener true
            }

            val handled = navigateTo(item.itemId)
            if (handled) {
                selectedItemId = item.itemId
            }
            handled
        }
    }

    private fun navigateTo(@IdRes itemId: Int): Boolean {
        val (tag, fragmentProvider) = when (itemId) {
            R.id.menu_schedule -> ScheduleFragment.TAG to { ScheduleFragment() }
            R.id.menu_highlights -> PlaceholderFragment.tagFor(itemId) to {
                PlaceholderFragment.newInstance(
                    getString(
                        R.string.placeholder_coming_soon,
                        getString(R.string.bottom_nav_highlights)
                    )
                )
            }

            R.id.menu_teams -> PlaceholderFragment.tagFor(itemId) to {
                PlaceholderFragment.newInstance(
                    getString(
                        R.string.placeholder_coming_soon,
                        getString(R.string.bottom_nav_teams)
                    )
                )
            }

            R.id.menu_standings -> PlaceholderFragment.tagFor(itemId) to {
                PlaceholderFragment.newInstance(
                    getString(
                        R.string.placeholder_coming_soon,
                        getString(R.string.bottom_nav_standings)
                    )
                )
            }

            R.id.menu_settings -> PlaceholderFragment.tagFor(itemId) to {
                PlaceholderFragment.newInstance(
                    getString(
                        R.string.placeholder_coming_soon,
                        getString(R.string.bottom_nav_settings)
                    )
                )
            }

            else -> return false
        }

        val fragment = supportFragmentManager.findFragmentByTag(tag) ?: fragmentProvider()
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragment_container, fragment, tag)
        }
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SELECTED_ITEM, selectedItemId)
    }

    companion object {
        private const val KEY_SELECTED_ITEM = "key_selected_item"
    }
}