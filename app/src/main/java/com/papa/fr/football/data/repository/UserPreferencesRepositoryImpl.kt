package com.papa.fr.football.data.repository

import com.papa.fr.football.data.local.preferences.UserPreferencesDataSource
import com.papa.fr.football.domain.model.UserPreferences
import com.papa.fr.football.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class UserPreferencesRepositoryImpl(
    private val dataSource: UserPreferencesDataSource,
) : UserPreferencesRepository {

    override val preferencesFlow: Flow<UserPreferences>
        get() = dataSource.preferencesFlow

    override suspend fun updatePreferences(
        isSignedIn: Boolean,
        selectedLeagueId: Int?,
        favoriteTeamIds: Set<Int>,
    ) {
        dataSource.updatePreferences(isSignedIn, selectedLeagueId, favoriteTeamIds)
    }
}
