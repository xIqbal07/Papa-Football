package com.papa.fr.football.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodedPath

class TeamApiService(private val httpClient: HttpClient) {
    suspend fun getTeamLogo(teamId: Int): TeamLogoRaw {
        val response = httpClient.get {
            url { encodedPath = "v1/teams/logo" }
            parameter("team_id", teamId)
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
