package com.papa.fr.football.data.remote

import com.papa.fr.football.data.remote.dto.TeamLogoResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.encodedPath

class TeamApiService(private val httpClient: HttpClient) {
    suspend fun getTeamLogo(teamId: Int): TeamLogoResponseDto {
        return httpClient.get {
            url { encodedPath = "v1/teams/logo" }
            parameter("team_id", teamId)
        }.body()
    }
}
