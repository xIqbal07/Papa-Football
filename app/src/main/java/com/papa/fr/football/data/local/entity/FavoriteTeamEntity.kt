package com.papa.fr.football.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_teams")
data class FavoriteTeamEntity(
    @PrimaryKey
    @ColumnInfo(name = "team_id")
    val teamId: Int,
    @ColumnInfo(name = "league_id")
    val leagueId: Int,
    @ColumnInfo(name = "team_name")
    val teamName: String,
    @ColumnInfo(name = "logo_base64")
    val logoBase64: String?,
)
