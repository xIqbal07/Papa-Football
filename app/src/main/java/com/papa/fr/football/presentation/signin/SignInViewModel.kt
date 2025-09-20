package com.papa.fr.football.presentation.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.papa.fr.football.common.league.LeagueCatalog
import com.papa.fr.football.common.league.LeagueDescriptor
import com.papa.fr.football.common.team.TeamCatalog
import com.papa.fr.football.common.team.TeamDescriptor
import com.papa.fr.football.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class SignInViewModel(
    private val leagueCatalog: LeagueCatalog,
    private val teamCatalog: TeamCatalog,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SignInUiState(
            leagues = leagueCatalog.leagues.map { it.toUiModel() },
        )
    )
    val uiState: StateFlow<SignInUiState> = _uiState

    private val _events = MutableSharedFlow<SignInEvent>()
    val events: SharedFlow<SignInEvent> = _events

    private var selectedTeamIds: MutableSet<Int> = mutableSetOf()
    private var pendingTeamIds: MutableSet<Int> = mutableSetOf()

    init {
        viewModelScope.launch {
            userPreferencesRepository.preferencesFlow.collect { preferences ->
                selectedTeamIds = preferences.favoriteTeamIds.toMutableSet()
                if (pendingTeamIds.isEmpty()) {
                    pendingTeamIds = selectedTeamIds.toMutableSet()
                }
                val favoriteTeams = resolveFavoriteTeams(selectedTeamIds)
                _uiState.value = _uiState.value.copy(
                    selectedLeagueId = preferences.selectedLeagueId,
                    favoriteTeams = favoriteTeams,
                    isSignedIn = preferences.isSignedIn,
                )
            }
        }
    }

    fun onLeagueSelected(leagueId: Int) {
        pendingTeamIds = selectedTeamIds.toMutableSet()
        updateAvailableTeams(leagueId)
        _uiState.value = _uiState.value.copy(selectedLeagueId = leagueId)
    }

    fun onTeamSelectionChanged(teamId: Int, isSelected: Boolean) {
        if (isSelected) {
            pendingTeamIds.add(teamId)
        } else {
            pendingTeamIds.remove(teamId)
        }
        val leagueId = _uiState.value.selectedLeagueId ?: return
        updateAvailableTeams(leagueId)
    }

    fun confirmTeamSelection() {
        selectedTeamIds = pendingTeamIds.toMutableSet()
        val favoriteTeams = resolveFavoriteTeams(selectedTeamIds)
        _uiState.value = _uiState.value.copy(
            favoriteTeams = favoriteTeams,
        )
        viewModelScope.launch {
            _events.emit(SignInEvent.TeamsConfirmed)
        }
    }

    fun onSignInClicked() {
        val state = _uiState.value
        viewModelScope.launch {
            userPreferencesRepository.updatePreferences(
                isSignedIn = true,
                selectedLeagueId = state.selectedLeagueId,
                favoriteTeamIds = selectedTeamIds,
            )
            _events.emit(SignInEvent.NavigateToSchedule)
        }
    }

    private fun updateAvailableTeams(leagueId: Int) {
        val teams = teamCatalog.teamsForLeague(leagueId)
        val availableTeams = teams.map { it.toSelectionUiModel(pendingTeamIds.contains(it.id)) }
        _uiState.value = _uiState.value.copy(
            availableTeams = availableTeams,
        )
    }

    private fun resolveFavoriteTeams(teamIds: Set<Int>): List<FavoriteTeamUiModel> {
        return teamIds.mapNotNull(teamCatalog::findTeam)
            .sortedBy { it.name }
            .map { descriptor -> descriptor.toFavoriteUiModel() }
    }

    private fun LeagueDescriptor.toUiModel(): LeagueUiModel {
        return LeagueUiModel(
            id = id,
            name = name,
            iconRes = iconRes,
        )
    }

    private fun TeamDescriptor.toSelectionUiModel(isSelected: Boolean): TeamSelectionUiModel {
        return TeamSelectionUiModel(
            id = id,
            name = name,
            logoRes = logoRes,
            isSelected = isSelected,
        )
    }

    private fun TeamDescriptor.toFavoriteUiModel(): FavoriteTeamUiModel {
        return FavoriteTeamUiModel(
            id = id,
            name = name,
            logoRes = logoRes,
        )
    }
}

sealed interface SignInEvent {
    data object TeamsConfirmed : SignInEvent
    data object NavigateToSchedule : SignInEvent
}

data class SignInUiState(
    val leagues: List<LeagueUiModel> = emptyList(),
    val selectedLeagueId: Int? = null,
    val availableTeams: List<TeamSelectionUiModel> = emptyList(),
    val favoriteTeams: List<FavoriteTeamUiModel> = emptyList(),
    val isSignedIn: Boolean = false,
)

data class LeagueUiModel(
    val id: Int,
    val name: String,
    val iconRes: Int?,
)

data class TeamSelectionUiModel(
    val id: Int,
    val name: String,
    val logoRes: Int?,
    val isSelected: Boolean,
)

data class FavoriteTeamUiModel(
    val id: Int,
    val name: String,
    val logoRes: Int?,
)
