package com.papa.fr.football.data.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.papa.fr.football.domain.model.UserPreferences
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class UserPreferencesDataSource(
    private val dataStore: DataStore<Preferences>,
) {

    val preferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { preferences ->
            val favoriteTeamIds = preferences[Keys.favoriteTeamIds]
                ?.mapNotNull { it.toIntOrNull() }
                ?.toSet()
                ?: emptySet()

            UserPreferences(
                isSignedIn = preferences[Keys.isSignedIn] ?: false,
                selectedLeagueId = preferences[Keys.selectedLeagueId],
                favoriteTeamIds = favoriteTeamIds,
            )
        }

    suspend fun updatePreferences(
        isSignedIn: Boolean,
        selectedLeagueId: Int?,
        favoriteTeamIds: Set<Int>,
    ) {
        dataStore.edit { preferences ->
            preferences[Keys.isSignedIn] = isSignedIn
            if (selectedLeagueId != null) {
                preferences[Keys.selectedLeagueId] = selectedLeagueId
            } else {
                preferences.remove(Keys.selectedLeagueId)
            }
            if (favoriteTeamIds.isNotEmpty()) {
                preferences[Keys.favoriteTeamIds] = favoriteTeamIds.map(Int::toString).toSet()
            } else {
                preferences.remove(Keys.favoriteTeamIds)
            }
        }
    }

    companion object {
        private object Keys {
            val isSignedIn = booleanPreferencesKey("is_signed_in")
            val selectedLeagueId = intPreferencesKey("selected_league_id")
            val favoriteTeamIds = stringSetPreferencesKey("favorite_team_ids")
        }

        fun create(
            context: android.content.Context,
        ): UserPreferencesDataSource {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val store = androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                scope = scope,
            ) {
                context.preferencesDataStoreFile("user_preferences")
            }
            return UserPreferencesDataSource(store)
        }
    }
}
