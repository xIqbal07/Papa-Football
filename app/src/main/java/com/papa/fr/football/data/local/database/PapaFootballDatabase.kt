package com.papa.fr.football.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.papa.fr.football.data.local.dao.LiveMatchDao
import com.papa.fr.football.data.local.dao.MatchDao
import com.papa.fr.football.data.local.dao.SeasonDao
import com.papa.fr.football.data.local.entity.LiveMatchEntity
import com.papa.fr.football.data.local.entity.MatchEntity
import com.papa.fr.football.data.local.entity.MatchRefreshEntity
import com.papa.fr.football.data.local.entity.SeasonEntity

@Database(
    entities = [SeasonEntity::class, MatchEntity::class, LiveMatchEntity::class, MatchRefreshEntity::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(DatabaseConverters::class)
abstract class PapaFootballDatabase : RoomDatabase() {
    abstract fun seasonDao(): SeasonDao
    abstract fun matchDao(): MatchDao
    abstract fun liveMatchDao(): LiveMatchDao
}
