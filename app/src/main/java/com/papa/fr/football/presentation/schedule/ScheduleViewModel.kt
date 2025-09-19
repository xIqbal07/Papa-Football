package com.papa.fr.football.presentation.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.papa.fr.football.R
import com.papa.fr.football.common.dropdown.LeagueItem
import com.papa.fr.football.domain.model.LiveMatch
import com.papa.fr.football.domain.model.Match
import com.papa.fr.football.domain.model.Season
import com.papa.fr.football.domain.usecase.GetLiveMatchesUseCase
import com.papa.fr.football.domain.usecase.GetSeasonsUseCase
import com.papa.fr.football.domain.usecase.GetUpcomingMatchesUseCase
import com.papa.fr.football.presentation.schedule.matches.MatchUiModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicReference

/**
 * Coordinates league metadata and season loading for the schedule feature.
 */
class ScheduleViewModel(
    private val getSeasonsUseCase: GetSeasonsUseCase,
    private val getUpcomingMatchesUseCase: GetUpcomingMatchesUseCase,
    private val getLiveMatchesUseCase: GetLiveMatchesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    private var liveMatchesJob: Job? = null

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

    // Remembers the user's last manual selection so background reloads don't override it.
    private val pendingUserLeagueId = AtomicReference<Int?>(null)
    private val pendingUserSeasonId = AtomicReference<Int?>(null)

    /**
     * Loads seasons for every league and progressively emits match updates as each response arrives.
     */
    fun loadAllLeagueSeasons(forceRefresh: Boolean = false) {
        val refreshInstant = Instant.now()
        loadLiveMatches(forceRefresh)
        if (!forceRefresh && _uiState.value.isDataLoaded) {
            _uiState.update { it.copy(lastUpdatedAt = refreshInstant) }
            return
        }

        _uiState.update { it.copy(lastUpdatedAt = refreshInstant) }

        viewModelScope.launch {
            val previousState = _uiState.value
            var desiredLeagueId = pendingUserLeagueId.get() ?: previousState.selectedLeagueId
            var desiredSeasonId = pendingUserSeasonId.get() ?: previousState.selectedSeasonId

            val seasonsByLeague = mutableMapOf<Int, List<Season>>()
            val matchesByLeagueSeason =
                mutableMapOf<Int, MutableMap<Int, List<MatchUiModel.Future>>>()
            val matchErrorsByLeagueSeason = mutableMapOf<Int, MutableMap<Int, String?>>()
            var encounteredSeasonError: String? = null
            var processedLeagueCount = 0

            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    isMatchesLoading = true,
                    matchesErrorMessage = null,
                    futureMatches = emptyList(),
                    matchesByLeagueSeason = emptyMap(),
                    matchErrorsByLeagueSeason = emptyMap(),
                    isDataLoaded = false,
                    lastUpdatedAt = refreshInstant,
                )
            }

            fun emitProgress() {
                val preferredLeagueId = pendingUserLeagueId.get() ?: desiredLeagueId
                val preferredSeasonId = pendingUserSeasonId.get() ?: desiredSeasonId
                val (resolvedLeagueId, resolvedSeasonId) = determineSelection(
                    seasonsByLeague = seasonsByLeague,
                    preferredLeagueId = preferredLeagueId,
                    preferredSeasonId = preferredSeasonId,
                )
                desiredLeagueId = resolvedLeagueId
                desiredSeasonId = resolvedSeasonId
                pendingUserLeagueId.set(resolvedLeagueId)
                pendingUserSeasonId.set(resolvedSeasonId)

                val selectedMatches = resolvedLeagueId?.let { leagueId ->
                    resolvedSeasonId?.let { seasonId ->
                        matchesByLeagueSeason[leagueId]?.get(seasonId).orEmpty()
                    }
                }.orEmpty()

                val selectedError = resolvedLeagueId?.let { leagueId ->
                    resolvedSeasonId?.let { seasonId ->
                        matchErrorsByLeagueSeason[leagueId]?.get(seasonId)
                    }
                }?.takeUnless { it.isNullOrBlank() }

                val hasLoadedAllSeasons = processedLeagueCount >= _leagueItems.value.size
                val hasLoadedSelectedMatches =
                    if (resolvedLeagueId == null || resolvedSeasonId == null) {
                        true
                    } else {
                        matchesByLeagueSeason[resolvedLeagueId]
                            ?.containsKey(resolvedSeasonId) == true
                    }

                _uiState.update {
                    it.copy(
                        isLoading = !hasLoadedAllSeasons,
                        seasonsByLeague = seasonsByLeague.toMap(),
                        errorMessage = encounteredSeasonError,
                        selectedLeagueId = resolvedLeagueId,
                        selectedSeasonId = resolvedSeasonId,
                        futureMatches = selectedMatches,
                        isMatchesLoading = !hasLoadedSelectedMatches,
                        matchesErrorMessage = selectedError,
                        matchesByLeagueSeason =
                            matchesByLeagueSeason.mapValues { entry -> entry.value.toMap() },
                        matchErrorsByLeagueSeason =
                            matchErrorsByLeagueSeason.mapValues { entry -> entry.value.toMap() },
                        isDataLoaded = hasLoadedAllSeasons,
                    )
                }
            }

            _leagueItems.value.forEach { league ->
                val seasonsResult = runCatching { getSeasonsUseCase(league.id) }
                val seasons = seasonsResult.getOrElse { throwable ->
                    if (encounteredSeasonError.isNullOrBlank()) {
                        encounteredSeasonError =
                            throwable.message ?: DEFAULT_SEASONS_ERROR_MESSAGE
                    }
                    emptyList()
                }

                seasonsByLeague[league.id] = seasons
                matchesByLeagueSeason.getOrPut(league.id) { mutableMapOf() }
                matchErrorsByLeagueSeason.getOrPut(league.id) { mutableMapOf() }
                processedLeagueCount += 1
                emitProgress()

                seasons.forEach { season ->
                    var hasEmittedMatches = false
                    var encounteredMatchesError: String? = null

                    getUpcomingMatchesUseCase(league.id, season.id)
                        .onEach { matches ->
                            hasEmittedMatches = true
                            matchesByLeagueSeason
                                .getOrPut(league.id) { mutableMapOf() }[season.id] =
                                matches.map { it.toUiModel() }
                            matchErrorsByLeagueSeason
                                .getOrPut(league.id) { mutableMapOf() }[season.id] = null
                            emitProgress()
                        }
                        .catch { throwable ->
                            hasEmittedMatches = true
                            encounteredMatchesError =
                                throwable.message ?: DEFAULT_MATCHES_ERROR_MESSAGE
                            matchesByLeagueSeason
                                .getOrPut(league.id) { mutableMapOf() }[season.id] = emptyList()
                            matchErrorsByLeagueSeason
                                .getOrPut(league.id) { mutableMapOf() }[season.id] =
                                encounteredMatchesError
                            emitProgress()
                        }
                        .onCompletion {
                            if (!hasEmittedMatches && encounteredMatchesError == null) {
                                matchesByLeagueSeason
                                    .getOrPut(league.id) { mutableMapOf() }[season.id] = emptyList()
                                matchErrorsByLeagueSeason
                                    .getOrPut(league.id) { mutableMapOf() }[season.id] = null
                                emitProgress()
                            }
                        }
                        .collect()
                }
            }

            emitProgress()
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

            val leagueMatches = state.matchesByLeagueSeason[leagueId]
            val matches = validSeasonId?.let { season ->
                leagueMatches?.get(season).orEmpty()
            }.orEmpty()
            val matchesError = validSeasonId?.let { season ->
                state.matchErrorsByLeagueSeason[leagueId]?.get(season)
            }?.takeUnless { it.isNullOrBlank() }

            val isSelectionLoaded = if (validSeasonId == null) {
                true
            } else {
                leagueMatches?.containsKey(validSeasonId) == true
            }

            state.copy(
                selectedLeagueId = leagueId,
                selectedSeasonId = validSeasonId,
                futureMatches = matches,
                matchesErrorMessage = matchesError,
                isMatchesLoading = !isSelectionLoaded,
            )
        }
        pendingUserLeagueId.set(leagueId)
        pendingUserSeasonId.set(_uiState.value.selectedSeasonId)
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
            val leagueMatches = state.matchesByLeagueSeason[leagueId]
            val matches = leagueMatches?.get(seasonId).orEmpty()
            val matchesError = state.matchErrorsByLeagueSeason[leagueId]?.get(seasonId)
                ?.takeUnless { it.isNullOrBlank() }
            val isSelectionLoaded = leagueMatches?.containsKey(seasonId) == true
            state.copy(
                selectedSeasonId = seasonId,
                futureMatches = matches,
                matchesErrorMessage = matchesError,
                isMatchesLoading = !isSelectionLoaded,
            )
        }
        pendingUserSeasonId.set(seasonId)
    }

    private fun loadLiveMatches(forceRefresh: Boolean) {
        if (!forceRefresh && (liveMatchesJob?.isActive == true || _uiState.value.isLiveMatchesLoading)) {
            return
        }

        liveMatchesJob?.cancel()
        val job = viewModelScope.launch {
            val jobReference = coroutineContext[Job]
            var hasEmitted = false
            _uiState.update {
                it.copy(
                    isLiveMatchesLoading = true,
                    liveMatchesErrorMessage = null,
                )
            }

            getLiveMatchesUseCase(LIVE_SPORT_ID)
                .onEach { matches ->
                    val uiModels = matches.map { it.toUiModel() }
                    val shouldClearLoading = !hasEmitted
                    hasEmitted = true
                    _uiState.update { state ->
                        var newState = state.copy(
                            liveMatches = uiModels,
                            liveMatchesErrorMessage = null,
                        )
                        if (shouldClearLoading) {
                            newState = newState.copy(isLiveMatchesLoading = false)
                        }
                        newState
                    }
                }
                .catch { throwable ->
                    hasEmitted = true
                    _uiState.update {
                        it.copy(
                            liveMatches = emptyList(),
                            liveMatchesErrorMessage =
                                throwable.message ?: DEFAULT_LIVE_MATCHES_ERROR_MESSAGE,
                            isLiveMatchesLoading = false,
                        )
                    }
                }
                .onCompletion {
                    if (liveMatchesJob == jobReference) {
                        if (!hasEmitted) {
                            _uiState.update { state ->
                                state.copy(isLiveMatchesLoading = false)
                            }
                        }
                        liveMatchesJob = null
                    }
                }
                .collect()
        }
        liveMatchesJob = job
    }

    private fun determineSelection(
        seasonsByLeague: Map<Int, List<Season>>,
        preferredLeagueId: Int?,
        preferredSeasonId: Int?,
    ): Pair<Int?, Int?> {
        val availableLeagueIds = _leagueItems.value.map { it.id }
        val resolvedLeagueId = when {
            preferredLeagueId != null && availableLeagueIds.contains(preferredLeagueId) ->
                preferredLeagueId

            else -> availableLeagueIds.firstOrNull { seasonsByLeague[it].orEmpty().isNotEmpty() }
        }

        val seasons = resolvedLeagueId?.let { seasonsByLeague[it] }.orEmpty()
        val resolvedSeasonId = when {
            seasons.isEmpty() -> null
            preferredSeasonId != null && seasons.any { it.id == preferredSeasonId } ->
                preferredSeasonId

            else -> seasons.firstOrNull()?.id
        }

        return resolvedLeagueId to resolvedSeasonId
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

    private fun LiveMatch.toUiModel(): MatchUiModel.Live {
        return MatchUiModel.Live(
            id = id,
            homeTeamId = homeTeam.id,
            homeTeamName = homeTeam.name,
            awayTeamId = awayTeam.id,
            awayTeamName = awayTeam.name,
            homeScore = homeScore,
            awayScore = awayScore,
            homeLogoBase64 = homeTeam.logoBase64,
            awayLogoBase64 = awayTeam.logoBase64,
            statusLabel = status,
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
        private const val DEFAULT_LIVE_MATCHES_ERROR_MESSAGE = "Unable to load live matches"
        private const val LIVE_SPORT_ID = 1
    }
}
