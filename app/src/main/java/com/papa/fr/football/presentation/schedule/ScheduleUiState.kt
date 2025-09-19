package com.papa.fr.football.presentation.schedule

import com.papa.fr.football.domain.model.Season
import com.papa.fr.football.presentation.schedule.matches.MatchUiModel
import com.papa.fr.football.presentation.schedule.matches.MatchesTabType
import java.time.Instant

/**
 * UI representation of the available leagues and seasons backing the schedule screen.
 */
data class ScheduleUiState(
    val isLoading: Boolean = false,
    val seasonsByLeague: Map<Int, List<Season>> = emptyMap(),
    val errorMessage: String? = null,
    val selectedLeagueId: Int? = null,
    val selectedSeasonId: Int? = null,
    val selectedMatchesTab: MatchesTabType = MatchesTabType.Future,
    val isMatchesLoading: Boolean = false,
    val matchesErrorMessage: String? = null,
    val futureMatches: List<MatchUiModel.Future> = emptyList(),
    val matchesByLeagueSeason: Map<Int, Map<Int, List<MatchUiModel.Future>>> = emptyMap(),
    val matchErrorsByLeagueSeason: Map<Int, Map<Int, String?>> = emptyMap(),
    val pastMatches: List<MatchUiModel.Past> = emptyList(),
    val isPastMatchesLoading: Boolean = false,
    val pastMatchesErrorMessage: String? = null,
    val pastMatchesByLeagueSeason: Map<Int, Map<Int, List<MatchUiModel.Past>>> = emptyMap(),
    val pastMatchErrorsByLeagueSeason: Map<Int, Map<Int, String?>> = emptyMap(),
    val liveMatches: List<MatchUiModel.Live> = emptyList(),
    val isLiveMatchesLoading: Boolean = false,
    val liveMatchesErrorMessage: String? = null,
    val isDataLoaded: Boolean = false,
    val lastUpdatedAt: Instant? = null,
)
