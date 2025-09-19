package com.papa.fr.football.presentation.schedule

import com.papa.fr.football.domain.model.Season
import com.papa.fr.football.presentation.schedule.matches.MatchUiModel

/**
 * UI representation of the available leagues and seasons backing the schedule screen.
 */
data class ScheduleUiState(
    val isLoading: Boolean = false,
    val seasonsByLeague: Map<Int, List<Season>> = emptyMap(),
    val errorMessage: String? = null,
    val selectedLeagueId: Int? = null,
    val selectedSeasonId: Int? = null,
    val isMatchesLoading: Boolean = false,
    val matchesErrorMessage: String? = null,
    val futureMatches: List<MatchUiModel.Future> = emptyList(),
    val matchesByLeagueSeason: Map<Int, Map<Int, List<MatchUiModel.Future>>> = emptyMap(),
    val matchErrorsByLeagueSeason: Map<Int, Map<Int, String?>> = emptyMap(),
    val isDataLoaded: Boolean = false,
)
