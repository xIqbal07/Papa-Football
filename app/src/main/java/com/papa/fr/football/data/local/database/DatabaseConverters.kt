package com.papa.fr.football.data.local.database

import androidx.room.TypeConverter
import com.papa.fr.football.data.local.entity.MatchTypeEntity

class DatabaseConverters {
    @TypeConverter
    fun toMatchType(value: String): MatchTypeEntity = MatchTypeEntity.valueOf(value)

    @TypeConverter
    fun fromMatchType(type: MatchTypeEntity): String = type.name
}
