package com.papa.fr.football

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.papa.fr.football.databinding.ActivityMainBinding
import com.papa.fr.football.presentation.navigation.BottomNavPlaceholderFragment
import com.papa.fr.football.presentation.schedule.ScheduleFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var selectedItemId: Int = R.id.menu_schedule

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        selectedItemId = savedInstanceState?.getInt(STATE_SELECTED_ITEM) ?: R.id.menu_schedule

        setupWindowInsets()
        setupBottomNav(binding.bottomNavigation)

        if (savedInstanceState == null) {
            binding.bottomNavigation.selectedItemId = selectedItemId
        } else {
            navigateTo(selectedItemId)
            binding.bottomNavigation.selectedItemId = selectedItemId
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_SELECTED_ITEM, selectedItemId)
    }

    private fun setupWindowInsets() {
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
    }

    private fun setupBottomNav(bottomNav: BottomNavigationView) {
        bottomNav.setOnItemSelectedListener { item ->
            val handled = navigateTo(item.itemId)
            if (handled) {
                selectedItemId = item.itemId
            }
            handled
        }
        bottomNav.setOnItemReselectedListener { /* no-op */ }
    }

    private fun navigateTo(itemId: Int): Boolean {
        val tag = itemId.toString()
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()

        fragmentManager.fragments
            .filter { it.id == R.id.fragment_container }
            .forEach { transaction.hide(it) }

        var fragment = fragmentManager.findFragmentByTag(tag)
        if (fragment == null) {
            fragment = when (itemId) {
                R.id.menu_schedule -> ScheduleFragment()
                R.id.menu_highlights -> BottomNavPlaceholderFragment.newInstance(getString(R.string.bottom_nav_highlights))
                R.id.menu_teams -> BottomNavPlaceholderFragment.newInstance(getString(R.string.bottom_nav_teams))
                R.id.menu_standings -> BottomNavPlaceholderFragment.newInstance(getString(R.string.bottom_nav_standings))
                R.id.menu_settings -> BottomNavPlaceholderFragment.newInstance(getString(R.string.bottom_nav_settings))
                else -> return false
            }
            transaction.add(R.id.fragment_container, fragment, tag)
        } else {
            transaction.show(fragment)
        }

        transaction.setReorderingAllowed(true)
        transaction.commit()
        return true
    }

    companion object {
        private const val STATE_SELECTED_ITEM = "state_selected_item"
    }
}
