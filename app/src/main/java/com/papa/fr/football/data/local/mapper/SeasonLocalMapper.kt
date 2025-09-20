package com.papa.fr.football.data.local.mapper

import com.papa.fr.football.data.local.entity.SeasonEntity
import com.papa.fr.football.domain.model.Season

fun SeasonEntity.toDomain(): Season {
    return Season(
        id = seasonId,
        name = name,
        year = year,
        editor = editor,
    )
}

fun Season.toEntity(leagueId: Int, orderIndex: Int): SeasonEntity {
    return SeasonEntity(
        leagueId = leagueId,
        seasonId = id,
        name = name,
        year = year,
        editor = editor,
        orderIndex = orderIndex,
    )
}
