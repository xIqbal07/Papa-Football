package com.papa.fr.football.data.remote

import com.papa.fr.football.data.remote.dto.LiveEventsResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.encodedPath

class LiveEventsApiService(
    private val httpClient: HttpClient,
    private val retryingCallExecutor: RetryingCallExecutor,
) {
    companion object {
        private const val RATE_LIMIT_KEY_LIVE_SCHEDULE = "liveSchedule"
    }

    suspend fun getLiveSchedule(sportId: Int): LiveEventsResponseDto {
        return retryingCallExecutor.execute(RATE_LIMIT_KEY_LIVE_SCHEDULE) {
            httpClient.get {
                url { encodedPath = "v1/events/schedule/live" }
                parameter("sport_id", sportId)
            }.body()
        }
    }
}
