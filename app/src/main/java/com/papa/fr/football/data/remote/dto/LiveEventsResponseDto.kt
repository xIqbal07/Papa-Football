package com.papa.fr.football.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LiveEventsResponseDto(
    @SerialName("data")
    val data: List<LiveEventDto> = emptyList(),
)

@Serializable
data class LiveEventDto(
    @SerialName("id")
    val id: Long? = null,
    @SerialName("homeTeam")
    val homeTeam: LiveEventTeamDto? = null,
    @SerialName("awayTeam")
    val awayTeam: LiveEventTeamDto? = null,
    @SerialName("homeScore")
    val homeScore: LiveEventScoreDto? = null,
    @SerialName("awayScore")
    val awayScore: LiveEventScoreDto? = null,
    @SerialName("statusDescription")
    val statusDescription: String? = null,
)

@Serializable
data class LiveEventTeamDto(
    @SerialName("id")
    val id: Int? = null,
    @SerialName("name")
    val name: String? = null,
)

@Serializable
data class LiveEventScoreDto(
    @SerialName("current")
    val current: Int? = null,
)
