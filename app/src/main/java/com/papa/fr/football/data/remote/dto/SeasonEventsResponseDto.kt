package com.papa.fr.football.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SeasonEventsResponseDto(
    @SerialName("data")
    val data: SeasonEventsDataDto = SeasonEventsDataDto(),
)

@Serializable
data class SeasonEventsDataDto(
    @SerialName("events")
    val events: List<EventDto> = emptyList(),
)

@Serializable
data class EventDto(
    @SerialName("id")
    val id: Long,
    @SerialName("homeTeam")
    val homeTeam: EventTeamDto? = null,
    @SerialName("awayTeam")
    val awayTeam: EventTeamDto? = null,
    @SerialName("startTimestamp")
    val startTimestamp: Long? = null,
    @SerialName("homeScore")
    val homeScore: EventScoreDto? = null,
    @SerialName("awayScore")
    val awayScore: EventScoreDto? = null,
)

@Serializable
data class EventTeamDto(
    @SerialName("id")
    val id: Int? = null,
    @SerialName("name")
    val name: String? = null,
)

@Serializable
data class EventScoreDto(
    @SerialName("current")
    val current: Int? = null,
)
