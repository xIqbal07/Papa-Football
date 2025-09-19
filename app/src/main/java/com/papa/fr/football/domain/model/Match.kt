package com.papa.fr.football.domain.model

/**
 * Domain representation of a scheduled match event.
 */
data class Match(
    val id: String,
    val startTimestamp: Long,
    val homeTeam: MatchTeam,
    val awayTeam: MatchTeam,
    val homeScore: Int? = null,
    val awayScore: Int? = null,
)

/**
 * Lightweight model describing a team participating in a match.
 */
data class MatchTeam(
    val id: Int,
    val name: String,
    val logoBase64: String,
)
