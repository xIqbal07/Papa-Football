package com.papa.fr.football.presentation.signin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.papa.fr.football.R
import com.papa.fr.football.databinding.FragmentProfileBinding
import com.papa.fr.football.presentation.MainActivity
import com.papa.fr.football.presentation.signin.bottomsheetchoose.ChooseTeamBottomSheet

class SignInFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

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
        setLayoutListener()
    }

    private fun setLayoutListener() {
        parentActivity()?.setBottomNavVisible(false)

        with(binding) {
            tvTitle.isVisible = true
            groupProfile.isVisible = false
            btnEdit.isVisible = false

            btnAddTeam.apply {
                setOnClickListener {
                    isVisible = !isVisible
                    rvChooseLeague.isVisible = true
                    groupTeamFavorite.isVisible = false
                }
            }
            btnSave.apply {
                isVisible = true
                text = getString(R.string.sign_in)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        parentActivity()?.setBottomNavVisible(true)
        _binding = null
    }

    private fun onSelectLeague(id: Int) {
        ChooseTeamBottomSheet().show(parentFragmentManager, "ChooseTeamBottomSheet")
    }
}