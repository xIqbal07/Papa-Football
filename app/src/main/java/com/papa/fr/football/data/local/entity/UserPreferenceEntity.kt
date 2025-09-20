package com.papa.fr.football.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreferenceEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = SINGLE_ROW_ID,
    @ColumnInfo(name = "is_signed_in")
    val isSignedIn: Boolean,
    @ColumnInfo(name = "selected_league_id")
    val selectedLeagueId: Int?,
) {
    companion object {
        const val SINGLE_ROW_ID = 0
    }
}
