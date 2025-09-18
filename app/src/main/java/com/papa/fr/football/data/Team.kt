package com.papa.fr.football.data

import androidx.annotation.DrawableRes

data class Team(
    val id: String,
    val name: String,
    @DrawableRes val emblemRes: Int?,
    val players: List<Player>
)
