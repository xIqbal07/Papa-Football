package com.papa.fr.football.data.repository

import com.papa.fr.football.data.local.dao.UserPreferencesDao
import com.papa.fr.football.data.local.entity.FavoriteTeamEntity
import com.papa.fr.football.data.local.entity.UserPreferenceEntity
import com.papa.fr.football.domain.model.UserFavoriteTeam
import com.papa.fr.football.domain.model.UserPreferences
import com.papa.fr.football.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class UserPreferencesRepositoryImpl(
    private val userPreferencesDao: UserPreferencesDao,
) : UserPreferencesRepository {

    override val preferencesFlow: Flow<UserPreferences> = combine(
        userPreferencesDao.observePreferences(),
        userPreferencesDao.observeFavoriteTeams(),
    ) { preferencesEntity, favoriteTeamEntities ->
        val favoriteTeams = favoriteTeamEntities.map(FavoriteTeamEntity::toDomain)
        UserPreferences(
            isSignedIn = preferencesEntity?.isSignedIn ?: false,
            selectedLeagueId = preferencesEntity?.selectedLeagueId,
            favoriteTeams = favoriteTeams,
        )
    }.distinctUntilChanged()

    override suspend fun updatePreferences(
        isSignedIn: Boolean,
        selectedLeagueId: Int?,
        favoriteTeams: List<UserFavoriteTeam>,
    ) {
        val preferenceEntity = UserPreferenceEntity(
            id = UserPreferenceEntity.SINGLE_ROW_ID,
            isSignedIn = isSignedIn,
            selectedLeagueId = selectedLeagueId,
        )
        val favoriteEntities = favoriteTeams.map(UserFavoriteTeam::toEntity)
        userPreferencesDao.updatePreferences(preferenceEntity, favoriteEntities)
    }
}

private fun FavoriteTeamEntity.toDomain(): UserFavoriteTeam {
    return UserFavoriteTeam(
        id = teamId,
        leagueId = leagueId,
        name = teamName,
        logoBase64 = logoBase64,
    )
}

private fun UserFavoriteTeam.toEntity(): FavoriteTeamEntity {
    return FavoriteTeamEntity(
        teamId = id,
        leagueId = leagueId,
        teamName = name,
        logoBase64 = logoBase64,
    )
}
