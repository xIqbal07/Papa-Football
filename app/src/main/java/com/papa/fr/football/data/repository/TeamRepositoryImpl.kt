package com.papa.fr.football.data.repository

import com.papa.fr.football.data.local.dao.LeagueTeamProjection
import com.papa.fr.football.data.local.dao.MatchDao
import com.papa.fr.football.data.repository.TeamLogoProvider
import com.papa.fr.football.domain.model.LeagueTeam
import com.papa.fr.football.domain.repository.TeamRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TeamRepositoryImpl(
    private val matchDao: MatchDao,
    private val teamLogoProvider: TeamLogoProvider,
) : TeamRepository {

    private val logoUpdateScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun getTeamsForLeague(leagueId: Int): List<LeagueTeam> {
        val projections = matchDao.getDistinctTeamsForLeague(leagueId)
        if (projections.isEmpty()) {
            return emptyList()
        }

        val teams = projections.map { it.toDomain(leagueId) }

        val missingLogos = projections.filter { it.logoBase64.isNullOrBlank() }
        if (missingLogos.isNotEmpty()) {
            logoUpdateScope.launch {
                for (projection in missingLogos) {
                    val cached = teamLogoProvider.peekCachedLogo(projection.teamId)
                    val logo = cached?.takeIf { it.isNotBlank() }
                        ?: runCatching { teamLogoProvider.getTeamLogo(projection.teamId) }
                            .getOrElse { "" }
                    if (logo.isNotBlank() && logo != projection.logoBase64) {
                        matchDao.updateTeamLogos(projection.teamId, logo)
                    }
                }
            }
        }

        return teams
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
