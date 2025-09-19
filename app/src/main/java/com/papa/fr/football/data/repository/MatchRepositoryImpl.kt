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

    override suspend fun getLiveMatches(sportId: Int): List<LiveMatch> = coroutineScope {
        val events = liveEventsApiService
            .getLiveSchedule(sportId)
            .data

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
            event.toLiveDomain(
                homeLogoBase64 = event.homeTeam?.id?.let { logos[it] },
                awayLogoBase64 = event.awayTeam?.id?.let { logos[it] },
            )
        }
    }
}
