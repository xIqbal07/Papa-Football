package com.papa.fr.football.data.repository

import com.papa.fr.football.data.mapper.toDomain
import com.papa.fr.football.data.mapper.toLiveDomain
import com.papa.fr.football.data.remote.LiveEventsApiService
import com.papa.fr.football.data.remote.SeasonApiService
import com.papa.fr.football.domain.model.LiveMatch
import com.papa.fr.football.domain.model.Match
import com.papa.fr.football.domain.repository.MatchRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
) : MatchRepository {

    private val liveLogoStateMutex = Mutex()
    private val liveLogoStateBySport = mutableMapOf<Int, LiveLogoPrefetchState>()

    override suspend fun getUpcomingMatches(uniqueTournamentId: Int, seasonId: Int): List<Match> =
        coroutineScope {
            val events = seasonApiService
                .getSeasonEvents(uniqueTournamentId, seasonId)
                .data
                .events

            if (events.isEmpty()) {
                return@coroutineScope emptyList()
            }

            val teamIds = events.flatMap { event ->
                listOfNotNull(event.homeTeam?.id, event.awayTeam?.id)
            }.toSet()

            val logos = teamIds.associateWith { teamId ->
                async { teamLogoProvider.getTeamLogo(teamId) }
            }.mapValues { (_, deferred) -> deferred.await() }

            events.map { event ->
                event.toDomain(
                    homeLogoBase64 = event.homeTeam?.id?.let { logos[it] },
                    awayLogoBase64 = event.awayTeam?.id?.let { logos[it] },
                )
            }
        }

    override fun getLiveMatches(sportId: Int): Flow<List<LiveMatch>> = channelFlow {
        val events = liveEventsApiService
            .getLiveSchedule(sportId)
            .data

        if (events.isEmpty()) {
            send(emptyList())
            return@channelFlow
        }

        val teamIds = events
            .flatMap { event -> listOfNotNull(event.homeTeam?.id, event.awayTeam?.id) }
            .toSet()

        val logos = mutableMapOf<Int, String>()
        teamIds.forEach { teamId ->
            teamLogoProvider.peekCachedLogo(teamId)
                ?.takeIf { it.isNotBlank() }
                ?.let { cachedLogo -> logos[teamId] = cachedLogo }
        }
        val emissionMutex = Mutex()

        suspend fun emitSnapshot() {
            val matches = events.map { event ->
                event.toLiveDomain(
                    homeLogoBase64 = event.homeTeam?.id?.let { logos[it] },
                    awayLogoBase64 = event.awayTeam?.id?.let { logos[it] },
                )
            }
            send(matches)
        }

        emissionMutex.withLock { emitSnapshot() }

        val orderedTeamIds = events
            .flatMap { event -> listOfNotNull(event.homeTeam?.id, event.awayTeam?.id) }
            .distinct()

        val pendingTeamIds = determineLiveLogoCandidates(
            sportId = sportId,
            orderedTeamIds = orderedTeamIds,
            cachedTeamIds = logos.keys,
        )
        val jobs = pendingTeamIds.map { teamId ->
            launch {
                val logo = runCatching { teamLogoProvider.getTeamLogo(teamId) }.getOrElse { "" }
                if (logo.isNotBlank()) {
                    emissionMutex.withLock {
                        val hasChanged = logos[teamId] != logo
                        if (hasChanged) {
                            logos[teamId] = logo
                            emitSnapshot()
                        }
                    }
                }
            }
        }

        jobs.joinAll()
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

    private data class LiveLogoPrefetchState(
        var nextIndex: Int,
        var signature: Int,
    )
}
