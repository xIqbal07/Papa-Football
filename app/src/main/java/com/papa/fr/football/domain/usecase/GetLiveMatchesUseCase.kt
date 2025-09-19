package com.papa.fr.football.domain.usecase

import com.papa.fr.football.domain.model.LiveMatch
import com.papa.fr.football.domain.repository.MatchRepository

class GetLiveMatchesUseCase(private val matchRepository: MatchRepository) {
    suspend operator fun invoke(sportId: Int): List<LiveMatch> {
        return matchRepository.getLiveMatches(sportId)
    }
}
