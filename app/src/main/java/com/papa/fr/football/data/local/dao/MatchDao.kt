package com.papa.fr.football.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.papa.fr.football.data.local.entity.MatchEntity
import com.papa.fr.football.data.local.entity.MatchRefreshEntity
import com.papa.fr.football.data.local.entity.MatchTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
    @Query(
        """
            SELECT * FROM matches
            WHERE league_id = :leagueId AND season_id = :seasonId AND type = :type
            ORDER BY start_timestamp ASC
        """
    )
    fun observeMatches(
        leagueId: Int,
        seasonId: Int,
        type: MatchTypeEntity,
    ): Flow<List<MatchEntity>>

    @Query(
        """
            SELECT * FROM matches
            WHERE league_id = :leagueId AND season_id = :seasonId AND type = :type
            ORDER BY start_timestamp ASC
        """
    )
    suspend fun getMatches(
        leagueId: Int,
        seasonId: Int,
        type: MatchTypeEntity,
    ): List<MatchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatches(matches: List<MatchEntity>)

    @Query(
        "DELETE FROM matches WHERE league_id = :leagueId AND season_id = :seasonId AND type = :type"
    )
    suspend fun deleteMatches(leagueId: Int, seasonId: Int, type: MatchTypeEntity)

    @Query(
        """
            UPDATE matches
            SET home_logo_base64 = :logo
            WHERE home_team_id = :teamId AND (
                home_logo_base64 IS NULL OR home_logo_base64 = '' OR home_logo_base64 != :logo
            )
        """
    )
    suspend fun updateHomeTeamLogo(teamId: Int, logo: String)

    @Query(
        """
            UPDATE matches
            SET away_logo_base64 = :logo
            WHERE away_team_id = :teamId AND (
                away_logo_base64 IS NULL OR away_logo_base64 = '' OR away_logo_base64 != :logo
            )
        """
    )
    suspend fun updateAwayTeamLogo(teamId: Int, logo: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRefreshTimestamp(entity: MatchRefreshEntity)

    @Query(
        """
            SELECT EXISTS(
                SELECT 1 FROM matches
                WHERE league_id = :leagueId AND season_id = :seasonId AND type = :type
                LIMIT 1
            )
        """
    )
    suspend fun hasMatches(
        leagueId: Int,
        seasonId: Int,
        type: MatchTypeEntity,
    ): Boolean

    @Query(
        """
            SELECT EXISTS(
                SELECT 1 FROM matches
                WHERE league_id = :leagueId AND season_id = :seasonId AND type = :type AND (
                    home_logo_base64 IS NULL OR home_logo_base64 = '' OR
                    away_logo_base64 IS NULL OR away_logo_base64 = ''
                )
                LIMIT 1
            )
        """
    )
    suspend fun hasMatchesMissingLogos(
        leagueId: Int,
        seasonId: Int,
        type: MatchTypeEntity,
    ): Boolean

    @Query(
        """
            SELECT refreshed_at FROM match_refresh
            WHERE league_id = :leagueId AND season_id = :seasonId AND type = :type
        """
    )
    suspend fun getRefreshTimestamp(
        leagueId: Int,
        seasonId: Int,
        type: MatchTypeEntity,
    ): Long?

    @Transaction
    suspend fun replaceMatches(
        leagueId: Int,
        seasonId: Int,
        type: MatchTypeEntity,
        matches: List<MatchEntity>,
        refreshedAt: Long,
    ) {
        deleteMatches(leagueId, seasonId, type)
        if (matches.isNotEmpty()) {
            insertMatches(matches)
        }
        upsertRefreshTimestamp(
            MatchRefreshEntity(
                leagueId = leagueId,
                seasonId = seasonId,
                type = type,
                refreshedAt = refreshedAt,
            )
        )
    }

    @Transaction
    suspend fun updateTeamLogos(teamId: Int, logo: String) {
        updateHomeTeamLogo(teamId, logo)
        updateAwayTeamLogo(teamId, logo)
    }
}
