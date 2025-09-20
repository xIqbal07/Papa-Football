package com.papa.fr.football.common.dropdown

import com.papa.fr.football.common.league.LeagueDescriptor

fun LeagueDescriptor.toLeagueItem(): LeagueItem =
    LeagueItem(id = id, name = name, iconRes = iconRes)
