package com.papa.fr.football.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.papa.fr.football.data.local.entity.RateLimitedRequestEntity

@Dao
interface RateLimitedRequestDao {
    @Query("SELECT * FROM rate_limited_request WHERE endpoint = :endpoint")
    suspend fun get(endpoint: String): RateLimitedRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RateLimitedRequestEntity)

    @Query("DELETE FROM rate_limited_request WHERE endpoint = :endpoint")
    suspend fun delete(endpoint: String)
}
