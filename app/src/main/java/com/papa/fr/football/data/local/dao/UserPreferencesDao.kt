package com.papa.fr.football.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.papa.fr.football.data.local.entity.FavoriteTeamEntity
import com.papa.fr.football.data.local.entity.UserPreferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPreferencesDao {

    @Query("SELECT * FROM user_preferences LIMIT 1")
    fun observePreferences(): Flow<UserPreferenceEntity?>

    @Query("SELECT * FROM user_preferences LIMIT 1")
    suspend fun getPreferences(): UserPreferenceEntity?

    @Query("SELECT * FROM favorite_teams ORDER BY team_name COLLATE NOCASE")
    fun observeFavoriteTeams(): Flow<List<FavoriteTeamEntity>>

    @Query("SELECT * FROM favorite_teams ORDER BY team_name COLLATE NOCASE")
    suspend fun getFavoriteTeams(): List<FavoriteTeamEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPreferences(entity: UserPreferenceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteTeams(entities: List<FavoriteTeamEntity>)

    @Query("DELETE FROM favorite_teams")
    suspend fun clearFavoriteTeams()

    @Transaction
    suspend fun updatePreferences(
        preferences: UserPreferenceEntity,
        favoriteTeams: List<FavoriteTeamEntity>,
    ) {
        upsertPreferences(preferences)
        clearFavoriteTeams()
        if (favoriteTeams.isNotEmpty()) {
            insertFavoriteTeams(favoriteTeams)
        }
    }
}
