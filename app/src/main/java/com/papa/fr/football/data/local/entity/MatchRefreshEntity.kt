package com.papa.fr.football.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "match_refresh",
    primaryKeys = ["league_id", "season_id", "type"],
)
data class MatchRefreshEntity(
    @ColumnInfo(name = "league_id")
    val leagueId: Int,
    @ColumnInfo(name = "season_id")
    val seasonId: Int,
    @ColumnInfo(name = "type")
    val type: MatchTypeEntity,
    @ColumnInfo(name = "refreshed_at")
    val refreshedAt: Long,
)
