package com.papa.fr.football.data.repository

import com.papa.fr.football.data.local.dao.LiveMatchDao
import com.papa.fr.football.data.local.dao.MatchDao
import com.papa.fr.football.data.local.entity.MatchRefreshEntity
import com.papa.fr.football.data.local.entity.MatchTypeEntity
import com.papa.fr.football.data.local.mapper.toDomain
import com.papa.fr.football.data.local.mapper.toEntity
import com.papa.fr.football.data.mapper.toDomain
import com.papa.fr.football.data.mapper.toLiveDomain
import com.papa.fr.football.data.remote.LiveEventsApiService
import com.papa.fr.football.data.remote.SeasonApiService
import com.papa.fr.football.domain.model.LiveMatch
import com.papa.fr.football.domain.model.Match
import com.papa.fr.football.domain.repository.MatchRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.LinkedHashSet

class MatchRepositoryImpl(
    private val seasonApiService: SeasonApiService,
    private val liveEventsApiService: LiveEventsApiService,
    private val teamLogoProvider: TeamLogoProvider,
    private val matchDao: MatchDao,
    private val liveMatchDao: LiveMatchDao,
) : MatchRepository {

    private val liveLogoStateMutex = Mutex()
    private val liveLogoStateBySport = mutableMapOf<Int, LiveLogoPrefetchState>()

    override fun getUpcomingMatches(
        uniqueTournamentId: Int,
        seasonId: Int,
        forceRefresh: Boolean,
    ): Flow<List<Match>> =
        channelFlow {
            val localJob = launch {
                matchDao.observeMatches(uniqueTournamentId, seasonId, MatchTypeEntity.UPCOMING)
                    .collect { entities ->
                        send(entities.map { it.toDomain() })
                    }
            }

            launch {
                try {
                    warmUpcomingMatches(uniqueTournamentId, seasonId, forceRefresh)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Throwable) {
                    // Keep emitting cached values when the network request fails.
                }
            }

            awaitClose { localJob.cancel() }
        }

    override fun getRecentMatches(
        uniqueTournamentId: Int,
        seasonId: Int,
        forceRefresh: Boolean,
    ): Flow<List<Match>> = channelFlow {
        val localJob = launch {
            matchDao.observeMatches(uniqueTournamentId, seasonId, MatchTypeEntity.PAST)
                .collect { entities ->
                    send(entities.map { it.toDomain() })
                }
        }

        launch {
            try {
                if (!shouldRefreshPastMatches(uniqueTournamentId, seasonId, forceRefresh)) {
                    return@launch
                }
                warmRecentMatches(uniqueTournamentId, seasonId, forceRefresh)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                val hasCached = matchDao.hasMatches(
                    uniqueTournamentId,
                    seasonId,
                    MatchTypeEntity.PAST,
                )
                if (!hasCached) {
                    close(throwable)
                }
            }
        }

        awaitClose { localJob.cancel() }
    }

    override fun getLiveMatches(sportId: Int): Flow<List<LiveMatch>> = channelFlow {
        val localJob = launch {
            liveMatchDao.observeLiveMatches(sportId).collect { entities ->
                send(entities.map { it.toDomain() })
            }
        }

        launch {
            try {
                warmLiveMatches(sportId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                // Ignore remote failures and keep the cached live matches.
            }
        }

        awaitClose { localJob.cancel() }
    }

    override suspend fun warmUpcomingMatches(
        uniqueTournamentId: Int,
        seasonId: Int,
        forceRefresh: Boolean,
    ) {
        if (!shouldRefreshUpcomingMatches(uniqueTournamentId, seasonId, forceRefresh)) {
            return
        }

        fetchUpcomingMatchesStream(uniqueTournamentId, seasonId).collect { matches ->
            val entities = matches.map {
                it.toEntity(uniqueTournamentId, seasonId, MatchTypeEntity.UPCOMING)
            }
            matchDao.replaceMatches(
                uniqueTournamentId,
                seasonId,
                MatchTypeEntity.UPCOMING,
                entities,
                System.currentTimeMillis(),
            )
        }
    }

    override suspend fun warmRecentMatches(
        uniqueTournamentId: Int,
        seasonId: Int,
        forceRefresh: Boolean,
    ) {
        if (!shouldRefreshPastMatches(uniqueTournamentId, seasonId, forceRefresh)) {
            return
        }

        fetchPastMatchesStream(uniqueTournamentId, seasonId).collect { matches ->
            val entities = matches.map {
                it.toEntity(uniqueTournamentId, seasonId, MatchTypeEntity.PAST)
            }
            matchDao.replaceMatches(
                uniqueTournamentId,
                seasonId,
                MatchTypeEntity.PAST,
                entities,
                System.currentTimeMillis(),
            )
        }
    }

    override suspend fun warmLiveMatches(sportId: Int, forceRefresh: Boolean) {
        if (!forceRefresh && liveMatchDao.getLiveMatches(sportId).isNotEmpty()) {
            return
        }

        fetchLiveMatchesStream(sportId).collect { matches ->
            liveMatchDao.replaceLiveMatches(sportId, matches.map { it.toEntity(sportId) })
        }
    }

    private fun fetchUpcomingMatchesStream(
        uniqueTournamentId: Int,
        seasonId: Int,
    ): Flow<List<Match>> = channelFlow {
        val events = seasonApiService
            .getSeasonEvents(uniqueTournamentId, seasonId)
            .data
            .events

        if (events.isEmpty()) {
            send(emptyList())
            return@channelFlow
        }

        val orderedTeamIds = events
            .flatMap { event -> listOfNotNull(event.homeTeam?.id, event.awayTeam?.id) }
            .distinct()

        val teamIndicesById = mutableMapOf<Int, MutableList<Int>>()
        val logosByTeamId = mutableMapOf<Int, String>()
        val matches = MutableList(events.size) { index ->
            val event = events[index]
            val homeId = event.homeTeam?.id
            val awayId = event.awayTeam?.id

            if (homeId != null) {
                teamIndicesById.getOrPut(homeId) { mutableListOf() }.add(index)
                teamLogoProvider.peekCachedLogo(homeId)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { logosByTeamId[homeId] = it }
            }

            if (awayId != null) {
                teamIndicesById.getOrPut(awayId) { mutableListOf() }.add(index)
                teamLogoProvider.peekCachedLogo(awayId)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { logosByTeamId[awayId] = it }
            }

            event.toDomain(
                homeLogoBase64 = homeId?.let { logosByTeamId[it] },
                awayLogoBase64 = awayId?.let { logosByTeamId[it] },
            )
        }

        val emissionMutex = Mutex()
        val initialSnapshot = emissionMutex.withLock { matches.toList() }
        send(initialSnapshot)

        val pendingTeamIds = orderedTeamIds.filterNot { teamId ->
            logosByTeamId.containsKey(teamId)
        }

        val jobs = pendingTeamIds.map { teamId ->
            launch {
                val logo = runCatching { teamLogoProvider.getTeamLogo(teamId) }
                    .getOrElse { "" }

                if (logo.isBlank()) {
                    return@launch
                }

                val snapshot = emissionMutex.withLock {
                    val previous = logosByTeamId[teamId]
                    if (previous == logo) {
                        return@withLock null
                    }

                    logosByTeamId[teamId] = logo
                    val indices = teamIndicesById[teamId].orEmpty()
                    var hasChanged = false

                    for (index in indices) {
                        val event = events[index]
                        val updated = event.toDomain(
                            homeLogoBase64 = event.homeTeam?.id?.let { logosByTeamId[it] },
                            awayLogoBase64 = event.awayTeam?.id?.let { logosByTeamId[it] },
                        )
                        if (matches[index] != updated) {
                            matches[index] = updated
                            hasChanged = true
                        }
                    }

                    if (!hasChanged) {
                        null
                    } else {
                        matches.toList()
                    }
                }

                if (snapshot != null) {
                    send(snapshot)
                }
            }
        }

        jobs.joinAll()
    }

    private fun fetchPastMatchesStream(
        uniqueTournamentId: Int,
        seasonId: Int,
    ): Flow<List<Match>> = channelFlow {
        val events = seasonApiService
            .getSeasonEvents(uniqueTournamentId, seasonId, courseEvents = "last")
            .data
            .events

        if (events.isEmpty()) {
            send(emptyList())
            return@channelFlow
        }

        val orderedTeamIds = events
            .flatMap { event -> listOfNotNull(event.homeTeam?.id, event.awayTeam?.id) }
            .distinct()

        val teamIndicesById = mutableMapOf<Int, MutableList<Int>>()
        val logosByTeamId = mutableMapOf<Int, String>()
        val matches = MutableList(events.size) { index ->
            val event = events[index]
            val homeId = event.homeTeam?.id
            val awayId = event.awayTeam?.id

            if (homeId != null) {
                teamIndicesById.getOrPut(homeId) { mutableListOf() }.add(index)
                teamLogoProvider.peekCachedLogo(homeId)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { logosByTeamId[homeId] = it }
            }

            if (awayId != null) {
                teamIndicesById.getOrPut(awayId) { mutableListOf() }.add(index)
                teamLogoProvider.peekCachedLogo(awayId)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { logosByTeamId[awayId] = it }
            }

            event.toDomain(
                homeLogoBase64 = homeId?.let { logosByTeamId[it] },
                awayLogoBase64 = awayId?.let { logosByTeamId[it] },
            )
        }

        val emissionMutex = Mutex()
        val initialSnapshot = emissionMutex.withLock { matches.toList() }
        send(initialSnapshot)

        val pendingTeamIds = orderedTeamIds.filterNot { teamId ->
            logosByTeamId.containsKey(teamId)
        }

        val jobs = pendingTeamIds.map { teamId ->
            launch {
                val logo = runCatching { teamLogoProvider.getTeamLogo(teamId) }
                    .getOrElse { "" }

                if (logo.isBlank()) {
                    return@launch
                }

                val snapshot = emissionMutex.withLock {
                    val previous = logosByTeamId[teamId]
                    if (previous == logo) {
                        return@withLock null
                    }

                    logosByTeamId[teamId] = logo
                    val indices = teamIndicesById[teamId].orEmpty()
                    var hasChanged = false

                    for (index in indices) {
                        val event = events[index]
                        val updated = event.toDomain(
                            homeLogoBase64 = event.homeTeam?.id?.let { logosByTeamId[it] },
                            awayLogoBase64 = event.awayTeam?.id?.let { logosByTeamId[it] },
                        )
                        if (matches[index] != updated) {
                            matches[index] = updated
                            hasChanged = true
                        }
                    }

                    if (!hasChanged) {
                        null
                    } else {
                        matches.toList()
                    }
                }

                if (snapshot != null) {
                    send(snapshot)
                }
            }
        }

        jobs.joinAll()
    }

    private fun fetchLiveMatchesStream(sportId: Int): Flow<List<LiveMatch>> = channelFlow {
        val events = liveEventsApiService
            .getLiveSchedule(sportId)
            .data

        if (events.isEmpty()) {
            send(emptyList())
            return@channelFlow
        }

        val orderedTeamIds = events
            .flatMap { event -> listOfNotNull(event.homeTeam?.id, event.awayTeam?.id) }
            .distinct()

        val teamIndicesById = mutableMapOf<Int, MutableList<Int>>()
        val logosByTeamId = mutableMapOf<Int, String>()
        val matches = MutableList(events.size) { index ->
            val event = events[index]
            val homeId = event.homeTeam?.id
            val awayId = event.awayTeam?.id

            if (homeId != null) {
                teamIndicesById.getOrPut(homeId) { mutableListOf() }.add(index)
                teamLogoProvider.peekCachedLogo(homeId)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { logosByTeamId[homeId] = it }
            }

            if (awayId != null) {
                teamIndicesById.getOrPut(awayId) { mutableListOf() }.add(index)
                teamLogoProvider.peekCachedLogo(awayId)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { logosByTeamId[awayId] = it }
            }

            event.toLiveDomain(
                homeLogoBase64 = homeId?.let { logosByTeamId[it] },
                awayLogoBase64 = awayId?.let { logosByTeamId[it] },
            )
        }

        val emissionMutex = Mutex()
        val initialSnapshot = emissionMutex.withLock { matches.toList() }
        send(initialSnapshot)

        val pendingTeamIds = determineLiveLogoCandidates(
            sportId = sportId,
            orderedTeamIds = orderedTeamIds,
            cachedTeamIds = logosByTeamId.keys.toSet(),
        )
        val jobs = pendingTeamIds.map { teamId ->
            launch {
                val logo = runCatching { teamLogoProvider.getTeamLogo(teamId) }.getOrElse { "" }
                if (logo.isBlank()) {
                    return@launch
                }

                val snapshot = emissionMutex.withLock {
                    val previous = logosByTeamId[teamId]
                    if (previous == logo) {
                        return@withLock null
                    }

                    logosByTeamId[teamId] = logo
                    val indices = teamIndicesById[teamId].orEmpty()
                    var hasChanged = false

                    for (index in indices) {
                        val event = events[index]
                        val updated = event.toLiveDomain(
                            homeLogoBase64 = event.homeTeam?.id?.let { logosByTeamId[it] },
                            awayLogoBase64 = event.awayTeam?.id?.let { logosByTeamId[it] },
                        )
                        if (matches[index] != updated) {
                            matches[index] = updated
                            hasChanged = true
                        }
                    }

                    if (!hasChanged) {
                        null
                    } else {
                        matches.toList()
                    }
                }

                if (snapshot != null) {
                    send(snapshot)
                }
            }
        }

        jobs.joinAll()
    }

    private suspend fun recordMatchesRefreshed(
        leagueId: Int,
        seasonId: Int,
        type: MatchTypeEntity,
    ) {
        matchDao.upsertRefreshTimestamp(
            MatchRefreshEntity(
                leagueId = leagueId,
                seasonId = seasonId,
                type = type,
                refreshedAt = System.currentTimeMillis(),
            )
        )
    }

    private companion object {
        private const val TOP_TEAM_LOGO_PREFETCH_COUNT = 12
        private const val MAX_TEAM_LOGO_REQUESTS_PER_REFRESH = 28
    }

    private suspend fun determineLiveLogoCandidates(
        sportId: Int,
        orderedTeamIds: List<Int>,
        cachedTeamIds: Set<Int>,
    ): List<Int> {
        if (orderedTeamIds.isEmpty()) {
            return emptyList()
        }

        val missingTeamIds = orderedTeamIds.filterNot { cachedTeamIds.contains(it) }
        if (missingTeamIds.isEmpty()) {
            return emptyList()
        }

        val maxRequests = MAX_TEAM_LOGO_REQUESTS_PER_REFRESH.coerceAtMost(missingTeamIds.size)
        val topPriority = missingTeamIds.take(TOP_TEAM_LOGO_PREFETCH_COUNT)
        if (topPriority.size >= maxRequests) {
            return topPriority.take(maxRequests)
        }

        val additionalCapacity = maxRequests - topPriority.size
        val rotatingCandidates = missingTeamIds.drop(TOP_TEAM_LOGO_PREFETCH_COUNT)
        val scheduleSignature = orderedTeamIds.signature()
        val rotatingSelection = selectRotatingLiveLogoCandidates(
            sportId = sportId,
            scheduleSignature = scheduleSignature,
            rotatingCandidates = rotatingCandidates,
            maxCount = additionalCapacity,
        )

        return (topPriority + rotatingSelection)
            .distinct()
            .take(maxRequests)
    }

    private suspend fun selectRotatingLiveLogoCandidates(
        sportId: Int,
        scheduleSignature: Int,
        rotatingCandidates: List<Int>,
        maxCount: Int,
    ): List<Int> {
        if (maxCount <= 0 || rotatingCandidates.isEmpty()) {
            return emptyList()
        }

        val uniqueCandidates = LinkedHashSet(rotatingCandidates).toList()
        if (uniqueCandidates.isEmpty()) {
            return emptyList()
        }

        val selection = liveLogoStateMutex.withLock {
            val existing = liveLogoStateBySport[sportId]
            val current = if (existing == null || existing.signature != scheduleSignature) {
                LiveLogoPrefetchState(nextIndex = 0, signature = scheduleSignature)
            } else {
                existing
            }

            if (uniqueCandidates.isNotEmpty()) {
                if (current.nextIndex >= uniqueCandidates.size) {
                    current.nextIndex %= uniqueCandidates.size
                }
            }

            val planned = mutableListOf<Int>()
            var index = current.nextIndex
            repeat(minOf(maxCount, uniqueCandidates.size)) {
                if (index >= uniqueCandidates.size) {
                    index = 0
                }
                planned += uniqueCandidates[index]
                index = (index + 1) % uniqueCandidates.size
            }

            current.signature = scheduleSignature
            current.nextIndex = if (uniqueCandidates.isEmpty()) 0 else index
            liveLogoStateBySport[sportId] = current
            planned
        }

        return selection
    }

    private fun List<Int>.signature(): Int {
        var result = 1
        for (value in this) {
            result = 31 * result + value
        }
        return result
    }

    private suspend fun shouldRefreshUpcomingMatches(
        uniqueTournamentId: Int,
        seasonId: Int,
        forceRefresh: Boolean,
    ): Boolean {
        if (forceRefresh) {
            return true
        }
        val refreshTimestamp = matchDao.getRefreshTimestamp(
            uniqueTournamentId,
            seasonId,
            MatchTypeEntity.UPCOMING,
        )
        if (refreshTimestamp != null) {
            return false
        }

        if (matchDao.hasMatches(uniqueTournamentId, seasonId, MatchTypeEntity.UPCOMING)) {
            recordMatchesRefreshed(
                leagueId = uniqueTournamentId,
                seasonId = seasonId,
                type = MatchTypeEntity.UPCOMING,
            )
            return false
        }

        return true
    }

    private suspend fun shouldRefreshPastMatches(
        uniqueTournamentId: Int,
        seasonId: Int,
        forceRefresh: Boolean,
    ): Boolean {
        if (forceRefresh) {
            return true
        }

        val refreshTimestamp = matchDao.getRefreshTimestamp(
            uniqueTournamentId,
            seasonId,
            MatchTypeEntity.PAST,
        )
        if (refreshTimestamp != null) {
            return false
        }

        if (matchDao.hasMatches(uniqueTournamentId, seasonId, MatchTypeEntity.PAST)) {
            recordMatchesRefreshed(
                leagueId = uniqueTournamentId,
                seasonId = seasonId,
                type = MatchTypeEntity.PAST,
            )
            return false
        }

        return true
    }

    private data class LiveLogoPrefetchState(
        var nextIndex: Int,
        var signature: Int,
    )
}
