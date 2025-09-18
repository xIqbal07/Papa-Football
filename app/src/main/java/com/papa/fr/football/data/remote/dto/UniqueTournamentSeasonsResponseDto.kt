package com.papa.fr.football.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UniqueTournamentSeasonsResponseDto(
    @SerialName("data")
    val data: List<SeasonDto> = emptyList()
)