package com.papa.fr.football.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.papa.fr.football.R
import com.papa.fr.football.common.matches.MatchesTabLayoutView
import com.papa.fr.football.databinding.FragmentScheduleBinding
import com.papa.fr.football.matches.MatchesListFragment
import com.papa.fr.football.matches.MatchesTabType

class ScheduleFragment : Fragment() {

    private var _binding: FragmentScheduleBinding? = null
    private val binding: FragmentScheduleBinding
        get() = requireNotNull(_binding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMatchesTabs()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
                },
            ),
        )
    }

    companion object {
        const val TAG = "ScheduleFragment"
    }
}
