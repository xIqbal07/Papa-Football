package com.papa.fr.football.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.papa.fr.football.data.local.entity.LiveMatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LiveMatchDao {
    @Query("SELECT * FROM live_matches WHERE sport_id = :sportId")
    fun observeLiveMatches(sportId: Int): Flow<List<LiveMatchEntity>>

    @Query("SELECT * FROM live_matches WHERE sport_id = :sportId")
    suspend fun getLiveMatches(sportId: Int): List<LiveMatchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLiveMatches(matches: List<LiveMatchEntity>)

    @Query("DELETE FROM live_matches WHERE sport_id = :sportId")
    suspend fun deleteLiveMatches(sportId: Int)

    @Transaction
    suspend fun replaceLiveMatches(sportId: Int, matches: List<LiveMatchEntity>) {
        deleteLiveMatches(sportId)
        if (matches.isNotEmpty()) {
            insertLiveMatches(matches)
        }
    }
}
