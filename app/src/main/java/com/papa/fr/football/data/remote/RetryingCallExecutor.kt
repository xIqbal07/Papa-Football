package com.papa.fr.football.data.remote

import com.papa.fr.football.data.local.dao.RateLimitedRequestDao
import com.papa.fr.football.data.local.entity.RateLimitedRequestEntity
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay

class RetryingCallExecutor(
    private val rateLimitedRequestDao: RateLimitedRequestDao,
    private val defaultRetryAfterMillis: Long = 1_000,
    private val timeSource: () -> Long = { System.currentTimeMillis() },
) {
    suspend fun <T> execute(
        endpointKey: String,
        block: suspend () -> T,
    ): T {
        while (true) {
            awaitIfRateLimited(endpointKey)

            try {
                val result = block()
                rateLimitedRequestDao.delete(endpointKey)
                return result
            } catch (exception: ResponseException) {
                if (exception.response.status == HttpStatusCode.NotFound) {
                    throw exception
                }

                if (exception.response.status == HttpStatusCode.TooManyRequests) {
                    val retryAfterMillis = parseRetryAfter(exception) ?: defaultRetryAfterMillis
                    val nextAttemptAt = timeSource() + retryAfterMillis
                    rateLimitedRequestDao.upsert(
                        RateLimitedRequestEntity(
                            endpoint = endpointKey,
                            nextAttemptAt = nextAttemptAt,
                        ),
                    )
                    delay(retryAfterMillis)
                    continue
                }

                throw exception
            }
        }
    }

    private suspend fun awaitIfRateLimited(endpointKey: String) {
        val pending = rateLimitedRequestDao.get(endpointKey) ?: return
        val waitMillis = pending.nextAttemptAt - timeSource()
        if (waitMillis <= 0) {
            rateLimitedRequestDao.delete(endpointKey)
            return
        }

        delay(waitMillis)
    }

    private fun parseRetryAfter(exception: ResponseException): Long? {
        val headerValue = exception.response.headers[RETRY_AFTER_HEADER] ?: return null
        headerValue.toLongOrNull()?.let { seconds ->
            return seconds * 1_000
        }

        return null
    }

    private companion object {
        const val RETRY_AFTER_HEADER = "Retry-After"
    }
}
