package com.papa.fr.football.data.remote

import com.papa.fr.football.data.remote.dto.SeasonEventsResponseDto
import com.papa.fr.football.data.remote.dto.UniqueTournamentSeasonsResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.encodedPath

class SeasonApiService(
    private val httpClient: HttpClient,
    rateLimiter: ApiRateLimiter,
) : RateLimitedApiService(rateLimiter) {

    companion object {
        const val RATE_LIMIT_KEY_UNIQUE_TOURNAMENT_SEASONS = "uniqueTournamentSeasons"
        const val RATE_LIMIT_KEY_SEASON_EVENTS = "seasonEvents"
    }

    suspend fun getUniqueTournamentSeasons(uniqueTournamentId: Int): UniqueTournamentSeasonsResponseDto {
        return executeRateLimited(RATE_LIMIT_KEY_UNIQUE_TOURNAMENT_SEASONS) {
            executeWithRetry {
                httpClient.get {
                    url {
                        encodedPath = "v1/unique-tournaments/seasons"
                    }
                    parameter("unique_tournament_id", uniqueTournamentId)
                }.body()
            }
        }
    }

    suspend fun getSeasonEvents(
        uniqueTournamentId: Int,
        seasonId: Int,
        page: Int = 1,
        courseEvents: String = "next",
    ): SeasonEventsResponseDto {
        return executeRateLimited(RATE_LIMIT_KEY_SEASON_EVENTS) {
            executeWithRetry {
                httpClient.get {
                    url { encodedPath = "v1/seasons/events" }
                    parameter("unique_tournament_id", uniqueTournamentId)
                    parameter("seasons_id", seasonId)
                    parameter("page", page)
                    parameter("course_events", courseEvents)
                }.body()
            }
        }
    }
}
