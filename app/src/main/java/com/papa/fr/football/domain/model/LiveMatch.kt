package com.papa.fr.football.domain.model

/**
 * Domain representation of an in-progress match event.
 */
data class LiveMatch(
    val id: String,
    val homeTeam: MatchTeam,
    val awayTeam: MatchTeam,
    val homeScore: Int,
    val awayScore: Int,
    val status: String,
)
