package com.papa.fr.football.data.remote

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Base abstraction that keeps rate limiting rules centralized so each API service can
 * declare its throttle configuration in one place and share the same enforcement logic.
 */
abstract class RateLimitedApiService(
    private val apiRateLimiter: ApiRateLimiter,
    private val retryingCallExecutor: RetryingCallExecutor,
) {
    protected suspend fun <T> executeRateLimited(key: String, block: suspend () -> T): T {
        return apiRateLimiter.run(key, block)
    }

    protected suspend fun <T> executeRateLimitedWithRetry(
        limitKey: String,
        retryKey: String,
        block: suspend () -> T,
    ): T {
        return executeRateLimited(limitKey) {
            retryingCallExecutor.execute(retryKey, block)
        }
    }

    protected suspend fun <T> executeWithRetry(key: String, block: suspend () -> T): T {
        return retryingCallExecutor.execute(key, block)
    }
}

class ApiRateLimiter(
    rateLimits: Map<String, RateLimitRule>,
) {
    private val limiters = rateLimits.mapValues { (_, rule) ->
        RateLimiter(
            minIntervalMillis = rule.minIntervalMillis,
            queueThreshold = rule.queueThreshold,
        )
    }

    suspend fun <T> run(key: String, block: suspend () -> T): T {
        val limiter = limiters[key] ?: return block()
        return limiter.execute(block)
    }
}

data class RateLimitRule(
    val minIntervalMillis: Long,
    val queueThreshold: Int = 10,
)

/**
 * Limits access to an endpoint by deferring callers only when there is already an active request
 * in-flight. This keeps back-to-back calls fast while protecting the service when bursts happen.
 */
private class RateLimiter(
    private val minIntervalMillis: Long,
    private val queueThreshold: Int,
) {
    private val mutex = Mutex()
    private var lastCompletedTimestamp: Long = 0L
    private var inFlightCount: Int = 0

    suspend fun <T> execute(block: suspend () -> T): T {
        var waitTime = 0L

        mutex.withLock {
            if (inFlightCount >= queueThreshold) {
                val now = System.currentTimeMillis()
                val elapsed = now - lastCompletedTimestamp
                waitTime = (minIntervalMillis - elapsed).coerceAtLeast(0)
            }
            inFlightCount += 1
        }

        if (waitTime > 0) {
            delay(waitTime)
        }

        return try {
            block()
        } finally {
            val completedAt = System.currentTimeMillis()
            mutex.withLock {
                inFlightCount -= 1
                lastCompletedTimestamp = completedAt
            }
        }
    }
}
