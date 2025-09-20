package com.papa.fr.football.data.remote

import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay

suspend fun <T> executeWithRetry(
    maxAttempts: Int = 3,
    initialDelayMillis: Long = 500,
    block: suspend () -> T,
): T {
    require(maxAttempts > 0) { "maxAttempts must be greater than 0" }

    var currentDelay = initialDelayMillis
    var attempt = 1
    var lastError: Throwable? = null

    while (attempt <= maxAttempts) {
        try {
            return block()
        } catch (exception: ResponseException) {
            if (exception.response.status == HttpStatusCode.NotFound) {
                throw exception
            }
            lastError = exception
        }

        if (attempt == maxAttempts) {
            break
        }

        if (currentDelay > 0) {
            delay(currentDelay)
            currentDelay *= 2
        }

        attempt++
    }

    throw lastError ?: IllegalStateException("Retry attempts failed")
}
