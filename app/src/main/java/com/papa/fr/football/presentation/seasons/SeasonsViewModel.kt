package com.papa.fr.football.presentation.seasons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.papa.fr.football.R
import com.papa.fr.football.common.dropdown.LeagueItem
import com.papa.fr.football.domain.model.Season
import com.papa.fr.football.domain.usecase.GetSeasonsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SeasonsViewModel(
    private val getSeasonsUseCase: GetSeasonsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeasonsUiState())
    val uiState: StateFlow<SeasonsUiState> = _uiState.asStateFlow()

    private val _leagueItems = MutableStateFlow(
        listOf(
            LeagueItem(
                id = 8,
                name = "La Liga",
                iconRes = R.drawable.ic_laliga
            ),
            LeagueItem(
                id = 17,
                name = "Premier League",
                iconRes = R.drawable.ic_premier_league
            ),
            LeagueItem(
                id = 35,
                name = "Bundesliga",
                iconRes = R.drawable.ic_bundesliga
            ),
            LeagueItem(
                id = 34,
                name = "La Liga",
                iconRes = R.drawable.ic_ligue
            ),
            LeagueItem(
                id = 23,
                name = "Serie A",
                iconRes = R.drawable.ic_serie_a
            ),
            LeagueItem(
                id = 37,
                name = "Eredivise",
                iconRes = R.drawable.eredivisie
            ),
        )
    )
    val leagueItems: StateFlow<List<LeagueItem>> = _leagueItems.asStateFlow()

    fun defaultLeagueLabel(): String = _leagueItems.value.firstOrNull()?.name.orEmpty()

    fun seasonsForLeague(leagueId: Int): List<Season> {
        return _uiState.value.seasonsByLeague[leagueId].orEmpty()
    }

    fun loadAllLeagueSeasons() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val currentSeasons = _uiState.value.seasonsByLeague.toMutableMap()
            var encounteredError: String? = null

            _leagueItems.value.forEach { league ->
                val result = runCatching { getSeasonsUseCase(league.id) }
                result.onSuccess { seasons ->
                    currentSeasons[league.id] = seasons
                }
                result.onFailure { throwable ->
                    if (encounteredError.isNullOrBlank()) {
                        encounteredError = throwable.message
                    }
                }
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    seasonsByLeague = currentSeasons,
                    errorMessage = encounteredError
                )
            }
        }
    }

    fun loadSeasonsForLeague(leagueId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = runCatching { getSeasonsUseCase(leagueId) }
            _uiState.update { state ->
                result.fold(
                    onSuccess = { seasons ->
                        state.copy(
                            isLoading = false,
                            seasonsByLeague = state.seasonsByLeague + (leagueId to seasons),
                            errorMessage = null
                        )
                    },
                    onFailure = { throwable ->
                        state.copy(
                            isLoading = false,
                            errorMessage = throwable.message
                        )
                    }
                )
            }
        }
    }
}
