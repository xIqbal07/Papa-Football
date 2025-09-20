package com.papa.fr.football.domain.model

data class UserPreferences(
    val isSignedIn: Boolean = false,
    val selectedLeagueId: Int? = null,
    val favoriteTeams: List<UserFavoriteTeam> = emptyList(),
)

data class UserFavoriteTeam(
    val id: Int,
    val leagueId: Int,
    val name: String,
    val logoBase64: String?,
)
