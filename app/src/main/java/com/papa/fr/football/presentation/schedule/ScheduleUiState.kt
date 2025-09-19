package com.papa.fr.football.presentation.schedule

import com.papa.fr.football.domain.model.Season

/**
 * UI representation of the available leagues and seasons backing the schedule screen.
 */
data class ScheduleUiState(
    val isLoading: Boolean = false,
    val seasonsByLeague: Map<Int, List<Season>> = emptyMap(),
    val errorMessage: String? = null
)
