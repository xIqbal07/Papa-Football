package com.papa.fr.football.common.league

import androidx.annotation.DrawableRes
import com.papa.fr.football.R

data class LeagueDescriptor(
    val id: Int,
    val name: String,
    @DrawableRes val iconRes: Int? = null,
)

interface LeagueCatalog {
    val leagues: List<LeagueDescriptor>
}

class StaticLeagueCatalog : LeagueCatalog {
    override val leagues: List<LeagueDescriptor> = listOf(
        LeagueDescriptor(id = 8, name = "La Liga", iconRes = R.drawable.ic_laliga),
        LeagueDescriptor(id = 17, name = "Premier League", iconRes = R.drawable.ic_premier_league),
        LeagueDescriptor(id = 35, name = "Bundesliga", iconRes = R.drawable.ic_bundesliga),
        LeagueDescriptor(id = 34, name = "La Liga", iconRes = R.drawable.ic_ligue),
        LeagueDescriptor(id = 23, name = "Serie A", iconRes = R.drawable.ic_serie_a),
        LeagueDescriptor(id = 37, name = "Eredivise", iconRes = R.drawable.eredivisie),
    )
}
