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
) {
    protected suspend fun <T> executeRateLimited(key: String, block: suspend () -> T): T {
        return apiRateLimiter.run(key, block)
    }
}

class ApiRateLimiter(
    rateLimits: Map<String, RateLimitRule>,
) {
    private val limiters = rateLimits.mapValues { RateLimiter(it.value.minIntervalMillis) }

    suspend fun <T> run(key: String, block: suspend () -> T): T {
        val limiter = limiters[key] ?: return block()
        return limiter.execute(block)
    }
}

data class RateLimitRule(val minIntervalMillis: Long)

private class RateLimiter(
    private val minIntervalMillis: Long,
) {
    private val mutex = Mutex()
    private var lastRequestTimestamp: Long = 0L

    suspend fun <T> execute(block: suspend () -> T): T {
        return mutex.withLock {
            val now = System.currentTimeMillis()
            val elapsed = now - lastRequestTimestamp
            val waitTime = (minIntervalMillis - elapsed).coerceAtLeast(0)
            if (waitTime > 0) {
                delay(waitTime)
            }
            lastRequestTimestamp = System.currentTimeMillis()
            block()
        }
    }
}
