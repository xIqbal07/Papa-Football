package com.papa.fr.football.domain.repository

import com.papa.fr.football.domain.model.LiveMatch
import com.papa.fr.football.domain.model.Match
import kotlinx.coroutines.flow.Flow

interface MatchRepository {
    fun getUpcomingMatches(
        uniqueTournamentId: Int,
        seasonId: Int,
        forceRefresh: Boolean = false,
    ): Flow<List<Match>>
    fun getRecentMatches(
        uniqueTournamentId: Int,
        seasonId: Int,
        forceRefresh: Boolean = false,
    ): Flow<List<Match>>
    fun getLiveMatches(sportId: Int): Flow<List<LiveMatch>>
    suspend fun warmUpcomingMatches(
        uniqueTournamentId: Int,
        seasonId: Int,
        forceRefresh: Boolean = false,
        prefetchLogos: Boolean = true,
    )
    suspend fun warmRecentMatches(
        uniqueTournamentId: Int,
        seasonId: Int,
        forceRefresh: Boolean = false,
        prefetchLogos: Boolean = true,
    )
    suspend fun warmLiveMatches(
        sportId: Int,
        forceRefresh: Boolean = false,
        prefetchLogos: Boolean = true,
    )
}
