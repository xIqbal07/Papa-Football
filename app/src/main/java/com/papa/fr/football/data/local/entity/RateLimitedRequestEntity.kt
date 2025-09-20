package com.papa.fr.football.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * Persists endpoints that were throttled by the remote API so subsequent launches respect the
 * server-imposed wait before attempting the same call again.
 */
@Entity(tableName = "rate_limited_request", primaryKeys = ["endpoint"])
data class RateLimitedRequestEntity(
    @ColumnInfo(name = "endpoint")
    val endpoint: String,
    @ColumnInfo(name = "next_attempt_at")
    val nextAttemptAt: Long,
)
