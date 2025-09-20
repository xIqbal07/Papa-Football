package com.papa.fr.football.data.local.mapper

import com.papa.fr.football.data.local.entity.LiveMatchEntity
import com.papa.fr.football.data.local.entity.MatchEntity
import com.papa.fr.football.data.local.entity.MatchTypeEntity
import com.papa.fr.football.domain.model.LiveMatch
import com.papa.fr.football.domain.model.Match
import com.papa.fr.football.domain.model.MatchTeam

fun MatchEntity.toDomain(): Match {
    return Match(
        id = matchId,
        startTimestamp = startTimestamp,
        homeTeam = MatchTeam(
            id = homeTeamId,
            name = homeTeamName,
            logoBase64 = homeLogoBase64,
        ),
        awayTeam = MatchTeam(
            id = awayTeamId,
            name = awayTeamName,
            logoBase64 = awayLogoBase64,
        ),
        homeScore = homeScore,
        awayScore = awayScore,
    )
}

fun Match.toEntity(
    leagueId: Int,
    seasonId: Int,
    type: MatchTypeEntity,
): MatchEntity {
    return MatchEntity(
        matchId = id,
        leagueId = leagueId,
        seasonId = seasonId,
        type = type,
        startTimestamp = startTimestamp,
        homeTeamId = homeTeam.id,
        homeTeamName = homeTeam.name,
        homeLogoBase64 = homeTeam.logoBase64,
        awayTeamId = awayTeam.id,
        awayTeamName = awayTeam.name,
        awayLogoBase64 = awayTeam.logoBase64,
        homeScore = homeScore,
        awayScore = awayScore,
    )
}

fun LiveMatchEntity.toDomain(): LiveMatch {
    return LiveMatch(
        id = matchId,
        homeTeam = MatchTeam(
            id = homeTeamId,
            name = homeTeamName,
            logoBase64 = homeLogoBase64,
        ),
        awayTeam = MatchTeam(
            id = awayTeamId,
            name = awayTeamName,
            logoBase64 = awayLogoBase64,
        ),
        homeScore = homeScore,
        awayScore = awayScore,
        status = statusLabel,
    )
}

fun LiveMatch.toEntity(sportId: Int): LiveMatchEntity {
    return LiveMatchEntity(
        matchId = id,
        sportId = sportId,
        homeTeamId = homeTeam.id,
        homeTeamName = homeTeam.name,
        homeLogoBase64 = homeTeam.logoBase64,
        awayTeamId = awayTeam.id,
        awayTeamName = awayTeam.name,
        awayLogoBase64 = awayTeam.logoBase64,
        homeScore = homeScore,
        awayScore = awayScore,
        statusLabel = status,
    )
}
