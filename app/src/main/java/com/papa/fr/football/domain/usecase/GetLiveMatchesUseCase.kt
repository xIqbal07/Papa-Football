package com.papa.fr.football.domain.usecase

import com.papa.fr.football.domain.model.LiveMatch
import com.papa.fr.football.domain.repository.MatchRepository
import kotlinx.coroutines.flow.Flow

class GetLiveMatchesUseCase(private val matchRepository: MatchRepository) {
    operator fun invoke(sportId: Int): Flow<List<LiveMatch>> =
        matchRepository.getLiveMatches(sportId)
}
