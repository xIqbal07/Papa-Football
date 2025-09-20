package com.papa.fr.football.presentation.signin.bottomsheetchoose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.papa.fr.football.databinding.BottomsheetChooseTeamBinding
import com.papa.fr.football.presentation.signin.SignInViewModel
import com.papa.fr.football.presentation.signin.SignInViewState
import com.papa.fr.football.presentation.signin.TeamSelectionState
import com.papa.fr.football.presentation.signin.adapter.TeamSelectionAdapter
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ChooseTeamBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomsheetChooseTeamBinding? = null
    private val binding get() = _binding!!

    private val signInViewModel: SignInViewModel by activityViewModel()
    private val teamAdapter = TeamSelectionAdapter { teamId, isSelected ->
        signInViewModel.onTeamSelectionChanged(teamId, isSelected)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomsheetChooseTeamBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvTeam.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTeam.adapter = teamAdapter
        binding.btnChooseTeam.setOnClickListener {
            signInViewModel.confirmTeamSelection()
            dismissAllowingStateLoss()
        }
        observeState()
    }

    override fun onStart() {
        super.onStart()
        val bottomSheet =
            dialog?.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                ?: return

        bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT

        BottomSheetBehavior.from(bottomSheet).apply {
            isFitToContents = false
            halfExpandedRatio = 0.5f
            state = BottomSheetBehavior.STATE_HALF_EXPANDED
            skipCollapsed = true
            isDraggable = true
        }
        ViewCompat.setOnApplyWindowInsetsListener(bottomSheet) { v, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, ime.bottom)
            insets
        }
    }

    private fun observeState() {
        val leagueId = requireArguments().getInt(ARG_LEAGUE_ID)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                signInViewModel.uiState.collect { state ->
                    val contentState = state as? SignInViewState.Content ?: return@collect
                    val selectionState =
                        contentState.teamSelectionState as? TeamSelectionState.Choosing
                            ?: return@collect
                    if (selectionState.selectedLeagueId != leagueId) return@collect
                    teamAdapter.submitList(selectionState.availableTeams)
                    val selectedCount = selectionState.availableTeams.count { it.isSelected }
                    binding.btnChooseTeam.isEnabled = selectedCount > 0
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ChooseTeamBottomSheet"
        private const val ARG_LEAGUE_ID = "arg_league_id"

        fun newInstance(leagueId: Int): ChooseTeamBottomSheet {
            return ChooseTeamBottomSheet().apply {
                arguments = Bundle().apply { putInt(ARG_LEAGUE_ID, leagueId) }
            }
        }
    }
}
