package com.papa.fr.football.data.repository

import com.papa.fr.football.data.local.dao.SeasonDao
import com.papa.fr.football.data.local.mapper.toDomain
import com.papa.fr.football.data.local.mapper.toEntity
import com.papa.fr.football.data.mapper.toDomain as toDomainDto
import com.papa.fr.football.data.remote.SeasonApiService
import com.papa.fr.football.domain.model.Season
import com.papa.fr.football.domain.repository.SeasonRepository

class SeasonRepositoryImpl(
    private val apiService: SeasonApiService,
    private val seasonDao: SeasonDao,
) : SeasonRepository {
    override suspend fun getUniqueTournamentSeasons(uniqueTournamentId: Int): List<Season> {
        val cached = seasonDao.getSeasons(uniqueTournamentId).map { it.toDomain() }
        val remoteResult = runCatching {
            apiService
                .getUniqueTournamentSeasons(uniqueTournamentId)
                .data
        }

        val remoteSeasons = remoteResult.getOrNull()?.map { it.toDomainDto() }
        if (remoteSeasons != null) {
            val entities = remoteSeasons.mapIndexed { index, season ->
                season.toEntity(uniqueTournamentId, index)
            }
            seasonDao.replaceSeasons(uniqueTournamentId, entities)
            return remoteSeasons
        }

        if (cached.isNotEmpty()) {
            return cached
        }

        throw remoteResult.exceptionOrNull() ?: IllegalStateException("Unable to load seasons")
    }
}
