package com.papa.fr.football.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodedPath

class TeamApiService(
    private val httpClient: HttpClient,
    private val retryingCallExecutor: RetryingCallExecutor,
) {
    companion object {
        private const val RATE_LIMIT_KEY_TEAM_LOGO = "teamLogo"
    }

    suspend fun getTeamLogo(teamId: Int): TeamLogoRaw {
        val response = retryingCallExecutor.execute(RATE_LIMIT_KEY_TEAM_LOGO) {
            httpClient.get {
                url { encodedPath = "v1/teams/logo" }
                parameter("team_id", teamId)
            }
        }

        return TeamLogoRaw(
            contentType = response.contentType(),
            body = response.body(),
        )
    }
}

data class TeamLogoRaw(
    val contentType: ContentType?,
    val body: ByteArray,
)
