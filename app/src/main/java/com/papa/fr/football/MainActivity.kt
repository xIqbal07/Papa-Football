package com.papa.fr.football

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.papa.fr.football.databinding.ActivityMainBinding
import com.papa.fr.football.presentation.placeholder.PlaceholderFragment
import com.papa.fr.football.presentation.schedule.ScheduleFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

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

        val initialItem = savedInstanceState?.getInt(KEY_SELECTED_ITEM) ?: R.id.menu_schedule
        showFragment(initialItem)
        setupBottomNav(initialItem)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SELECTED_ITEM, binding.bottomNavigation.selectedItemId)
    }

    private fun setupBottomNav(initialItem: Int) {
        var ignoreInitialSelection = true

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (ignoreInitialSelection && item.itemId == initialItem) {
                ignoreInitialSelection = false
                return@setOnItemSelectedListener true
            }

            ignoreInitialSelection = false
            showFragment(item.itemId)
        }
        binding.bottomNavigation.setOnItemReselectedListener { /* no-op */ }

        if (binding.bottomNavigation.selectedItemId == initialItem) {
            ignoreInitialSelection = false
        } else {
            binding.bottomNavigation.selectedItemId = initialItem
        }
    }

    private fun showFragment(menuItemId: Int): Boolean {
        val tag = fragmentTagFor(menuItemId) ?: return false
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()

        FRAGMENT_TAGS.forEach { existingTag ->
            fragmentManager.findFragmentByTag(existingTag)?.let { transaction.hide(it) }
        }

        val fragment = fragmentManager.findFragmentByTag(tag) ?: createFragment(menuItemId).also {
            transaction.add(R.id.fragment_container, it, tag)
        }

        transaction.show(fragment)
        transaction.setReorderingAllowed(true)
        transaction.commit()
        return true
    }

    private fun createFragment(menuItemId: Int): Fragment = when (menuItemId) {
        R.id.menu_schedule -> ScheduleFragment()
        R.id.menu_highlights -> PlaceholderFragment.newInstance(R.string.bottom_nav_highlights)
        R.id.menu_teams -> PlaceholderFragment.newInstance(R.string.bottom_nav_teams)
        R.id.menu_standings -> PlaceholderFragment.newInstance(R.string.bottom_nav_standings)
        R.id.menu_settings -> PlaceholderFragment.newInstance(R.string.bottom_nav_settings)
        else -> ScheduleFragment()
    }

    private fun fragmentTagFor(menuItemId: Int): String? = when (menuItemId) {
        R.id.menu_schedule -> TAG_SCHEDULE
        R.id.menu_highlights -> TAG_HIGHLIGHTS
        R.id.menu_teams -> TAG_TEAMS
        R.id.menu_standings -> TAG_STANDINGS
        R.id.menu_settings -> TAG_SETTINGS
        else -> null
    }

    companion object {
        private const val KEY_SELECTED_ITEM = "selected_bottom_nav_item"
        private const val TAG_SCHEDULE = "tag_schedule"
        private const val TAG_HIGHLIGHTS = "tag_highlights"
        private const val TAG_TEAMS = "tag_teams"
        private const val TAG_STANDINGS = "tag_standings"
        private const val TAG_SETTINGS = "tag_settings"

        private val FRAGMENT_TAGS = listOf(
            TAG_SCHEDULE,
            TAG_HIGHLIGHTS,
            TAG_TEAMS,
            TAG_STANDINGS,
            TAG_SETTINGS
        )
    }
}
