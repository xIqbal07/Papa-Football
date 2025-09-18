package com.papa.fr.football.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SeasonDto(
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String,
    @SerialName("year")
    val year: String? = null,
    @SerialName("editor")
    val editor: Boolean? = null
)
