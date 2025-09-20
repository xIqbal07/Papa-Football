package com.papa.fr.football.data.repository

import com.papa.fr.football.data.local.dao.MatchDao
import com.papa.fr.football.data.local.dao.LeagueTeamProjection
import com.papa.fr.football.domain.model.LeagueTeam
import com.papa.fr.football.domain.repository.TeamRepository

class TeamRepositoryImpl(
    private val matchDao: MatchDao,
) : TeamRepository {

    override suspend fun getTeamsForLeague(leagueId: Int): List<LeagueTeam> {
        return matchDao.getDistinctTeamsForLeague(leagueId).map { it.toDomain(leagueId) }
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
