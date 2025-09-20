package com.papa.fr.football.presentation.signin

data class TeamSelectionUiModel(
    val id: Int,
    val leagueId: Int,
    val name: String,
    val logoBase64: String?,
    val isSelected: Boolean,
)
