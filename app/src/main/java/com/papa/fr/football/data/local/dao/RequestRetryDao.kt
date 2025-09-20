package com.papa.fr.football.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.papa.fr.football.data.local.entity.RequestRetryEntity

@Dao
interface RequestRetryDao {
    @Query("SELECT * FROM request_retry WHERE endpoint = :endpoint")
    suspend fun get(endpoint: String): RequestRetryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RequestRetryEntity)

    @Query("DELETE FROM request_retry WHERE endpoint = :endpoint")
    suspend fun delete(endpoint: String)
}
