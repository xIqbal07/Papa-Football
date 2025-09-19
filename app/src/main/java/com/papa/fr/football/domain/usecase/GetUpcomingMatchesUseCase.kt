package com.papa.fr.football.domain.usecase

import com.papa.fr.football.domain.model.Match
import com.papa.fr.football.domain.repository.MatchRepository

class GetUpcomingMatchesUseCase(private val matchRepository: MatchRepository) {
    suspend operator fun invoke(uniqueTournamentId: Int, seasonId: Int): List<Match> {
        return matchRepository.getUpcomingMatches(uniqueTournamentId, seasonId)
    }
}
