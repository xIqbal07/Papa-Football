package com.papa.fr.football.data.mapper

import com.papa.fr.football.data.remote.dto.EventDto
import com.papa.fr.football.data.remote.dto.LiveEventDto
import com.papa.fr.football.domain.model.LiveMatch
import com.papa.fr.football.domain.model.Match
import com.papa.fr.football.domain.model.MatchTeam

fun EventDto.toDomain(homeLogoBase64: String?, awayLogoBase64: String?): Match {
    val safeStartTimestamp = startTimestamp ?: 0L
    return Match(
        id = id.toString(),
        startTimestamp = safeStartTimestamp,
        homeTeam = MatchTeam(
            id = homeTeam?.id ?: -1,
            name = homeTeam?.name.orEmpty(),
            logoBase64 = homeLogoBase64.orEmpty(),
        ),
        awayTeam = MatchTeam(
            id = awayTeam?.id ?: -1,
            name = awayTeam?.name.orEmpty(),
            logoBase64 = awayLogoBase64.orEmpty(),
        ),
        homeScore = homeScore?.current,
        awayScore = awayScore?.current,
    )
}

fun LiveEventDto.toLiveDomain(
    homeLogoBase64: String?,
    awayLogoBase64: String?,
): LiveMatch {
    val fallbackId = listOfNotNull(homeTeam?.id, awayTeam?.id)
        .takeIf { it.isNotEmpty() }
        ?.joinToString(prefix = "live-", separator = "-")
        ?: "live-unknown"

    return LiveMatch(
        id = (id ?: 0L).takeIf { it != 0L }?.toString() ?: fallbackId,
        homeTeam = MatchTeam(
            id = homeTeam?.id ?: -1,
            name = homeTeam?.name.orEmpty(),
            logoBase64 = homeLogoBase64.orEmpty(),
        ),
        awayTeam = MatchTeam(
            id = awayTeam?.id ?: -1,
            name = awayTeam?.name.orEmpty(),
            logoBase64 = awayLogoBase64.orEmpty(),
        ),
        homeScore = homeScore?.current ?: 0,
        awayScore = awayScore?.current ?: 0,
        status = statusDescription?.ifBlank { DEFAULT_LIVE_STATUS } ?: DEFAULT_LIVE_STATUS,
    )
}

private const val DEFAULT_LIVE_STATUS = "Live"
