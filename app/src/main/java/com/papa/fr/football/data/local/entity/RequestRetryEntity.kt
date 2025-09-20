package com.papa.fr.football.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * Persists endpoints that require a deferred retry (i.e. due to rate limiting or socket
 * timeouts) so subsequent launches can respect the back-off before attempting the same call
 * again.
 */
@Entity(tableName = "request_retry", primaryKeys = ["endpoint"])
data class RequestRetryEntity(
    @ColumnInfo(name = "endpoint")
    val endpoint: String,
    @ColumnInfo(name = "next_attempt_at")
    val nextAttemptAt: Long,
)
