package com.papa.fr.football.domain.usecase

import com.papa.fr.football.domain.model.Match
import com.papa.fr.football.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow

class GetRecentMatchesUseCase(private val matchRepository: MatchRepository) {
    operator fun invoke(
        uniqueTournamentId: Int,
        seasonId: Int,
        forceRefresh: Boolean = false,
    ): Flow<List<Match>> =
        matchRepository.getRecentMatches(uniqueTournamentId, seasonId, forceRefresh)
}
