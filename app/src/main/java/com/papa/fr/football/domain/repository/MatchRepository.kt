package com.papa.fr.football.domain.repository

import com.papa.fr.football.domain.model.Match

interface MatchRepository {
    suspend fun getUpcomingMatches(uniqueTournamentId: Int, seasonId: Int): List<Match>
    suspend fun getRecentMatches(uniqueTournamentId: Int, seasonId: Int): List<Match>
}
