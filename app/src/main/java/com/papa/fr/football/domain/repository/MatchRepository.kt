package com.papa.fr.football.domain.repository

import com.papa.fr.football.domain.model.LiveMatch
import com.papa.fr.football.domain.model.Match

interface MatchRepository {
    suspend fun getUpcomingMatches(uniqueTournamentId: Int, seasonId: Int): List<Match>
    suspend fun getLiveMatches(sportId: Int): List<LiveMatch>
}
