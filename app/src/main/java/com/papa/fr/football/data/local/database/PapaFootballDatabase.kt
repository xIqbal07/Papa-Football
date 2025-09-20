package com.papa.fr.football.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.papa.fr.football.data.local.dao.LiveMatchDao
import com.papa.fr.football.data.local.dao.MatchDao
import com.papa.fr.football.data.local.dao.MatchPrefetchDao
import com.papa.fr.football.data.local.dao.RequestRetryDao
import com.papa.fr.football.data.local.dao.SeasonDao
import com.papa.fr.football.data.local.dao.UserPreferencesDao
import com.papa.fr.football.data.local.entity.LiveMatchEntity
import com.papa.fr.football.data.local.entity.MatchEntity
import com.papa.fr.football.data.local.entity.MatchRefreshEntity
import com.papa.fr.football.data.local.entity.MatchPrefetchTaskEntity
import com.papa.fr.football.data.local.entity.RequestRetryEntity
import com.papa.fr.football.data.local.entity.SeasonEntity
import com.papa.fr.football.data.local.entity.FavoriteTeamEntity
import com.papa.fr.football.data.local.entity.UserPreferenceEntity

@Database(
    entities = [
        SeasonEntity::class,
        MatchEntity::class,
        LiveMatchEntity::class,
        MatchRefreshEntity::class,
        RequestRetryEntity::class,
        MatchPrefetchTaskEntity::class,
        FavoriteTeamEntity::class,
        UserPreferenceEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
@TypeConverters(DatabaseConverters::class)
abstract class PapaFootballDatabase : RoomDatabase() {
    abstract fun seasonDao(): SeasonDao
    abstract fun matchDao(): MatchDao
    abstract fun liveMatchDao(): LiveMatchDao
    abstract fun requestRetryDao(): RequestRetryDao
    abstract fun matchPrefetchDao(): MatchPrefetchDao
    abstract fun userPreferencesDao(): UserPreferencesDao
}
