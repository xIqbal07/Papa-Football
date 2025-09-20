package com.papa.fr.football.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.papa.fr.football.data.local.entity.MatchEntity
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

    @Transaction
    suspend fun replaceMatches(
        leagueId: Int,
        seasonId: Int,
        type: MatchTypeEntity,
        matches: List<MatchEntity>,
    ) {
        deleteMatches(leagueId, seasonId, type)
        if (matches.isNotEmpty()) {
            insertMatches(matches)
        }
    }
}
