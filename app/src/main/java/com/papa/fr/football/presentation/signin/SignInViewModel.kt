package com.papa.fr.football.presentation.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.papa.fr.football.common.league.LeagueCatalog
import com.papa.fr.football.common.league.LeagueDescriptor
import com.papa.fr.football.domain.model.LeagueTeam
import com.papa.fr.football.domain.model.UserFavoriteTeam
import com.papa.fr.football.domain.repository.TeamRepository
import com.papa.fr.football.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class SignInViewModel(
    private val leagueCatalog: LeagueCatalog,
    private val teamRepository: TeamRepository,
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
    private var favoriteTeamDetails: MutableMap<Int, FavoriteTeamUiModel> = mutableMapOf()
    private var availableTeamsById: Map<Int, TeamSelectionUiModel> = emptyMap()
    private var teamCollectionJob: Job? = null

    init {
        viewModelScope.launch {
            userPreferencesRepository.preferencesFlow.collect { preferences ->
                val currentFavorites = preferences.favoriteTeams
                selectedTeamIds = currentFavorites.map { it.id }.toMutableSet()
                if (pendingTeamIds.isEmpty()) {
                    pendingTeamIds = selectedTeamIds.toMutableSet()
                }
                favoriteTeamDetails = currentFavorites
                    .associateBy({ it.id }) { it.toFavoriteUiModel() }
                    .toMutableMap()
                _uiState.value = _uiState.value.copy(
                    selectedLeagueId = preferences.selectedLeagueId,
                    favoriteTeams = favoriteTeamDetails.values.sortedBy { it.name },
                    isSignedIn = preferences.isSignedIn,
                )
            }
        }
    }

    fun onLeagueSelected(leagueId: Int) {
        pendingTeamIds = selectedTeamIds.toMutableSet()
        startObservingTeams(leagueId)
        _uiState.value = _uiState.value.copy(selectedLeagueId = leagueId)
    }

    fun onTeamSelectionChanged(teamId: Int, isSelected: Boolean) {
        if (isSelected) {
            pendingTeamIds.add(teamId)
        } else {
            pendingTeamIds.remove(teamId)
        }
        val updatedTeams = _uiState.value.availableTeams.map { team ->
            if (team.id == teamId) {
                team.copy(isSelected = isSelected)
            } else {
                team
            }
        }
        availableTeamsById = updatedTeams.associateBy { it.id }
        _uiState.value = _uiState.value.copy(availableTeams = updatedTeams)
    }

    fun confirmTeamSelection() {
        selectedTeamIds = pendingTeamIds.toMutableSet()
        val updatedFavorites = favoriteTeamDetails.toMutableMap()
        availableTeamsById
            .filterKeys { selectedTeamIds.contains(it) }
            .values
            .forEach { selection ->
                updatedFavorites[selection.id] = selection.toFavoriteUiModel()
            }
        updatedFavorites.keys.retainAll(selectedTeamIds)
        favoriteTeamDetails = updatedFavorites
        _uiState.value = _uiState.value.copy(
            favoriteTeams = favoriteTeamDetails.values.sortedBy { it.name },
        )
        viewModelScope.launch {
            _events.emit(SignInEvent.TeamsConfirmed)
        }
    }

    fun onSignInClicked() {
        val state = _uiState.value
        val favorites = favoriteTeamDetails.values
            .sortedBy { it.name }
            .map { it.toDomain() }
        viewModelScope.launch {
            userPreferencesRepository.updatePreferences(
                isSignedIn = true,
                selectedLeagueId = state.selectedLeagueId,
                favoriteTeams = favorites,
            )
            _events.emit(SignInEvent.NavigateToSchedule)
        }
    }

    private fun startObservingTeams(leagueId: Int) {
        teamCollectionJob?.cancel()
        teamCollectionJob = viewModelScope.launch {
            teamRepository.getTeamsForLeague(leagueId)
                .catch {
                    _uiState.value = _uiState.value.copy(availableTeams = emptyList())
                }
                .collect { teams ->
                    val availableTeams = teams.map { team ->
                        team.toSelectionUiModel(pendingTeamIds.contains(team.id))
                    }
                    availableTeamsById = availableTeams.associateBy { it.id }
                    _uiState.value = _uiState.value.copy(availableTeams = availableTeams)
                }
        }
    }

    private fun LeagueDescriptor.toUiModel(): LeagueUiModel {
        return LeagueUiModel(
            id = id,
            name = name,
            iconRes = iconRes,
        )
    }

    private fun LeagueTeam.toSelectionUiModel(isSelected: Boolean): TeamSelectionUiModel {
        return TeamSelectionUiModel(
            id = id,
            leagueId = leagueId,
            name = name,
            logoBase64 = logoBase64,
            isSelected = isSelected,
        )
    }

    private fun TeamSelectionUiModel.toFavoriteUiModel(): FavoriteTeamUiModel {
        return FavoriteTeamUiModel(
            id = id,
            leagueId = leagueId,
            name = name,
            logoBase64 = logoBase64,
        )
    }

    private fun UserFavoriteTeam.toFavoriteUiModel(): FavoriteTeamUiModel {
        return FavoriteTeamUiModel(
            id = id,
            leagueId = leagueId,
            name = name,
            logoBase64 = logoBase64,
        )
    }

    private fun FavoriteTeamUiModel.toDomain(): UserFavoriteTeam {
        return UserFavoriteTeam(
            id = id,
            leagueId = leagueId,
            name = name,
            logoBase64 = logoBase64,
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
    val leagueId: Int,
    val name: String,
    val logoBase64: String?,
    val isSelected: Boolean,
)

data class FavoriteTeamUiModel(
    val id: Int,
    val leagueId: Int,
    val name: String,
    val logoBase64: String?,
)
