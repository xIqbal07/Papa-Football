package com.papa.fr.football.presentation.seasons

import com.papa.fr.football.domain.model.Season

data class SeasonsUiState(
    val isLoading: Boolean = false,
    val seasonsByLeague: Map<Int, List<Season>> = emptyMap(),
    val errorMessage: String? = null
)
