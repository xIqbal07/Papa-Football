package com.papa.fr.football.domain.repository

import com.papa.fr.football.domain.model.LeagueTeam
import kotlinx.coroutines.flow.Flow

interface TeamRepository {
    fun getTeamsForLeague(leagueId: Int): Flow<List<LeagueTeam>>
}
