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

class MatchRepositoryImpl(
    private val seasonApiService: SeasonApiService,
    private val liveEventsApiService: LiveEventsApiService,
    private val teamLogoProvider: TeamLogoProvider,
) : MatchRepository {

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

        val prioritizedTeamIds = events
            .take(MAX_EVENTS_FOR_LOGOS)
            .flatMap { event -> listOfNotNull(event.homeTeam?.id, event.awayTeam?.id) }
            .distinct()

        val pendingTeamIds = prioritizedTeamIds.filterNot { logos.containsKey(it) }
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
        const val MAX_EVENTS_FOR_LOGOS = 50
    }
}
