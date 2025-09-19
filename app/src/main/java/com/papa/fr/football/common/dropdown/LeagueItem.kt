package com.papa.fr.football.common.dropdown

import androidx.annotation.DrawableRes

data class LeagueItem(
    val id: Int,
    val name: String,
    @DrawableRes val iconRes: Int? = null
) {
    override fun toString(): String = name // used by accessibility & default adapters
}
