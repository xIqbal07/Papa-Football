package com.papa.fr.football.data.repository

import com.papa.fr.football.data.local.dao.LeagueTeamProjection
import com.papa.fr.football.data.local.dao.MatchDao
import com.papa.fr.football.data.repository.TeamLogoProvider
import com.papa.fr.football.domain.model.LeagueTeam
import com.papa.fr.football.domain.repository.TeamRepository

class TeamRepositoryImpl(
    private val matchDao: MatchDao,
    private val teamLogoProvider: TeamLogoProvider,
) : TeamRepository {

    override suspend fun getTeamsForLeague(leagueId: Int): List<LeagueTeam> {
        val projections = matchDao.getDistinctTeamsForLeague(leagueId)
        if (projections.isEmpty()) {
            return emptyList()
        }

        val enriched = projections.toMutableList()
        for ((index, projection) in projections.withIndex()) {
            if (!projection.logoBase64.isNullOrBlank()) {
                continue
            }

            val logo = runCatching { teamLogoProvider.getTeamLogo(projection.teamId) }
                .getOrElse { "" }
            if (logo.isBlank()) {
                continue
            }

            matchDao.updateTeamLogos(projection.teamId, logo)
            enriched[index] = projection.copy(logoBase64 = logo)
        }

        return enriched.map { it.toDomain(leagueId) }
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
