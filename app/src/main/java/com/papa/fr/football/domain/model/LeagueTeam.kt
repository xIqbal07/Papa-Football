package com.papa.fr.football.domain.model

data class LeagueTeam(
    val id: Int,
    val leagueId: Int,
    val name: String,
    val logoBase64: String?,
)
