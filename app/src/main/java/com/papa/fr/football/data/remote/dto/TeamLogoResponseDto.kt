package com.papa.fr.football.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TeamLogoResponseDto(
    @SerialName("data")
    val data: String? = null,
)
