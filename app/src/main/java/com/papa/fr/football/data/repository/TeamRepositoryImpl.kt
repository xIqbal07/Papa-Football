package com.papa.fr.football.data.repository

import com.papa.fr.football.data.local.dao.LeagueTeamProjection
import com.papa.fr.football.data.local.dao.MatchDao
import com.papa.fr.football.data.repository.TeamLogoProvider
import com.papa.fr.football.domain.model.LeagueTeam
import com.papa.fr.football.domain.repository.TeamRepository
import java.util.Collections
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class TeamRepositoryImpl(
    private val matchDao: MatchDao,
    private val teamLogoProvider: TeamLogoProvider,
) : TeamRepository {

    private val logoUpdateScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlightLogoFetches = Collections.synchronizedSet(mutableSetOf<Int>())

    override fun getTeamsForLeague(leagueId: Int): Flow<List<LeagueTeam>> {
        return matchDao.observeDistinctTeamsForLeague(leagueId)
            .onEach { projections ->
                scheduleMissingLogoFetches(projections)
            }
            .map { projections ->
                projections.map { it.toDomain(leagueId) }
            }
    }

    private fun scheduleMissingLogoFetches(projections: List<LeagueTeamProjection>) {
        projections
            .filter { it.logoBase64.isNullOrBlank() }
            .forEach { projection ->
                if (!inFlightLogoFetches.add(projection.teamId)) {
                    return@forEach
                }
                logoUpdateScope.launch {
                    try {
                        val cached = teamLogoProvider.peekCachedLogo(projection.teamId)
                        val logo = cached?.takeIf { it.isNotBlank() }
                            ?: runCatching { teamLogoProvider.getTeamLogo(projection.teamId) }
                                .getOrElse { "" }
                        if (logo.isNotBlank() && logo != projection.logoBase64) {
                            matchDao.updateTeamLogos(projection.teamId, logo)
                        }
                    } finally {
                        inFlightLogoFetches.remove(projection.teamId)
                    }
                }
            }
    }
}

private fun LeagueTeamProjection.toDomain(leagueId: Int): LeagueTeam {
    return LeagueTeam(
        id = teamId,
        leagueId = leagueId,
        name = teamName,
        logoBase64 = logoBase64,
    )
}
