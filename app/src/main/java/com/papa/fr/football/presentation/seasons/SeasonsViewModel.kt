package com.papa.fr.football.presentation.seasons

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.papa.fr.football.R
import com.papa.fr.football.common.dropdown.LeagueItem
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
                id = 8.toString(),
                name = "La Liga",
                iconRes = R.drawable.ic_laliga
            ),
            LeagueItem(
                id = 17.toString(),
                name = "Premier League",
                iconRes = R.drawable.ic_premier_league
            ),
            LeagueItem(
                id = 35.toString(),
                name = "Bundesliga",
                iconRes = R.drawable.ic_bundesliga
            ),
            LeagueItem(
                id = 34.toString(),
                name = "La Liga",
                iconRes = R.drawable.ic_ligue
            ),
            LeagueItem(
                id = 23.toString(),
                name = "Serie A",
                iconRes = R.drawable.ic_serie_a
            ),
            LeagueItem(
                id = 37.toString(),
                name = "Eredivise",
                iconRes = R.drawable.eredivisie
            ),
        )
    )
    val leagueItems: StateFlow<List<LeagueItem>> = _leagueItems.asStateFlow()

    fun defaultLeagueLabel(): String = _leagueItems.value.firstOrNull()?.name.orEmpty()

    fun loadSeasons(uniqueTournamentId: Int) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            runCatching { getSeasonsUseCase(uniqueTournamentId) }
                .onSuccess { seasons ->
                    Log.d("IQBAL-TEST", "loadSeasons: $seasons")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            seasons = seasons,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message.orEmpty()
                        )
                    }
                }
        }
    }
}
