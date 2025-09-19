package com.papa.fr.football.data.repository

import com.papa.fr.football.data.mapper.toDomain
import com.papa.fr.football.data.remote.SeasonApiService
import com.papa.fr.football.data.remote.TeamApiService
import com.papa.fr.football.domain.model.Match
import com.papa.fr.football.domain.repository.MatchRepository
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class MatchRepositoryImpl(
    private val seasonApiService: SeasonApiService,
    private val teamApiService: TeamApiService,
) : MatchRepository {

    private val teamLogoCache = ConcurrentHashMap<Int, String>()
    private val logoSemaphore = Semaphore(LOGO_REQUEST_CONCURRENCY)

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
                async { fetchTeamLogo(teamId) }
            }.mapValues { (_, deferred) -> deferred.await() }

            events.map { event ->
                event.toDomain(
                    homeLogoBase64 = event.homeTeam?.id?.let { logos[it] },
                    awayLogoBase64 = event.awayTeam?.id?.let { logos[it] },
                )
            }
        }

    private suspend fun fetchTeamLogo(teamId: Int): String {
        teamLogoCache[teamId]?.let { return it }

        val sanitizedLogo = logoSemaphore.withPermit {
            runCatching { teamApiService.getTeamLogo(teamId).data }
                .map { it.sanitizeBase64() }
                .getOrElse { "" }
        }

        if (sanitizedLogo.isNotBlank()) {
            teamLogoCache[teamId] = sanitizedLogo
        }

        return sanitizedLogo
    }

    private fun String?.sanitizeBase64(): String {
        if (this.isNullOrBlank()) return ""
        val delimiter = "base64,"
        val index = indexOf(delimiter)
        val trimmed = if (index >= 0) {
            substring(index + delimiter.length)
        } else {
            this
        }
        return trimmed.trim()
    }

    private companion object {
        private const val LOGO_REQUEST_CONCURRENCY = 3
    }
}
