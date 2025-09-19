package com.papa.fr.football.presentation.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.papa.fr.football.R
import com.papa.fr.football.common.dropdown.LeagueItem
import com.papa.fr.football.domain.model.Match
import com.papa.fr.football.domain.model.Season
import com.papa.fr.football.domain.usecase.GetSeasonsUseCase
import com.papa.fr.football.domain.usecase.GetUpcomingMatchesUseCase
import com.papa.fr.football.matches.MatchUiModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Coordinates league metadata and season loading for the schedule feature.
 */
class ScheduleViewModel(
    private val getSeasonsUseCase: GetSeasonsUseCase,
    private val getUpcomingMatchesUseCase: GetUpcomingMatchesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    private var matchesJob: Job? = null

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

            val (leagueId, seasonId) = determineSelection(currentSeasons)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    seasonsByLeague = currentSeasons,
                    errorMessage = encounteredError,
                    selectedLeagueId = leagueId,
                    selectedSeasonId = seasonId,
                )
            }

            if (leagueId != null && seasonId != null) {
                loadUpcomingMatches(leagueId, seasonId)
            }
        }
    }

    fun loadSeasonsForLeague(leagueId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = runCatching { getSeasonsUseCase(leagueId) }
            result.onSuccess { seasons ->
                _uiState.update { state ->
                    val updatedSeasons = state.seasonsByLeague + (leagueId to seasons)
                    val selectedSeasonId = when {
                        state.selectedLeagueId != leagueId -> state.selectedSeasonId
                        seasons.any { it.id == state.selectedSeasonId } -> state.selectedSeasonId
                        else -> seasons.firstOrNull()?.id
                    }
                    state.copy(
                        isLoading = false,
                        seasonsByLeague = updatedSeasons,
                        errorMessage = null,
                        selectedLeagueId = state.selectedLeagueId ?: leagueId,
                        selectedSeasonId = selectedSeasonId,
                    )
                }

                val selectedSeasonId = _uiState.value.selectedSeasonId
                val selectedLeagueId = _uiState.value.selectedLeagueId
                if (selectedLeagueId == leagueId && selectedSeasonId != null) {
                    loadUpcomingMatches(leagueId, selectedSeasonId)
                }
            }
            result.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = throwable.message,
                    )
                }
            }
        }
    }

    fun onLeagueSelected(leagueId: Int) {
        val seasons = seasonsForLeague(leagueId)
        val previousState = _uiState.value
        _uiState.update { it.copy(selectedLeagueId = leagueId) }

        if (seasons.isEmpty()) {
            loadSeasonsForLeague(leagueId)
            return
        }

        val currentSeasonId = previousState.selectedSeasonId
        val validSeasonId = seasons.firstOrNull { it.id == currentSeasonId }?.id
            ?: seasons.firstOrNull()?.id

        if (validSeasonId != currentSeasonId) {
            _uiState.update { it.copy(selectedSeasonId = validSeasonId) }
        }

        if (validSeasonId != null) {
            loadUpcomingMatches(leagueId, validSeasonId)
        }
    }

    fun onSeasonSelected(seasonId: Int) {
        val leagueId = _uiState.value.selectedLeagueId ?: return
        if (_uiState.value.selectedSeasonId == seasonId) {
            return
        }
        _uiState.update { it.copy(selectedSeasonId = seasonId) }
        loadUpcomingMatches(leagueId, seasonId)
    }

    fun refreshSelectedLeagueData() {
        val leagueId = _uiState.value.selectedLeagueId ?: _leagueItems.value.firstOrNull()?.id
        val seasonId = _uiState.value.selectedSeasonId

        if (leagueId != null) {
            loadSeasonsForLeague(leagueId)
        }

        if (leagueId != null && seasonId != null) {
            loadUpcomingMatches(leagueId, seasonId)
        }
    }

    private fun loadUpcomingMatches(leagueId: Int, seasonId: Int) {
        matchesJob?.cancel()
        matchesJob = viewModelScope.launch {
            _uiState.update {
                it.copy(isMatchesLoading = true, matchesErrorMessage = null)
            }

            val result = runCatching { getUpcomingMatchesUseCase(leagueId, seasonId) }
            _uiState.update { state ->
                result.fold(
                    onSuccess = { matches ->
                        state.copy(
                            isMatchesLoading = false,
                            matchesErrorMessage = null,
                            futureMatches = matches.map { it.toUiModel() },
                        )
                    },
                    onFailure = { throwable ->
                        state.copy(
                            isMatchesLoading = false,
                            matchesErrorMessage = throwable.message,
                            futureMatches = emptyList(),
                        )
                    }
                )
            }
        }
    }

    private fun determineSelection(seasonsByLeague: Map<Int, List<Season>>): Pair<Int?, Int?> {
        val availableLeagueIds = _leagueItems.value.map { it.id }
        val state = _uiState.value
        val leagueId = state.selectedLeagueId
            ?.takeIf { seasonsByLeague[it].orEmpty().isNotEmpty() }
            ?: availableLeagueIds.firstOrNull { seasonsByLeague[it].orEmpty().isNotEmpty() }

        val seasons = leagueId?.let { seasonsByLeague[it] }.orEmpty()
        val seasonId = state.selectedSeasonId?.takeIf { id ->
            seasons.any { it.id == id }
        } ?: seasons.firstOrNull()?.id

        return leagueId to seasonId
    }

    private fun Match.toUiModel(): MatchUiModel.Future {
        val instant = toInstant(startTimestamp)
        val zonedDateTime = instant.atZone(ZoneId.systemDefault())
        val dateLabel = zonedDateTime.format(DATE_FORMATTER)
        val timeLabel = zonedDateTime.format(TIME_FORMATTER)
        val isToday = zonedDateTime.toLocalDate() == LocalDate.now()

        return MatchUiModel.Future(
            id = id,
            homeTeamId = homeTeam.id,
            homeTeamName = homeTeam.name,
            awayTeamId = awayTeam.id,
            awayTeamName = awayTeam.name,
            startTimestamp = startTimestamp,
            startDateLabel = dateLabel,
            startTimeLabel = timeLabel,
            isToday = isToday,
            homeLogoBase64 = homeTeam.logoBase64,
            awayLogoBase64 = awayTeam.logoBase64,
            odds = null,
        )
    }

    private fun toInstant(timestamp: Long): Instant {
        return if (timestamp < 1_000_000_000_000L) {
            Instant.ofEpochSecond(timestamp)
        } else {
            Instant.ofEpochMilli(timestamp)
        }
    }

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd")
        private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
