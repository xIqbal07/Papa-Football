package com.papa.fr.football.presentation.signin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.papa.fr.football.R
import com.papa.fr.football.databinding.FragmentProfileBinding
import com.papa.fr.football.presentation.MainActivity
import com.papa.fr.football.presentation.signin.bottomsheetchoose.ChooseTeamBottomSheet
import com.papa.fr.football.presentation.signin.adapter.FavoriteTeamsAdapter
import com.papa.fr.football.presentation.signin.adapter.LeagueSelectionAdapter
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class SignInFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val signInViewModel: SignInViewModel by activityViewModel()

    private val leagueAdapter = LeagueSelectionAdapter { league ->
        signInViewModel.onLeagueSelected(league.id)
        onSelectLeague(league.id)
    }
    private val favoriteTeamsAdapter = FavoriteTeamsAdapter {

    }
    private var isEditingTeams: Boolean = false

    private fun parentActivity() = (requireActivity() as? MainActivity)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUi()
        setupRecyclerViews()
        observeState()
        observeEvents()
    }

    private fun setupUi() = with(binding) {
        parentActivity()?.setBottomNavVisible(false)

        tvTitle.isVisible = true
        groupProfile.isVisible = false
        btnEdit.isVisible = false

        btnAddTeam.setOnClickListener {
            isEditingTeams = !isEditingTeams
            binding.btnAddTeam.isVisible = false
            if (isEditingTeams) {
                rvChooseLeague.isVisible = true
                groupTeamFavorite.isVisible = false
            } else {
                rvChooseLeague.isVisible = false
                groupTeamFavorite.isVisible = signInViewModel.uiState.value.favoriteTeams.isNotEmpty()
            }
        }
        btnSave.apply {
            isVisible = true
            text = getString(R.string.sign_in)
            setOnClickListener { signInViewModel.onSignInClicked() }
        }
    }

    private fun setupRecyclerViews() = with(binding) {
        rvChooseLeague.layoutManager = LinearLayoutManager(requireContext())
        rvChooseLeague.adapter = leagueAdapter

        rvFavoriteTeams.layoutManager = LinearLayoutManager(requireContext())
        rvFavoriteTeams.adapter = favoriteTeamsAdapter
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                signInViewModel.uiState.collect(::renderState)
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                signInViewModel.events.collect { event ->
                    Log.d("IQBAL-TEST", "observeEvents: ${event is SignInEvent.TeamsConfirmed}")
                    when (event) {
                        SignInEvent.TeamsConfirmed -> {
                            isEditingTeams = false
                            binding.btnAddTeam.isVisible = true
                            binding.rvChooseLeague.isVisible = false
                            binding.groupTeamFavorite.isVisible = true
                        }

                        SignInEvent.NavigateToSchedule -> {
                            isEditingTeams = false
                            parentActivity()?.onUserSignedIn()
                        }
                    }
                }
            }
        }
    }

    private fun renderState(state: SignInUiState) = with(binding) {
        leagueAdapter.submitList(state.leagues)
        favoriteTeamsAdapter.submitList(state.favoriteTeams)
        btnSave.isEnabled = state.favoriteTeams.isNotEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isEditingTeams = false
        _binding = null
    }

    private fun onSelectLeague(id: Int) {
        ChooseTeamBottomSheet.newInstance(id)
            .show(childFragmentManager, ChooseTeamBottomSheet.TAG)
    }

    companion object {
        const val TAG = "SignInFragment"
    }
}