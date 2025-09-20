package com.papa.fr.football.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "matches",
    primaryKeys = ["match_id", "league_id", "season_id", "type"],
)
data class MatchEntity(
    @ColumnInfo(name = "match_id")
    val matchId: String,
    @ColumnInfo(name = "league_id")
    val leagueId: Int,
    @ColumnInfo(name = "season_id")
    val seasonId: Int,
    @ColumnInfo(name = "type")
    val type: MatchTypeEntity,
    @ColumnInfo(name = "start_timestamp")
    val startTimestamp: Long,
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
    val homeScore: Int?,
    @ColumnInfo(name = "away_score")
    val awayScore: Int?,
)
