package com.papa.fr.football.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "live_matches")
data class LiveMatchEntity(
    @PrimaryKey
    @ColumnInfo(name = "match_id")
    val matchId: String,
    @ColumnInfo(name = "sport_id")
    val sportId: Int,
    @ColumnInfo(name = "home_team_id")
    val homeTeamId: Int,
    @ColumnInfo(name = "home_team_name")
    val homeTeamName: String,
    @ColumnInfo(name = "home_logo_base64")
    val homeLogoBase64: String,
    @ColumnInfo(name = "away_team_id")
    val awayTeamId: Int,
    @ColumnInfo(name = "away_team_name")
    val awayTeamName: String,
    @ColumnInfo(name = "away_logo_base64")
    val awayLogoBase64: String,
    @ColumnInfo(name = "home_score")
    val homeScore: Int,
    @ColumnInfo(name = "away_score")
    val awayScore: Int,
    @ColumnInfo(name = "status_label")
    val statusLabel: String,
)
