package com.papa.fr.football.domain.repository

import com.papa.fr.football.domain.model.UserFavoriteTeam
import com.papa.fr.football.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val preferencesFlow: Flow<UserPreferences>

    suspend fun updatePreferences(
        isSignedIn: Boolean,
        selectedLeagueId: Int?,
        favoriteTeams: List<UserFavoriteTeam>,
    )
}
