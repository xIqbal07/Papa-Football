package com.papa.fr.football.domain.repository

import com.papa.fr.football.domain.model.LeagueTeam

interface TeamRepository {
    suspend fun getTeamsForLeague(leagueId: Int): List<LeagueTeam>
}
