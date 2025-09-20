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

    private val leagues = leagueCatalog.leagues.map { it.toUiModel() }
    private var currentState = SignInViewState.Content(
        leagues = leagues,
        teamSelectionState = TeamSelectionState.Choosing(
            favoriteTeams = emptyList(),
            selectedLeagueId = null,
            availableTeams = emptyList(),
        ),
    )
    private val _uiState = MutableStateFlow<SignInViewState>(currentState)
    val uiState: StateFlow<SignInViewState> = _uiState

    private val _events = MutableSharedFlow<SignInEvent>()
    val events: SharedFlow<SignInEvent> = _events

    private var selectedTeamIds: MutableSet<Int> = mutableSetOf()
    private var pendingTeamIds: MutableSet<Int> = mutableSetOf()
    private var favoriteTeamDetails: MutableMap<Int, FavoriteTeamUiModel> = mutableMapOf()
    private var availableTeamsById: Map<Int, TeamSelectionUiModel> = emptyMap()
    private var teamCollectionJob: Job? = null
    private var currentSelectedLeagueId: Int? = null
    private var currentAvailableTeams: List<TeamSelectionUiModel> = emptyList()
    private var isEditingTeams: Boolean = true
    private var hasInitializedFromPreferences = false

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
                currentSelectedLeagueId = preferences.selectedLeagueId

                if (!hasInitializedFromPreferences) {
                    isEditingTeams = currentFavorites.isEmpty()
                    hasInitializedFromPreferences = true
                } else if (currentFavorites.isEmpty()) {
                    isEditingTeams = true
                }

                setSelectionState(resolveSelectionState(currentFavorites()))
            }
        }
    }

    fun onAddTeamClicked() {
        val favorites = currentFavorites()
        if (favorites.isEmpty()) {
            isEditingTeams = true
        } else {
            isEditingTeams = !isEditingTeams
        }
        setSelectionState(resolveSelectionState(favorites))
    }

    fun onLeagueSelected(leagueId: Int) {
        pendingTeamIds = selectedTeamIds.toMutableSet()
        currentSelectedLeagueId = leagueId
        isEditingTeams = true
        currentAvailableTeams = emptyList()
        availableTeamsById = emptyMap()
        setSelectionState(
            TeamSelectionState.Choosing(
                favoriteTeams = currentFavorites(),
                selectedLeagueId = leagueId,
                availableTeams = emptyList(),
            )
        )
        startObservingTeams(leagueId)
    }

    fun onTeamSelectionChanged(teamId: Int, isSelected: Boolean) {
        if (isSelected) {
            pendingTeamIds.add(teamId)
        } else {
            pendingTeamIds.remove(teamId)
        }
        val updatedTeams = currentAvailableTeams.map { team ->
            if (team.id == teamId) {
                team.copy(isSelected = isSelected)
            } else {
                team
            }
        }
        currentAvailableTeams = updatedTeams
        availableTeamsById = updatedTeams.associateBy { it.id }
        setSelectionState(
            TeamSelectionState.Choosing(
                favoriteTeams = currentFavorites(),
                selectedLeagueId = currentSelectedLeagueId,
                availableTeams = updatedTeams,
            )
        )
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
        val favorites = currentFavorites()
        if (favorites.isNotEmpty()) {
            isEditingTeams = false
        }
        setSelectionState(resolveSelectionState(favorites))
    }

    fun onSignInClicked() {
        val favorites = currentFavorites()
            .map { it.toDomain() }
        viewModelScope.launch {
            userPreferencesRepository.updatePreferences(
                isSignedIn = true,
                selectedLeagueId = currentSelectedLeagueId,
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
                    currentAvailableTeams = emptyList()
                    availableTeamsById = emptyMap()
                    setSelectionState(
                        TeamSelectionState.Choosing(
                            favoriteTeams = currentFavorites(),
                            selectedLeagueId = leagueId,
                            availableTeams = emptyList(),
                        )
                    )
                }
                .collect { teams ->
                    val availableTeams = teams.map { team ->
                        team.toSelectionUiModel(pendingTeamIds.contains(team.id))
                    }
                    currentAvailableTeams = availableTeams
                    availableTeamsById = availableTeams.associateBy { it.id }
                    setSelectionState(
                        TeamSelectionState.Choosing(
                            favoriteTeams = currentFavorites(),
                            selectedLeagueId = leagueId,
                            availableTeams = availableTeams,
                        )
                    )
                }
        }
    }

    private fun setSelectionState(selectionState: TeamSelectionState) {
        updateState { it.copy(teamSelectionState = selectionState) }
    }

    private fun resolveSelectionState(favorites: List<FavoriteTeamUiModel>): TeamSelectionState {
        return if (isEditingTeams || favorites.isEmpty()) {
            TeamSelectionState.Choosing(
                favoriteTeams = favorites,
                selectedLeagueId = currentSelectedLeagueId,
                availableTeams = currentAvailableTeams,
            )
        } else {
            TeamSelectionState.Favorites(favorites)
        }
    }

    private fun updateState(transform: (SignInViewState.Content) -> SignInViewState.Content) {
        val updated = transform(currentState)
        currentState = updated
        _uiState.value = updated
    }

    private fun currentFavorites(): List<FavoriteTeamUiModel> {
        return favoriteTeamDetails.values.sortedBy { it.name }
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
