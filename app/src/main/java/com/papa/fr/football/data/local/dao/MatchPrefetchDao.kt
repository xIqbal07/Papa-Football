package com.papa.fr.football.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.papa.fr.football.data.local.entity.MatchPrefetchTaskEntity
import com.papa.fr.football.data.local.entity.MatchTypeEntity

@Dao
interface MatchPrefetchDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueue(task: MatchPrefetchTaskEntity): Long

    @Query(
        """
            UPDATE match_prefetch_queue
            SET force_refresh = CASE WHEN :forceRefresh THEN 1 ELSE force_refresh END,
                priority = CASE WHEN priority > :priority THEN priority ELSE :priority END,
                available_after = CASE
                    WHEN available_after < :availableAfter THEN available_after
                    ELSE :availableAfter
                END,
                attempts = CASE WHEN :forceRefresh THEN 0 ELSE attempts END
            WHERE league_id = :leagueId AND season_id = :seasonId AND type = :type
        """
    )
    suspend fun promote(
        leagueId: Int,
        seasonId: Int,
        type: MatchTypeEntity,
        forceRefresh: Boolean,
        priority: Int,
        availableAfter: Long,
    )

    @Query(
        """
            SELECT * FROM match_prefetch_queue
            WHERE available_after <= :now
            ORDER BY priority DESC, available_after ASC, id ASC
            LIMIT 1
        """
    )
    suspend fun peekReady(now: Long): MatchPrefetchTaskEntity?

    @Query("DELETE FROM match_prefetch_queue WHERE id = :id")
    suspend fun delete(id: Long)

    @Query(
        """
            UPDATE match_prefetch_queue
            SET available_after = :availableAfter, attempts = :attempts
            WHERE id = :id
        """
    )
    suspend fun reschedule(id: Long, availableAfter: Long, attempts: Int)
}
