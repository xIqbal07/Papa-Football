package com.papa.fr.football.presentation.signin

import android.os.Bundle
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
    private val favoriteTeamsAdapter = FavoriteTeamsAdapter()

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

        btnAddTeam.setOnClickListener { signInViewModel.onAddTeamClicked() }
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
                    if (event is SignInEvent.NavigateToSchedule) {
                        parentActivity()?.onUserSignedIn()
                    }
                }
            }
        }
    }

    private fun renderState(state: SignInViewState) {
        when (state) {
            is SignInViewState.Content -> renderContentState(state)
        }
    }

    private fun renderContentState(state: SignInViewState.Content) = with(binding) {
        leagueAdapter.submitList(state.leagues)
        favoriteTeamsAdapter.submitList(state.teamSelectionState.favoriteTeams)

        when (val selectionState = state.teamSelectionState) {
            is TeamSelectionState.Choosing -> {
                groupTeamFavorite.isVisible = false
                rvChooseLeague.isVisible = true
            }

            is TeamSelectionState.Favorites -> {
                val hasFavorites = selectionState.favoriteTeams.isNotEmpty()
                groupTeamFavorite.isVisible = hasFavorites
                rvChooseLeague.isVisible = !hasFavorites
            }
        }

        btnSave.isEnabled = state.canSignIn
    }

    override fun onDestroyView() {
        super.onDestroyView()
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