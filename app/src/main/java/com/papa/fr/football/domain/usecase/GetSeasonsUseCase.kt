package com.papa.fr.football.domain.usecase

import com.papa.fr.football.domain.model.Season
import com.papa.fr.football.domain.repository.SeasonRepository

class GetSeasonsUseCase(private val seasonRepository: SeasonRepository) {
    suspend operator fun invoke(
        uniqueTournamentId: Int,
        forceRefresh: Boolean = false,
    ): List<Season> {
        return seasonRepository.getUniqueTournamentSeasons(uniqueTournamentId, forceRefresh)
    }
}
