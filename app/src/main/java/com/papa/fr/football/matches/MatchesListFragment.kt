package com.papa.fr.football.matches

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.papa.fr.football.databinding.FragmentMatchesListBinding

class MatchesListFragment : Fragment() {

    private var _binding: FragmentMatchesListBinding? = null
    private val binding: FragmentMatchesListBinding
        get() = requireNotNull(_binding)

    private val matchesAdapter = MatchesAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMatchesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvMatches.adapter = matchesAdapter

        val matchesType = requireArguments().getString(ARG_TYPE)
            ?.let { runCatching { MatchesTabType.valueOf(it) }.getOrNull() }
            ?: MatchesTabType.FUTURE

        val matches = when (matchesType) {
            MatchesTabType.FUTURE -> demoFutureMatches()
            MatchesTabType.LIVE -> demoLiveMatches()
            MatchesTabType.PAST -> demoPastMatches()
        }

        matchesAdapter.submitMatches(matches)
        binding.tvPlaceholder.isVisible = matches.isEmpty()
        binding.rvMatches.isVisible = matches.isNotEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvMatches.adapter = null
        _binding = null
    }

    private fun demoFutureMatches(): List<MatchUiModel> = listOf(
        MatchUiModel.Future(
            id = "future-1",
            homeTeam = "Royal Vallecano",
            awayTeam = "Real Betis",
            date = "Today 20:00",
            kickoffTime = "20:00",
            odds = MatchUiModel.Odds("2.6", "2.9", "2.7")
        ),
        MatchUiModel.Future(
            id = "future-2",
            homeTeam = "Villarreal",
            awayTeam = "Barcelona",
            date = "02.07.2025 21:00",
            kickoffTime = "21:00",
            odds = MatchUiModel.Odds("2.2", "3.1", "2.9")
        )
    )

    private fun demoLiveMatches(): List<MatchUiModel> = listOf(
        MatchUiModel.Live(
            id = "live-1",
            homeTeam = "Royal Vallecano",
            awayTeam = "Real Betis",
            score = "0-1",
            contextText = "Today 20:00",
            elapsed = "55'",
            status = "Live"
        ),
        MatchUiModel.Live(
            id = "live-2",
            homeTeam = "Eibar",
            awayTeam = "Valencia",
            score = "1-1",
            contextText = "Today 19:00",
            elapsed = "63'",
            status = "Live"
        )
    )

    private fun demoPastMatches(): List<MatchUiModel> = listOf(
        MatchUiModel.Past(
            id = "past-1",
            homeTeam = "Royal Vallecano",
            awayTeam = "Real Betis",
            score = "2-3",
            date = "01.08.2025",
            resultLabel = "FT",
            status = "Full Time"
        ),
        MatchUiModel.Past(
            id = "past-2",
            homeTeam = "Sevilla",
            awayTeam = "Real Madrid",
            score = "1-2",
            date = "28.07.2025",
            resultLabel = "FT",
            status = "Full Time"
        )
    )

    companion object {
        private const val ARG_TYPE = "arg_type"

        fun newInstance(type: MatchesTabType): MatchesListFragment {
            return MatchesListFragment().apply {
                arguments = bundleOf(ARG_TYPE to type.name)
            }
        }
    }
}

enum class MatchesTabType {
    FUTURE,
    LIVE,
    PAST
}
