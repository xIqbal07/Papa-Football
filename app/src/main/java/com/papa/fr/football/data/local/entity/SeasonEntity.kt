package com.papa.fr.football.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "seasons",
    primaryKeys = ["league_id", "season_id"],
)
data class SeasonEntity(
    @ColumnInfo(name = "league_id")
    val leagueId: Int,
    @ColumnInfo(name = "season_id")
    val seasonId: Int,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "year")
    val year: String?,
    @ColumnInfo(name = "editor")
    val editor: Boolean,
    @ColumnInfo(name = "order_index")
    val orderIndex: Int,
)
