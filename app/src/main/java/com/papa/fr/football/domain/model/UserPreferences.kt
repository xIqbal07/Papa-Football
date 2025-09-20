package com.papa.fr.football.domain.model

data class UserPreferences(
    val isSignedIn: Boolean = false,
    val selectedLeagueId: Int? = null,
    val favoriteTeamIds: Set<Int> = emptySet(),
)
