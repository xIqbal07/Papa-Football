package com.papa.fr.football.presentation.schedule.season

import com.papa.fr.football.domain.model.Season

/**
 * UI representation of the available seasons grouped by league along with the loading state
 * that drives the season dropdown inside the schedule screen.
 */
data class SeasonsUiState(
    val isLoading: Boolean = false,
    val seasonsByLeague: Map<Int, List<Season>> = emptyMap(),
    val errorMessage: String? = null
)
