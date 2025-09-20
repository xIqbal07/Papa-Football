package com.papa.fr.football.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persists queued match warm requests so the bootstrapper can spread heavy season loads across
 * multiple app sessions instead of firing every HTTP request at once.
 */
@Entity(
    tableName = "match_prefetch_queue",
    indices = [
        Index(value = ["league_id", "season_id", "type"], unique = true),
    ],
)
data class MatchPrefetchTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "league_id")
    val leagueId: Int,
    @ColumnInfo(name = "season_id")
    val seasonId: Int,
    @ColumnInfo(name = "type")
    val type: MatchTypeEntity,
    @ColumnInfo(name = "force_refresh")
    val forceRefresh: Boolean = false,
    @ColumnInfo(name = "priority")
    val priority: Int = 0,
    @ColumnInfo(name = "available_after")
    val availableAfter: Long = 0,
    @ColumnInfo(name = "attempts")
    val attempts: Int = 0,
)
