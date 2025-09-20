package com.papa.fr.football.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.papa.fr.football.data.local.entity.SeasonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SeasonDao {
    @Query(
        "SELECT * FROM seasons WHERE league_id = :leagueId ORDER BY order_index ASC"
    )
    fun observeSeasons(leagueId: Int): Flow<List<SeasonEntity>>

    @Query(
        "SELECT * FROM seasons WHERE league_id = :leagueId ORDER BY order_index ASC"
    )
    suspend fun getSeasons(leagueId: Int): List<SeasonEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeasons(seasons: List<SeasonEntity>)

    @Query("DELETE FROM seasons WHERE league_id = :leagueId")
    suspend fun deleteSeasons(leagueId: Int)

    @Transaction
    suspend fun replaceSeasons(leagueId: Int, seasons: List<SeasonEntity>) {
        deleteSeasons(leagueId)
        if (seasons.isNotEmpty()) {
            insertSeasons(seasons)
        }
    }
}
