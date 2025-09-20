package com.papa.fr.football.data.remote

import com.papa.fr.football.data.remote.dto.LiveEventsResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.encodedPath

class LiveEventsApiService(private val httpClient: HttpClient) {
    suspend fun getLiveSchedule(sportId: Int): LiveEventsResponseDto {
        return executeWithRetry {
            httpClient.get {
                url { encodedPath = "v1/events/schedule/live" }
                parameter("sport_id", sportId)
            }.body()
        }
    }
}
