package com.papa.fr.football.domain.repository

import com.papa.fr.football.domain.model.Season

interface SeasonRepository {
    suspend fun getUniqueTournamentSeasons(
        uniqueTournamentId: Int,
        forceRefresh: Boolean = false,
    ): List<Season>
}
