package com.papa.fr.football.presentation.signin

sealed interface SignInViewState {

    data class Content(
        val leagues: List<LeagueUiModel>,
        val teamSelectionState: TeamSelectionState,
    ) : SignInViewState {

        val canSignIn: Boolean
            get() = teamSelectionState.favoriteTeams.isNotEmpty()
    }
}
