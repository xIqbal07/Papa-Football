package com.papa.fr.football.common.dropdown

import androidx.annotation.DrawableRes

data class LeagueItem(
    val id: String,
    val name: String,
    @DrawableRes val iconRes: Int
) {
    override fun toString(): String = name // used by accessibility & default adapters
}
