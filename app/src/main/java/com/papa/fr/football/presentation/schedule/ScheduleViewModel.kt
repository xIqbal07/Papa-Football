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

    fun loadAllLeagueSeasons(forceRefresh: Boolean = false) {
        if (!forceRefresh && _uiState.value.isDataLoaded) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    isMatchesLoading = true,
                    matchesErrorMessage = null,
                    futureMatches = emptyList(),
                    isDataLoaded = false,
                )
            }

            val seasonsByLeague = mutableMapOf<Int, List<Season>>()
            val matchesByLeagueSeason = mutableMapOf<Int, MutableMap<Int, List<MatchUiModel.Future>>>()
            val matchErrorsByLeagueSeason = mutableMapOf<Int, MutableMap<Int, String?>>()
            var encounteredSeasonError: String? = null

            _leagueItems.value.forEach { league ->
                val seasonsResult = runCatching { getSeasonsUseCase(league.id) }
                seasonsResult.onSuccess { seasons ->
                    seasonsByLeague[league.id] = seasons
                    val leagueMatches = matchesByLeagueSeason.getOrPut(league.id) { mutableMapOf() }
                    val leagueErrors = matchErrorsByLeagueSeason.getOrPut(league.id) { mutableMapOf() }

                    seasons.forEach { season ->
                        val matchesResult = runCatching {
                            getUpcomingMatchesUseCase(league.id, season.id)
                        }
                        matchesResult.onSuccess { matches ->
                            leagueMatches[season.id] = matches.map { it.toUiModel() }
                            leagueErrors[season.id] = null
                        }
                        matchesResult.onFailure { throwable ->
                            leagueMatches[season.id] = emptyList()
                            leagueErrors[season.id] =
                                throwable.message ?: DEFAULT_MATCHES_ERROR_MESSAGE
                        }
                    }
                }
                seasonsResult.onFailure { throwable ->
                    if (encounteredSeasonError.isNullOrBlank()) {
                        encounteredSeasonError =
                            throwable.message ?: DEFAULT_SEASONS_ERROR_MESSAGE
                    }
                    seasonsByLeague[league.id] = emptyList()
                    matchesByLeagueSeason.getOrPut(league.id) { mutableMapOf() }
                    matchErrorsByLeagueSeason.getOrPut(league.id) { mutableMapOf() }
                }
            }

            val seasonsSnapshot = seasonsByLeague.toMap()
            val matchesSnapshot = matchesByLeagueSeason.mapValues { it.value.toMap() }
            val matchErrorsSnapshot = matchErrorsByLeagueSeason.mapValues { it.value.toMap() }
            val (selectedLeagueId, selectedSeasonId) = determineSelection(seasonsSnapshot)

            val selectedMatches = matchesSnapshot[selectedLeagueId]?.get(selectedSeasonId).orEmpty()
            val selectedError = matchErrorsSnapshot[selectedLeagueId]?.get(selectedSeasonId)
                ?.takeUnless { it.isNullOrBlank() }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    seasonsByLeague = seasonsSnapshot,
                    errorMessage = encounteredSeasonError,
                    selectedLeagueId = selectedLeagueId,
                    selectedSeasonId = selectedSeasonId,
                    futureMatches = selectedMatches,
                    isMatchesLoading = false,
                    matchesErrorMessage = selectedError,
                    matchesByLeagueSeason = matchesSnapshot,
                    matchErrorsByLeagueSeason = matchErrorsSnapshot,
                    isDataLoaded = true,
                )
            }
        }
    }

    fun refreshSchedule() {
        loadAllLeagueSeasons(forceRefresh = true)
    }

    fun onLeagueSelected(leagueId: Int) {
        _uiState.update { state ->
            val seasons = state.seasonsByLeague[leagueId].orEmpty()
            val currentSeasonId = state.selectedSeasonId
            val validSeasonId = when {
                seasons.isEmpty() -> null
                seasons.any { it.id == currentSeasonId } -> currentSeasonId
                else -> seasons.firstOrNull()?.id
            }

            val matches = if (validSeasonId != null) {
                state.matchesByLeagueSeason[leagueId]?.get(validSeasonId).orEmpty()
            } else {
                emptyList()
            }
            val matchesError = validSeasonId?.let { season ->
                state.matchErrorsByLeagueSeason[leagueId]?.get(season)
            }?.takeUnless { it.isNullOrBlank() }

            state.copy(
                selectedLeagueId = leagueId,
                selectedSeasonId = validSeasonId,
                futureMatches = matches,
                matchesErrorMessage = matchesError,
            )
        }
    }

    fun onSeasonSelected(seasonId: Int) {
        val leagueId = _uiState.value.selectedLeagueId ?: return
        val seasons = _uiState.value.seasonsByLeague[leagueId].orEmpty()
        if (seasons.none { it.id == seasonId }) {
            return
        }
        if (_uiState.value.selectedSeasonId == seasonId) {
            return
        }

        _uiState.update { state ->
            val matches = state.matchesByLeagueSeason[leagueId]?.get(seasonId).orEmpty()
            val matchesError = state.matchErrorsByLeagueSeason[leagueId]?.get(seasonId)
                ?.takeUnless { it.isNullOrBlank() }
            state.copy(
                selectedSeasonId = seasonId,
                futureMatches = matches,
                matchesErrorMessage = matchesError,
            )
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
        private const val DEFAULT_SEASONS_ERROR_MESSAGE = "Unable to load seasons"
        private const val DEFAULT_MATCHES_ERROR_MESSAGE = "Unable to load matches"
    }
}
