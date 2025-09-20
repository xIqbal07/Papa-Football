package com.papa.fr.football.presentation

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.papa.fr.football.R
import com.papa.fr.football.databinding.ActivityMainBinding
import com.papa.fr.football.presentation.navigation.PlaceholderFragment
import com.papa.fr.football.presentation.schedule.ScheduleFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var selectedItemId: Int = R.id.menu_schedule
    private val navigationDestinations by lazy {
        listOf(
            NavigationDestination(
                id = R.id.menu_schedule,
                tag = ScheduleFragment.TAG,
                fragmentProvider = { ScheduleFragment() },
            ),
            placeholderDestination(
                itemId = R.id.menu_highlights,
                titleRes = R.string.bottom_nav_highlights,
            ),
            placeholderDestination(
                itemId = R.id.menu_teams,
                titleRes = R.string.bottom_nav_teams,
            ),
            placeholderDestination(
                itemId = R.id.menu_standings,
                titleRes = R.string.bottom_nav_standings,
            ),
            placeholderDestination(
                itemId = R.id.menu_settings,
                titleRes = R.string.bottom_nav_settings,
            )
        ).associateBy { it.id }
    }

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
        val destination = navigationDestinations[itemId] ?: return false
        val fragment = supportFragmentManager.findFragmentByTag(destination.tag)
            ?: destination.fragmentProvider()
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragment_container, fragment, destination.tag)
        }
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SELECTED_ITEM, selectedItemId)
    }

    fun setBottomNavVisible(isVisible: Boolean) {
        binding.bottomNavigation.isVisible = isVisible
    }

    private fun placeholderDestination(
        @IdRes itemId: Int,
        @StringRes titleRes: Int,
    ): NavigationDestination {
        return NavigationDestination(
            id = itemId,
            tag = PlaceholderFragment.tagFor(itemId),
            fragmentProvider = {
                val sectionName = getString(titleRes)
                PlaceholderFragment.newInstance(
                    getString(R.string.placeholder_coming_soon, sectionName)
                )
            }
        )
    }

    private data class NavigationDestination(
        @IdRes val id: Int,
        val tag: String,
        val fragmentProvider: () -> Fragment,
    )

    companion object {
        private const val KEY_SELECTED_ITEM = "key_selected_item"
    }
}