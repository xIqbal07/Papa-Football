package com.papa.fr.football

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.papa.fr.football.data.Team
import com.papa.fr.football.data.TeamsRepository

class MainViewModel : ViewModel() {

    private val _teams = MutableLiveData<List<Team>>()
    val teams: LiveData<List<Team>> = _teams

    private val _selectedLeagueId = MutableLiveData<String>()
    val selectedLeagueId: LiveData<String> = _selectedLeagueId

    init {
        val defaultLeague = TeamsRepository.getLeagues().firstOrNull()?.id
        if (defaultLeague != null) {
            selectLeague(defaultLeague)
        } else {
            _teams.value = emptyList()
        }
    }

    fun selectLeague(leagueId: String) {
        if (_selectedLeagueId.value == leagueId) return
        _selectedLeagueId.value = leagueId
        _teams.value = TeamsRepository.getTeamsForLeague(leagueId)
    }
}
