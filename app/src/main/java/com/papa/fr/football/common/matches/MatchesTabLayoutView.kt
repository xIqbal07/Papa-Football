package com.papa.fr.football.common.matches

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.papa.fr.football.R
import com.papa.fr.football.databinding.ItemMatchesTabBinding
import com.papa.fr.football.databinding.ViewMatchesTabLayoutBinding

class MatchesTabLayoutView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding =
        ViewMatchesTabLayoutBinding.inflate(LayoutInflater.from(context), this, true)
    private var mediator: TabLayoutMediator? = null
    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null

    init {
        context.withStyledAttributes(attrs, R.styleable.MatchesTabLayoutView, defStyleAttr, 0) {
            val titleText = getString(R.styleable.MatchesTabLayoutView_titleText)
                ?: context.getString(R.string.matches_title)
            binding.tvMatchesLabel.text = titleText
        }
        configureTabLayout()
    }

    private fun configureTabLayout() {
        binding.tabLayout.apply {
            tabMode = TabLayout.MODE_FIXED
            tabGravity = TabLayout.GRAVITY_FILL
            setSelectedTabIndicator(null)
            tabRippleColor = null
        }
    }

    fun setTitle(title: CharSequence) {
        binding.tvMatchesLabel.text = title
    }

    fun setupWith(
        fragmentActivity: FragmentActivity,
        tabs: List<TabItem>,
        defaultTabIndex: Int = 0,
        onTabSelected: ((Int) -> Unit)? = null,
    ) {
        require(tabs.isNotEmpty()) { "Tabs list cannot be empty." }

        binding.viewPager.adapter = MatchesPagerAdapter(fragmentActivity, tabs)

        mediator?.detach()
        mediator = TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.customView = createTabView(tabs[position].title)
        }.also { it.attach() }

        pageChangeCallback?.let { binding.viewPager.unregisterOnPageChangeCallback(it) }
        pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateTabSelection(position)
                onTabSelected?.invoke(position)
            }
        }.also { binding.viewPager.registerOnPageChangeCallback(it) }

        val startIndex = defaultTabIndex.coerceIn(0, tabs.lastIndex)
        binding.viewPager.setCurrentItem(startIndex, false)
        binding.tabLayout.post {
            updateTabSelection(startIndex)
            onTabSelected?.invoke(startIndex)
        }
    }

    private fun createTabView(title: String) =
        ItemMatchesTabBinding.inflate(LayoutInflater.from(context), binding.tabLayout, false)
            .apply {
                tvTabTitle.text = title
            }.root

    private fun updateTabSelection(selectedPosition: Int) {
        for (index in 0 until binding.tabLayout.tabCount) {
            val tab = binding.tabLayout.getTabAt(index) ?: continue
            val tabView = tab.customView ?: continue
            val tabBinding = ItemMatchesTabBinding.bind(tabView)
            val isSelected = index == selectedPosition
            val backgroundRes = if (isSelected) {
                R.drawable.bg_matches_tab_selected
            } else {
                R.drawable.bg_matches_tab_unselected
            }
            val textColorRes = if (isSelected) {
                R.color.matches_tab_selected_text
            } else {
                R.color.white
            }
            tabBinding.tabRoot.background = ContextCompat.getDrawable(context, backgroundRes)
            tabBinding.tvTabTitle.setTextColor(ContextCompat.getColor(context, textColorRes))
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mediator?.detach()
        mediator = null
        pageChangeCallback?.let { binding.viewPager.unregisterOnPageChangeCallback(it) }
        pageChangeCallback = null
    }

    data class TabItem(
        val title: String,
        val fragmentProvider: () -> Fragment
    )

    private class MatchesPagerAdapter(
        fragmentActivity: FragmentActivity,
        private val items: List<TabItem>
    ) : FragmentStateAdapter(fragmentActivity) {
        override fun getItemCount(): Int = items.size

        override fun createFragment(position: Int): Fragment = items[position].fragmentProvider()
    }
}
