package com.papa.fr.football.data.repository

import com.papa.fr.football.data.mapper.toDomain
import com.papa.fr.football.data.remote.SeasonApiService
import com.papa.fr.football.domain.model.Season
import com.papa.fr.football.domain.repository.SeasonRepository

class SeasonRepositoryImpl(
    private val apiService: SeasonApiService
) : SeasonRepository {
    override suspend fun getUniqueTournamentSeasons(uniqueTournamentId: Int): List<Season> {
        return apiService
            .getUniqueTournamentSeasons(uniqueTournamentId)
            .data
            .take(5)
            .map { it.toDomain() }
    }
}