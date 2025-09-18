package com.papa.fr.football.data.mapper

import com.papa.fr.football.data.remote.dto.SeasonDto
import com.papa.fr.football.domain.model.Season

fun SeasonDto.toDomain(): Season {
    return Season(
        id = id,
        name = name,
        year = year,
        editor = editor ?: false
    )
}
