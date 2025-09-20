package com.papa.fr.football.presentation.signin

sealed interface TeamSelectionState {
    val favoriteTeams: List<FavoriteTeamUiModel>

    data class Favorites(
        override val favoriteTeams: List<FavoriteTeamUiModel>,
    ) : TeamSelectionState

    data class Choosing(
        override val favoriteTeams: List<FavoriteTeamUiModel>,
        val selectedLeagueId: Int?,
        val availableTeams: List<TeamSelectionUiModel>,
    ) : TeamSelectionState
}
