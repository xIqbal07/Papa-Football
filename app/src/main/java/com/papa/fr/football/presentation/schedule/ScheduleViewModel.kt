package com.papa.fr.football.presentation.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.papa.fr.football.R
import com.papa.fr.football.common.dropdown.LeagueItem
import com.papa.fr.football.domain.model.LiveMatch
import com.papa.fr.football.domain.model.Match
import com.papa.fr.football.domain.model.Season
import com.papa.fr.football.domain.usecase.GetLiveMatchesUseCase
import com.papa.fr.football.domain.usecase.GetRecentMatchesUseCase
import com.papa.fr.football.domain.usecase.GetSeasonsUseCase
import com.papa.fr.football.domain.usecase.GetUpcomingMatchesUseCase
import com.papa.fr.football.presentation.schedule.matches.MatchUiModel
import com.papa.fr.football.presentation.schedule.matches.MatchesTabType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val getRecentMatchesUseCase: GetRecentMatchesUseCase,
    private val getLiveMatchesUseCase: GetLiveMatchesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    private var liveMatchesJob: Job? = null
    private var loadSeasonsJob: Job? = null
    private val upcomingMatchJobs = mutableMapOf<Pair<Int, Int>, Job>()
    private var forcePastMatchesRefresh = false

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

    private val loadingPastMatches = mutableSetOf<Pair<Int, Int>>()

    fun defaultLeagueLabel(): String = _leagueItems.value.firstOrNull()?.name.orEmpty()

    fun seasonsForLeague(leagueId: Int): List<Season> {
        val seasons = _uiState.value.seasonsByLeague[leagueId].orEmpty()
        return filterSeasonsForTab(seasons, _uiState.value.selectedMatchesTab)
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

        loadSeasonsJob?.cancel()
        loadSeasonsJob = viewModelScope.launch {
            val loadingData = createSeasonLoadingData(refreshInstant, forceRefresh)
            _leagueItems.value.forEach { league ->
                loadingData.processLeague(league)
            }
            emitSeasonLoadingProgress(loadingData)
        }
    }

    fun refreshSchedule() {
        loadAllLeagueSeasons(forceRefresh = true)
    }

    fun onMatchesTabSelected(tabType: MatchesTabType) {
        if (_uiState.value.selectedMatchesTab == tabType) {
            return
        }

        _uiState.update { state ->
            val leagueId = state.selectedLeagueId
            val seasons = leagueId?.let { state.seasonsByLeague[it].orEmpty() }.orEmpty()
            val filteredSeasons = filterSeasonsForTab(seasons, tabType)
            val currentSeasonId = state.selectedSeasonId
            val validSeasonId = when {
                filteredSeasons.isEmpty() -> null
                currentSeasonId != null && filteredSeasons.any { it.id == currentSeasonId } ->
                    currentSeasonId

                else -> filteredSeasons.firstOrNull()?.id
            }

            val futureMatchesMap = leagueId?.let { state.matchesByLeagueSeason[it] }
            val futureMatches = if (leagueId != null && validSeasonId != null) {
                futureMatchesMap?.get(validSeasonId).orEmpty()
            } else {
                emptyList()
            }
            val futureError = if (leagueId != null && validSeasonId != null) {
                state.matchErrorsByLeagueSeason[leagueId]?.get(validSeasonId)
                    ?.takeUnless { it.isNullOrBlank() }
            } else {
                null
            }
            val isFutureLoaded = if (validSeasonId == null) {
                true
            } else {
                futureMatchesMap?.containsKey(validSeasonId) == true
            }

            val pastMatchesMap = leagueId?.let { state.pastMatchesByLeagueSeason[it] }
            val pastMatches = if (leagueId != null && validSeasonId != null) {
                pastMatchesMap?.get(validSeasonId).orEmpty()
            } else {
                emptyList()
            }
            val pastError = if (leagueId != null && validSeasonId != null) {
                state.pastMatchErrorsByLeagueSeason[leagueId]?.get(validSeasonId)
                    ?.takeUnless { it.isNullOrBlank() }
            } else {
                null
            }
            val isPastLoaded = if (leagueId == null || validSeasonId == null) {
                true
            } else {
                pastMatchesMap?.containsKey(validSeasonId) == true ||
                    loadingPastMatches.contains(leagueId to validSeasonId)
            }

            state.copy(
                selectedMatchesTab = tabType,
                selectedSeasonId = validSeasonId,
                futureMatches = futureMatches,
                matchesErrorMessage = futureError,
                isMatchesLoading = !isFutureLoaded,
                pastMatches = pastMatches,
                pastMatchesErrorMessage = pastError,
                isPastMatchesLoading = !isPastLoaded,
            )
        }
        pendingUserSeasonId.set(_uiState.value.selectedSeasonId)
        when (tabType) {
            MatchesTabType.Past -> {
                val currentState = _uiState.value
                loadPastMatchesIfNeeded(currentState.selectedLeagueId, currentState.selectedSeasonId)
            }

            MatchesTabType.Live -> loadLiveMatches(forceRefresh = false)
            MatchesTabType.Future -> Unit
        }
    }

    fun onLeagueSelected(leagueId: Int) {
        _uiState.update { state ->
            val allSeasons = state.seasonsByLeague[leagueId].orEmpty()
            val filteredSeasons = filterSeasonsForTab(allSeasons, state.selectedMatchesTab)
            val currentSeasonId = state.selectedSeasonId
            val validSeasonId = when {
                filteredSeasons.isEmpty() -> null
                currentSeasonId != null && filteredSeasons.any { it.id == currentSeasonId } ->
                    currentSeasonId

                else -> filteredSeasons.firstOrNull()?.id
            }

            val futureMatchesMap = state.matchesByLeagueSeason[leagueId]
            val futureMatches = validSeasonId?.let { season ->
                futureMatchesMap?.get(season).orEmpty()
            }.orEmpty()
            val futureError = validSeasonId?.let { season ->
                state.matchErrorsByLeagueSeason[leagueId]?.get(season)
            }?.takeUnless { it.isNullOrBlank() }

            val pastMatchesMap = state.pastMatchesByLeagueSeason[leagueId]
            val pastMatches = validSeasonId?.let { season ->
                pastMatchesMap?.get(season).orEmpty()
            }.orEmpty()
            val pastError = validSeasonId?.let { season ->
                state.pastMatchErrorsByLeagueSeason[leagueId]?.get(season)
            }?.takeUnless { it.isNullOrBlank() }

            val isFutureLoaded = if (validSeasonId == null) {
                true
            } else {
                futureMatchesMap?.containsKey(validSeasonId) == true
            }
            val isPastLoaded = if (validSeasonId == null) {
                true
            } else {
                pastMatchesMap?.containsKey(validSeasonId) == true ||
                    loadingPastMatches.contains(leagueId to validSeasonId)
            }

            state.copy(
                selectedLeagueId = leagueId,
                selectedSeasonId = validSeasonId,
                futureMatches = futureMatches,
                matchesErrorMessage = futureError,
                isMatchesLoading = !isFutureLoaded,
                pastMatches = pastMatches,
                pastMatchesErrorMessage = pastError,
                isPastMatchesLoading = !isPastLoaded,
            )
        }
        pendingUserLeagueId.set(leagueId)
        pendingUserSeasonId.set(_uiState.value.selectedSeasonId)
        if (_uiState.value.selectedMatchesTab == MatchesTabType.Past) {
            loadPastMatchesIfNeeded(leagueId, _uiState.value.selectedSeasonId)
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
            val futureMatchesMap = state.matchesByLeagueSeason[leagueId]
            val futureMatches = futureMatchesMap?.get(seasonId).orEmpty()
            val futureError = state.matchErrorsByLeagueSeason[leagueId]?.get(seasonId)
                ?.takeUnless { it.isNullOrBlank() }
            val isFutureLoaded = futureMatchesMap?.containsKey(seasonId) == true

            val pastMatchesMap = state.pastMatchesByLeagueSeason[leagueId]
            val pastMatches = pastMatchesMap?.get(seasonId).orEmpty()
            val pastError = state.pastMatchErrorsByLeagueSeason[leagueId]?.get(seasonId)
                ?.takeUnless { it.isNullOrBlank() }
            val isPastLoaded = pastMatchesMap?.containsKey(seasonId) == true ||
                loadingPastMatches.contains(leagueId to seasonId)

            state.copy(
                selectedSeasonId = seasonId,
                futureMatches = futureMatches,
                matchesErrorMessage = futureError,
                isMatchesLoading = !isFutureLoaded,
                pastMatches = pastMatches,
                pastMatchesErrorMessage = pastError,
                isPastMatchesLoading = !isPastLoaded,
            )
        }
        pendingUserSeasonId.set(seasonId)
        if (_uiState.value.selectedMatchesTab == MatchesTabType.Past) {
            loadPastMatchesIfNeeded(leagueId, seasonId)
        }
    }

    private fun loadPastMatchesIfNeeded(leagueId: Int?, seasonId: Int?) {
        if (leagueId == null || seasonId == null) {
            return
        }
        if (_uiState.value.pastMatchesByLeagueSeason[leagueId]?.containsKey(seasonId) == true) {
            return
        }
        if (!loadingPastMatches.add(leagueId to seasonId)) {
            return
        }

        val shouldForceRefresh = forcePastMatchesRefresh
        if (shouldForceRefresh) {
            forcePastMatchesRefresh = false
        }

        _uiState.update { state ->
            if (state.selectedLeagueId == leagueId && state.selectedSeasonId == seasonId) {
                state.copy(
                    isPastMatchesLoading = true,
                    pastMatchesErrorMessage = null,
                )
            } else {
                state
            }
        }

        viewModelScope.launch {
            val result = runCatching {
                getRecentMatchesUseCase(leagueId, seasonId, shouldForceRefresh)
            }
            val matches = result.getOrElse { emptyList() }
            val matchesUi = matches.map { it.toPastUiModel() }
            val errorMessage = result.exceptionOrNull()?.message ?: DEFAULT_MATCHES_ERROR_MESSAGE
            loadingPastMatches.remove(leagueId to seasonId)

            _uiState.update { state ->
                val updatedPastMatchesByLeague = state.pastMatchesByLeagueSeason
                    .toMutableMap()
                    .apply {
                        val leagueMap = getOrPut(leagueId) { emptyMap() }.toMutableMap()
                        leagueMap[seasonId] = matchesUi
                        put(leagueId, leagueMap)
                    }
                val updatedErrorsByLeague = state.pastMatchErrorsByLeagueSeason
                    .toMutableMap()
                    .apply {
                        val leagueErrors = getOrPut(leagueId) { emptyMap() }.toMutableMap()
                        leagueErrors[seasonId] = if (result.isSuccess) null else errorMessage
                        put(leagueId, leagueErrors)
                    }

                val isCurrentSelection =
                    state.selectedLeagueId == leagueId && state.selectedSeasonId == seasonId

                state.copy(
                    pastMatchesByLeagueSeason = updatedPastMatchesByLeague,
                    pastMatchErrorsByLeagueSeason = updatedErrorsByLeague,
                    pastMatches = if (isCurrentSelection) matchesUi else state.pastMatches,
                    pastMatchesErrorMessage = if (isCurrentSelection) {
                        if (result.isSuccess) null else errorMessage
                    } else {
                        state.pastMatchesErrorMessage
                    },
                    isPastMatchesLoading = if (isCurrentSelection) {
                        false
                    } else {
                        state.isPastMatchesLoading
                    },
                )
            }
        }
    }

    private fun createSeasonLoadingData(
        refreshInstant: Instant,
        forceRefresh: Boolean,
    ): SeasonLoadingData {
        val previousState = _uiState.value
        val desiredLeagueId = pendingUserLeagueId.get() ?: previousState.selectedLeagueId
        val desiredSeasonId = pendingUserSeasonId.get() ?: previousState.selectedSeasonId

        loadingPastMatches.clear()
        upcomingMatchJobs.values.forEach { it.cancel() }
        upcomingMatchJobs.clear()
        forcePastMatchesRefresh = forceRefresh

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                isMatchesLoading = true,
                matchesErrorMessage = null,
                futureMatches = emptyList(),
                matchesByLeagueSeason = emptyMap(),
                matchErrorsByLeagueSeason = emptyMap(),
                pastMatches = emptyList(),
                isPastMatchesLoading = false,
                pastMatchesErrorMessage = null,
                pastMatchesByLeagueSeason = emptyMap(),
                pastMatchErrorsByLeagueSeason = emptyMap(),
                isDataLoaded = false,
                lastUpdatedAt = refreshInstant,
            )
        }

        return SeasonLoadingData(
            desiredLeagueId = desiredLeagueId,
            desiredSeasonId = desiredSeasonId,
            forceRefresh = forceRefresh,
        )
    }

    private suspend fun SeasonLoadingData.processLeague(league: LeagueItem) {
        val seasonsResult = runCatching { getSeasonsUseCase(league.id, forceRefresh) }
        val seasons = seasonsResult.getOrElse { throwable ->
            guard {
                if (encounteredSeasonError.isNullOrBlank()) {
                    encounteredSeasonError = throwable.message ?: DEFAULT_SEASONS_ERROR_MESSAGE
                }
            }
            emptyList()
        }

        guard {
            seasonsByLeague[league.id] = seasons
            matchesByLeagueSeason.getOrPut(league.id) { mutableMapOf() }
            matchErrorsByLeagueSeason.getOrPut(league.id) { mutableMapOf() }
            processedLeagueCount += 1
        }
        emitSeasonLoadingProgress(this)

        val latestSeason = seasons.firstOrNull() ?: return
        collectUpcomingMatches(this, league.id, latestSeason.id)
    }

    private fun collectUpcomingMatches(
        loadingData: SeasonLoadingData,
        leagueId: Int,
        seasonId: Int,
    ) {
        val key = leagueId to seasonId
        upcomingMatchJobs.remove(key)?.cancel()

        val job = viewModelScope.launch {
            var hasEmittedMatches = false
            var encounteredMatchesError: String? = null

            getUpcomingMatchesUseCase(leagueId, seasonId, loadingData.forceRefresh)
                .onEach { matches ->
                    hasEmittedMatches = true
                    loadingData.guard {
                        matchesByLeagueSeason
                            .getOrPut(leagueId) { mutableMapOf() }[seasonId] =
                            matches.map { it.toFutureUiModel() }
                        matchErrorsByLeagueSeason
                            .getOrPut(leagueId) { mutableMapOf() }[seasonId] = null
                    }
                    emitSeasonLoadingProgress(loadingData)
                }
                .catch { throwable ->
                    hasEmittedMatches = true
                    encounteredMatchesError = throwable.message ?: DEFAULT_MATCHES_ERROR_MESSAGE
                    loadingData.guard {
                        matchesByLeagueSeason
                            .getOrPut(leagueId) { mutableMapOf() }[seasonId] = emptyList()
                        matchErrorsByLeagueSeason
                            .getOrPut(leagueId) { mutableMapOf() }[seasonId] = encounteredMatchesError
                    }
                    emitSeasonLoadingProgress(loadingData)
                }
                .onCompletion { cause ->
                    if (cause is CancellationException) {
                        return@onCompletion
                    }
                    if (!hasEmittedMatches && encounteredMatchesError == null) {
                        loadingData.guard {
                            matchesByLeagueSeason
                                .getOrPut(leagueId) { mutableMapOf() }[seasonId] = emptyList()
                            matchErrorsByLeagueSeason
                                .getOrPut(leagueId) { mutableMapOf() }[seasonId] = null
                        }
                        emitSeasonLoadingProgress(loadingData)
                    }
                }
                .collect()
        }

        upcomingMatchJobs[key] = job
    }

    private suspend fun emitSeasonLoadingProgress(loadingData: SeasonLoadingData) {
        val snapshot = loadingData.guard {
            val preferredLeagueId = pendingUserLeagueId.get() ?: desiredLeagueId
            val preferredSeasonId = pendingUserSeasonId.get() ?: desiredSeasonId
            val currentTab = _uiState.value.selectedMatchesTab
            val (resolvedLeagueId, resolvedSeasonId) = determineSelection(
                seasonsByLeague = seasonsByLeague,
                preferredLeagueId = preferredLeagueId,
                preferredSeasonId = preferredSeasonId,
                tabType = currentTab,
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

            SeasonProgressSnapshot(
                resolvedLeagueId = resolvedLeagueId,
                resolvedSeasonId = resolvedSeasonId,
                selectedMatches = selectedMatches,
                selectedError = selectedError,
                hasLoadedAllSeasons = hasLoadedAllSeasons,
                hasLoadedSelectedMatches = hasLoadedSelectedMatches,
                seasonsByLeague = seasonsByLeague.toMap(),
                matchesByLeagueSeason = matchesByLeagueSeason.mapValues { entry -> entry.value.toMap() },
                matchErrorsByLeagueSeason =
                    matchErrorsByLeagueSeason.mapValues { entry -> entry.value.toMap() },
                encounteredSeasonError = encounteredSeasonError,
            )
        }

        _uiState.update {
            it.copy(
                isLoading = !snapshot.hasLoadedAllSeasons,
                seasonsByLeague = snapshot.seasonsByLeague,
                errorMessage = snapshot.encounteredSeasonError,
                selectedLeagueId = snapshot.resolvedLeagueId,
                selectedSeasonId = snapshot.resolvedSeasonId,
                futureMatches = snapshot.selectedMatches,
                isMatchesLoading = !snapshot.hasLoadedSelectedMatches,
                matchesErrorMessage = snapshot.selectedError,
                matchesByLeagueSeason = snapshot.matchesByLeagueSeason,
                matchErrorsByLeagueSeason = snapshot.matchErrorsByLeagueSeason,
                isDataLoaded = snapshot.hasLoadedAllSeasons,
            )
        }
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
        tabType: MatchesTabType,
    ): Pair<Int?, Int?> {
        val availableLeagueIds = _leagueItems.value.map { it.id }
        val resolvedLeagueId = when {
            preferredLeagueId != null && availableLeagueIds.contains(preferredLeagueId) ->
                preferredLeagueId

            else -> availableLeagueIds.firstOrNull { seasonsByLeague[it].orEmpty().isNotEmpty() }
        }

        val seasons = resolvedLeagueId?.let { seasonsByLeague[it] }.orEmpty()
        val filteredSeasons = filterSeasonsForTab(seasons, tabType)
        val resolvedSeasonId = when {
            filteredSeasons.isEmpty() -> null
            preferredSeasonId != null && filteredSeasons.any { it.id == preferredSeasonId } ->
                preferredSeasonId

            else -> filteredSeasons.firstOrNull()?.id
        }

        return resolvedLeagueId to resolvedSeasonId
    }

    private fun filterSeasonsForTab(
        seasons: List<Season>,
        tabType: MatchesTabType,
    ): List<Season> {
        return when (tabType) {
            MatchesTabType.Future, MatchesTabType.Live -> seasons.take(1)
            MatchesTabType.Past -> seasons.take(10)
        }
    }

    private fun Match.toFutureUiModel(): MatchUiModel.Future {
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

    private fun Match.toPastUiModel(): MatchUiModel.Past {
        val instant = toInstant(startTimestamp)
        val zonedDateTime = instant.atZone(ZoneId.systemDefault())
        val dateLabel = zonedDateTime.format(PAST_DATE_FORMATTER)
        val scoreLabel = formatScore(homeScore, awayScore)

        return MatchUiModel.Past(
            id = id,
            homeTeamName = homeTeam.name,
            awayTeamName = awayTeam.name,
            startDateLabel = dateLabel,
            scoreLabel = scoreLabel,
            homeLogoBase64 = homeTeam.logoBase64,
            awayLogoBase64 = awayTeam.logoBase64,
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

    private fun formatScore(homeScore: Int?, awayScore: Int?): String {
        if (homeScore == null || awayScore == null) {
            return SCORE_PLACEHOLDER
        }
        return buildString {
            append(homeScore)
            append(':')
            append(awayScore)
        }
    }

    private fun toInstant(timestamp: Long): Instant {
        return if (timestamp < 1_000_000_000_000L) {
            Instant.ofEpochSecond(timestamp)
        } else {
            Instant.ofEpochMilli(timestamp)
        }
    }

    private class SeasonLoadingData(
        var desiredLeagueId: Int?,
        var desiredSeasonId: Int?,
        val forceRefresh: Boolean,
    ) {
        val seasonsByLeague: MutableMap<Int, List<Season>> = mutableMapOf()
        val matchesByLeagueSeason:
            MutableMap<Int, MutableMap<Int, List<MatchUiModel.Future>>> = mutableMapOf()
        val matchErrorsByLeagueSeason:
            MutableMap<Int, MutableMap<Int, String?>> = mutableMapOf()
        var encounteredSeasonError: String? = null
        var processedLeagueCount: Int = 0
        private val mutex = Mutex()

        suspend fun <T> guard(block: SeasonLoadingData.() -> T): T = mutex.withLock { block() }
    }

    private data class SeasonProgressSnapshot(
        val resolvedLeagueId: Int?,
        val resolvedSeasonId: Int?,
        val selectedMatches: List<MatchUiModel.Future>,
        val selectedError: String?,
        val hasLoadedAllSeasons: Boolean,
        val hasLoadedSelectedMatches: Boolean,
        val seasonsByLeague: Map<Int, List<Season>>,
        val matchesByLeagueSeason: Map<Int, Map<Int, List<MatchUiModel.Future>>>,
        val matchErrorsByLeagueSeason: Map<Int, Map<Int, String?>>, 
        val encounteredSeasonError: String?,
    )

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd")
        private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        private val PAST_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        private const val SCORE_PLACEHOLDER = "-"
        private const val DEFAULT_SEASONS_ERROR_MESSAGE = "Unable to load seasons"
        private const val DEFAULT_MATCHES_ERROR_MESSAGE = "Unable to load matches"
        private const val DEFAULT_LIVE_MATCHES_ERROR_MESSAGE = "Unable to load live matches"
        private const val LIVE_SPORT_ID = 1
    }
}
