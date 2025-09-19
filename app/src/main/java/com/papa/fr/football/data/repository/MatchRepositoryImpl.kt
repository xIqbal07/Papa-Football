package com.papa.fr.football.data.repository

import com.papa.fr.football.data.mapper.toDomain
import com.papa.fr.football.data.remote.SeasonApiService
import com.papa.fr.football.data.remote.TeamApiService
import com.papa.fr.football.domain.model.Match
import com.papa.fr.football.domain.repository.MatchRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class MatchRepositoryImpl(
    private val seasonApiService: SeasonApiService,
    private val teamApiService: TeamApiService,
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
                async {
                    runCatching { teamApiService.getTeamLogo(teamId).data }
                        .map { it.sanitizeBase64() }
                        .getOrElse { "" }
                }
            }.mapValues { (_, deferred) -> deferred.await() }

            events.map { event ->
                event.toDomain(
                    homeLogoBase64 = event.homeTeam?.id?.let { logos[it] },
                    awayLogoBase64 = event.awayTeam?.id?.let { logos[it] },
                )
            }
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
}
