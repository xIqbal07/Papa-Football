package com.papa.fr.football.data.remote

import com.papa.fr.football.data.local.dao.RequestRetryDao
import com.papa.fr.football.data.local.entity.RequestRetryEntity
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import io.ktor.network.sockets.SocketTimeoutException as KtorSocketTimeoutException
import java.net.SocketTimeoutException as JavaSocketTimeoutException

class RetryingCallExecutor(
    private val requestRetryDao: RequestRetryDao,
    private val defaultRetryAfterMillis: Long = 1_000,
    private val timeSource: () -> Long = { System.currentTimeMillis() },
) {
    suspend fun <T> execute(
        endpointKey: String,
        block: suspend () -> T,
    ): T {
        while (true) {
            awaitIfQueued(endpointKey)

            try {
                val result = block()
                requestRetryDao.delete(endpointKey)
                return result
            } catch (exception: Exception) {
                when (exception) {
                    is ResponseException -> {
                        if (exception.response.status == HttpStatusCode.NotFound) {
                            throw exception
                        }

                        if (exception.response.status == HttpStatusCode.TooManyRequests) {
                            val retryAfterMillis =
                                parseRetryAfter(exception) ?: defaultRetryAfterMillis
                            scheduleRetry(endpointKey, retryAfterMillis)
                            delay(retryAfterMillis)
                            continue
                        }

                        throw exception
                    }

                    is JavaSocketTimeoutException,
                    is KtorSocketTimeoutException -> {
                        scheduleRetry(endpointKey, defaultRetryAfterMillis)
                        delay(defaultRetryAfterMillis)
                        continue
                    }

                    else -> throw exception
                }
            }
        }
    }

    private suspend fun awaitIfQueued(endpointKey: String) {
        val pending = requestRetryDao.get(endpointKey) ?: return
        val waitMillis = pending.nextAttemptAt - timeSource()
        if (waitMillis <= 0) {
            requestRetryDao.delete(endpointKey)
            return
        }

        delay(waitMillis)
    }

    private suspend fun scheduleRetry(endpointKey: String, retryAfterMillis: Long) {
        val nextAttemptAt = timeSource() + retryAfterMillis
        requestRetryDao.upsert(
            RequestRetryEntity(
                endpoint = endpointKey,
                nextAttemptAt = nextAttemptAt,
            ),
        )
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
