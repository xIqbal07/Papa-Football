package com.papa.fr.football.presentation.signin

sealed interface SignInEvent {
    data object NavigateToSchedule : SignInEvent
}
