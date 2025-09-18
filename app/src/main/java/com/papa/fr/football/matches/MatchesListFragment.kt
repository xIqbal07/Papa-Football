package com.papa.fr.football.matches

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.papa.fr.football.MainViewModel
import com.papa.fr.football.R
import com.papa.fr.football.databinding.FragmentMatchesListBinding
import com.papa.fr.football.players.TeamPlayersActivity

class MatchesListFragment : Fragment() {

    private var _binding: FragmentMatchesListBinding? = null
    private val binding: FragmentMatchesListBinding
        get() = requireNotNull(_binding)
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var teamsAdapter: TeamsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMatchesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvPlaceholder.text = getString(R.string.matches_empty_placeholder)

        teamsAdapter = TeamsAdapter { team ->
            val leagueId = viewModel.selectedLeagueId.value ?: return@TeamsAdapter
            startActivity(TeamPlayersActivity.createIntent(requireContext(), leagueId, team.id))
        }

        binding.rvTeams.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTeams.adapter = teamsAdapter

        viewModel.teams.observe(viewLifecycleOwner) { teams ->
            teamsAdapter.submitList(teams)
            binding.rvTeams.isVisible = teams.isNotEmpty()
            binding.tvPlaceholder.isVisible = teams.isEmpty()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_LABEL = "arg_label"

        fun newInstance(label: String): MatchesListFragment {
            return MatchesListFragment().apply {
                arguments = bundleOf(ARG_LABEL to label)
            }
        }
    }
}
