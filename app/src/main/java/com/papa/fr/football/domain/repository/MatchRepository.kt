package com.papa.fr.football.domain.repository

import com.papa.fr.football.domain.model.LiveMatch
import com.papa.fr.football.domain.model.Match
import kotlinx.coroutines.flow.Flow

interface MatchRepository {
    fun getUpcomingMatches(uniqueTournamentId: Int, seasonId: Int): Flow<List<Match>>
    suspend fun getRecentMatches(uniqueTournamentId: Int, seasonId: Int): List<Match>
    fun getLiveMatches(sportId: Int): Flow<List<LiveMatch>>
}
