package com.papa.fr.football.domain.repository

import com.papa.fr.football.domain.model.LiveMatch
import com.papa.fr.football.domain.model.Match
import kotlinx.coroutines.flow.Flow

interface MatchRepository {
    suspend fun getUpcomingMatches(uniqueTournamentId: Int, seasonId: Int): List<Match>
    fun getLiveMatches(sportId: Int): Flow<List<LiveMatch>>
}
