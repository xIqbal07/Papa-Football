package com.papa.fr.football.domain.usecase

import com.papa.fr.football.domain.model.Season
import com.papa.fr.football.domain.repository.SeasonRepository

class GetSeasonsUseCase(private val seasonRepository: SeasonRepository) {
    suspend operator fun invoke(uniqueTournamentId: Int): List<Season> {
        return seasonRepository.getUniqueTournamentSeasons(uniqueTournamentId)
    }
}
